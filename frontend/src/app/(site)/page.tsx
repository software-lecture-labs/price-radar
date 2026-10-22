import { AlertPromo } from "@/components/home/alert-promo";
import { CategoryRail } from "@/components/home/category-rail";
import { DealsSection, type DealsStatus } from "@/components/home/deals-section";
import { GuidesSection } from "@/components/home/guides-section";
import { HomeHero } from "@/components/home/home-hero";
import { StoresStrip } from "@/components/home/stores-strip";
import { guides } from "@/content/guides";
import { getStores, getTopBrands, getTopCategories } from "@/lib/api/catalog";
import { getDeals, getProduct, type PricePoint, type ProductSummary } from "@/lib/api/products";

const CATEGORY_COUNT = 12;
const POPULAR_BRAND_COUNT = 5;
const DEAL_COUNT = 6;

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

export default async function HomePage() {
  const [{ status, deals }, categories, brands, stores] = await Promise.all([
    loadDeals(),
    orEmpty(getTopCategories(CATEGORY_COUNT)),
    orEmpty(getTopBrands(POPULAR_BRAND_COUNT)),
    orEmpty(getStores()),
  ]);

  const [featured, ...rest] = deals;
  const gridDeals = rest.length > 0 ? rest : deals;
  const history = await loadHistory(featured);

  return (
    <div className="flex flex-col gap-16 md:gap-24">
      <HomeHero
        featured={featured}
        history={history}
        storeNames={stores.map((store) => store.name)}
        popularSearches={brands.map((brand) => brand.name)}
      />
      {categories.length > 0 ? <CategoryRail categories={categories} /> : null}
      <DealsSection status={status} deals={gridDeals} />
      <AlertPromo product={featured} />
      {stores.length > 0 ? <StoresStrip stores={stores} /> : null}
      <GuidesSection guides={guides} />
    </div>
  );
}
