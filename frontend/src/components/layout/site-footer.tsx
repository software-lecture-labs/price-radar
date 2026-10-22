import { RadarIcon } from "lucide-react";
import Link from "next/link";

import { publicEnv } from "@/lib/env";
import { routes } from "@/lib/routes";

const sections = [
  {
    title: "Keşfet",
    links: [
      { href: routes.categories, label: "Kategoriler" },
      { href: routes.merchants, label: "Mağazalar" },
      { href: routes.search(), label: "Ürün ara" },
    ],
  },
  {
    title: "Hesabım",
    links: [
      { href: routes.login, label: "Giriş yap" },
      { href: routes.register, label: "Kayıt ol" },
      { href: routes.favorites, label: "Favoriler" },
      { href: routes.alerts, label: "Fiyat alarmları" },
    ],
  },
];

const linkClass =
  "inline-flex min-h-11 items-center rounded-sm text-sm text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring";

export function SiteFooter() {
  return (
    <footer className="border-t bg-background">
      <div className="mx-auto flex w-full max-w-[1240px] flex-col gap-9 px-5 py-10 sm:px-8 md:flex-row md:justify-between md:gap-16 md:py-12">
        <div className="max-w-sm">
          <Link
            href={routes.home}
            className="inline-flex min-h-11 items-center gap-2.5 rounded-sm text-xl font-extrabold tracking-[-0.055em] text-foreground focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
            aria-label={`${publicEnv.siteName} ana sayfa`}
          >
            <span className="flex size-8 items-center justify-center rounded-lg bg-radar-lime">
              <RadarIcon className="size-5" strokeWidth={1.8} aria-hidden />
            </span>
            {publicEnv.siteName}
          </Link>
          <p className="mt-3 max-w-[280px] text-sm leading-6 text-muted-foreground">
            Aynı ürün. Farklı fiyatlar.
            <br />
            Karşılaştır, kararını bilerek ver.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-x-10 gap-y-8 sm:gap-x-20 md:gap-x-24">
          {sections.map((section) => (
            <nav key={section.title} aria-label={`Alt menü: ${section.title}`}>
              <h2 className="mb-2 text-xs font-bold tracking-[0.12em] text-foreground uppercase">
                {section.title}
              </h2>
              <ul>
                {section.links.map((link) => (
                  <li key={link.href}>
                    <Link href={link.href} className={linkClass}>
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>
      </div>

      <div className="mx-auto flex max-w-[1240px] flex-col gap-1 border-t px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-8">
        <p className="text-xs leading-6 text-muted-foreground">
          {publicEnv.siteName} <span className="mx-1.5 text-border">/</span> Fiyat karşılaştırma
        </p>
        <nav aria-label="Proje bağlantıları" className="flex items-center gap-6">
          <Link href={routes.status} className={`${linkClass} text-xs`}>
            Sistem durumu
          </Link>
          <Link href={routes.admin} className={`${linkClass} text-xs`}>
            Yönetim paneli
          </Link>
        </nav>
      </div>
    </footer>
  );
}
