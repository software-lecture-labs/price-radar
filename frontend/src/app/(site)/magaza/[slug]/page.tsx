import type { Metadata } from "next";

export const metadata: Metadata = { title: "Mağaza" };

export default async function MerchantPage(props: PageProps<"/magaza/[slug]">) {
  const { slug } = await props.params;

  return <h1 className="text-2xl font-semibold tracking-tight">Mağaza: {slug}</h1>;
}
