import { publicEnv, serverEnv } from "@/lib/env";
import { ApiError, type ProblemDetail } from "@/lib/api/problem";

const isServer = typeof window === "undefined";

export interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  /** Serialized as JSON unless it is already a string or FormData. */
  body?: unknown;
  /** Bearer token to send; omit for public endpoints. */
  token?: string;
  /** Query parameters; `undefined` and `null` values are dropped. */
  query?: Record<string, string | number | boolean | undefined | null>;
}

/**
 * Builds the request URL.
 *
 * On the server there is no origin to be relative to, so the absolute backend
 * URL is used. In the browser a relative path keeps the request same-origin and
 * lets the rewrite in `next.config.ts` forward it.
 */
function buildUrl(path: string, query?: ApiRequestOptions["query"]): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const base = isServer ? `${serverEnv.backendUrl}${publicEnv.apiBasePath}` : publicEnv.apiBasePath;

  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null) {
      search.append(key, String(value));
    }
  }

  const queryString = search.toString();
  return `${base}${normalizedPath}${queryString ? `?${queryString}` : ""}`;
}

async function toProblem(response: Response): Promise<ProblemDetail> {
  try {
    return (await response.json()) as ProblemDetail;
  } catch {
    return { status: response.status, title: response.statusText };
  }
}

/**
 * Calls the Price Radar API and returns the parsed body.
 *
 * @throws {ApiError} for any non-2xx response, carrying the problem+json body
 */
export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { body, token, query, headers, ...rest } = options;

  const requestHeaders = new Headers(headers);
  requestHeaders.set("Accept", "application/json");
  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  let requestBody: BodyInit | undefined;
  if (body instanceof FormData || typeof body === "string") {
    requestBody = body;
  } else if (body !== undefined) {
    requestBody = JSON.stringify(body);
    requestHeaders.set("Content-Type", "application/json");
  }

  const response = await fetch(buildUrl(path, query), {
    ...rest,
    headers: requestHeaders,
    body: requestBody,
  });

  if (!response.ok) {
    throw new ApiError(response.status, await toProblem(response));
  }

  if (response.status === 204 || response.headers.get("content-length") === "0") {
    return undefined as T;
  }

  return (await response.json()) as T;
}
