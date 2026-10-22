"use client";

import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import Link from "next/link";
import { useRef } from "react";

import { ProductImage } from "@/components/product/product-image";
import type { CategorySummary } from "@/lib/api/catalog";
import { routes } from "@/lib/routes";

export function CategoryRail({ categories }: { categories: CategorySummary[] }) {
  const rail = useRef<HTMLUListElement>(null);

  function scroll(direction: 1 | -1) {
    const element = rail.current;
    if (!element) {
      return;
    }
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    element.scrollBy({
      left: direction * element.clientWidth * 0.8,
      behavior: reduceMotion ? "auto" : "smooth",
    });
  }

  return (
    <section aria-labelledby="categories-heading" className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h2 id="categories-heading" className="text-[26px] font-semibold tracking-tight text-ink">
          Kategoriler
        </h2>
        <div className="flex gap-2">
          <RailButton label="Önceki kategoriler" onClick={() => scroll(-1)}>
            <ChevronLeftIcon className="size-[18px]" aria-hidden />
          </RailButton>
          <RailButton label="Sonraki kategoriler" onClick={() => scroll(1)}>
            <ChevronRightIcon className="size-[18px]" aria-hidden />
          </RailButton>
        </div>
      </div>

      <ul
        ref={rail}
        className="-mx-4 flex snap-x snap-mandatory scroll-px-4 [scrollbar-width:none] gap-4 overflow-x-auto px-4 pb-2 [&::-webkit-scrollbar]:hidden"
      >
        {categories.map((category) => (
          <li key={category.name} className="shrink-0 snap-start">
            <Link
              href={routes.category(category.name)}
              className="group flex w-[150px] flex-col gap-3 rounded-[14px] border border-border-subtle bg-card p-3 transition-colors hover:border-border-strong sm:w-[164px]"
            >
              <span className="flex aspect-[4/5] items-center justify-center rounded-[12px] bg-tile p-4 transition-transform group-hover:scale-[0.98]">
                <ProductImage src={category.imageUrl} alt="" sizes="132px" className="size-full" />
              </span>
              <span className="flex flex-col items-center gap-0.5">
                <span className="w-full truncate text-center text-[15px] font-semibold text-ink">
                  {category.name}
                </span>
                <span className="font-mono text-xs text-ink-muted tabular-nums">
                  {category.productCount} ürün
                </span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

function RailButton({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="flex size-8 items-center justify-center rounded-full border border-border-subtle bg-card text-ink-muted transition-colors hover:bg-subtle hover:text-ink"
    >
      {children}
    </button>
  );
}
