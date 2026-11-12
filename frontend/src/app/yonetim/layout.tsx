import { ArrowLeftIcon } from "lucide-react";
import Link from "next/link";

import { NavLink } from "@/components/layout/nav-link";
import { publicEnv } from "@/lib/env";
import { routes } from "@/lib/routes";

const adminNav = [
  { href: routes.admin, label: "Panel", exact: true },
  { href: routes.adminMatches, label: "Eşleştirmeler" },
];

export default function AdminLayout({ children }: LayoutProps<"/yonetim">) {
  return (
    <>
      <header className="border-b bg-background">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between gap-4 px-4">
          <Link href={routes.admin} className="font-semibold">
            {publicEnv.siteName} · Yönetim
          </Link>
          <Link
            href={routes.home}
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeftIcon className="size-4" aria-hidden />
            Siteye dön
          </Link>
        </div>
      </header>

      <div className="mx-auto grid w-full max-w-6xl flex-1 gap-8 px-4 py-8 md:grid-cols-[200px_1fr]">
        <nav aria-label="Yönetim menüsü" className="flex gap-1 overflow-x-auto md:flex-col">
          {adminNav.map((item) => (
            <NavLink
              key={item.href}
              href={item.href}
              exact={item.exact}
              className="shrink-0 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-muted hover:text-foreground aria-[current=page]:bg-muted aria-[current=page]:font-medium aria-[current=page]:text-foreground"
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <main>{children}</main>
      </div>
    </>
  );
}
