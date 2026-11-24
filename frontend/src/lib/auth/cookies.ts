// In production the names carry the __Host- prefix. Browsers accept such a cookie only over HTTPS, with Secure,
// Path=/ and no Domain, so another subdomain cannot plant a cookie under the same name. Development runs on
// http://localhost, where not every browser accepts Secure cookies, so it keeps the bare names.
const SECURE_COOKIES = process.env.NODE_ENV === "production";
const COOKIE_PREFIX = SECURE_COOKIES ? "__Host-" : "";

export const ACCESS_TOKEN_COOKIE = `${COOKIE_PREFIX}pr_access`;
export const REFRESH_TOKEN_COOKIE = `${COOKIE_PREFIX}pr_refresh`;

// The access cookie is dropped this long before the token expires, so a request never carries a token that
// expires on its way to the backend. The proxy then renews it from the refresh token.
const ACCESS_TOKEN_MARGIN_MS = 30_000;

export interface SessionCookieOptions {
  httpOnly: true;
  secure: boolean;
  sameSite: "lax";
  path: "/";
  expires: Date;
}

function sessionCookie(expires: Date): SessionCookieOptions {
  return {
    httpOnly: true,
    secure: SECURE_COOKIES,
    sameSite: "lax",
    path: "/",
    expires,
  };
}

export function accessTokenCookie(expiresAt: string): SessionCookieOptions {
  return sessionCookie(new Date(Date.parse(expiresAt) - ACCESS_TOKEN_MARGIN_MS));
}

export function refreshTokenCookie(expiresAt: string): SessionCookieOptions {
  return sessionCookie(new Date(expiresAt));
}

/**
 * Set a session cookie to an empty value with these options to remove it. `cookies.delete(name)` sends the expiry
 * without Secure, which a browser ignores for a __Host- cookie.
 */
export function expiredCookie(): SessionCookieOptions {
  return sessionCookie(new Date(0));
}
