import { ArrowLeftIcon, ArrowUpRightIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";

import { EmptyState } from "@/components/product/empty-state";
import { PageLinks } from "@/components/product/page-links";
import { ProductGrid } from "@/components/product/product-card";
import { Button } from "@/components/ui/button";
import { getStores } from "@/lib/api/catalog";
import { searchProducts, type Page, type ProductSummary } from "@/lib/api/products";
import { PAGE_PARAM, routes } from "@/lib/routes";
import { parsePage } from "@/lib/search-params";

const PAGE_SIZE = 24;

// The metadata and the page both need the store list; cache() fetches it once per request.
const loadStores = cache(async () => {
  try {
    return await getStores();
  } catch {
    return null;
  }
});

export async function generateMetadata(props: PageProps<"/magaza/[slug]">): Promise<Metadata> {
  const { slug } = await props.params;
  const store = (await loadStores())?.find((item) => item.code === slug);
  return { title: store ? `${store.name} ürünleri` : "Mağaza" };
}

export default async function StorePage(props: PageProps<"/magaza/[slug]">) {
  const { slug } = await props.params;
  const page = parsePage((await props.searchParams)[PAGE_PARAM]);

  const stores = await loadStores();
  if (!stores) {
    return (
      <EmptyState title="Mağazaya şu an ulaşılamıyor." text="Biraz sonra yeniden deneyebilirsin." />
    );
  }

  const store = stores.find((item) => item.code === slug);
  if (!store) notFound();

  const products = await loadProducts(store.code, page);
  const pageHref = (target: number) =>
    target > 1
      ? `${routes.merchant(store.code)}?${PAGE_PARAM}=${target}`
      : routes.merchant(store.code);

  return (
    <div className="space-y-8">
      <div>
        <Link
          href={routes.merchants}
          className="inline-flex min-h-10 items-center gap-1.5 text-sm text-ink-muted underline-offset-4 hover:underline"
        >
          <ArrowLeftIcon className="size-4" aria-hidden /> Tüm mağazalar
        </Link>
        <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight sm:text-[28px]">{store.name}</h1>
            {products ? (
              <p className="mt-2 text-sm text-ink-muted">
                {products.totalItems.toLocaleString("tr-TR")} ürün
              </p>
            ) : null}
          </div>
          <Button asChild variant="outline" size="lg">
            <a href={store.baseUrl} target="_blank" rel="noopener noreferrer">
              Mağazanın sitesi <ArrowUpRightIcon aria-hidden />
            </a>
          </Button>
        </div>
      </div>

      {!products ? (
        <EmptyState
          title="Ürünlere şu an ulaşılamıyor."
          text="Biraz sonra yeniden deneyebilirsin."
        />
      ) : products.items.length === 0 ? (
        <EmptyState
          title={products.totalItems > 0 ? "Bu sayfada ürün yok." : "Bu mağazada henüz ürün yok."}
          text={
            products.totalItems > 0
              ? "İlk sayfadan devam edebilirsin."
              : "Ürünler yüklendiğinde burada görünecek."
          }
          action={products.totalItems > 0 ? { href: pageHref(1), label: "İlk sayfa" } : undefined}
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

async function loadProducts(store: string, page: number): Promise<Page<ProductSummary> | null> {
  try {
    return await searchProducts({ store, page: page - 1, size: PAGE_SIZE });
  } catch {
    return null;
  }
}
