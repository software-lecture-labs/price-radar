import { ArrowDownIcon, ArrowUpRightIcon, ChevronRightIcon, StoreIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";

import { Price } from "@/components/product/price";
import { PriceHistory } from "@/components/product/price-history";
import { ProductGallery } from "@/components/product/product-gallery";
import { StoreComparison, observedDate } from "@/components/product/store-comparison";
import { getProduct } from "@/lib/api/products";
import { isApiError } from "@/lib/api/problem";
import { prepareComparison } from "@/lib/product-comparison";
import { routes } from "@/lib/routes";

const loadProduct = cache(async (id: string) => {
  if (!/^[1-9]\d{0,17}$/.test(id)) return null;
  try {
    return await getProduct(id);
  } catch (error) {
    if (isApiError(error) && error.status === 404) return null;
    throw error;
  }
});

export async function generateMetadata(props: PageProps<"/urun/[slug]">): Promise<Metadata> {
  const { slug } = await props.params;
  try {
    const detail = await loadProduct(slug);
    return detail
      ? {
          title: detail.product.title,
          description: `${detail.product.title} için Trendyol, Hepsiburada, N11 ve Teknosa fiyatlarını karşılaştır.`,
        }
      : { title: "Ürün bulunamadı" };
  } catch {
    return { title: "Ürün fiyatları" };
  }
}

export default async function ProductPage(props: PageProps<"/urun/[slug]">) {
  const { slug } = await props.params;
  const detail = await loadProduct(slug);
  if (!detail) notFound();

  const { product } = detail;
  const comparison = prepareComparison(detail.comparisonOffers, product);
  const pricedStoreCount = comparison.rows.filter((offer) => offer.hasPrice).length;
  const best = comparison.bestOffer;
  const lastObserved = observedDate(best?.observedAt ?? product.observedAt);
  const productFacts = [
    { label: "Marka", value: product.brand },
    { label: "Kategori", value: product.category ?? detail.categoryPath.at(-1) },
    { label: "Üretici model kodu", value: detail.mpn },
    { label: "Barkod", value: product.barcode },
  ].filter((fact): fact is { label: string; value: string } => Boolean(fact.value));

  return (
    <div className="space-y-10 pb-4 sm:space-y-14">
      <nav
        aria-label="İçerik yolu"
        className="flex min-w-0 items-center gap-2 text-xs text-ink-muted"
      >
        <Link
          href={routes.home}
          className="inline-flex min-h-10 shrink-0 items-center underline-offset-4 hover:underline"
        >
          Ana sayfa
        </Link>
        <ChevronRightIcon className="size-3 shrink-0" aria-hidden />
        {product.category ? (
          <>
            <Link
              href={routes.category(product.category)}
              className="inline-flex min-h-10 min-w-0 items-center underline-offset-4 hover:underline"
            >
              <span className="truncate">{product.category}</span>
            </Link>
            <ChevronRightIcon className="size-3 shrink-0" aria-hidden />
          </>
        ) : null}
        <span aria-current="page" className="truncate text-ink-secondary">
          {product.title}
        </span>
      </nav>

      <section
        aria-labelledby="product-title"
        className="grid items-start gap-8 lg:grid-cols-[1fr_1.15fr] lg:gap-12"
      >
        <ProductGallery src={product.imageUrl} title={product.title} />
        <div>
          {product.brand ? (
            <Link
              href={routes.search(product.brand)}
              className="mb-3 inline-flex min-h-8 items-center text-sm font-semibold text-ink-secondary underline-offset-4 hover:underline"
            >
              {product.brand}
            </Link>
          ) : null}
          <h1
            id="product-title"
            className="text-[28px] leading-[1.25] font-semibold tracking-[-0.04em] break-words text-ink sm:text-[34px]"
          >
            {product.title}
          </h1>
          <a
            href="#magaza-fiyatlari"
            className="mt-4 inline-flex min-h-11 items-center gap-2 text-sm font-medium text-ink-secondary underline-offset-4 hover:underline"
          >
            <StoreIcon className="size-4" aria-hidden /> 4 mağazayı karşılaştır{" "}
            <ArrowDownIcon className="size-3.5" aria-hidden />
          </a>

          <div className="mt-5 rounded-xl border border-border-subtle bg-card p-5 sm:p-6">
            {best ? (
              <>
                <p className="mb-3 text-xs font-medium text-ink-muted">
                  Karşılaştırılan tekliflerde en düşük fiyat
                </p>
                <Price amount={best.price!} currency={best.normalizedCurrency} size="lg" />
                <p className="mt-3 text-sm font-semibold text-ink">
                  {best.store.name}
                  {best.sellerName ? (
                    <span className="font-normal text-ink-muted"> · {best.sellerName}</span>
                  ) : null}
                </p>
                <a
                  href={best.productUrl!}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={`${best.store.name} mağazasındaki ürün sayfasına git (yeni sekme)`}
                  className="mt-5 flex min-h-12 w-full items-center justify-center gap-3 rounded-md bg-ink px-5 text-sm font-semibold text-white transition-colors hover:bg-ink-secondary"
                >
                  {best.store.name} mağazasına git{" "}
                  <ArrowUpRightIcon className="size-4 shrink-0" aria-hidden />
                </a>
              </>
            ) : (
              <>
                <p className="text-base font-semibold text-ink">
                  {comparison.availableCount > 0
                    ? "Karşılaştırılabilir fiyat bulunamadı"
                    : "Satın alınabilir teklif bulunamadı"}
                </p>
                <p className="mt-2 text-sm leading-6 text-ink-muted">
                  Mevcut fiyat ve stok bilgilerini aşağıdaki mağaza listesinde inceleyebilirsin.
                </p>
                <a
                  href="#magaza-fiyatlari"
                  className="mt-4 inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-ink"
                >
                  Mağaza fiyatlarına bak <ArrowDownIcon className="size-4" aria-hidden />
                </a>
              </>
            )}
            <div className="mt-4 flex flex-wrap justify-between gap-x-4 gap-y-2 border-t border-border-subtle pt-4 text-xs text-ink-muted">
              <span>{pricedStoreCount}/4 mağazada fiyat bulundu</span>
              {lastObserved ? <span>Son gözlem: {lastObserved}</span> : null}
            </div>
          </div>
          <p className="mt-4 text-xs leading-6 text-ink-muted">
            Price Radar fiyatları karşılaştırır. Satın alma işlemi seçtiğin mağazada tamamlanır.
          </p>
        </div>
      </section>

      <StoreComparison comparison={comparison} />

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <PriceHistory
          history={detail.priceHistory}
          currency={product.currency}
          storeName={product.store.name}
        />
        <section
          id="urun-bilgileri"
          aria-labelledby="information-heading"
          className="rounded-xl border border-border-subtle bg-card p-6 sm:p-8"
        >
          <h2 id="information-heading" className="text-xl font-semibold tracking-tight text-ink">
            Ürün bilgileri
          </h2>
          <p className="mt-2 text-sm leading-6 text-ink-muted">
            Karşılaştırdığın ürüne ait katalog bilgileri.
          </p>
          {productFacts.length ? (
            <dl className="mt-6 divide-y divide-border-subtle">
              {productFacts.map((fact) => (
                <div key={fact.label} className="grid grid-cols-[1fr_1.4fr] gap-4 py-4 text-sm">
                  <dt className="text-ink-muted">{fact.label}</dt>
                  <dd className="font-medium break-words text-ink">{fact.value}</dd>
                </div>
              ))}
            </dl>
          ) : (
            <p className="mt-6 text-sm text-ink-muted">Ek ürün bilgisi henüz bulunmuyor.</p>
          )}
        </section>
      </div>
    </div>
  );
}
