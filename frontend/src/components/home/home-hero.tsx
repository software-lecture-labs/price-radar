import { SearchIcon } from "lucide-react";
import Form from "next/form";
import Link from "next/link";

import { DiscountBadge, ListPrice, Price } from "@/components/product/price";
import { ProductImage } from "@/components/product/product-image";
import { Sparkline } from "@/components/product/sparkline";
import type { PricePoint, ProductSummary } from "@/lib/api/products";
import { routes, SEARCH_PARAM } from "@/lib/routes";

const storeList = new Intl.ListFormat("tr", { type: "conjunction" });

interface HomeHeroProps {
  featured?: ProductSummary;
  history: PricePoint[];
  storeNames: string[];
  popularSearches: string[];
}

export function HomeHero({ featured, history, storeNames, popularSearches }: HomeHeroProps) {
  const sources = storeNames.length > 0 ? storeList.format(storeNames) : "Mağaza";

  return (
    <section className="grid grid-cols-1 items-center gap-12 pb-6 lg:grid-cols-12">
      <div className="flex flex-col items-start lg:col-span-7">
        <h1 className="mb-4 text-[34px] leading-[1.15] font-semibold tracking-[-0.02em] text-ink md:text-[44px]">
          Aynı ürün. En düşük fiyat.
        </h1>
        <p className="mb-8 max-w-xl text-base leading-relaxed text-ink-muted">
          {sources} fiyatlarını tek aramada karşılaştır, fiyat düştüğünde ilk sen öğren.
        </p>

        <Form
          action={routes.search()}
          role="search"
          className="mb-4 flex w-full max-w-xl items-center rounded-[10px] border border-border-strong bg-card p-1.5 transition-colors focus-within:border-ink"
        >
          <SearchIcon className="mx-3 size-5 shrink-0 text-ink-muted" aria-hidden />
          <input
            type="search"
            name={SEARCH_PARAM}
            placeholder="Ürün, marka veya model ara"
            aria-label="Ürün, marka veya model ara"
            enterKeyHint="search"
            className="h-11 w-full min-w-0 bg-transparent text-base text-ink outline-none placeholder:text-ink-muted"
          />
          <button
            type="submit"
            className="h-11 shrink-0 rounded-[10px] bg-ink px-6 text-sm font-semibold text-white transition-colors hover:bg-ink-secondary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink"
          >
            Ara
          </button>
        </Form>

        {popularSearches.length > 0 ? (
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="mr-1 font-medium text-ink-secondary">Popüler:</span>
            {popularSearches.map((term) => (
              <Link
                key={term}
                href={routes.search(term)}
                className="rounded-full bg-subtle px-3 py-1 text-ink-secondary transition-colors hover:bg-border-subtle"
              >
                {term}
              </Link>
            ))}
          </div>
        ) : null}
      </div>

      <div className="relative lg:col-span-5">
        <FeaturedTile product={featured} />
        {featured ? <PriceCard product={featured} history={history} /> : null}
      </div>
    </section>
  );
}

function FeaturedTile({ product }: { product?: ProductSummary }) {
  const tile = (
    <ProductImage
      src={product?.imageUrl}
      alt={product?.title ?? ""}
      sizes="(min-width: 1024px) 32vw, 90vw"
      eager
      className="absolute inset-10 md:inset-14"
    />
  );
  const className = "relative block aspect-square w-full overflow-hidden rounded-[14px] bg-tile";

  return product ? (
    <Link href={routes.product(String(product.id))} className={className}>
      {tile}
    </Link>
  ) : (
    <div className={className}>{tile}</div>
  );
}

function PriceCard({ product, history }: { product: ProductSummary; history: PricePoint[] }) {
  return (
    <div className="absolute -bottom-6 -left-2 flex w-[248px] flex-col gap-2 rounded-[14px] border border-border-subtle bg-card p-4 shadow-[0_4px_12px_-2px_rgba(18,19,22,0.06),0_1px_3px_0_rgba(18,19,22,0.04)] md:-left-6">
      <div className="flex items-center justify-between gap-2">
        <span className="truncate font-mono text-xs text-ink-muted">{product.store.name}</span>
        {product.discountPercent ? <DiscountBadge percent={product.discountPercent} /> : null}
      </div>
      <p className="truncate text-sm font-medium text-ink">{product.title}</p>
      {history.length >= 2 ? (
        <Sparkline prices={history.map((point) => point.price)} className="h-10 w-full text-ink" />
      ) : product.listPrice ? (
        <ListPrice amount={product.listPrice} currency={product.currency} />
      ) : null}
      <Price amount={product.price} currency={product.currency} />
    </div>
  );
}
