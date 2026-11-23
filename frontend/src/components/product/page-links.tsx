import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";

interface PageLinksProps {
  page: number;
  totalPages: number;
  /** Address of a page, counted from 1. */
  hrefFor: (page: number) => string;
}

export function PageLinks({ page, totalPages, hrefFor }: PageLinksProps) {
  if (totalPages <= 1) return null;

  return (
    <nav aria-label="Sayfalar" className="flex items-center justify-center gap-3">
      {page > 1 ? (
        <Button asChild variant="outline" size="lg">
          <Link href={hrefFor(page - 1)}>
            <ChevronLeftIcon aria-hidden /> Önceki
          </Link>
        </Button>
      ) : null}
      <span className="text-sm text-ink-muted">
        Sayfa {page} / {totalPages}
      </span>
      {page < totalPages ? (
        <Button asChild variant="outline" size="lg">
          <Link href={hrefFor(page + 1)}>
            Sonraki <ChevronRightIcon aria-hidden />
          </Link>
        </Button>
      ) : null}
    </nav>
  );
}
