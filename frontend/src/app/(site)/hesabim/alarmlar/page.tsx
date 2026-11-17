import type { Metadata } from "next";

import { requireUser } from "@/lib/auth/dal";
import { routes } from "@/lib/routes";

export const metadata: Metadata = { title: "Fiyat alarmları" };

export default async function AlertsPage() {
  await requireUser(routes.alerts);
  return <h1 className="text-2xl font-semibold tracking-tight">Fiyat alarmları</h1>;
}
