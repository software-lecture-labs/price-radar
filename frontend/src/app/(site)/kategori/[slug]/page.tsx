import { ArrowLeftIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { EmptyState } from "@/components/product/empty-state";
import { PageLinks } from "@/components/product/page-links";
import { ProductGrid } from "@/components/product/product-card";
import { searchProducts, type Page, type ProductSummary } from "@/lib/api/products";
import { PAGE_PARAM, routes } from "@/lib/routes";
import { decodeSegment, parsePage } from "@/lib/search-params";

const PAGE_SIZE = 24;

export async function generateMetadata(props: PageProps<"/kategori/[slug]">): Promise<Metadata> {
  const { slug } = await props.params;
  return { title: decodeSegment(slug) ?? "Kategori" };
}

export default async function CategoryPage(props: PageProps<"/kategori/[slug]">) {
  const { slug } = await props.params;
  const category = decodeSegment(slug);
  if (!category) notFound();

  const page = parsePage((await props.searchParams)[PAGE_PARAM]);
  const products = await loadProducts(category, page);
  const pageHref = (target: number) =>
    target > 1 ? `${routes.category(category)}?${PAGE_PARAM}=${target}` : routes.category(category);

  return (
    <div className="space-y-8">
      <div>
        <Link
          href={routes.categories}
          className="inline-flex min-h-10 items-center gap-1.5 text-sm text-ink-muted underline-offset-4 hover:underline"
        >
          <ArrowLeftIcon className="size-4" aria-hidden /> Tüm kategoriler
        </Link>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight sm:text-[28px]">{category}</h1>
        {products ? (
          <p className="mt-2 text-sm text-ink-muted">
            {products.totalItems.toLocaleString("tr-TR")} ürün
          </p>
        ) : null}
      </div>

      {!products ? (
        <EmptyState
          title="Ürünlere şu an ulaşılamıyor."
          text="Biraz sonra yeniden deneyebilirsin."
        />
      ) : products.items.length === 0 ? (
        <EmptyState
          title={
            products.totalItems > 0 ? "Bu sayfada ürün yok." : "Bu kategoride ürün bulunamadı."
          }
          text={
            products.totalItems > 0
              ? "İlk sayfadan devam edebilirsin."
              : "Diğer kategorilere göz atabilirsin."
          }
          action={
            products.totalItems > 0
              ? { href: pageHref(1), label: "İlk sayfa" }
              : { href: routes.categories, label: "Kategoriler" }
          }
        />
      ) : (
        <>
          <ProductGrid products={products.items} />
          <PageLinks page={page} totalPages={products.totalPages} hrefFor={pageHref} />
        </>
      )}
    </div>
  );
}

async function loadProducts(category: string, page: number): Promise<Page<ProductSummary> | null> {
  try {
    return await searchProducts({ category, page: page - 1, size: PAGE_SIZE });
  } catch {
    return null;
  }
}
