import type { Metadata } from "next";
import Link from "next/link";

import { EmptyState } from "@/components/product/empty-state";
import { PageLinks } from "@/components/product/page-links";
import { ProductGrid } from "@/components/product/product-card";
import { Button } from "@/components/ui/button";
import {
  searchProducts,
  type Page,
  type ProductSort,
  type ProductSummary,
} from "@/lib/api/products";
import { PAGE_PARAM, routes, SEARCH_PARAM, SORT_PARAM } from "@/lib/routes";
import { parsePage, parseQuery, parseSort, sortOptions } from "@/lib/search-params";

const PAGE_SIZE = 24;

export async function generateMetadata(props: PageProps<"/arama">): Promise<Metadata> {
  const query = parseQuery((await props.searchParams)[SEARCH_PARAM]);
  return { title: query ? `${query} araması` : "Tüm ürünler" };
}

export default async function SearchPage(props: PageProps<"/arama">) {
  const searchParams = await props.searchParams;
  const query = parseQuery(searchParams[SEARCH_PARAM]);
  const sort = parseSort(searchParams[SORT_PARAM]);
  const page = parsePage(searchParams[PAGE_PARAM]);
  const results = await loadResults(query, sort, page);

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight sm:text-[28px]">
            {query ? `“${query}” için sonuçlar` : "Tüm ürünler"}
          </h1>
          {results ? (
            <p className="mt-2 text-sm text-ink-muted">
              {results.totalItems.toLocaleString("tr-TR")} ürün bulundu
            </p>
          ) : null}
        </div>

        <nav aria-label="Sıralama" className="flex flex-wrap gap-2">
          {sortOptions.map((option) => (
            <Button
              key={option.value}
              asChild
              size="lg"
              variant={option.value === sort ? "default" : "outline"}
            >
              <Link
                href={searchHref(query, option.value)}
                aria-current={option.value === sort ? "true" : undefined}
              >
                {option.label}
              </Link>
            </Button>
          ))}
        </nav>
      </div>

      {!results ? (
        <EmptyState
          title="Ürünlere şu an ulaşılamıyor."
          text="Biraz sonra yeniden deneyebilirsin."
        />
      ) : results.items.length === 0 ? (
        <EmptyState
          title={results.totalItems > 0 ? "Bu sayfada ürün yok." : "Sonuç bulunamadı."}
          text={
            results.totalItems > 0
              ? "İlk sayfadan devam edebilirsin."
              : "Başka bir kelimeyle ya da marka adıyla aramayı deneyebilirsin."
          }
          action={
            results.totalItems > 0
              ? { href: searchHref(query, sort), label: "İlk sayfa" }
              : undefined
          }
        />
      ) : (
        <>
          <ProductGrid products={results.items} />
          <PageLinks
            page={page}
            totalPages={results.totalPages}
            hrefFor={(target) => searchHref(query, sort, target)}
          />
        </>
      )}
    </div>
  );
}

async function loadResults(
  query: string,
  sort: ProductSort,
  page: number,
): Promise<Page<ProductSummary> | null> {
  try {
    return await searchProducts({ q: query || undefined, sort, page: page - 1, size: PAGE_SIZE });
  } catch {
    return null;
  }
}

function searchHref(query: string, sort: ProductSort, page = 1): string {
  const params = new URLSearchParams();
  if (query) params.set(SEARCH_PARAM, query);
  if (sort !== "newest") params.set(SORT_PARAM, sort);
  if (page > 1) params.set(PAGE_PARAM, String(page));

  const search = params.toString();
  return search ? `${routes.search()}?${search}` : routes.search();
}
