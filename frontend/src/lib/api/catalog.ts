import { apiFetch } from "@/lib/api/client";

export interface StoreSummary {
  code: string;
  name: string;
  baseUrl: string;
  productCount: number;
}

export interface CategorySummary {
  name: string;
  productCount: number;
  imageUrl?: string;
}

export interface BrandSummary {
  name: string;
  productCount: number;
}

export function getStores(): Promise<StoreSummary[]> {
  return apiFetch<StoreSummary[]>("/stores", { cache: "no-store" });
}

export function getTopCategories(limit = 12): Promise<CategorySummary[]> {
  return apiFetch<CategorySummary[]>("/categories", { query: { limit }, cache: "no-store" });
}

export function getTopBrands(limit = 5): Promise<BrandSummary[]> {
  return apiFetch<BrandSummary[]>("/brands", { query: { limit }, cache: "no-store" });
}
