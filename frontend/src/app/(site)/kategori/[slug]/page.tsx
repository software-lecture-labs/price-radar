import type { Metadata } from "next";

export const metadata: Metadata = { title: "Kategori" };

export default async function CategoryPage(props: PageProps<"/kategori/[slug]">) {
  const { slug } = await props.params;

  return <h1 className="text-2xl font-semibold tracking-tight">Kategori: {slug}</h1>;
}
