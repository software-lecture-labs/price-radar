import Link from "next/link";

import { routes } from "@/lib/routes";

export default function ProductNotFound() {
  return (
    <div className="mx-auto max-w-xl py-16 text-center">
      <p className="mb-4 text-sm text-ink-muted">Ürün bulunamadı</p>
      <h1 className="text-3xl font-semibold tracking-tight text-ink">
        Bu ürün artık burada görünmüyor.
      </h1>
      <p className="mt-4 text-sm leading-6 text-ink-muted">
        Bağlantıyı kontrol edebilir veya ana sayfadan başka bir ürüne bakabilirsin.
      </p>
      <Link
        href={routes.home}
        className="mt-8 inline-flex min-h-12 items-center justify-center rounded-md bg-ink px-6 text-sm font-semibold text-white hover:bg-ink-secondary"
      >
        Ürünleri keşfet
      </Link>
    </div>
  );
}
