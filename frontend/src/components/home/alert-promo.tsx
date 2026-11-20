import { ArrowUpRightIcon, BellRingIcon } from "lucide-react";
import Link from "next/link";

import { routes } from "@/lib/routes";

export function AlertPromo() {
  return (
    <section
      aria-labelledby="alert-heading"
      className="flex flex-col gap-6 rounded-xl bg-ink px-6 py-7 text-white sm:flex-row sm:items-center sm:justify-between sm:px-8 lg:px-10"
    >
      <div className="flex items-start gap-5 sm:items-center">
        <span className="hidden size-12 shrink-0 items-center justify-center rounded-full border border-white/25 text-radar-lime sm:flex">
          <BellRingIcon className="size-5" strokeWidth={1.5} aria-hidden />
        </span>
        <div>
          <h2 id="alert-heading" className="text-xl font-semibold tracking-tight sm:text-2xl">
            Acele etme. Fiyatı takip et.
          </h2>
          <p className="mt-2 max-w-xl text-sm leading-6 text-white/75">
            Aklındaki ürünün fiyatını takip etmek için kendine zaman tanı.
          </p>
        </div>
      </div>
      <Link
        href={routes.alerts}
        className="inline-flex min-h-12 shrink-0 items-center justify-center gap-5 rounded-md bg-radar-lime px-5 text-sm font-semibold text-ink transition-colors hover:bg-white focus-visible:outline-radar-lime"
      >
        Fiyat alarmları <ArrowUpRightIcon className="size-4" aria-hidden />
      </Link>
    </section>
  );
}
