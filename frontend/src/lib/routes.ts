export const SEARCH_PARAM = "q";

export const routes = {
  home: "/",
  search: (query?: string) =>
    query ? `/arama?${new URLSearchParams({ [SEARCH_PARAM]: query })}` : "/arama",
  categories: "/kategoriler",
  category: (slug: string) => `/kategori/${encodeURIComponent(slug)}`,
  product: (slug: string) => `/urun/${encodeURIComponent(slug)}`,
  merchants: "/magazalar",
  merchant: (slug: string) => `/magaza/${encodeURIComponent(slug)}`,
  login: "/giris",
  register: "/kayit",
  account: "/hesabim",
  favorites: "/hesabim/favoriler",
  alerts: "/hesabim/alarmlar",
  status: "/durum",
  admin: "/yonetim",
  adminMatches: "/yonetim/eslestirmeler",
} as const;
