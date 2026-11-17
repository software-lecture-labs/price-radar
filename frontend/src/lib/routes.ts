export const SEARCH_PARAM = "q";
/** Where to return after signing in. Validate with `safeRedirectPath` before redirecting. */
export const NEXT_PARAM = "next";

function withNext(path: string, next?: string): string {
  return next ? `${path}?${new URLSearchParams({ [NEXT_PARAM]: next })}` : path;
}

export const routes = {
  home: "/",
  search: (query?: string) =>
    query ? `/arama?${new URLSearchParams({ [SEARCH_PARAM]: query })}` : "/arama",
  categories: "/kategoriler",
  category: (slug: string) => `/kategori/${encodeURIComponent(slug)}`,
  product: (slug: string) => `/urun/${encodeURIComponent(slug)}`,
  merchants: "/magazalar",
  merchant: (slug: string) => `/magaza/${encodeURIComponent(slug)}`,
  login: (next?: string) => withNext("/giris", next),
  register: (next?: string) => withNext("/kayit", next),
  account: "/hesabim",
  favorites: "/hesabim/favoriler",
  alerts: "/hesabim/alarmlar",
  status: "/durum",
  admin: "/yonetim",
  adminMatches: "/yonetim/eslestirmeler",
} as const;
