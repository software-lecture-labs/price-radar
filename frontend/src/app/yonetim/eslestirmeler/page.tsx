import type { Metadata } from "next";

import { requireAdmin } from "@/lib/auth/dal";
import { routes } from "@/lib/routes";

export const metadata: Metadata = { title: "Eşleştirmeler" };

export default async function AdminMatchesPage() {
  await requireAdmin(routes.adminMatches);
  return <h1 className="text-2xl font-semibold tracking-tight">Eşleştirmeler</h1>;
}
