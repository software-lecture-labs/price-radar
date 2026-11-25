import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { LoginForm } from "@/components/auth/login-form";
import { findCurrentUser } from "@/lib/auth/dal";
import { safeRedirectPath } from "@/lib/auth/redirect";
import { NEXT_PARAM } from "@/lib/routes";

export const metadata: Metadata = {
  title: "Giriş yap",
  description: "Price Radar hesabınıza giriş yapın.",
};

export default async function LoginPage(props: PageProps<"/giris">) {
  const next = safeRedirectPath((await props.searchParams)[NEXT_PARAM]);
  if (await findCurrentUser()) {
    redirect(next);
  }
  return <LoginForm next={next} />;
}
