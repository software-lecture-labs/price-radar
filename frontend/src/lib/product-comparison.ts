import type { ProductComparisonOffer, ProductSummary, StoreRef } from "./api/products";

export const COMPARISON_STORES = [
  { code: "trendyol", name: "Trendyol" },
  { code: "hepsiburada", name: "Hepsiburada" },
  { code: "n11", name: "N11" },
  { code: "teknosa", name: "Teknosa" },
] as const satisfies readonly StoreRef[];

const storeDomains: Record<string, string> = {
  trendyol: "trendyol.com",
  hepsiburada: "hepsiburada.com",
  n11: "n11.com",
  teknosa: "teknosa.com",
};

export interface PreparedOffer extends ProductComparisonOffer {
  /** Validated destination, suitable for an external purchase link. */
  productUrl: string | null;
  normalizedCurrency: string;
  hasPrice: boolean;
  /** The displayed price is in the current product's currency. */
  comparable: boolean;
  /** A real price and destination exist, and the listing is not out of stock. */
  available: boolean;
  isBest: boolean;
}

export interface PreparedComparison {
  rows: PreparedOffer[];
  bestOffer: PreparedOffer | null;
  availableCount: number;
}

/** Do not turn a marketplace homepage, search page, or foreign URL into a buy link. */
export function getStoreProductUrl(
  storeCode: string,
  value: string | null | undefined,
): string | null {
  const domain = storeDomains[storeCode.toLowerCase()];
  if (!domain || !value || /[\u0000-\u001f\u007f]/.test(value)) return null;

  try {
    const url = new URL(value);
    if (url.protocol !== "https:" && url.protocol !== "http:") return null;
    if (url.username || url.password || url.port) return null;
    if (url.hostname !== domain && !url.hostname.endsWith(`.${domain}`)) return null;

    const path = decodeURIComponent(url.pathname).replace(/\/+$/, "");
    if (!path || /^\/(?:sr|ara|arama|search|login|giris|sepet|cart)(?:\/|$)/i.test(path)) {
      return null;
    }

    return url.href;
  } catch {
    return null;
  }
}

function normalizeCurrency(currency: string | null | undefined): string {
  const normalized = currency?.trim().toUpperCase() || "TRY";
  return normalized === "TL" ? "TRY" : normalized;
}

function emptyOffer(store: StoreRef): ProductComparisonOffer {
  return {
    store,
    productId: null,
    title: null,
    price: null,
    listPrice: null,
    currency: null,
    inStock: null,
    freeShipping: null,
    url: null,
    observedAt: null,
    sellerName: null,
    offerCount: 0,
    matchType: null,
  };
}

function fallbackOffer(product: ProductSummary): ProductComparisonOffer {
  return {
    store: product.store,
    productId: product.id,
    title: product.title,
    price: product.price,
    listPrice: product.listPrice ?? null,
    currency: product.currency ?? null,
    inStock: product.inStock ?? null,
    freeShipping: null,
    url: product.url,
    observedAt: product.observedAt,
    sellerName: null,
    offerCount: product.offerCount,
    matchType: "listing",
  };
}

/** Keep all four stores visible; only existing, buyable listings compete on price. */
export function prepareComparison(
  offers: readonly ProductComparisonOffer[] | null | undefined,
  product: ProductSummary,
): PreparedComparison {
  const source = offers?.length ? offers : [fallbackOffer(product)];
  const currency = normalizeCurrency(product.currency);
  const storeOrder = new Map<string, number>(
    COMPARISON_STORES.map((store, index) => [store.code, index]),
  );

  function prepare(offer: ProductComparisonOffer, store: StoreRef): PreparedOffer {
    const hasPrice =
      typeof offer.price === "number" && Number.isFinite(offer.price) && offer.price > 0;
    const normalizedCurrency = normalizeCurrency(offer.currency);
    const productUrl = getStoreProductUrl(store.code, offer.url);
    return {
      ...offer,
      store,
      productUrl,
      normalizedCurrency,
      hasPrice,
      comparable: hasPrice && normalizedCurrency === currency,
      available: hasPrice && productUrl !== null && offer.inStock !== false,
      isBest: false,
    };
  }

  function rank(offer: PreparedOffer): number {
    if (offer.available) return offer.comparable ? 0 : 1;
    if (offer.hasPrice && offer.productUrl) return offer.comparable ? 2 : 3;
    return 4;
  }

  function compare(a: PreparedOffer, b: PreparedOffer): number {
    const aRank = rank(a);
    const rankDifference = aRank - rank(b);
    if (rankDifference) return rankDifference;
    // Prices in different currencies have no meaningful numerical ordering.
    if ((aRank === 0 || aRank === 2) && a.comparable && b.comparable) {
      const priceDifference = a.price! - b.price!;
      if (priceDifference) return priceDifference;
    }
    return (storeOrder.get(a.store.code) ?? 0) - (storeOrder.get(b.store.code) ?? 0);
  }

  const rows = COMPARISON_STORES.map((store) => {
    const candidates = source
      .filter((offer) => offer.store.code.toLowerCase() === store.code)
      .map((offer) => prepare(offer, store))
      .sort(compare);
    return candidates[0] ?? prepare(emptyOffer(store), store);
  }).sort(compare);

  const bestOffer = rows.find((offer) => offer.available && offer.comparable) ?? null;
  if (bestOffer) bestOffer.isBest = true;

  return {
    rows,
    bestOffer,
    availableCount: rows.filter((offer) => offer.available).length,
  };
}
