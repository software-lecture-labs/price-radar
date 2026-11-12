import type { Metadata } from "next";

import { requireAdmin } from "@/lib/auth/dal";
import { routes } from "@/lib/routes";

export const metadata: Metadata = { title: "Yönetim paneli" };

export default async function AdminPage() {
  await requireAdmin(routes.admin);
  return <h1 className="text-2xl font-semibold tracking-tight">Yönetim paneli</h1>;
}
