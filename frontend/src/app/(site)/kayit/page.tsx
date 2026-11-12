import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { RegisterForm } from "@/components/auth/register-form";
import { findCurrentUser } from "@/lib/auth/dal";
import { safeRedirectPath } from "@/lib/auth/redirect";
import { NEXT_PARAM, routes } from "@/lib/routes";

export const metadata: Metadata = {
  title: "Kayıt ol",
  description: "Price Radar hesabı oluşturun ve fiyatları takip etmeye başlayın.",
};

export default async function RegisterPage(props: PageProps<"/kayit">) {
  const next = safeRedirectPath((await props.searchParams)[NEXT_PARAM], routes.account);
  if (await findCurrentUser()) {
    redirect(next);
  }
  return <RegisterForm next={next} />;
}
