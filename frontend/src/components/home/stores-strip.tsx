import { ArrowUpRightIcon } from "lucide-react";
import Link from "next/link";

import type { StoreSummary } from "@/lib/api/catalog";
import { routes } from "@/lib/routes";

const productCount = new Intl.NumberFormat("tr");

export function StoresStrip({ stores }: { stores: StoreSummary[] }) {
  if (stores.length === 0) {
    return null;
  }

  return (
    <section
      aria-labelledby="stores-heading"
      className="flex flex-col gap-5 border-y border-border-subtle py-6 lg:flex-row lg:items-center lg:gap-8"
    >
      <div className="shrink-0 lg:max-w-48">
        <h2 id="stores-heading" className="text-sm font-semibold text-ink">
          Mağazalar, bir arada.
        </h2>
        <p className="mt-1 text-xs leading-relaxed text-ink-muted">
          Takip ettiğimiz mağazaları incele.
        </p>
      </div>

      <ul className="grid flex-1 grid-cols-2 gap-y-5 sm:flex sm:flex-wrap sm:gap-x-0">
        {stores.map((store) => (
          <li
            key={store.code}
            className="min-w-0 border-l border-border-subtle px-4 sm:flex-1 sm:px-6"
          >
            <Link
              href={routes.merchant(store.code)}
              className="group flex h-full flex-col gap-1 rounded-sm focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ink"
            >
              <span className="flex items-center justify-between gap-3 text-[15px] font-semibold text-ink group-hover:text-ink-muted">
                {store.name}
                <ArrowUpRightIcon className="size-3.5 shrink-0 text-ink-muted" aria-hidden />
              </span>
              <span className="text-xs text-ink-muted tabular-nums">
                {productCount.format(store.productCount)} ürün
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
