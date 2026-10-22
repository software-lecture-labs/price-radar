import { apiFetch } from "@/lib/api/client";

export interface StoreRef {
  code: string;
  name: string;
}

export interface ProductSummary {
  id: number;
  title: string;
  brand?: string;
  category?: string;
  imageUrl?: string;
  barcode?: string;
  store: StoreRef;
  price: number;
  listPrice?: number;
  discountPercent?: number;
  currency?: string;
  inStock?: boolean;
  url: string;
  observedAt: string;
  offerCount: number;
}

export interface ProductOffer {
  sellerName?: string;
  price: number;
  listPrice?: number;
  currency?: string;
  inStock?: boolean;
  freeShipping?: boolean;
  url: string;
  observedAt: string;
}

export interface PricePoint {
  observedAt: string;
  price: number;
}

export interface ProductComparisonOffer {
  store: StoreRef;
  productId: number | null;
  title: string | null;
  price: number | null;
  listPrice: number | null;
  currency: string | null;
  inStock: boolean | null;
  freeShipping: boolean | null;
  url: string | null;
  observedAt: string | null;
  sellerName: string | null;
  offerCount: number;
  matchType: "listing" | "barcode" | "mpn" | "exact_title" | null;
}

export interface ProductDetail {
  product: ProductSummary;
  externalId: string;
  externalGroupId?: string;
  mpn?: string;
  categoryPath: string[];
  offers: ProductOffer[];
  /** Lowest price at each observation, oldest first. */
  priceHistory: PricePoint[];
  comparisonOffers?: ProductComparisonOffer[];
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type ProductSort = "newest" | "price_asc" | "price_desc";

export interface ProductSearch {
  q?: string;
  store?: string;
  sort?: ProductSort;
  /** Zero-based. */
  page?: number;
  size?: number;
}

export function searchProducts(search: ProductSearch = {}): Promise<Page<ProductSummary>> {
  return apiFetch<Page<ProductSummary>>("/products", { query: { ...search }, cache: "no-store" });
}

export function getDeals(limit = 5): Promise<ProductSummary[]> {
  return apiFetch<ProductSummary[]>("/products/deals", { query: { limit }, cache: "no-store" });
}

export function getProduct(id: number | string): Promise<ProductDetail> {
  return apiFetch<ProductDetail>(`/products/${encodeURIComponent(id)}`, { cache: "no-store" });
}
