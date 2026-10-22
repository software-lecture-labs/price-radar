package com.priceradar.ingestion;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.LogManager;

/** Validated import command; invoked through import.ps1, never through Spring Boot. */
public final class ReefImportCommand {
    private static final List<String> SECRETS = new ArrayList<>();

    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        String confirmedDataStatus = "UNCONFIRMED";
        try {
            if (args.length != 4 || !Set.of("preview", "dry-run", "commit",
                    "batch-preview", "batch-dry-run", "batch-commit").contains(args[2])) {
                throw new IllegalArgumentException("Expected repository, source file, mode and expected SHA-256");
            }
            var root = Path.of(args[0]).toRealPath();
            if (args[2].startsWith("batch-")) {
                runBatch(root, Path.of(args[1]), args[2].substring(6), args[3]);
                return;
            }
            var source = ReefSearchImporter.readSource(root, Path.of(args[1]));
            var expectedSha = args[3].equals("-") ? "" : args[3];
            if (!expectedSha.isEmpty() && !source.sha256().equals(expectedSha)) {
                throw new IllegalStateException("Source hash changed; import stopped before connecting");
            }
            if (args[2].equals("commit") && !expectedSha.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Commit requires the previously reviewed source SHA-256");
            }
            System.out.println("SOURCE=" + source.path());
            System.out.println("STORE=" + source.store());
            System.out.println("SHA256=" + source.sha256());
            System.out.println("VALIDATED_SOURCE_ROWS=" + source.rows().size());
            if (args[2].equals("preview")) {
                System.out.println("STATUS=PREVIEW; database not contacted");
                return;
            }

            try (var connection = openConnection(root)) {
                var result = ReefSearchImporter.importSource(connection, source, args[2].equals("commit"));
                confirmedDataStatus = result.status();
                System.out.println("STATUS=" + result.status());
                System.out.println("SOURCE_FILE_ID=" + result.sourceFileId());
                System.out.println("VERIFIED_SOURCE_ROWS=" + result.rows());
                System.out.println((result.status().equals("DRY_RUN") ? "PRODUCTS_PLANNED=" : "PRODUCTS_INSERTED=")
                        + result.productsInserted());
                System.out.println("PRICE_SUM=" + result.priceSum().toPlainString());
                printTableCounts(connection);
            }
        }
        catch (Exception error) {
            System.err.println("IMPORT_COMMAND_OK=false");
            System.err.println("DATA_STATUS=" + confirmedDataStatus);
            reportError(error);
            System.exit(1);
        }
    }

    private static void runBatch(Path root, Path manifest, String mode, String expectedSha) throws Exception {
        byte[] bytes = Files.readAllBytes(manifest);
        var manifestSha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!expectedSha.equals("-") && !manifestSha.equals(expectedSha)) {
            throw new IllegalStateException("Manifest hash changed; import stopped before connecting");
        }
        if (mode.equals("commit") && !expectedSha.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Commit requires the previously reviewed manifest SHA-256");
        }
        var sources = new ArrayList<ReefSearchImporter.Source>();
        var paths = new HashSet<String>();
        int validationErrors = 0;
        int lineNumber = 0;
        for (var line : new String(bytes, StandardCharsets.UTF_8).lines().toList()) {
            lineNumber++;
            try {
                var fields = line.split("\t", -1);
                if (fields.length != 2 || !fields[0].matches("[0-9a-f]{64}") || fields[1].isBlank()) {
                    throw new IllegalArgumentException("Expected SHA-256<TAB>source-path");
                }
                var source = ReefSearchImporter.readSource(root, Path.of(fields[1]));
                if (!source.sha256().equals(fields[0])) throw new IllegalStateException("Source hash mismatch: " + fields[1]);
                if (!paths.add(source.path())) throw new IllegalArgumentException("Duplicate source path: " + source.path());
                sources.add(source);
            }
            catch (Exception error) {
                validationErrors++;
                System.err.println("PREFLIGHT_FAILED_LINE=" + lineNumber);
                reportError(error);
            }
        }
        if (sources.isEmpty() || validationErrors != 0) {
            throw new IllegalArgumentException("Manifest preflight failed; database not contacted; invalid lines=" + validationErrors);
        }
        System.out.println("MANIFEST_SHA256=" + manifestSha);
        System.out.println("VALIDATED_FILES=" + sources.size());
        System.out.println("VALIDATED_SOURCE_ROWS=" + sources.stream().mapToLong(s -> s.rows().size()).sum());
        if (mode.equals("preview")) {
            System.out.println("STATUS=PREVIEW; database not contacted");
            return;
        }
        int imported = 0, already = 0, planned = 0, failed = 0;
        Connection connection = null;
        try {
            for (int index = 0; index < sources.size(); index++) {
                var source = sources.get(index);
                ReefSearchImporter.ImportResult result = null;
                for (int attempt = 0; attempt < 2; attempt++) {
                    try {
                        if (connection == null || connection.isClosed()) connection = openConnection(root);
                        result = ReefSearchImporter.importSource(connection, source, mode.equals("commit"));
                        break;
                    }
                    catch (Exception error) {
                        boolean retry = attempt == 0 && retryable(error);
                        closeConnection(connection);
                        connection = null;
                        System.err.println((retry ? "RETRY_SOURCE=" : "FAILED_SOURCE=") + source.path());
                        reportError(error);
                        if (!retry) break;
                    }
                }
                if (result == null) failed++;
                else {
                    switch (result.status()) {
                        case "IMPORTED" -> imported++;
                        case "ALREADY_IMPORTED" -> already++;
                        case "DRY_RUN" -> planned++;
                        default -> throw new IllegalStateException("Unexpected import status " + result.status());
                    }
                }
                System.out.println("PROGRESS=" + (index + 1) + "/" + sources.size() + " | SOURCE=" + source.path()
                        + " | STATUS=" + (result == null ? "FAILED" : result.status())
                        + " | ROWS=" + source.rows().size()
                        + (result == null ? "" : (result.status().equals("DRY_RUN") ? " | PRODUCTS_PLANNED=" : " | PRODUCTS_INSERTED=") + result.productsInserted()
                                + " | PRICE_SUM=" + result.priceSum().toPlainString()));
                System.out.flush();
            }
            System.out.println("BATCH_PROCESSED=" + sources.size() + " | IMPORTED=" + imported
                    + " | ALREADY_IMPORTED=" + already + " | DRY_RUN=" + planned + " | FAILED=" + failed);
            if (connection == null || connection.isClosed()) connection = openConnection(root);
            printTableCounts(connection);
        }
        finally { closeConnection(connection); }
        if (failed != 0) throw new IllegalStateException("Batch completed with failed sources=" + failed);
        System.out.println("IMPORT_COMMAND_OK=true");
    }

    private static boolean retryable(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && ((sql.getSQLState() != null && sql.getSQLState().startsWith("08"))
                    || String.valueOf(sql.getMessage()).startsWith("Commit acknowledgement failed")
                    || String.valueOf(sql.getMessage()).startsWith("Import committed;"))) return true;
        }
        return false;
    }

    private static void closeConnection(Connection connection) {
        if (connection == null) return;
        try { connection.close(); }
        catch (SQLException error) { reportError(error); }
    }

    private static void printTableCounts(Connection connection) throws SQLException {
        connection.setReadOnly(true);
        connection.setAutoCommit(false);
        try {
            for (var table : List.of("stores", "source_files", "marketplace_products", "product_observations")) {
                try (var statement = connection.createStatement();
                     var rows = statement.executeQuery("SELECT count(*) FROM public." + table)) {
                    rows.next();
                    System.out.println("TABLE=" + table + " | ROW_COUNT=" + rows.getLong(1));
                }
            }
        }
        finally { connection.rollback(); }
    }

    private static Connection openConnection(Path root) throws Exception {
            var env = readEnv(root.resolve(".env"));
            var raw = env.getOrDefault("POSTGRES_URL", env.get("POSTGRES_DB"));
            if (raw == null || !raw.matches("^(jdbc:)?postgres(ql)?://.*")) {
                throw new IllegalArgumentException("Expected a PostgreSQL URL in POSTGRES_URL or POSTGRES_DB");
            }
            SECRETS.add(raw);
            var uri = URI.create(raw.startsWith("jdbc:") ? raw.substring(5) : raw);
            if (uri.getHost() == null || uri.getRawPath() == null || uri.getRawPath().length() <= 1) {
                throw new IllegalArgumentException("PostgreSQL URL requires a host and database");
            }
            SECRETS.add(uri.getHost());
            var properties = new Properties();
            if (uri.getRawUserInfo() != null) {
                var credentials = uri.getRawUserInfo().split(":", 2);
                properties.setProperty("user", decodeCredential(credentials[0]));
                if (credentials.length == 2) properties.setProperty("password", decodeCredential(credentials[1]));
            }
            if (uri.getRawQuery() != null) {
                for (var part : uri.getRawQuery().split("&")) {
                    var pair = part.split("=", 2);
                    var key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                    if (key.equals("channel_binding")) key = "channelBinding";
                    properties.setProperty(key, pair.length == 2
                            ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
                }
            }
            for (var key : List.of("user", "password")) {
                var fallback = env.get(key.equals("user") ? "POSTGRES_USER" : "POSTGRES_PASSWORD");
                if (!properties.containsKey(key) && fallback != null) properties.setProperty(key, fallback);
                if (properties.getProperty(key) != null) SECRETS.add(properties.getProperty(key));
            }
            properties.setProperty("connectTimeout", "10");
            properties.setProperty("socketTimeout", "30");
            properties.setProperty("ApplicationName", "price-radar-single-file-import");
            DriverManager.setLoginTimeout(15);
            var jdbc = "jdbc:postgresql://" + uri.getHost() + ":"
                    + (uri.getPort() < 0 ? 5432 : uri.getPort()) + uri.getRawPath();
            return DriverManager.getConnection(jdbc, properties);
    }

    private static void reportError(Throwable error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                var message = String.valueOf(cause.getMessage());
                for (var secret : SECRETS.stream().filter(s -> !s.isEmpty())
                        .sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
                    message = message.replace(secret, "[REDACTED]");
                }
                System.err.println("ERROR=" + cause.getClass().getSimpleName()
                        + (cause instanceof SQLException sql ? " SQLSTATE=" + sql.getSQLState() : "")
                        + " " + message.replaceAll("[\\r\\n]+", " "));
            }
    }

    private static String decodeCredential(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static Map<String, String> readEnv(Path file) throws Exception {
        var env = new HashMap<String, String>();
        for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            var text = line.replace("\uFEFF", "").trim();
            if (text.isEmpty() || text.startsWith("#")) continue;
            if (text.startsWith("export ")) text = text.substring(7).trim();
            int separator = text.indexOf('=');
            if (separator < 1) continue;
            var value = text.substring(separator + 1).trim();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            env.put(text.substring(0, separator).trim(), value);
        }
        return env;
    }
}
