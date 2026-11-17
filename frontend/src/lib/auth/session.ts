import "server-only";

import { cookies } from "next/headers";

import type { AuthTokens } from "@/lib/api/auth";
import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  accessTokenCookie,
  refreshTokenCookie,
} from "@/lib/auth/cookies";

/** Only callable where cookies can be written: Server Actions and Route Handlers. */
export async function startSession(tokens: AuthTokens): Promise<void> {
  const store = await cookies();
  store.set(
    ACCESS_TOKEN_COOKIE,
    tokens.accessToken,
    accessTokenCookie(tokens.accessTokenExpiresAt),
  );
  store.set(
    REFRESH_TOKEN_COOKIE,
    tokens.refreshToken,
    refreshTokenCookie(tokens.refreshTokenExpiresAt),
  );
}

/** Clears the session cookies and returns the refresh token so the caller can revoke it. */
export async function endSession(): Promise<string | undefined> {
  const store = await cookies();
  const refreshToken = store.get(REFRESH_TOKEN_COOKIE)?.value;
  store.delete(ACCESS_TOKEN_COOKIE);
  store.delete(REFRESH_TOKEN_COOKIE);
  return refreshToken;
}

export async function readAccessToken(): Promise<string | undefined> {
  return (await cookies()).get(ACCESS_TOKEN_COOKIE)?.value;
}
