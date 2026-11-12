import { ArrowRightIcon, SearchIcon } from "lucide-react";
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
    <section className="grid items-center gap-8 md:grid-cols-12 lg:gap-16">
      <div className="md:col-span-7">
        <p className="mb-5 text-sm font-semibold text-ink-secondary">
          İyi alışveriş, doğru fiyatla başlar.
        </p>
        <h1 className="max-w-2xl text-[42px] leading-[1.1] font-semibold tracking-[-0.055em] text-ink sm:text-5xl lg:text-[64px]">
          <span className="block">Aynı ürün.</span>
          <span className="block">En düşük fiyat.</span>
        </h1>
        <p className="mt-6 max-w-md text-[15px] leading-7 text-ink-muted sm:text-base">
          {sources} fiyatlarını tek aramada karşılaştır.
          <span className="block">Aklındaki ürünü, içine sinen fiyata bul.</span>
        </p>

        <Form
          action={routes.search()}
          role="search"
          aria-label="Ürün keşfet"
          className="mt-8 flex max-w-xl items-center rounded-lg border border-border-strong bg-card p-1.5 transition-colors focus-within:border-ink focus-within:ring-1 focus-within:ring-ink"
        >
          <SearchIcon
            className="mx-3 size-5 shrink-0 text-ink-muted"
            strokeWidth={1.75}
            aria-hidden
          />
          <label htmlFor="home-search" className="sr-only">
            Ürün, marka veya model ara
          </label>
          <input
            id="home-search"
            type="search"
            name={SEARCH_PARAM}
            placeholder="Aklında hangi ürün var?"
            enterKeyHint="search"
            className="h-12 w-full min-w-0 bg-transparent text-sm text-ink outline-none placeholder:text-ink-muted focus-visible:outline-none sm:text-base"
          />
          <button
            type="submit"
            className="inline-flex h-12 shrink-0 items-center gap-3 rounded-md bg-ink px-5 text-sm font-semibold text-white transition-colors hover:bg-ink-secondary active:bg-ink"
          >
            Ara <ArrowRightIcon className="hidden size-4 sm:block" aria-hidden />
          </button>
        </Form>

        {popularSearches.length > 0 ? (
          <div className="mt-3 flex flex-wrap items-center gap-x-4 text-xs">
            <span className="text-ink-muted">Sık arananlar</span>
            {popularSearches.map((term) => (
              <Link
                key={term}
                href={routes.search(term)}
                className="inline-flex min-h-10 items-center font-medium text-ink-secondary underline-offset-4 hover:underline"
              >
                {term}
              </Link>
            ))}
          </div>
        ) : null}
      </div>

      <div className="min-w-0 md:col-span-5">
        {featured ? <FeaturedProduct product={featured} history={history} /> : <DiscoveryFeature />}
      </div>
    </section>
  );
}

function FeaturedProduct({ product, history }: { product: ProductSummary; history: PricePoint[] }) {
  return (
    <article className="overflow-hidden rounded-xl border border-border-subtle bg-card">
      <Link href={routes.product(String(product.id))} className="group block p-6">
        <div className="mb-3 flex items-center justify-between gap-3">
          <span className="text-xs font-semibold text-ink-muted">Radarın seçimi</span>
          {product.discountPercent ? <DiscountBadge percent={product.discountPercent} /> : null}
        </div>
        <ProductImage
          src={product.imageUrl}
          alt={product.title}
          sizes="(min-width: 768px) 35vw, 85vw"
          eager
          className="my-4 h-48 transition-transform duration-300 group-hover:scale-[1.03] lg:h-56"
        />
        <p className="mb-1 text-xs text-ink-muted">{product.brand ?? product.store.name}</p>
        <h2 className="line-clamp-2 text-lg leading-6 font-semibold tracking-tight text-ink">
          {product.title}
        </h2>
        <div className="mt-4 flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-col gap-1">
            {product.listPrice ? (
              <ListPrice amount={product.listPrice} currency={product.currency} />
            ) : null}
            <Price amount={product.price} currency={product.currency} size="lg" />
          </div>
          <ArrowRightIcon
            className="mb-1 size-5 text-ink transition-transform group-hover:translate-x-1"
            aria-hidden
          />
        </div>
      </Link>
      <div className="flex items-center justify-between gap-4 border-t border-border-subtle bg-subtle/60 px-6 py-3 text-xs text-ink-muted">
        <span>
          {product.store.name} ·{" "}
          {product.offerCount > 0 ? `${product.offerCount} teklif` : "Fiyat ayrıntıları"}
        </span>
        {history.length >= 2 ? (
          <span className="flex items-center gap-2">
            <span className="sr-only">Fiyat geçmişi mevcut</span>
            <Sparkline prices={history.map((point) => point.price)} className="h-6 w-20 text-ink" />
          </span>
        ) : (
          <span>Ürünü incele</span>
        )}
      </div>
    </article>
  );
}

function DiscoveryFeature() {
  return (
    <Link
      href={routes.search("kulaklık")}
      className="group flex items-center gap-4 rounded-xl bg-radar-lime-surface px-5 py-6 md:flex-col md:items-stretch md:gap-0 md:px-8 md:pt-6 md:pb-7"
    >
      <ProductImage
        src="/discover/headphones.jpg"
        alt=""
        sizes="(min-width: 768px) 30vw, 35vw"
        eager
        className="h-36 w-2/5 shrink-0 transition-transform duration-300 group-hover:scale-[1.03] md:mb-5 md:h-56 md:w-full lg:h-60"
      />
      <div>
        <p className="mb-2 text-xs font-medium text-ink-secondary">Günlük hayata iyi gelenler</p>
        <h2 className="text-xl leading-tight font-semibold tracking-tight text-ink md:text-2xl">
          Biraz müzik.
          <br />
          Biraz kendine zaman.
        </h2>
        <span className="mt-4 inline-flex min-h-10 items-center gap-2 text-xs font-semibold text-ink md:text-sm">
          Kulaklıkları keşfet{" "}
          <ArrowRightIcon
            className="size-4 transition-transform group-hover:translate-x-1"
            aria-hidden
          />
        </span>
      </div>
    </Link>
  );
}
