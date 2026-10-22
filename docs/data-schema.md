# Marketplace data schema

`V2__marketplace_catalog.sql` creates four empty tables. It does not seed stores
or import files. `V1__baseline.sql` remains unchanged; Flyway records both versions.

| Table | Purpose | Identity |
| --- | --- | --- |
| `stores` | Marketplace identity and base URL | Unique `code` |
| `source_files` | File provenance, SHA-256, collection time and complete saved JSON envelope | Unique `source_path`; unique `sha256` |
| `marketplace_products` | A product as identified by one marketplace | Unique `(store_id, external_id)` |
| `product_observations` | A single result row, including its seller, price and original URL | Unique `(source_file_id, row_index)` |

The initial sample contained 199 search-response files and 4,671 result rows
(4,536 distinct marketplace product identities). Files were still being added
during inspection; these counts describe that sample, not the final import.
`ledger.jsonl` is an API request log and is not a product source.

## Source mapping

| Target | Hepsiburada | N11 | Teknosa | Trendyol |
| --- | --- | --- | --- | --- |
| Result array | `response.data.results` | `response.data.products` | `response.data.results` | `response.data.results` |
| `external_id` | `sku` | `product_id` as text | `product_id` as text | `content_id` as text |
| `external_group_id` | NULL | NULL | NULL | `product_group_id` as text |
| `price` | Numeric `price` | Numeric `price` | Numeric `price` | Numeric `price_value` |
| `list_price` | `original_price` | NULL | `was_price` | `list_price_value` |
| `member_price` | NULL | NULL | `member_price` | NULL |
| `currency` | `currency` | `currency` | `currency` | NULL: absent in the inspected source |
| `seller_external_id` | NULL in inspected source | NULL | `seller.shop_id` as text | `merchant_id` as text |
| `seller_name` | NULL in inspected source | `seller` | `seller.name` | NULL |
| `seller_is_store` | NULL | NULL | `seller.is_teknosa` | NULL |
| `in_stock` | NULL | `in_stock` | `in_stock` | NULL |
| `free_shipping` | NULL | NULL | `free_shipping` | `free_cargo` |
| `sponsored` | NULL | NULL | `sponsored` | NULL |
| `review_count` | `rating_count` | `rating_count` | `review_count` | `comment_count` |

Common catalogue fields are `title`, `brand`, `category`, `image` to `image_url`,
and, where present, `category_path`, `barcode` and `mpn`. `rating` maps directly;
`rating_scale` remains NULL unless explicitly provided. URLs belong to
observations because they can identify a seller-specific offer. Catalogue fields
can reflect the latest verified observation; the original values remain in
`raw_item` and `raw_document`.

`source_files.fetched_at` and `product_observations.observed_at` use the saved
envelope's `fetchedAt`, never the import time. `row_index` is the zero-based
array index. Source timestamps inside `response.meta` remain in `raw_document`.
Hash the original file bytes before parsing; JSONB preserves JSON values, not
the original whitespace or key order. Keep the source files on disk.

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

## Verification

Run from `backend`:

```powershell
.\mvnw.cmd -Dtest=MarketplaceSchemaTest test
```

The tests start an isolated PostgreSQL 18.6 container, apply the actual Flyway
migrations, and check duplicates, decimal precision, NULL values, seller-specific
observations and cross-store foreign keys. They do not read `.env` or connect to
the configured remote database.

## Import one reviewed search file

The importer reads local saved responses; it does not call ReefAPI. Build and
test it from `backend` before connecting to the configured database:

```powershell
.\mvnw.cmd '-Dtest=ReefSearchImporterTest,MarketplaceSchemaTest' test
```

The launcher reuses runtime libraries from the existing backend JAR. On a fresh
checkout, create that JAR with `.\mvnw.cmd -DskipTests package` first.

