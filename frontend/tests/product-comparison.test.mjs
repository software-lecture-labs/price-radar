import assert from "node:assert/strict";
import test from "node:test";

import { getStoreProductUrl, prepareComparison } from "../src/lib/product-comparison.ts";

const product = {
  id: 42,
  title: "Apple iPhone 16 128 GB Siyah",
  store: { code: "trendyol", name: "Trendyol" },
  price: 44000,
  currency: "TRY",
  inStock: true,
  url: "https://www.trendyol.com/apple/iphone-16-p-42",
  observedAt: "2026-10-22T09:00:00Z",
  offerCount: 8,
};

function offer(code, price, overrides = {}) {
  return {
    store: { code, name: code },
    productId: 1,
    title: product.title,
    price,
    listPrice: null,
    currency: "TRY",
    inStock: true,
    freeShipping: null,
    url: `https://www.${code}.com/iphone-16-p-42`,
    observedAt: product.observedAt,
    sellerName: null,
    offerCount: 1,
    matchType: "barcode",
    ...overrides,
  };
}

test("sorts real offers by price and counts stores instead of marketplace sellers", () => {
  const offers = [
    offer("teknosa", 46000),
    offer("n11", 42000),
    offer("hepsiburada", 43000, { offerCount: 26 }),
    offer("trendyol", 44000),
  ];
  const result = prepareComparison(offers, product);

  assert.deepEqual(
    result.rows.map((row) => row.store.code),
    ["n11", "hepsiburada", "trendyol", "teknosa"],
  );
  assert.equal(result.bestOffer.store.code, "n11");
  assert.equal(result.availableCount, 4);
  assert.equal(result.rows.filter((row) => row.isBest).length, 1);
  assert.equal(result.rows[1].offerCount, 26);
  assert.equal(offers[1].isBest, undefined, "the input is not mutated");
});

test("does not recommend a cheaper out-of-stock listing", () => {
  const result = prepareComparison(
    [offer("n11", 39000, { inStock: false }), offer("trendyol", 44000)],
    product,
  );

  assert.equal(result.bestOffer.store.code, "trendyol");
  assert.equal(result.availableCount, 1);
  assert.equal(result.rows[1].store.code, "n11");
  assert.equal(result.rows[1].available, false);
  assert.equal(result.rows[1].inStock, false);
});

test("keeps all four store rows without inventing missing prices or links", () => {
  const result = prepareComparison([offer("n11", 42000)], product);

  assert.equal(result.rows.length, 4);
  assert.equal(result.availableCount, 1);
  for (const row of result.rows.slice(1)) {
    assert.equal(row.productId, null);
    assert.equal(row.price, null);
    assert.equal(row.productUrl, null);
    assert.equal(row.matchType, null);
    assert.equal(row.available, false);
    assert.equal(row.isBest, false);
  }
  assert.equal(
    result.rows.find((row) => row.store.code === "hepsiburada").store.name,
    "Hepsiburada",
  );
});

test("rejects malformed destinations, homepage/search links and domain spoofing", () => {
  const invalid = [
    "javascript:alert(1)",
    "https://www.trendyol.com.evil.test/item",
    "https://nottrendyol.com/item",
    "https://trendyol.com@evil.test/item",
    "https://evil.test@trendyol.com/item",
    "https://www.hepsiburada.com/item",
    "https://www.trendyol.com/",
    "https://www.trendyol.com/?q=iphone",
    "https://www.trendyol.com/sr?q=iphone",
    "https://www.trendyol.com/%73earch?q=iphone",
    "https://www.trendyol.com/item\n",
    "not a URL",
    "https://www.trendyol.com/%zz",
  ];
  for (const url of invalid) {
    assert.equal(getStoreProductUrl("trendyol", url), null, url);
  }
  assert.equal(getStoreProductUrl("unknown", "https://example.com/item"), null);
  assert.equal(getStoreProductUrl("trendyol", null), null);
  assert.equal(
    getStoreProductUrl("trendyol", "https://m.trendyol.com/apple/iphone-p-42?merchantId=7"),
    "https://m.trendyol.com/apple/iphone-p-42?merchantId=7",
  );
  assert.equal(
    getStoreProductUrl("N11", "http://www.n11.com/urun/iphone-42"),
    "http://www.n11.com/urun/iphone-42",
  );
});

