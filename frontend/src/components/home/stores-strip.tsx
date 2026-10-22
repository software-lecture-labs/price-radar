import Link from "next/link";

import type { StoreSummary } from "@/lib/api/catalog";
import { routes } from "@/lib/routes";

export function StoresStrip({ stores }: { stores: StoreSummary[] }) {
  return (
    <section aria-labelledby="stores-heading" className="flex flex-col items-center gap-6">
      <h2 id="stores-heading" className="text-sm text-ink-muted">
        Fiyatlarını takip ettiğimiz mağazalar
      </h2>
      <ul className="flex flex-wrap items-center justify-center gap-x-12 gap-y-4">
        {stores.map((store) => (
          <li key={store.code}>
            <Link
              href={routes.merchant(store.code)}
              className="font-mono text-sm font-semibold tracking-wider text-ink-secondary uppercase transition-colors hover:text-ink"
            >
              {store.name}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