Run the following from the repository root. With no mode switch the command
validates the file and prints its hash without contacting the database:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\reefapi\import.ps1 -File data/reefapi/raw/2026-10-22/hepsiburada/search/airpods-pro-p1.json
```

`-DryRun` additionally checks the target without inserting application records.
`-Commit` imports exactly that one file and requires the SHA-256 reviewed in the
preview; a changed file is rejected before connecting. Both database modes read
the root `.env` without displaying credentials. The connection uses
`POSTGRES_URL`, or a full PostgreSQL URL in `POSTGRES_DB`, with optional
`POSTGRES_USER` and `POSTGRES_PASSWORD` when the URL has no credentials.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\reefapi\import.ps1 -File data/reefapi/raw/2026-10-22/hepsiburada/search/airpods-pro-p1.json -DryRun
$reviewedSha = '59d321e31777f9d30b6e0fa8f0608f0bc433a54a2a8ee9ffa640568723f7a142'
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\reefapi\import.ps1 -File data/reefapi/raw/2026-10-22/hepsiburada/search/airpods-pro-p1.json -Commit -ExpectedSha256 $reviewedSha
```

The first selected file contains 35 distinct products and 35 observations, with
a price sum of `823972.67 TRY`. Missing seller and stock remain NULL. No
catalogue values already stored for an existing product are overwritten by this
initial importer; every source's exact values remain in its observation JSON.
The importer verifies stored normalized values and JSON before commit and reads
the source back again afterwards. Repeating the command verifies the existing
data and returns `ALREADY_IMPORTED` without adding records.

First checkpoint, 2026-10-22: that Hepsiburada file was committed and verified
using a new connection. The database contained 1 store, 1 source file, 35
marketplace products and 35 observations. The repeated check returned
`ALREADY_IMPORTED` with zero new products. Other source files were left pending.

## Import a complete manifest

`-Batch` processes a fixed manifest with one transaction per file. Each UTF-8
line contains the original file SHA-256, a tab, and the repository-relative path.
The command validates the manifest and every source before connecting, reuses
the database connection, and reports each file's outcome. Failed files are
reported explicitly and make the final exit code nonzero; committed files stay
available and are verified/skipped on a repeat run.

The complete 2026-10-22 snapshot contains 199 source files, 4,671 observations
and 4,536 distinct marketplace product identities. Its local manifest is
`data/reefapi/imports/manifest-2026-10-22-8b9b8372c52a.tsv`, with SHA-256
`8b9b8372c52ac4e70e24f8559296508af7f90b0dd003d0bc92b0f5ec570f33c7`.

```powershell
$manifest = 'data/reefapi/imports/manifest-2026-10-22-8b9b8372c52a.tsv'
$manifestSha = '8b9b8372c52ac4e70e24f8559296508af7f90b0dd003d0bc92b0f5ec570f33c7'
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\reefapi\import.ps1 -Batch -File $manifest -Commit -ExpectedSha256 $manifestSha
```

For a final readback, run the same command with `-DryRun` instead of `-Commit`.
Every file must return `ALREADY_IMPORTED` and the summary must report zero
failures. The snapshot's expected store-level totals and exact price sums are
stored beside the manifest in its `.expected.json` file. Trendyol's missing
currency stays NULL; its amounts are checked without assigning a currency.

Full-snapshot checkpoint, 2026-10-22: all 199 files were processed successfully
(198 newly imported, 1 already present), with zero failed files. An independent
read-only `REPEATABLE READ` audit confirmed the complete path/hash manifest,
store-level exact price sums, source JSON/row identities, stock/currency NULLs
and the following database totals:

| Store | Source files | Marketplace products | Observations |
| --- | ---: | ---: | ---: |
| Hepsiburada | 52 | 1,286 | 1,294 |
| N11 | 52 | 816 | 818 |
| Teknosa | 42 | 713 | 758 |
| Trendyol | 53 | 1,721 | 1,801 |
| Total | 199 | 4,536 | 4,671 |

No missing, changed or extra sources, and no invalid source/observation
relationships were found. A fresh disk scan also matched the same 199 files.
The local audit report is `data/reefapi/imports/verification-2026-10-22.json`;
the manifest, expected totals and execution logs remain under the same directory.
The complete second pass returned `ALREADY_IMPORTED` for all 199 files, inserted
zero new records and reported zero failures; database totals stayed unchanged.
