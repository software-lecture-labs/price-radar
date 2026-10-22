import {
  BellPlusIcon,
  BellRingIcon,
  PackageIcon,
  SlidersHorizontalIcon,
  ZapIcon,
} from "lucide-react";
import Link from "next/link";

import { Price } from "@/components/product/price";
import type { ProductSummary } from "@/lib/api/products";
import { currencyLabel, formatAmount } from "@/lib/format";
import { routes } from "@/lib/routes";

const channels = ["E-posta", "Bildirim", "İkisi de"];

export function AlertPromo({ product }: { product?: ProductSummary }) {
  return (
    <section aria-labelledby="alert-heading" className="rounded-[14px] bg-panel p-6 sm:p-10">
      <div className="grid grid-cols-1 items-center gap-8 lg:grid-cols-12 lg:gap-12">
        <div className="flex flex-col items-start lg:col-span-5">
          <span className="mb-4 inline-flex items-center gap-2 rounded-full bg-card px-3 py-1 font-mono text-[11px] font-semibold text-ink">
            <BellRingIcon className="size-3.5" aria-hidden />
            Otomatik takip
          </span>
          <h2
            id="alert-heading"
            className="mb-4 text-[28px] leading-tight font-semibold tracking-tight text-ink sm:text-[32px]"
          >
            Fiyat düşünce biz haber veririz.
          </h2>
          <p className="mb-8 text-base leading-relaxed text-ink-muted">
            Hedef fiyatını yaz, ürün o fiyata indiğinde e-posta ya da bildirim al.
          </p>
          <Link
            href={routes.alerts}
            className="inline-flex h-11 items-center gap-2 rounded-[10px] bg-ink px-6 text-sm font-semibold text-white transition-colors hover:bg-ink-secondary"
          >
            <BellPlusIcon className="size-[18px]" aria-hidden />
            Fiyat alarmı kur
          </Link>
        </div>

        <div className="lg:col-span-7">
          <AlertFormPreview product={product} />
        </div>
      </div>
    </section>
  );
}

function AlertFormPreview({ product }: { product?: ProductSummary }) {
  const target = product ? Math.floor((product.price * 0.9) / 100) * 100 : undefined;

  return (
    <div
      role="img"
      aria-label="Örnek fiyat alarmı: hedef fiyat ve bildirim yöntemi seçimi"
      className="flex flex-col gap-5 rounded-[14px] bg-card p-6"
    >
      <div className="flex items-center gap-3.5">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-[10px] bg-tile text-ink-secondary">
          <PackageIcon className="size-6" strokeWidth={1.5} />
        </span>
        <div className="min-w-0">
          <p className="truncate text-[15px] font-semibold text-ink">
            {product?.title ?? "Takip ettiğin ürün"}
          </p>
          <p className="flex items-baseline gap-1 text-xs text-ink-muted">
            Şu an:
            {product ? (
              <Price amount={product.price} currency={product.currency} size="sm" />
            ) : (
              " -"
            )}
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <span className="text-sm font-medium text-ink">Hedef fiyat</span>
        <div className="flex h-10 items-center justify-between rounded-[10px] bg-subtle px-3.5">
          {target !== undefined ? (
            <Price amount={target} currency={product?.currency} size="sm" />
          ) : (
            <span className="font-mono text-sm text-ink-muted">-</span>
          )}
          <SlidersHorizontalIcon className="size-[18px] text-ink-muted" />
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <span className="text-sm font-medium text-ink">Bildirim yöntemi</span>
        <div className="grid grid-cols-3 gap-1.5 rounded-[10px] bg-subtle p-1">
          {channels.map((channel) => (
            <span
              key={channel}
              className={
                channel === "Bildirim"
                  ? "rounded-[8px] bg-ink py-1.5 text-center text-[13px] font-medium text-white"
                  : "py-1.5 text-center text-[13px] text-ink-muted"
              }
            >
              {channel}
            </span>
          ))}
        </div>
      </div>

      {product && target !== undefined ? (
        <p className="flex items-center gap-2 border-l-2 border-radar-lime bg-subtle px-3 py-2.5 text-xs text-ink-secondary">
          <ZapIcon className="size-3.5 shrink-0" />
          <span className="truncate">
            Fiyat düştü: {product.title} şimdi{" "}
            <span className="font-mono font-semibold tabular-nums">
              {formatAmount(target)} {currencyLabel(product.currency)}
            </span>
          </span>
        </p>
      ) : null}
    </div>
  );
}
