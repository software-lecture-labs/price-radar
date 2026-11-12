import { Price } from "@/components/product/price";
import { Sparkline } from "@/components/product/sparkline";
import type { PricePoint } from "@/lib/api/products";
import { currencyLabel, formatAmount } from "@/lib/format";

export function PriceHistory({
  history,
  currency,
  storeName,
}: {
  history: PricePoint[];
  currency?: string;
  storeName: string;
}) {
  const points = history
    .filter(
      (point) =>
        Number.isFinite(point.price) &&
        point.price > 0 &&
        Number.isFinite(Date.parse(point.observedAt)),
    )
    .sort((a, b) => Date.parse(a.observedAt) - Date.parse(b.observedAt));
  const prices = points.map((point) => point.price);
  const low = prices.length ? Math.min(...prices) : null;
  const high = prices.length ? Math.max(...prices) : null;
  const date = (value: string) =>
    new Intl.DateTimeFormat("tr-TR", {
      day: "numeric",
      month: "short",
      year: "numeric",
      timeZone: "Europe/Istanbul",
    }).format(new Date(value));

  return (
    <section
      id="fiyat-gecmisi"
      aria-labelledby="history-heading"
      className="rounded-xl border border-border-subtle bg-card p-6 sm:p-8"
    >
      <h2 id="history-heading" className="text-xl font-semibold tracking-tight text-ink">
        Fiyat geçmişi
      </h2>
      <p className="mt-2 text-sm leading-6 text-ink-muted">
        {storeName} mağazasındaki fiyat gözlemleri.
      </p>
      {points.length >= 2 ? (
        <>
          <figure
            className="mt-6"
            role="img"
            aria-label={`${points.length} fiyat gözlemi. En düşük ${formatAmount(low!)} ${currencyLabel(currency)}, en yüksek ${formatAmount(high!)} ${currencyLabel(currency)}.`}
          >
            <Sparkline prices={prices} className="h-28 w-full text-ink sm:h-36" />
            <figcaption className="mt-4 flex justify-between gap-3 text-xs text-ink-muted">
              <span>{date(points[0].observedAt)}</span>
              <span>{date(points[points.length - 1].observedAt)}</span>
            </figcaption>
          </figure>
          <dl className="mt-6 grid grid-cols-2 gap-4 border-t border-border-subtle pt-5">
            <div>
              <dt className="mb-2 text-xs text-ink-muted">Görülen en düşük</dt>
              <dd>
                <Price amount={low!} currency={currency} size="sm" />
              </dd>
            </div>
            <div>
              <dt className="mb-2 text-xs text-ink-muted">Görülen en yüksek</dt>
              <dd>
                <Price amount={high!} currency={currency} size="sm" />
              </dd>
            </div>
          </dl>
        </>
      ) : (
        <p className="mt-6 rounded-lg bg-subtle p-5 text-sm leading-6 text-ink-muted">
          Grafik için henüz yeterli fiyat gözlemi yok. Yeni fiyatlar kaydedildikçe geçmiş burada
          görünecek.
        </p>
      )}
    </section>
  );
}
