import type { Metadata } from "next";

export const metadata: Metadata = { title: "Yönetim paneli" };

export default function AdminPage() {
  return <h1 className="text-2xl font-semibold tracking-tight">Yönetim paneli</h1>;
}
