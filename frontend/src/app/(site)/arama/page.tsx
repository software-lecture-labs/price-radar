import type { Metadata } from "next";

import { SEARCH_PARAM } from "@/lib/routes";

export const metadata: Metadata = { title: "Arama" };

export default async function SearchPage(props: PageProps<"/arama">) {
  const query = (await props.searchParams)[SEARCH_PARAM];

  return (
    <h1 className="text-2xl font-semibold tracking-tight">
      Arama{typeof query === "string" && query ? `: ${query}` : ""}
    </h1>
  );
}
