const PLACEHOLDER_ORIGIN = "http://placeholder.invalid";

/**
 * Returns `value` only if it is a path on this site, so a crafted `?next=` cannot send someone to another site
 * after they sign in. `//evil.example` and `/\evil.example` are rejected because browsers read them as hosts.
 */
export function safeRedirectPath(value: unknown, fallback = "/"): string {
  if (typeof value !== "string" || !value.startsWith("/")) {
    return fallback;
  }
  let url: URL;
  try {
    url = new URL(value, PLACEHOLDER_ORIGIN);
  } catch {
    return fallback;
  }
  if (url.origin !== PLACEHOLDER_ORIGIN) {
    return fallback;
  }
  return `${url.pathname}${url.search}${url.hash}`;
}
