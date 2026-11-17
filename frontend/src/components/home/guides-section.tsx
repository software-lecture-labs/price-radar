import { ArrowUpRightIcon } from "lucide-react";
import Link from "next/link";

import { ProductImage } from "@/components/product/product-image";
import type { Guide } from "@/content/guides";
import { routes } from "@/lib/routes";

export function GuidesSection({ guides }: { guides: Guide[] }) {
  const [main, ...others] = guides;
  if (!main) return null;

  return (
    <section aria-labelledby="guides-heading">
      <h2
        id="guides-heading"
        className="text-2xl font-semibold tracking-[-0.035em] text-ink sm:text-[28px]"
      >
        Biraz ilham
      </h2>
      <p className="mt-2 mb-6 text-sm text-ink-muted">Aklındaki ihtiyaca bir yerden başla.</p>
      <div className="grid gap-5 lg:grid-cols-[1.15fr_1fr]">
        <Link
          href={routes.search(main.searchQuery)}
          className="group flex flex-col justify-between gap-5 rounded-xl bg-radar-lime-surface p-6 sm:flex-row sm:items-center sm:p-8"
        >
          <div className="sm:w-1/2">
            <p className="mb-3 text-xs font-medium text-ink-secondary">{main.category}</p>
            <h3 className="max-w-[16ch] text-[28px] leading-[1.15] font-semibold tracking-[-0.04em] text-ink">
              {main.title}
            </h3>
            <p className="mt-3 text-sm leading-6 text-ink-muted">{main.summary}</p>
            <span className="mt-6 inline-flex min-h-11 items-center gap-3 text-sm font-semibold text-ink">
              Modelleri keşfet{" "}
              <ArrowUpRightIcon
                className="size-4 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"
                aria-hidden
              />
            </span>
          </div>
          <ProductImage
            src={main.imageUrl}
            alt={main.imageAlt}
            sizes="(min-width: 1024px) 20vw, (min-width: 640px) 35vw, 70vw"
            className="h-44 w-full transition-transform duration-300 group-hover:scale-[1.03] sm:h-56 sm:w-1/2"
          />
        </Link>
        <div className="flex flex-col divide-y divide-border-subtle">
          {others.map((guide) => (
            <Link
              key={guide.searchQuery}
              href={routes.search(guide.searchQuery)}
              className="group flex flex-1 items-center justify-between gap-4 py-5 first:pt-0 last:pb-0"
            >
              <div className="min-w-0">
                <p className="mb-2 text-xs text-ink-muted">{guide.category}</p>
                <h3 className="max-w-[22ch] text-xl leading-[1.3] font-semibold tracking-[-0.025em] text-ink">
                  {guide.title}
                </h3>
                <span className="mt-3 inline-flex min-h-10 items-center gap-2 text-xs font-semibold text-ink">
                  Modelleri keşfet{" "}
                  <ArrowUpRightIcon
                    className="size-4 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"
                    aria-hidden
                  />
                </span>
              </div>
              <ProductImage
                src={guide.imageUrl}
                alt={guide.imageAlt}
                sizes="(min-width: 640px) 144px, 104px"
                className="h-36 w-24 shrink-0 transition-transform duration-300 group-hover:scale-[1.04] sm:w-36"
              />
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
