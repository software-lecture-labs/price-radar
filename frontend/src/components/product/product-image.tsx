import { PackageIcon } from "lucide-react";
import Image from "next/image";

import { cn } from "@/lib/utils";

interface ProductImageProps {
  src?: string;
  alt: string;
  sizes: string;
  className?: string;
  eager?: boolean;
}

export function ProductImage({ src, alt, sizes, className, eager = false }: ProductImageProps) {
  return (
    <div className={cn("relative", className)}>
      {src ? (
        <Image
          src={src}
          alt={alt}
          fill
          sizes={sizes}
          loading={eager ? "eager" : "lazy"}
          fetchPriority={eager ? "high" : undefined}
          className="object-contain mix-blend-multiply"
        />
      ) : (
        <PackageIcon
          className="absolute inset-0 m-auto size-10 text-ink-muted/60"
          strokeWidth={1.25}
          aria-hidden
        />
      )}
    </div>
  );
}
