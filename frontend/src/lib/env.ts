/**
 * Single place that reads `process.env`.
 *
 * Server-only values must never be referenced from a Client Component — only
 * `NEXT_PUBLIC_*` names are inlined into the browser bundle.
 */

const DEFAULT_BACKEND_URL = "http://localhost:8080";

export const serverEnv = {
  /** Absolute backend URL used by Server Components and Route Handlers. */
  backendUrl: process.env.BACKEND_INTERNAL_URL ?? DEFAULT_BACKEND_URL,
} as const;

export const publicEnv = {
  /**
   * Path the browser uses. Relative by default so requests go through the
   * rewrite in `next.config.ts` and stay same-origin.
   */
  apiBasePath: process.env.NEXT_PUBLIC_API_BASE_PATH ?? "/api/v1",
  siteName: process.env.NEXT_PUBLIC_SITE_NAME ?? "Price Radar",
} as const;
