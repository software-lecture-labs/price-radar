export const ACCESS_TOKEN_COOKIE = "pr_access";
export const REFRESH_TOKEN_COOKIE = "pr_refresh";

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
    secure: process.env.NODE_ENV === "production",
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
