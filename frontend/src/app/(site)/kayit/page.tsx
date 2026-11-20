import type { Metadata } from "next";

import { RegisterForm } from "@/components/auth/register-form";

export const metadata: Metadata = {
  title: "Kayıt ol",
  description: "Price Radar hesabı oluşturun ve fiyatları takip etmeye başlayın.",
};

export default function RegisterPage() {
  return <RegisterForm />;
}