test("invalid prices or destinations cannot become the cheapest available offer", () => {
  for (const price of [0, -1, Number.NaN, Number.POSITIVE_INFINITY, null]) {
    const result = prepareComparison([offer("n11", price)], product);
    assert.equal(result.bestOffer, null);
    assert.equal(result.availableCount, 0);
    assert.equal(result.rows.find((row) => row.store.code === "n11").hasPrice, false);
  }
  const result = prepareComparison(
    [offer("n11", 1, { url: "https://evil.test/iphone" }), offer("trendyol", 44000)],
    product,
  );
  assert.equal(result.bestOffer.store.code, "trendyol");
  assert.equal(result.availableCount, 1);
});

test("normalizes lira and compares only prices in the product currency", () => {
  const result = prepareComparison(
    [
      offer("n11", 100, { currency: "USD" }),
      offer("hepsiburada", 43000, { currency: null }),
      offer("trendyol", 44000, { currency: " try " }),
      offer("teknosa", 45000, { currency: "TL" }),
    ],
    product,
  );
  assert.equal(result.bestOffer.store.code, "hepsiburada");
  assert.equal(result.availableCount, 4);
  assert.equal(result.rows.at(-1).currency, "USD");
  assert.equal(result.rows.at(-1).comparable, false);
  assert.equal(result.rows.find((row) => row.store.code === "teknosa").normalizedCurrency, "TRY");

  const onlyForeign = prepareComparison([offer("n11", 1, { currency: "XYZ" })], product);
  assert.equal(onlyForeign.bestOffer, null);
  assert.equal(onlyForeign.rows[0].normalizedCurrency, "XYZ");
  const dollarProduct = { ...product, currency: "USD" };
  assert.equal(
    prepareComparison([offer("n11", 100, { currency: "usd" })], dollarProduct).bestOffer.store.code,
    "n11",
  );
});

test("uses a stable store order to break equal-price ties", () => {
  const result = prepareComparison(
    [offer("teknosa", 44000), offer("n11", 44000), offer("trendyol", 44000)],
    product,
  );
  assert.equal(result.bestOffer.store.code, "trendyol");
  assert.deepEqual(
    result.rows.slice(0, 3).map((row) => row.store.code),
    ["trendyol", "n11", "teknosa"],
  );
});

test("legacy responses retain only the known current listing as a fallback", () => {
  for (const comparison of [undefined, null, []]) {
    const result = prepareComparison(comparison, product);
    assert.equal(result.rows.length, 4);
    assert.equal(result.bestOffer.store.code, "trendyol");
    assert.equal(result.bestOffer.productId, 42);
    assert.equal(result.bestOffer.matchType, "listing");
    assert.equal(result.availableCount, 1);
    assert.equal(result.rows.filter((row) => row.price !== null).length, 1);
  }
});

test("unknown stock remains purchasable but no in-stock state is fabricated", () => {
  const result = prepareComparison([offer("n11", 42000, { inStock: null })], product);
  assert.equal(result.bestOffer.store.code, "n11");
  assert.equal(result.bestOffer.inStock, null);
});

test("unusable destinations remain in store order and every out-of-stock row stays ineligible", () => {
  const unusable = prepareComparison(
    [
      offer("n11", 1, { url: "https://evil.test/phone" }),
      offer("hepsiburada", 100, { url: null }),
      offer("trendyol", 1000, { url: "https://www.trendyol.com/" }),
    ],
    product,
  );
  assert.equal(unusable.bestOffer, null);
  assert.equal(unusable.availableCount, 0);
  assert.deepEqual(
    unusable.rows.map((row) => row.store.code),
    ["trendyol", "hepsiburada", "n11", "teknosa"],
  );

  const outOfStock = prepareComparison(
    [offer("trendyol", 44000, { inStock: false }), offer("n11", 42000, { inStock: false })],
    product,
  );
  assert.equal(outOfStock.bestOffer, null);
  assert.equal(outOfStock.availableCount, 0);
  assert.equal(outOfStock.rows[0].store.code, "n11");
});
