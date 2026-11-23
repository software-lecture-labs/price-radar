import type { Metadata } from "next";
import Link from "next/link";

import { EmptyState } from "@/components/product/empty-state";
import { ProductImage } from "@/components/product/product-image";
import { getTopCategories, type CategorySummary } from "@/lib/api/catalog";
import { routes } from "@/lib/routes";

// The backend returns at most 50 categories, ordered by product count.
const CATEGORY_LIMIT = 50;

export const metadata: Metadata = { title: "Kategoriler" };

export default async function CategoriesPage() {
  const categories = await loadCategories();

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight sm:text-[28px]">Kategoriler</h1>
        <p className="mt-2 text-sm text-ink-muted">
          En çok ürün bulunan kategoriler. Bir kategori seçip ürünlerine bakabilirsin.
        </p>
      </div>

      {!categories ? (
        <EmptyState
          title="Kategorilere şu an ulaşılamıyor."
          text="Biraz sonra yeniden deneyebilirsin."
        />
      ) : categories.length === 0 ? (
        <EmptyState
          title="Henüz kategori yok."
          text="Ürünler yüklendiğinde kategoriler burada görünecek."
        />
      ) : (
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-5">
          {categories.map((category) => (
            <li key={category.name}>
              <Link
                href={routes.category(category.name)}
                className="group flex h-full flex-col items-center rounded-lg border border-border-subtle bg-card p-4 text-center transition-colors hover:border-border-strong"
              >
                <ProductImage
                  src={category.imageUrl}
                  alt=""
                  sizes="96px"
                  className="mb-3 size-20 transition-transform duration-200 group-hover:scale-105"
                />
                <span className="line-clamp-2 text-sm font-semibold text-ink">{category.name}</span>
                <span className="mt-1 text-xs text-ink-muted">
                  {category.productCount.toLocaleString("tr-TR")} ürün
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

async function loadCategories(): Promise<CategorySummary[] | null> {
  try {
    return await getTopCategories(CATEGORY_LIMIT);
  } catch {
    return null;
  }
}
