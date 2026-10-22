# Marketplace data schema

`V2__marketplace_catalog.sql` creates four empty tables. It does not seed stores
or import files. `V1__baseline.sql` remains unchanged; Flyway records both versions.

| Table | Purpose | Identity |
| --- | --- | --- |
| `stores` | Marketplace identity and base URL | Unique `code` |
| `source_files` | File provenance, SHA-256, collection time and complete saved JSON envelope | Unique `source_path`; unique `sha256` |
| `marketplace_products` | A product as identified by one marketplace | Unique `(store_id, external_id)` |
| `product_observations` | A single result row, including its seller, price and original URL | Unique `(source_file_id, row_index)` |

`ledger.jsonl` is an API request log and is not a product source.

## Source mapping

Where each marketplace keeps a value in its search results is defined in one
place: [`Marketplace.java`](../backend/src/main/java/com/priceradar/ingestion/Marketplace.java).
Each marketplace lists only what differs from the common location (for example
Trendyol's numeric price is `price_value`, N11's results are under `products`);
a value it does not provide is stored as NULL. Supporting a new marketplace means
adding one constant there.

URLs belong to observations because they can identify a seller-specific offer.
A product keeps the catalogue values of the first file it was imported from; the
original values of every file remain in `raw_item` and `raw_document`.

`source_files.fetched_at` and `product_observations.observed_at` use the saved
envelope's `fetchedAt`, never the import time. `row_index` is the zero-based
array index. The SHA-256 is taken over the file bytes as saved; JSONB preserves
JSON values, not whitespace or key order, so keep the source files on disk.

## Import rules

- Process one file in one transaction: its provenance, products and observations
  either commit together or roll back together.
- Re-importing the same path and hash is a no-op. If an existing path has
  different bytes, stop and review it; do not overwrite its history. An identical
  hash at another path is also a duplicate and must not create observations.
- Missing stock, seller and currency remain NULL. Never turn unknown stock into
  `false`, use a search term as a product name, or parse the formatted Trendyol
  `price` string when `price_value` exists.
- Store prices as decimal amounts. The schema rejects missing/negative/nonfinite
  prices, malformed hashes, invalid currency codes and negative result indexes.
- A repeated product may have multiple sellers/prices in the same response.
  N11's `philips-oneblade-p1.json` contains product `39734945` at indexes 1 and 20
  with different sellers and prices. Do not deduplicate observations by product.
- Composite foreign keys prevent an observation from referencing a product or
  source file from another store. Deletion does not cascade into observations.
- No cross-store product matching is assumed. Search results include accessories
  and unrelated products; matching needs a separate, reviewed step.

Promotions, installments, badges and other store-specific fields remain in
`raw_item`. No seller table is created yet: some sources give only a name and
Hepsiburada currently supplies no seller identity.

## Importing

The importer runs inside the backend, with the `import` profile, and uses the
same database connection as the application (`POSTGRES_URL`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`). `tools/reefapi/import.ps1` reads them from the root `.env`
and never prints them. Run it from the repository root:

```powershell
.	oolseefapiimport.ps1                               # dry run over data/reefapi/raw
.	oolseefapiimport.ps1 -Source 2026-10-22            # dry run over one fetch
.	oolseefapiimport.ps1 -Source 2026-10-22 -Commit    # write
```

`-Source` is a file or folder relative to `data/reefapi/raw` and can be given
more than once. Only files in `search/` folders are picked up.

- Every file is read and validated before the first one is written. A single
  invalid file stops the run with nothing written.
- Each file is one transaction. A run that stops halfway can be repeated: files
  already imported report `ALREADY_IMPORTED` and are skipped.
- Without `-Commit` the run is read-only and reports, per file, whether it
  would be imported and how many products would be new.

The parts:

| Class | Responsibility |
| --- | --- |
| `Marketplace` | Identity of each marketplace and where its values are |
| `SearchFileReader` | Reads a saved file and rejects anything the database would refuse or change |
| `SearchFileImporter` | One transaction per file; dry run or write |
| `MarketplaceImportRepository` | The SQL |
| `SearchImportRunner` | Command-line entry point for the `import` profile |

## Verification

```powershell
cd backend
.mvnw.cmd "-Dtest=SearchFileReaderTest,SearchFileImporterTest,MarketplaceSchemaTest" test
```

The tests start an isolated PostgreSQL 18.6 container, apply the real Flyway
migrations and do not read `.env`. They cover each marketplace's mapping, exact
decimals, NULLs for unknown values, rejected inputs, repeated imports, a changed
file at an imported path, rollback of a failed file and repeated products with
several sellers.
