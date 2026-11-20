import { Skeleton } from "@/components/ui/skeleton";

export default function ProductLoading() {
  return (
    <div role="status" aria-busy="true" className="space-y-10">
      <span className="sr-only">Ürün ve mağaza fiyatları yükleniyor.</span>
      <Skeleton className="h-5 w-48" />
      <div className="grid gap-8 lg:grid-cols-2">
        <Skeleton className="h-80 w-full sm:h-[440px]" />
        <div className="space-y-5">
          <Skeleton className="h-8 w-24" />
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-56 w-full" />
        </div>
      </div>
      <Skeleton className="h-8 w-64" />
      <div className="space-y-3">
        {[0, 1, 2, 3].map((item) => (
          <Skeleton key={item} className="h-24 w-full" />
        ))}
      </div>
    </div>
  );
}
