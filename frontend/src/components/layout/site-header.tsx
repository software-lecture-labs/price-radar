import { ArrowRightIcon, MenuIcon, RadarIcon, SearchIcon, UserIcon, XIcon } from "lucide-react";
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
  "inline-flex min-h-11 items-center rounded-md px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring aria-[current=page]:text-foreground";

export function SiteHeader() {
  return (
    <>
      <a
        href="#main-content"
        className="fixed top-3 left-5 z-[100] -translate-y-24 rounded-md bg-foreground px-5 py-3 text-sm font-semibold text-background focus:translate-y-0 focus:outline-2 focus:outline-offset-4 focus:outline-ring"
      >
        İçeriğe geç
      </a>
      <header className="sticky top-0 z-40 border-b bg-background">
        <div className="mx-auto flex h-20 w-full max-w-[1240px] items-center gap-5 px-5 sm:px-8 lg:gap-7">
          <Link
            href={routes.home}
            className="inline-flex min-h-11 shrink-0 items-center gap-2.5 rounded-sm text-[21px] font-extrabold tracking-[-0.055em] text-foreground focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
            aria-label={`${publicEnv.siteName} ana sayfa`}
          >
            <span className="flex size-9 items-center justify-center rounded-[10px] bg-radar-lime">
              <RadarIcon className="size-[23px]" strokeWidth={1.8} aria-hidden />
            </span>
            {publicEnv.siteName}
          </Link>

          <SearchForm className="hidden min-w-0 flex-1 md:block" />

          <nav aria-label="Ana menü" className="hidden shrink-0 items-center gap-1 lg:flex">
            {mainNav.map((item) => (
              <NavLink key={item.href} href={item.href} className={navLinkClass}>
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex shrink-0 items-center gap-2 md:ml-0">
            <Button
              asChild
              variant="ghost"
              className="hidden h-11 gap-2 rounded-md px-2 text-sm sm:inline-flex lg:rounded-none lg:border-l lg:border-border lg:pr-0 lg:pl-6 lg:hover:bg-transparent"
            >
              <Link href={routes.login}>
                <UserIcon className="size-[18px]" strokeWidth={1.7} aria-hidden />
                Giriş yap
              </Link>
            </Button>
            <MobileMenu />
          </div>
        </div>

        <div className="mx-auto max-w-[1240px] px-5 pb-4 sm:px-8 md:hidden">
          <SearchForm />
        </div>
      </header>
    </>
  );
}

function SearchForm({ className }: { className?: string }) {
  return (
    <Form
      action={routes.search()}
      role="search"
      aria-label="Ürün arama"
      className={cn("relative", className)}
    >
      <Input
        type="search"
        name={SEARCH_PARAM}
        placeholder="Ürün, marka veya model ara"
        aria-label="Ürün, marka veya model"
        enterKeyHint="search"
        className="h-11 rounded-lg border-border bg-muted/60 pr-12 pl-4 text-sm placeholder:text-muted-foreground focus-visible:border-foreground focus-visible:bg-background focus-visible:ring-2 focus-visible:ring-foreground/15"
      />
      <button
        type="submit"
        aria-label="Ara"
        className="absolute top-0 right-0 inline-flex size-11 items-center justify-center rounded-r-lg text-foreground transition-colors hover:bg-foreground/5 focus-visible:z-10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
      >
        <SearchIcon className="size-[19px]" strokeWidth={1.8} aria-hidden />
      </button>
    </Form>
  );
}

function MobileMenu() {
  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="size-11 rounded-md lg:hidden"
          aria-label="Menüyü aç"
        >
          <MenuIcon className="size-5" strokeWidth={1.8} aria-hidden />
        </Button>
      </SheetTrigger>
      <SheetContent
        side="right"
        aria-describedby={undefined}
        showCloseButton={false}
        className="gap-8 overflow-y-auto"
      >
        <SheetClose asChild>
          <Button
            variant="ghost"
            size="icon"
            className="absolute top-2 right-2 size-11"
            aria-label="Menüyü kapat"
          >
            <XIcon className="size-5" strokeWidth={1.8} aria-hidden />
          </Button>
        </SheetClose>
        <SheetHeader className="border-b pb-6">
          <SheetTitle className="flex items-center gap-2.5 text-lg font-bold tracking-tight">
            <RadarIcon className="size-5" aria-hidden />
            {publicEnv.siteName}
          </SheetTitle>
        </SheetHeader>
        <nav aria-label="Mobil menü" className="flex flex-col gap-2 px-5">
          {[
            ...mainNav,
            { href: routes.account, label: "Hesabım" },
            { href: routes.favorites, label: "Favoriler" },
            { href: routes.alerts, label: "Fiyat alarmları" },
          ].map((item) => (
            <SheetClose key={item.href} asChild>
              <NavLink href={item.href} className={cn(navLinkClass, "justify-between px-3")}>
                {item.label}
                <ArrowRightIcon className="size-4 text-muted-foreground" aria-hidden />
              </NavLink>
            </SheetClose>
          ))}
          <SheetClose asChild>
            <Button asChild className="mt-5 h-12 rounded-lg">
              <Link href={routes.login}>Giriş yap</Link>
            </Button>
          </SheetClose>
        </nav>
      </SheetContent>
    </Sheet>
  );
}
