import { ArrowRightIcon, RefreshCwIcon, SearchXIcon } from "lucide-react";
import Link from "next/link";

import { ProductGrid } from "@/components/product/product-card";
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
        <ProductGrid products={deals} />
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
