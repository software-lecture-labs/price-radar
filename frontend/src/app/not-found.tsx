import Link from "next/link";

import { Button } from "@/components/ui/button";
import { routes } from "@/lib/routes";

export default function NotFound() {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center gap-4 px-4 py-16 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">Sayfa bulunamadı</h1>
      <p className="text-muted-foreground">
        Aradığın sayfa taşınmış ya da hiç var olmamış olabilir.
      </p>
      <Button asChild>
        <Link href={routes.home}>Ana sayfaya dön</Link>
      </Button>
    </main>
  );
}
