import { publicEnv, serverEnv } from "@/lib/env";
import { ApiError } from "@/lib/api/problem";

const isServer = typeof window === "undefined";

export interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  token?: string;
  query?: Record<string, string | number | boolean | undefined | null>;
}

function buildUrl(path: string, query?: ApiRequestOptions["query"]): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const base = isServer ? `${serverEnv.backendUrl}${serverEnv.apiBasePath}` : publicEnv.apiBasePath;

  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null) {
      search.append(key, String(value));
    }
  }

  const queryString = search.toString();
  return `${base}${normalizedPath}${queryString ? `?${queryString}` : ""}`;
}

async function toProblem(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return { status: response.status, title: response.statusText };
  }
}

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
