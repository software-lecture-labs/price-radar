"use client";

import { ImageOffIcon } from "lucide-react";
import Image from "next/image";
import { useState } from "react";

import catalogImages from "../../../public/catalog/sources.json";
import { cn } from "@/lib/utils";

interface ProductImageProps {
  src?: string;
  alt: string;
  sizes: string;
  className?: string;
  eager?: boolean;
}

export function ProductImage({ src, alt, sizes, className, eager = false }: ProductImageProps) {
  // A cached photo belongs only to the exact image URL from its catalog record.
  const imageSrc = catalogImages.find((image) => image.source === src)?.asset ?? src;
  const [failedSources, setFailedSources] = useState<string[]>([]);
  const isRemote = Boolean(imageSrc?.startsWith("https://"));
  const optimizedFailed = failedSources.includes(`${imageSrc}:optimized`);
  const directFailed = failedSources.includes(`${imageSrc}:direct`);
  const useDirectSource = isRemote && optimizedFailed;
  const unavailable = !imageSrc || (optimizedFailed && (!isRemote || directFailed));

  return (
    <div className={cn("relative", className)}>
      {!unavailable ? (
        <Image
          key={`${imageSrc}:${useDirectSource}`}
          src={imageSrc}
          alt={alt}
          fill
          sizes={sizes}
          loading={eager ? "eager" : "lazy"}
          fetchPriority={eager ? "high" : undefined}
          unoptimized={useDirectSource}
          onError={() => {
            setFailedSources((previous) => [
              ...previous,
              `${imageSrc}:${useDirectSource ? "direct" : "optimized"}`,
            ]);
          }}
          className="object-contain mix-blend-multiply"
        />
      ) : (
        <span
          role="img"
          aria-label={alt ? `${alt}: görsel yüklenemedi` : "Ürün görseli yüklenemedi"}
          className="absolute inset-0 flex flex-col items-center justify-center gap-2 rounded-md bg-subtle px-2 text-center text-ink-muted"
        >
          <ImageOffIcon className="size-7 shrink-0" strokeWidth={1.25} aria-hidden />
          <span className="text-[11px] leading-4" aria-hidden>
            Görsel yüklenemedi
          </span>
        </span>
      )}
    </div>
  );
}
