import type { ProductSort } from "./api/products";

type QueryValue = string | string[] | undefined;

// The backend rejects longer queries and page indexes above 10 000.
const MAX_QUERY_LENGTH = 100;
const MAX_PAGE = 10_000;

export const sortOptions: { value: ProductSort; label: string }[] = [
  { value: "newest", label: "En yeni" },
  { value: "price_asc", label: "Önce en ucuz" },
  { value: "price_desc", label: "Önce en pahalı" },
];

// A parameter repeated in the address (?q=a&q=b) arrives as an array; only the first one is used.
function firstValue(value: QueryValue): string {
  return (Array.isArray(value) ? value[0] : value) ?? "";
}

export function parseQuery(value: QueryValue): string {
  return firstValue(value).trim().slice(0, MAX_QUERY_LENGTH);
}

export function parseSort(value: QueryValue): ProductSort {
  const option = sortOptions.find((sort) => sort.value === firstValue(value));
  return option ? option.value : "newest";
}

/** Pages in the address start at 1. Anything that is not a valid page number shows the first page. */
export function parsePage(value: QueryValue): number {
  const page = Number(firstValue(value));
  return Number.isInteger(page) && page >= 1 && page <= MAX_PAGE ? page : 1;
}
