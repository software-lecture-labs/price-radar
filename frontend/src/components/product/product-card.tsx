import { ArrowRightIcon } from "lucide-react";
import Link from "next/link";

import { DiscountBadge, ListPrice, Price } from "@/components/product/price";
import { ProductImage } from "@/components/product/product-image";
import type { ProductSummary } from "@/lib/api/products";
import { routes } from "@/lib/routes";

export function ProductGrid({ products }: { products: ProductSummary[] }) {
  return (
    <ul className="grid grid-cols-1 gap-3 min-[360px]:grid-cols-2 sm:gap-4 lg:grid-cols-4">
      {products.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </ul>
  );
}

export function ProductCard({ product }: { product: ProductSummary }) {
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
