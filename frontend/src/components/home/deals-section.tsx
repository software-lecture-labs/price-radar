import { ArrowRightIcon } from "lucide-react";
import Link from "next/link";

import { DiscountBadge, ListPrice, Price } from "@/components/product/price";
import { ProductImage } from "@/components/product/product-image";
import type { ProductSummary } from "@/lib/api/products";
import { routes } from "@/lib/routes";
import { cn } from "@/lib/utils";

export type DealsStatus = "ok" | "empty" | "unavailable";

const emptyMessages: Record<Exclude<DealsStatus, "ok">, string> = {
  empty: "Henüz ürün yok. Fiyatlar içe aktarıldıkça burada görünecek.",
  unavailable: "Fiyat verilerine şu an ulaşılamıyor.",
};

export function DealsSection({ status, deals }: { status: DealsStatus; deals: ProductSummary[] }) {
  return (
    <section aria-labelledby="deals-heading" className="flex flex-col gap-6">
      <div className="flex flex-col justify-between gap-2 md:flex-row md:items-end">
        <div>
          <p className="mb-1 font-mono text-xs font-medium tracking-wider text-ink-muted uppercase">
            Liste fiyatına göre
          </p>
          <h2 id="deals-heading" className="text-[26px] font-semibold tracking-tight text-ink">
            En çok indirimde olanlar
          </h2>
        </div>
        <Link
          href={routes.search()}
          className="group inline-flex items-center gap-1.5 text-sm font-semibold text-ink hover:text-ink-secondary"
        >
          Tüm ürünler
          <ArrowRightIcon
            className="size-[18px] transition-transform group-hover:translate-x-1"
            aria-hidden
          />
        </Link>
      </div>

      {status === "ok" ? (
        <ul className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {deals.map((product, index) => (
            <DealCard key={product.id} product={product} featured={index === 0} />
          ))}
        </ul>
      ) : (
        <p className="rounded-[14px] border border-dashed border-border-strong bg-card px-6 py-12 text-center text-sm text-ink-muted">
          {emptyMessages[status]}
        </p>
      )}
    </section>
  );
}

function DealCard({ product, featured }: { product: ProductSummary; featured: boolean }) {
  return (
    <li className={cn(featured && "md:col-span-2 lg:row-span-2")}>
      <Link
        href={routes.product(String(product.id))}
        className={cn(
          "group flex h-full flex-col justify-between rounded-[14px]",
          featured ? "bg-drop/50 p-6 sm:p-8" : "bg-tile p-5",
        )}
      >
        <div className="flex items-start justify-between gap-2">
          {product.discountPercent ? <DiscountBadge percent={product.discountPercent} /> : <span />}
          <span className="text-xs text-ink-muted">{product.store.name}</span>
        </div>

        <ProductImage
          src={product.imageUrl}
          alt=""
          sizes={featured ? "(min-width: 768px) 40vw, 90vw" : "(min-width: 1024px) 20vw, 45vw"}
          className={cn(
            "transition-transform group-hover:scale-105",
            featured ? "my-8 h-56" : "my-3 h-28",
          )}
        />

        <div className="flex flex-col gap-1.5">
          <h3
            className={cn(
              "font-semibold text-ink",
              featured ? "line-clamp-2 text-lg" : "truncate text-[15px]",
            )}
          >
            {product.title}
          </h3>
          <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
            {product.listPrice ? (
              <ListPrice
                amount={product.listPrice}
                currency={product.currency}
                className={featured ? "text-[15px]" : undefined}
              />
            ) : null}
            <Price
              amount={product.price}
              currency={product.currency}
              size={featured ? "lg" : "md"}
            />
          </div>
        </div>
      </Link>
    </li>
  );
}
