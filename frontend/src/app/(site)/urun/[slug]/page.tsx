import type { Metadata } from "next";

export const metadata: Metadata = { title: "Ürün" };

export default async function ProductPage(props: PageProps<"/urun/[slug]">) {
  const { slug } = await props.params;

  return <h1 className="text-2xl font-semibold tracking-tight">Ürün: {slug}</h1>;
}
