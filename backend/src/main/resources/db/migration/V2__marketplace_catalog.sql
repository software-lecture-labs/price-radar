-- Search results identify marketplace listings, not cross-store canonical products.
-- Keep seller-specific prices and source URLs in observations: a single response
-- can contain the same product more than once with different sellers and prices.

CREATE TABLE stores (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code text NOT NULL UNIQUE CHECK (code ~ '^[a-z0-9][a-z0-9_-]*$'),
    name text NOT NULL CHECK (btrim(name) <> ''),
    base_url text NOT NULL CHECK (base_url ~ '^https?://[^/[:space:]]+'),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE source_files (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id bigint NOT NULL REFERENCES stores(id),
    source_path text NOT NULL UNIQUE CHECK (btrim(source_path) <> ''),
    sha256 text NOT NULL UNIQUE CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    action text NOT NULL CHECK (btrim(action) <> ''),
    fetched_at timestamptz NOT NULL,
    result_count integer NOT NULL CHECK (result_count >= 0),
    -- The complete saved envelope includes request, response, status and credits.
    raw_document jsonb NOT NULL CHECK (jsonb_typeof(raw_document) = 'object'),
    imported_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, store_id)
);

CREATE TABLE marketplace_products (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id bigint NOT NULL REFERENCES stores(id),
    -- Preserve alphanumeric SKUs and leading zeroes. IDs are scoped to a store.
    external_id text NOT NULL CHECK (btrim(external_id) <> ''),
    external_group_id text,
    title text NOT NULL CHECK (btrim(title) <> ''),
    brand text,
    category text,
    category_path text[],
    image_url text,
    barcode text,
    mpn text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (store_id, external_id),
    UNIQUE (id, store_id)
);

CREATE TABLE product_observations (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id bigint NOT NULL,
    marketplace_product_id bigint NOT NULL,
    source_file_id bigint NOT NULL,
    -- Zero-based position in the source results/products array; NOT the
    -- store's optional search-ranking field. Repeated products remain intact.
    row_index integer NOT NULL CHECK (row_index >= 0),
    observed_at timestamptz NOT NULL,
    source_url text NOT NULL CHECK (source_url ~ '^https?://[^/[:space:]]+'),
    price numeric(16, 2) NOT NULL CHECK (price >= 0 AND price < 'Infinity'::numeric),
    -- NULL means the source did not provide a currency. Do not infer TRY.
    currency text CHECK (currency ~ '^[A-Z]{3}$'),
    list_price numeric(16, 2) CHECK (list_price >= 0 AND list_price < 'Infinity'::numeric),
    member_price numeric(16, 2) CHECK (member_price >= 0 AND member_price < 'Infinity'::numeric),
    seller_external_id text,
    seller_name text,
    seller_is_store boolean,
    in_stock boolean,
    free_shipping boolean,
    sponsored boolean,
    rating numeric CHECK (rating >= 0 AND rating < 'Infinity'::numeric),
    rating_scale numeric(8, 3) CHECK (rating_scale > 0 AND rating_scale < 'Infinity'::numeric),
    review_count integer CHECK (review_count >= 0),
    raw_item jsonb NOT NULL CHECK (jsonb_typeof(raw_item) = 'object'),
    imported_at timestamptz NOT NULL DEFAULT now(),
    CHECK (rating_scale IS NULL OR rating IS NULL OR rating <= rating_scale),
    UNIQUE (source_file_id, row_index),
    -- Independent foreign keys would allow mixing stores by mistake.
    FOREIGN KEY (marketplace_product_id, store_id)
        REFERENCES marketplace_products(id, store_id),
    FOREIGN KEY (source_file_id, store_id)
        REFERENCES source_files(id, store_id)
);

CREATE INDEX source_files_store_fetched_at_idx
    ON source_files (store_id, fetched_at DESC);
CREATE INDEX product_observations_product_observed_at_idx
    ON product_observations (marketplace_product_id, store_id, observed_at DESC, id DESC);
-- UNIQUE(source_file_id, row_index) already indexes the source foreign key.
CREATE INDEX product_observations_store_observed_at_idx
    ON product_observations (store_id, observed_at DESC);
