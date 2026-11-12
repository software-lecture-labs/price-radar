import { LogOutIcon } from "lucide-react";
import type { Metadata } from "next";

import { logout } from "@/app/actions/auth";
import { Button } from "@/components/ui/button";
import { requireUser } from "@/lib/auth/dal";
import { formatLongDate, formatPhone } from "@/lib/format";
import { routes } from "@/lib/routes";

export const metadata: Metadata = { title: "Hesabım" };

export default async function AccountPage() {
  const user = await requireUser(routes.account);
  const details = [
    { label: "Ad soyad", value: user.fullName },
    { label: "E-posta", value: user.email },
    { label: "Telefon", value: formatPhone(user.phone) },
    { label: "Üyelik tarihi", value: formatLongDate(user.createdAt) },
  ];

  return (
    <div className="max-w-2xl space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Hesabım</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Hoş geldiniz, {user.fullName.split(/\s+/)[0]}.
        </p>
      </div>

      <dl className="divide-y rounded-xl border">
        {details.map((item) => (
          <div key={item.label} className="grid gap-1 px-5 py-4 sm:grid-cols-[160px_1fr] sm:gap-4">
            <dt className="text-sm text-muted-foreground">{item.label}</dt>
            <dd className="text-sm font-medium break-words">{item.value}</dd>
          </div>
        ))}
      </dl>

      <form action={logout}>
        <Button type="submit" variant="outline" className="h-11 gap-2 rounded-lg">
          <LogOutIcon className="size-4" aria-hidden />
          Çıkış yap
        </Button>
      </form>
    </div>
  );
}
