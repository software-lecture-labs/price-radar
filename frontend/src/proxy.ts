import { NextResponse, type NextRequest } from "next/server";

import { refreshAccessToken } from "@/lib/api/auth";
import { isApiError } from "@/lib/api/problem";
import { ACCESS_TOKEN_COOKIE, REFRESH_TOKEN_COOKIE, accessTokenCookie } from "@/lib/auth/cookies";

/**
 * Renews the short-lived access token from the refresh token before a page renders, because Server Components
 * cannot set cookies. This is not an authorization check: pages call the DAL, and the backend verifies every
 * token.
 */
export async function proxy(request: NextRequest) {
  const refreshToken = request.cookies.get(REFRESH_TOKEN_COOKIE)?.value;
  if (!refreshToken || request.cookies.has(ACCESS_TOKEN_COOKIE)) {
    return NextResponse.next();
  }

  try {
    const { accessToken, accessTokenExpiresAt } = await refreshAccessToken(refreshToken);
    // Also hand the new token to this request, so the page rendering now sees the session.
    request.cookies.set(ACCESS_TOKEN_COOKIE, accessToken);
    const response = NextResponse.next({ request: { headers: request.headers } });
    response.cookies.set(ACCESS_TOKEN_COOKIE, accessToken, accessTokenCookie(accessTokenExpiresAt));
    return response;
  } catch (error) {
    if (isApiError(error) && (error.status === 400 || error.status === 401)) {
      // Revoked or expired: drop it instead of retrying on every request.
      request.cookies.delete(REFRESH_TOKEN_COOKIE);
      const response = NextResponse.next({ request: { headers: request.headers } });
      response.cookies.delete(REFRESH_TOKEN_COOKIE);
      return response;
    }
    // Backend unreachable: keep the refresh token and try again on the next request.
    console.error("Could not refresh the access token", error);
    return NextResponse.next();
  }
}

export const config = {
  // Pages and Server Actions only; not the API rewrite, build assets or files from public/.
  matcher: [
    "/((?!api/|_next/static|_next/image|favicon\\.ico|.*\\.(?:png|jpe?g|gif|svg|webp|avif|ico|txt|xml|webmanifest)$).*)",
  ],
};
