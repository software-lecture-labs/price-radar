import { MenuIcon, RadarIcon, SearchIcon, UserIcon } from "lucide-react";
import Form from "next/form";
import Link from "next/link";

import { NavLink } from "@/components/layout/nav-link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { publicEnv } from "@/lib/env";
import { routes, SEARCH_PARAM } from "@/lib/routes";
import { cn } from "@/lib/utils";

const mainNav = [
  { href: routes.categories, label: "Kategoriler" },
  { href: routes.merchants, label: "Mağazalar" },
];

const navLinkClass =
  "rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground aria-[current=page]:font-medium aria-[current=page]:text-foreground";

export function SiteHeader() {
  return (
    <header className="border-b bg-background">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center gap-4 px-4">
        <Link href={routes.home} className="flex shrink-0 items-center gap-2 font-semibold">
          <RadarIcon className="size-5" aria-hidden />
          {publicEnv.siteName}
        </Link>

        <SearchForm className="hidden flex-1 md:flex" />

        <nav aria-label="Ana menü" className="hidden items-center md:flex">
          {mainNav.map((item) => (
            <NavLink key={item.href} href={item.href} className={navLinkClass}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-1 md:ml-0">
          <Button asChild variant="ghost" size="icon" aria-label="Hesabım">
            <Link href={routes.account}>
              <UserIcon />
            </Link>
          </Button>
          <Button asChild size="sm" className="hidden sm:inline-flex">
            <Link href={routes.login}>Giriş yap</Link>
          </Button>
          <MobileMenu />
        </div>
      </div>

      <div className="px-4 pb-3 md:hidden">
        <SearchForm />
      </div>
    </header>
  );
}

function SearchForm({ className }: { className?: string }) {
  return (
    <Form action={routes.search()} role="search" className={cn("relative", className)}>
      <SearchIcon
        className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
        aria-hidden
      />
      <Input
        type="search"
        name={SEARCH_PARAM}
        placeholder="Ürün, marka veya kategori ara"
        aria-label="Ürün, marka veya kategori ara"
        enterKeyHint="search"
        className="pl-8"
      />
    </Form>
  );
}

function MobileMenu() {
  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button variant="ghost" size="icon" className="md:hidden" aria-label="Menüyü aç">
          <MenuIcon />
        </Button>
      </SheetTrigger>
      <SheetContent side="right" aria-describedby={undefined}>
        <SheetHeader>
          <SheetTitle>Menü</SheetTitle>
        </SheetHeader>
        <nav aria-label="Mobil menü" className="flex flex-col px-2">
          {[...mainNav, { href: routes.login, label: "Giriş yap" }].map((item) => (
            <SheetClose key={item.href} asChild>
              <NavLink href={item.href} className={navLinkClass}>
                {item.label}
              </NavLink>
            </SheetClose>
          ))}
        </nav>
      </SheetContent>
    </Sheet>
  );
}
