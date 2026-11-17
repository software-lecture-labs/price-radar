const wholeAmount = new Intl.NumberFormat("tr-TR", { maximumFractionDigits: 0 });
const centAmount = new Intl.NumberFormat("tr-TR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatAmount(value: number): string {
  return Number.isInteger(value) ? wholeAmount.format(value) : centAmount.format(value);
}

const longDate = new Intl.DateTimeFormat("tr-TR", { dateStyle: "long" });

export function formatLongDate(iso: string): string {
  return longDate.format(new Date(iso));
}

/** +905321234567 → +90 532 123 45 67; anything else is returned unchanged. */
export function formatPhone(e164: string): string {
  const match = /^\+90(\d{3})(\d{3})(\d{2})(\d{2})$/.exec(e164);
  return match ? `+90 ${match.slice(1).join(" ")}` : e164;
}

// All imported stores sell in lira; Trendyol omits the currency code.
export function currencyLabel(currency?: string): string {
  return !currency || currency === "TRY" ? "TL" : currency;
}
