import { ArrowUpRightIcon, CheckIcon, MinusIcon, PackageCheckIcon } from "lucide-react";

import { Price } from "@/components/product/price";
import type { prepareComparison } from "@/lib/product-comparison";
import { cn } from "@/lib/utils";

type Comparison = ReturnType<typeof prepareComparison>;

export function observedDate(value?: string | null): string | null {
  if (!value || !Number.isFinite(Date.parse(value))) return null;
  return new Intl.DateTimeFormat("tr-TR", {
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Europe/Istanbul",
  }).format(new Date(value));
}

export function StoreComparison({ comparison }: { comparison: Comparison }) {
  return (
    <section id="magaza-fiyatlari" aria-labelledby="comparison-heading">
      <div className="mb-6">
        <h2
          id="comparison-heading"
          className="text-2xl font-semibold tracking-tight text-ink sm:text-[28px]"
        >
          4 mağazada karşılaştır
        </h2>
        <p className="mt-2 text-sm leading-6 text-ink-muted">
          Aynı ürün için bulunan mağaza fiyatları. Satın almak için mağazanın ürün sayfasına geç.
        </p>
      </div>
      <div
        aria-hidden
        className="hidden grid-cols-12 gap-5 px-6 pb-3 text-xs font-medium text-ink-muted md:grid"
      >
        <span className="col-span-4">Mağaza / satıcı</span>
        <span className="col-span-3">Stok ve kargo</span>
        <span className="col-span-3">Ürün fiyatı</span>
        <span className="col-span-2 text-right">Mağazaya git</span>
      </div>
      <ul className="overflow-hidden rounded-xl border border-border-subtle bg-card">
        {comparison.rows.map((offer) => {
          const date = observedDate(offer.observedAt);
          return (
            <li
              key={offer.store.code}
              className={cn(
                "grid gap-4 border-b border-border-subtle p-5 last:border-0 sm:p-6 md:grid-cols-12 md:items-center md:gap-5",
                offer.isBest && "bg-radar-lime-surface/70",
              )}
            >
              <div className="md:col-span-4">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                  <h3 className="text-base font-bold tracking-tight text-ink">
                    {offer.store.name}
                  </h3>
                  {offer.isBest ? (
                    <span className="inline-flex items-center gap-1 rounded bg-ink px-2 py-1 text-[11px] font-semibold text-white">
                      <CheckIcon className="size-3" aria-hidden /> En düşük fiyat
                    </span>
                  ) : null}
                </div>
                {offer.sellerName ? (
                  <p className="mt-2 text-xs text-ink-muted">Satıcı: {offer.sellerName}</p>
                ) : null}
                {date ? (
                  <p className="mt-2 text-xs text-ink-muted">
                    Son gözlem: <time dateTime={offer.observedAt!}>{date}</time>
                  </p>
                ) : null}
              </div>
              <div className="text-xs leading-6 text-ink-muted md:col-span-3">
                {offer.hasPrice ? (
                  <>
                    <p
                      className={cn(
                        "flex items-center gap-1.5 font-medium",
                        offer.inStock !== false && "text-ink-secondary",
                      )}
                    >
                      {offer.inStock === true ? (
                        <PackageCheckIcon className="size-3.5" aria-hidden />
                      ) : offer.inStock === false ? (
                        <MinusIcon className="size-3.5" aria-hidden />
                      ) : null}
                      {offer.inStock === true
                        ? "Stokta"
                        : offer.inStock === false
                          ? "Stokta yok"
                          : "Stok bilgisi mağazada"}
                    </p>
                    <p>
                      {offer.freeShipping === true
                        ? "Ücretsiz kargo"
                        : offer.freeShipping === false
                          ? "Kargo ücreti mağazada"
                          : "Kargo bilgisi mağazada"}
                    </p>
                  </>
                ) : (
                  <p>Bu ürün için eşleşme bulunamadı.</p>
                )}
              </div>
              <div className="md:col-span-3">
                {offer.hasPrice ? (
                  <div>
                    <span className="sr-only">{offer.store.name} fiyatı: </span>
                    <Price amount={offer.price!} currency={offer.normalizedCurrency} />
                    {!offer.comparable ? (
                      <p className="mt-1 text-xs text-ink-muted">Farklı para birimi</p>
                    ) : null}
                  </div>
                ) : (
                  <span className="text-sm text-ink-muted">Fiyat bulunamadı</span>
                )}
              </div>
              <div className="md:col-span-2">
                {offer.productUrl ? (
                  <a
                    href={offer.productUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={`${offer.store.name} mağazasında ürünü gör (yeni sekme)`}
                    className={cn(
                      "inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-md border px-3 text-xs font-semibold transition-colors",
                      offer.isBest
                        ? "border-ink bg-ink text-white hover:bg-ink-secondary"
                        : "border-border-strong text-ink hover:bg-subtle",
                    )}
                  >
                    Mağazada gör <ArrowUpRightIcon className="size-4 shrink-0" aria-hidden />
                  </a>
                ) : (
                  <span className="inline-flex min-h-11 w-full items-center justify-center rounded-md bg-subtle px-3 text-xs text-ink-muted">
                    Teklif yok
                  </span>
                )}
              </div>
            </li>
          );
        })}
      </ul>
      <p className="mt-4 text-xs leading-6 text-ink-muted">
        Fiyatlar son gözlem zamanına aittir. Güncel fiyat ve kargo tutarı mağazanın ürün sayfasında
        gösterilir.
      </p>
    </section>
  );
}
