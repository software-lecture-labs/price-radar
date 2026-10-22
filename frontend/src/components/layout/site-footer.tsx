import Link from "next/link";

import { publicEnv } from "@/lib/env";
import { routes } from "@/lib/routes";

const sections = [
  {
    title: "Keşfet",
    links: [
      { href: routes.categories, label: "Kategoriler" },
      { href: routes.merchants, label: "Mağazalar" },
      { href: routes.search(), label: "Arama" },
    ],
  },
  {
    title: "Hesap",
    links: [
      { href: routes.login, label: "Giriş yap" },
      { href: routes.register, label: "Kayıt ol" },
      { href: routes.favorites, label: "Favoriler" },
      { href: routes.alerts, label: "Fiyat alarmları" },
    ],
  },
  {
    title: "Proje",
    links: [
      { href: routes.status, label: "Sistem durumu" },
      { href: routes.admin, label: "Yönetim paneli" },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className="border-t bg-muted/40">
      <div className="mx-auto grid w-full max-w-6xl gap-8 px-4 py-10 sm:grid-cols-3">
        {sections.map((section) => (
          <nav key={section.title} aria-label={section.title}>
            <h2 className="mb-3 text-sm font-medium">{section.title}</h2>
            <ul className="space-y-2 text-sm text-muted-foreground">
              {section.links.map((link) => (
                <li key={link.href}>
                  <Link href={link.href} className="hover:text-foreground">
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        ))}
      </div>
      <div className="border-t">
        <p className="mx-auto w-full max-w-6xl px-4 py-4 text-xs text-muted-foreground">
          {publicEnv.siteName} · Fiyat karşılaştırma
        </p>
      </div>
    </footer>
  );
}
