import Link from "next/link";

import type { Guide } from "@/content/guides";
import { routes } from "@/lib/routes";
import { cn } from "@/lib/utils";

export function GuidesSection({ guides }: { guides: Guide[] }) {
  const [main, ...others] = guides;
  if (!main) {
    return null;
  }

  return (
    <section aria-labelledby="guides-heading" className="flex flex-col gap-6">
      <h2 id="guides-heading" className="text-[26px] font-semibold tracking-tight text-ink">
        Satın almadan önce oku
      </h2>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <GuideCard guide={main} large className="lg:col-span-7" />
        <div className="flex flex-col gap-4 lg:col-span-5">
          {others.map((guide) => (
            <GuideCard key={guide.searchQuery} guide={guide} className="flex-1" />
          ))}
        </div>
      </div>
    </section>
  );
}

function GuideCard({
  guide,
  large = false,
  className,
}: {
  guide: Guide;
  large?: boolean;
  className?: string;
}) {
  const Icon = guide.icon;

  return (
    <Link
      href={routes.search(guide.searchQuery)}
      className={cn(
        "group flex gap-5 rounded-[14px] border border-border-subtle bg-card p-5 transition-colors hover:border-border-strong",
        large ? "flex-col" : "items-center",
        className,
      )}
    >
      <span
        className={cn(
          "flex shrink-0 items-center justify-center rounded-[12px] bg-tile text-ink-secondary",
          large ? "aspect-[16/9] w-full" : "size-20",
        )}
      >
        <Icon className={large ? "size-14" : "size-8"} strokeWidth={1.25} aria-hidden />
      </span>
      <span className="flex min-w-0 flex-col gap-2">
        <span className="w-fit rounded-full bg-subtle px-2 py-0.5 font-mono text-[11px] text-ink-muted">
          Rehber yakında
        </span>
        <span className={cn("font-semibold text-ink", large ? "text-xl" : "text-[15px]")}>
          {guide.title}
        </span>
        <span className="text-sm text-ink-muted">{guide.summary}</span>
      </span>
    </Link>
  );
}
