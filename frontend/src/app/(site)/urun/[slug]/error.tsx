"use client";

import { RefreshCwIcon } from "lucide-react";
import Link from "next/link";

import { routes } from "@/lib/routes";

export default function ProductError({
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  return (
    <div className="mx-auto max-w-xl rounded-xl border border-border-subtle bg-card px-6 py-12 text-center sm:p-12">
      <h1 className="text-2xl font-semibold tracking-tight text-ink">Ürüne şu an ulaşamıyoruz.</h1>
      <p className="mt-4 text-sm leading-6 text-ink-muted">
        Ürün ve mağaza fiyatları yüklenemedi. Biraz sonra tekrar deneyebilirsin.
      </p>
      <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
        <button
          type="button"
          onClick={retry}
          className="inline-flex min-h-12 items-center justify-center gap-2 rounded-md bg-ink px-5 text-sm font-semibold text-white hover:bg-ink-secondary"
        >
          <RefreshCwIcon className="size-4" aria-hidden /> Yeniden dene
        </button>
        <Link
          href={routes.home}
          className="inline-flex min-h-12 items-center px-4 text-sm font-semibold text-ink underline-offset-4 hover:underline"
        >
          Ana sayfaya dön
        </Link>
      </div>
    </div>
  );
}
