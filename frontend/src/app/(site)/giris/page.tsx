import type { Metadata } from "next";

import { LoginForm } from "@/components/auth/login-form";

export const metadata: Metadata = {
  title: "Giriş yap",
  description: "Price Radar hesabınıza giriş yapın.",
};

export default function LoginPage() {
  return <LoginForm />;
}
