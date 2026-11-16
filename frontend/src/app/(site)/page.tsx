import { AlertPromo } from "@/components/home/alert-promo";
import { CategoryRail } from "@/components/home/category-rail";
import { DealsSection, type DealsStatus } from "@/components/home/deals-section";
import { GuidesSection } from "@/components/home/guides-section";
import { HomeHero } from "@/components/home/home-hero";
import { StoresStrip } from "@/components/home/stores-strip";
import { guides } from "@/content/guides";
import { getStores, getTopBrands, getTopCategories } from "@/lib/api/catalog";
import {
  getDeals,
  getProduct,
  searchProducts,
  type PricePoint,
  type ProductSummary,
} from "@/lib/api/products";

const CATEGORY_COUNT = 12;
const POPULAR_BRAND_COUNT = 5;
const DEAL_COUNT = 4;
const DEVICE_COUNT = 9;
const DEVICE_SEARCHES = ["iPhone 17 Pro Max", "Galaxy S25 Ultra", "IdeaPad Slim", "WH-1000XM5"];
const ACCESSORY_TITLE =
  /kılıf|kilif|kapak|koruyucu|adaptör|adaptoru|şarj|sarj|kablo|askı|aski|yedek|standı|standi|silikon|ped[ -]|çantası|cantasi/i;

async function orEmpty<T>(request: Promise<T[]>): Promise<T[]> {
  try {
    return await request;
  } catch {
    return [];
  }
}

async function loadDeals(): Promise<{ status: DealsStatus; deals: ProductSummary[] }> {
  try {
    const deals = await getDeals(DEAL_COUNT);
    return { status: deals.length > 0 ? "ok" : "empty", deals };
  } catch {
    return { status: "unavailable", deals: [] };
  }
}

async function loadHistory(product?: ProductSummary): Promise<PricePoint[]> {
  return product ? orEmpty(getProduct(product.id).then((detail) => detail.priceHistory)) : [];
}

async function loadDevices(): Promise<ProductSummary[]> {
  const groups = await Promise.all(
    DEVICE_SEARCHES.map((q) =>
      orEmpty(searchProducts({ q, size: 36 }).then((result) => result.items)).then((items) =>
        items.filter((product) => product.imageUrl && !ACCESSORY_TITLE.test(product.title)),
      ),
    ),
  );
  const devices: ProductSummary[] = [];
  const seenIds = new Set<number>();
  const seenImages = new Set<string>();

  // Interleave device families so a single model's color variants do not fill the page.
  for (let index = 0; index < 36 && devices.length < DEVICE_COUNT; index++) {
    for (const group of groups) {
      const product = group[index];
      if (!product || seenIds.has(product.id) || seenImages.has(product.imageUrl!)) continue;
      seenIds.add(product.id);
      seenImages.add(product.imageUrl!);
      devices.push(product);
      if (devices.length === DEVICE_COUNT) break;
    }
  }

  return devices;
}

export default async function HomePage() {
  const [{ status, deals }, devices, categories, brands, stores] = await Promise.all([
    loadDeals(),
    loadDevices(),
    orEmpty(getTopCategories(CATEGORY_COUNT)),
    orEmpty(getTopBrands(POPULAR_BRAND_COUNT)),
    orEmpty(getStores()),
  ]);

  const featured = devices[0] ?? deals[0];
  const gridDevices = devices.filter((product) => product.id !== featured?.id);
  const gridDeals = deals.filter((product) => product.id !== featured?.id);
  const history = await loadHistory(featured);

  return (
    <div className="flex flex-col gap-12 pb-4 md:gap-16">
      <HomeHero
        featured={featured}
        history={history}
        storeNames={stores.map((store) => store.name)}
        popularSearches={brands.map((brand) => brand.name)}
      />
      {categories.length > 0 ? <CategoryRail categories={categories} /> : null}
      {gridDevices.length > 0 ? (
        <DealsSection variant="devices" status="ok" deals={gridDevices} />
      ) : null}
      {gridDeals.length > 0 || devices.length === 0 ? (
        <DealsSection status={status} deals={gridDeals} />
      ) : null}
      <AlertPromo />
      {stores.length > 0 ? <StoresStrip stores={stores} /> : null}
      <GuidesSection guides={guides} />
    </div>
  );
}
