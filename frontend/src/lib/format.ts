const wholeAmount = new Intl.NumberFormat("tr-TR", { maximumFractionDigits: 0 });
const centAmount = new Intl.NumberFormat("tr-TR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatAmount(value: number): string {
  return Number.isInteger(value) ? wholeAmount.format(value) : centAmount.format(value);
}

// All imported stores sell in lira; Trendyol omits the currency code.
export function currencyLabel(currency?: string): string {
  return !currency || currency === "TRY" ? "TL" : currency;
}
