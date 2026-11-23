import assert from "node:assert/strict";
import test from "node:test";

import { parsePage, parseQuery, parseSort } from "../src/lib/search-params.ts";

test("the query is trimmed and cut to the length the backend accepts", () => {
  assert.equal(parseQuery("  iphone 17 "), "iphone 17");
  assert.equal(parseQuery(undefined), "");
  assert.equal(parseQuery(["kulaklık", "kılıf"]), "kulaklık");
  assert.equal(parseQuery("a".repeat(150)).length, 100);
});

test("unknown sort values fall back to the newest products", () => {
  assert.equal(parseSort("price_asc"), "price_asc");
  assert.equal(parseSort("price_desc"), "price_desc");
  for (const value of [undefined, "", "cheapest", ["popular", "price_asc"]]) {
    assert.equal(parseSort(value), "newest", String(value));
  }
});

test("pages start at 1 and invalid page numbers show the first page", () => {
  assert.equal(parsePage("3"), 3);
  assert.equal(parsePage(["2", "5"]), 2);
  for (const value of [undefined, "", "0", "-1", "2.5", "abc", "10001"]) {
    assert.equal(parsePage(value), 1, String(value));
  }
});
