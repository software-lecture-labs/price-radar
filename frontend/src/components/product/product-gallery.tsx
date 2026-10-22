"use client";

import { ExpandIcon, XIcon } from "lucide-react";

import { ProductImage } from "@/components/product/product-image";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

export function ProductGallery({ src, title }: { src?: string; title: string }) {
  if (!src) {
    return (
      <ProductImage
        alt={title}
        sizes="(min-width: 1024px) 40vw, 90vw"
        className="h-80 rounded-xl border border-border-subtle bg-card lg:h-[440px]"
      />
    );
  }

  return (
    <Dialog>
      <DialogTrigger asChild>
        <button
          type="button"
          aria-label={`${title} ürün görselini büyüt`}
          className="group relative block w-full rounded-xl border border-border-subtle bg-card p-8 transition-colors hover:border-border-strong sm:p-10"
        >
          <ProductImage
            src={src}
            alt={title}
            sizes="(min-width: 1024px) 40vw, 90vw"
            eager
            className="h-64 w-full transition-transform duration-300 group-hover:scale-[1.025] sm:h-80 lg:h-[360px]"
          />
          <span className="absolute right-4 bottom-4 inline-flex min-h-10 items-center gap-2 rounded-md bg-subtle px-3 text-xs font-semibold text-ink">
            <ExpandIcon className="size-4" aria-hidden /> Görseli büyüt
          </span>
        </button>
      </DialogTrigger>
      <DialogContent
        showCloseButton={false}
        aria-describedby={undefined}
        className="max-h-[90dvh] overflow-y-auto bg-card p-6 sm:max-w-3xl sm:p-10"
      >
        <DialogTitle className="max-w-[90%] text-base leading-6 font-semibold">{title}</DialogTitle>
        <DialogClose asChild>
          <button
            type="button"
            aria-label="Görseli kapat"
            className="absolute top-3 right-3 flex size-11 items-center justify-center rounded-md bg-subtle text-ink transition-colors hover:bg-panel"
          >
            <XIcon className="size-5" aria-hidden />
          </button>
        </DialogClose>
        <ProductImage
          src={src}
          alt={title}
          sizes="(min-width: 768px) 700px, 85vw"
          eager
          className="h-[55dvh] w-full"
        />
      </DialogContent>
    </Dialog>
  );
}
