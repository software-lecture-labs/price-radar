import { ArrowRightIcon, RefreshCwIcon, SearchXIcon } from "lucide-react";
import Link from "next/link";

import { DiscountBadge, ListPrice, Price } from "@/components/product/price";
import { ProductImage } from "@/components/product/product-image";
import type { ProductSummary } from "@/lib/api/products";
import { routes } from "@/lib/routes";

export type DealsStatus = "ok" | "empty" | "unavailable";

export function DealsSection({
  status,
  deals,
  variant = "deals",
}: {
  status: DealsStatus;
  deals: ProductSummary[];
  variant?: "deals" | "devices";
}) {
  const isDeviceShowcase = variant === "devices";
  const headingId = isDeviceShowcase ? "devices-heading" : "deals-heading";

  return (
    <section aria-labelledby={headingId}>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-x-5 gap-y-3">
        <div>
          <h2
            id={headingId}
            className="text-2xl font-semibold tracking-[-0.035em] text-ink sm:text-[28px]"
          >
            {isDeviceShowcase ? "Bir sonraki cihazın burada." : "En çok indirimde olanlar"}
          </h2>
          <p className="mt-2 text-sm text-ink-muted">
            {isDeviceShowcase
              ? "Telefon, bilgisayar ve kulaklıklar. Ürününü seç, mağaza fiyatlarını karşılaştır."
              : "Liste fiyatına göre öne çıkan fırsatlar."}
          </p>
        </div>
        <Link
          href={routes.search()}
          className="group inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-ink underline-offset-4 hover:underline"
        >
          Tüm ürünler{" "}
          <ArrowRightIcon
            className="size-4 transition-transform group-hover:translate-x-1"
            aria-hidden
          />
        </Link>
      </div>

      {status === "ok" ? (
        <ul className="grid grid-cols-1 gap-3 min-[360px]:grid-cols-2 sm:gap-4 lg:grid-cols-4">
          {deals.map((product) => (
            <DealCard key={product.id} product={product} />
          ))}
        </ul>
      ) : (
        <div className="flex flex-col items-start gap-5 rounded-lg border border-border-subtle bg-card p-6 sm:flex-row sm:items-center sm:p-8">
          <SearchXIcon className="size-7 shrink-0 text-ink-muted" strokeWidth={1.5} aria-hidden />
          <div className="flex-1">
            <h3 className="text-base font-semibold text-ink">
              {status === "unavailable" ? "Fiyatlara kısa bir ara." : "Yeni fırsatlar yolda."}
            </h3>
            <p className="mt-1.5 max-w-lg text-sm leading-6 text-ink-muted">
              {status === "unavailable"
                ? "Fiyat verilerine şu an ulaşılamıyor. Biraz sonra yeniden deneyebilirsin."
                : "Ürünler eklendiğinde indirimli seçenekleri burada bulabilirsin."}
            </p>
          </div>
          {status === "unavailable" ? (
            <a
              href={routes.home}
              className="inline-flex min-h-11 shrink-0 items-center gap-2 rounded-md border border-border-strong px-4 text-sm font-semibold text-ink transition-colors hover:bg-subtle"
            >
              <RefreshCwIcon className="size-4" aria-hidden /> Yeniden dene
            </a>
          ) : null}
        </div>
      )}
    </section>
  );
}

function DealCard({ product }: { product: ProductSummary }) {
  return (
    <li>
      <Link
        href={routes.product(String(product.id))}
        className="group flex h-full flex-col rounded-lg border border-border-subtle bg-card p-3 transition-colors hover:border-border-strong sm:p-4"
      >
        <div className="flex min-h-6 items-center justify-between gap-2">
          {product.discountPercent ? <DiscountBadge percent={product.discountPercent} /> : <span />}
          {product.inStock === false ? (
            <span className="text-xs text-ink-muted">Stokta yok</span>
          ) : null}
        </div>
        <ProductImage
          src={product.imageUrl}
          alt={product.title}
          sizes="(min-width: 1024px) 22vw, (min-width: 360px) 42vw, 85vw"
          className="my-5 aspect-[4/3] transition-transform duration-300 group-hover:scale-[1.04]"
        />
        <p className="mb-2 text-xs text-ink-muted">
          {product.brand ?? product.category ?? product.store.name}
        </p>
        <h3 className="mb-4 line-clamp-2 min-h-11 text-sm leading-[1.6] font-semibold text-ink">
          {product.title}
        </h3>
        <div className="mt-auto flex flex-col gap-1.5">
          <span className="min-h-5">
            {product.listPrice ? (
              <ListPrice amount={product.listPrice} currency={product.currency} />
            ) : null}
          </span>
          <Price amount={product.price} currency={product.currency} />
        </div>
        <div className="mt-4 flex items-center justify-between gap-2 border-t border-border-subtle pt-3 text-xs">
          <span className="truncate text-ink-muted">{product.store.name}</span>
          <span className="flex shrink-0 items-center gap-1 font-semibold text-ink">
            {product.offerCount > 1 ? `${product.offerCount} teklif` : "İncele"}{" "}
            <ArrowRightIcon className="size-3.5" aria-hidden />
          </span>
        </div>
      </Link>
    </li>
  );
}
