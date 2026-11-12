import assert from "node:assert/strict";
import test from "node:test";

import { accessTokenCookie, refreshTokenCookie } from "../src/lib/auth/cookies.ts";
import { safeRedirectPath } from "../src/lib/auth/redirect.ts";
import { validateLogin, validateRegistration } from "../src/lib/auth/validation.ts";

const valid = {
  fullName: "  Ayşe Yılmaz ",
  email: " ayse@example.com ",
  phone: "0532 123 45 67",
  password: "doğru at pili",
  confirmPassword: "doğru at pili",
};

test("a valid registration is trimmed, and the password is kept as typed", () => {
  const result = validateRegistration(valid);

  assert.equal(result.ok, true);
  assert.deepEqual(result.data, {
    fullName: "Ayşe Yılmaz",
    email: "ayse@example.com",
    phone: "0532 123 45 67",
    password: "doğru at pili",
  });
});

test("each invalid registration field gets its own Turkish message", () => {
  const result = validateRegistration({
    fullName: " ",
    email: "ayse@",
    phone: "0212 123 45 67",
    password: "kısa",
    confirmPassword: "kısa",
  });

  assert.equal(result.ok, false);
  assert.deepEqual(Object.keys(result.errors).sort(), ["email", "fullName", "password", "phone"]);
});

test("accepts the same Turkish mobile formats as the backend", () => {
  for (const phone of ["05321234567", "+90 532 123 45 67", "0 (532) 123 45 67", "532-123-45-67"]) {
    assert.equal(validateRegistration({ ...valid, phone }).ok, true, phone);
  }
  for (const phone of ["0532 123 45 6", "+1 532 123 45 67", "0532 ABC 45 67"]) {
    assert.equal(validateRegistration({ ...valid, phone }).ok, false, phone);
  }
});

test("passwords must match, be at least 8 characters, not blank, and fit BCrypt's 72 bytes", () => {
  const mismatch = validateRegistration({ ...valid, confirmPassword: "başka şifre" });
  assert.deepEqual(Object.keys(mismatch.errors), ["confirmPassword"]);

  assert.ok(
    validateRegistration({ ...valid, password: "        ", confirmPassword: "        " }).errors
      .password,
  );
  assert.ok(
    validateRegistration({ ...valid, password: "1234567", confirmPassword: "1234567" }).errors
      .password,
  );

  // 40 characters, 80 bytes in UTF-8.
  const long = "ş".repeat(40);
  assert.ok(
    validateRegistration({ ...valid, password: long, confirmPassword: long }).errors.password,
  );
  const ascii = "a".repeat(72);
  assert.equal(
    validateRegistration({ ...valid, password: ascii, confirmPassword: ascii }).ok,
    true,
  );
});

test("login only requires both fields, so older passwords still work", () => {
  assert.deepEqual(validateLogin({ email: " ayse@example.com ", password: "x" }), {
    ok: true,
    data: { email: "ayse@example.com", password: "x" },
  });
  assert.deepEqual(Object.keys(validateLogin({ email: "", password: null }).errors).sort(), [
    "email",
    "password",
  ]);
});

test("redirects after sign-in stay on this site", () => {
  assert.equal(
    safeRedirectPath("/hesabim/favoriler?sayfa=2#liste"),
    "/hesabim/favoriler?sayfa=2#liste",
  );
  for (const value of [
    "//evil.example",
    "/\\evil.example",
    "/\t/evil.example",
    "https://evil.example",
    "javascript:alert(1)",
    "hesabim",
    "",
    undefined,
    ["/hesabim"],
  ]) {
    assert.equal(safeRedirectPath(value), "/", String(value));
  }
  assert.equal(safeRedirectPath(undefined, "/hesabim"), "/hesabim");
});

test("session cookies are HttpOnly and the access cookie ends before its token", () => {
  const access = accessTokenCookie("2026-11-12T10:15:00Z");
  assert.equal(access.httpOnly, true);
  assert.equal(access.sameSite, "lax");
  assert.equal(access.path, "/");
  assert.equal(access.expires.toISOString(), "2026-11-12T10:14:30.000Z");

  assert.equal(
    refreshTokenCookie("2026-12-12T10:00:00Z").expires.toISOString(),
    "2026-12-12T10:00:00.000Z",
  );
});
