import assert from "node:assert/strict";
import test from "node:test";

import { ApiError, isApiError } from "../src/lib/api/problem.ts";

test("preserves valid problem details", () => {
  const problem = {
    type: "urn:example:validation",
    title: "Invalid request",
    status: 400,
    detail: "The title is required.",
    instance: "/products",
    errors: { title: "Required" },
  };
  const error = new ApiError(400, problem);

  assert.equal(error.name, "ApiError");
  assert.equal(error.message, problem.detail);
  assert.equal(error.status, 400);
  assert.deepEqual(error.problem, problem);
  assert.deepEqual(error.fieldErrors, problem.errors);
});

test("falls back to a valid title or the HTTP status for its message", () => {
  for (const [problem, message] of [
    [{ title: "Bad Gateway" }, "Bad Gateway"],
    [{ title: "Bad Gateway", detail: 42 }, "Bad Gateway"],
    [{ title: false }, "Request failed with status 502"],
    [{}, "Request failed with status 502"],
  ]) {
    assert.equal(new ApiError(502, problem).message, message);
  }
});

test("accepts missing and non-object error bodies", () => {
  for (const problem of [undefined, null, false, 17, "upstream error", []]) {
    const error = new ApiError(502, problem);

    assert.equal(error.message, "Request failed with status 502");
    assert.equal(error.status, 502);
    assert.equal(error.problem.status, 502);
    assert.deepEqual(error.fieldErrors, {});
  }
});

test("discards incorrectly typed problem fields", () => {
  const error = new ApiError(502, {
    type: {},
    title: 42,
    detail: ["invalid"],
    instance: false,
  });

  for (const field of ["type", "title", "detail", "instance"]) {
    assert.equal(error.problem[field], undefined);
  }
  assert.equal(error.message, "Request failed with status 502");
});

test("uses the actual HTTP status regardless of the body status", () => {
  for (const status of [400, "400", null, undefined]) {
    const error = new ApiError(502, { status });

    assert.equal(error.status, 502);
    assert.equal(error.problem.status, 502);
  }
});

test("keeps only string field validation messages", () => {
  const error = new ApiError(400, {
    errors: {
      title: "Required",
      empty: "",
      count: 12,
      active: false,
      missing: null,
      nested: { message: "Invalid" },
      list: ["Invalid"],
    },
  });

  assert.deepEqual(error.fieldErrors, { title: "Required", empty: "" });
});

test("ignores invalid field-error containers", () => {
  for (const errors of [null, [], "invalid", false, 12]) {
    assert.deepEqual(new ApiError(400, { errors }).fieldErrors, {});
  }
});

test("preserves special field names without changing the object prototype", () => {
  const errors = JSON.parse('{"__proto__":"Invalid field","constructor":"Required"}');
  const error = new ApiError(400, { errors });

  assert.equal(Object.getPrototypeOf(error.fieldErrors), Object.prototype);
  assert.equal(Object.hasOwn(error.fieldErrors, "__proto__"), true);
  assert.equal(error.fieldErrors.__proto__, "Invalid field");
  assert.equal(error.fieldErrors.constructor, "Required");
});

test("identifies ApiError instances without accepting unrelated errors", () => {
  const error = new ApiError(400, {});

  assert.ok(error instanceof Error);
  assert.equal(isApiError(error), true);
  for (const value of [null, undefined, new Error("Other"), { name: "ApiError" }]) {
    assert.equal(isApiError(value), false);
  }
});
