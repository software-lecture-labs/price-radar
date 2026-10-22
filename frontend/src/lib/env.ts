const DEFAULT_BACKEND_URL = "http://localhost:8080";
const BACKEND_API_BASE_PATH = "/api/v1";

export const serverEnv = {
  backendUrl: (process.env.BACKEND_INTERNAL_URL ?? DEFAULT_BACKEND_URL).replace(/\/+$/, ""),
  apiBasePath: BACKEND_API_BASE_PATH,
} as const;

export const publicEnv = {
  apiBasePath: (process.env.NEXT_PUBLIC_API_BASE_PATH ?? BACKEND_API_BASE_PATH).replace(/\/+$/, ""),
  siteName: process.env.NEXT_PUBLIC_SITE_NAME ?? "Price Radar",
} as const;
