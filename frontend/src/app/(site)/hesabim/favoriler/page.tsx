import type { Metadata } from "next";

import { requireUser } from "@/lib/auth/dal";
import { routes } from "@/lib/routes";

export const metadata: Metadata = { title: "Favoriler" };

export default async function FavoritesPage() {
  await requireUser(routes.favorites);
  return <h1 className="text-2xl font-semibold tracking-tight">Favoriler</h1>;
}
