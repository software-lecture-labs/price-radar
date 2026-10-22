"use client";

import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";

import { ProductImage } from "@/components/product/product-image";
import type { CategorySummary } from "@/lib/api/catalog";
import { routes } from "@/lib/routes";

export function CategoryRail({ categories }: { categories: CategorySummary[] }) {
  const rail = useRef<HTMLUListElement>(null);
  const [canScroll, setCanScroll] = useState({ previous: false, next: false });

  function updateScrollState() {
    const element = rail.current;
    if (!element) return;
    setCanScroll({
      previous: element.scrollLeft > 2,
      next: element.scrollLeft + element.clientWidth < element.scrollWidth - 2,
    });
  }

  useEffect(() => {
    const element = rail.current;
    if (!element) return;
    const observer = new ResizeObserver(updateScrollState);
    observer.observe(element);
    return () => observer.disconnect();
  }, [categories.length]);

  function scroll(direction: 1 | -1) {
    const element = rail.current;
    if (!element) return;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    element.scrollBy({
      left: direction * element.clientWidth * 0.8,
      behavior: reduceMotion ? "auto" : "smooth",
    });
  }

  return (
    <section aria-labelledby="categories-heading" className="border-y border-border-subtle py-6">
      <div className="mb-5 flex items-center justify-between gap-4">
        <h2 id="categories-heading" className="text-lg font-semibold tracking-tight text-ink">
          Neye bakmıştın?
        </h2>
        <div className="flex gap-2">
          <RailButton
            label="Önceki kategoriler"
            disabled={!canScroll.previous}
            onClick={() => scroll(-1)}
          >
            <ChevronLeftIcon className="size-4" aria-hidden />
          </RailButton>
          <RailButton
            label="Sonraki kategoriler"
            disabled={!canScroll.next}
            onClick={() => scroll(1)}
          >
            <ChevronRightIcon className="size-4" aria-hidden />
          </RailButton>
        </div>
      </div>
      <ul
        ref={rail}
        onScroll={updateScrollState}
        className="-mx-2 flex snap-x snap-mandatory [scrollbar-width:none] gap-3 overflow-x-auto px-2 pb-2 [&::-webkit-scrollbar]:hidden"
      >
        {categories.map((category) => (
          <li key={category.name} className="w-32 shrink-0 snap-start sm:w-36">
            <Link
              href={routes.category(category.name)}
              className="group flex h-full flex-col items-center rounded-lg px-2 py-1"
            >
              <span className="mb-3 flex size-20 items-center justify-center rounded-full bg-card p-3 transition-colors group-hover:bg-panel">
                <ProductImage
                  src={category.imageUrl}
                  alt=""
                  sizes="64px"
                  className="size-full transition-transform duration-200 group-hover:scale-105"
                />
              </span>
              <span className="line-clamp-2 min-h-10 text-center text-[13px] leading-5 font-semibold text-ink">
                {category.name}
              </span>
              <span className="mt-1 text-xs text-ink-muted">
                {category.productCount.toLocaleString("tr-TR")} ürün
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
  disabled,
  children,
}: {
  label: string;
  onClick: () => void;
  disabled: boolean;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      disabled={disabled}
      className="flex size-11 items-center justify-center rounded-full border border-border-strong text-ink transition-colors hover:bg-card disabled:cursor-default disabled:border-border-subtle disabled:opacity-35"
    >
      {children}
    </button>
  );
}
