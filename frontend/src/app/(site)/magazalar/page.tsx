import { ArrowRightIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";

import { EmptyState } from "@/components/product/empty-state";
import { getStores, type StoreSummary } from "@/lib/api/catalog";
import { routes } from "@/lib/routes";

export const metadata: Metadata = { title: "Mağazalar" };

export default async function StoresPage() {
  const stores = await loadStores();

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight sm:text-[28px]">Mağazalar</h1>
        <p className="mt-2 text-sm text-ink-muted">
          Fiyatlarını takip ettiğimiz mağazalar. Bir mağazayı seçip ürünlerine bakabilirsin.
        </p>
      </div>

      {!stores ? (
        <EmptyState
          title="Mağazalara şu an ulaşılamıyor."
          text="Biraz sonra yeniden deneyebilirsin."
        />
      ) : stores.length === 0 ? (
        <EmptyState
          title="Henüz mağaza yok."
          text="Ürünler yüklendiğinde mağazalar burada görünecek."
        />
      ) : (
        <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4 lg:grid-cols-4">
          {stores.map((store) => (
            <li key={store.code}>
              <Link
                href={routes.merchant(store.code)}
                className="group flex h-full flex-col gap-1 rounded-lg border border-border-subtle bg-card p-5 transition-colors hover:border-border-strong"
              >
                <span className="text-lg font-semibold text-ink">{store.name}</span>
                <span className="text-sm text-ink-muted">
                  {store.productCount.toLocaleString("tr-TR")} ürün
                </span>
                <span className="mt-4 flex items-center gap-1 text-sm font-semibold text-ink">
                  Ürünleri gör{" "}
                  <ArrowRightIcon
                    className="size-4 transition-transform group-hover:translate-x-1"
                    aria-hidden
                  />
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

async function loadStores(): Promise<StoreSummary[] | null> {
  try {
    return await getStores();
  } catch {
    return null;
  }
}
