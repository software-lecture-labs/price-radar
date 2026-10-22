import { NavLink } from "@/components/layout/nav-link";
import { routes } from "@/lib/routes";

const accountNav = [
  { href: routes.account, label: "Profil", exact: true },
  { href: routes.favorites, label: "Favoriler" },
  { href: routes.alerts, label: "Fiyat alarmları" },
];

export default function AccountLayout({ children }: LayoutProps<"/hesabim">) {
  return (
    <div className="grid gap-8 md:grid-cols-[200px_1fr]">
      <nav aria-label="Hesap menüsü" className="flex gap-1 overflow-x-auto md:flex-col">
        {accountNav.map((item) => (
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
      <div>{children}</div>
    </div>
  );
}
