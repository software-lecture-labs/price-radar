import { ArrowDownRightIcon } from "lucide-react";

import { currencyLabel, formatAmount } from "@/lib/format";
import { cn } from "@/lib/utils";

const priceSizes = {
  lg: { amount: "text-[32px] leading-none", unit: "text-sm" },
  md: { amount: "text-xl leading-tight sm:text-[22px]", unit: "text-xs" },
  sm: { amount: "text-sm", unit: "text-[11px]" },
};

interface PriceProps {
  amount: number;
  currency?: string;
  size?: keyof typeof priceSizes;
  className?: string;
}

export function Price({ amount, currency, size = "md", className }: PriceProps) {
  return (
    <span className={cn("inline-flex items-baseline gap-1", className)}>
      <span
        className={cn(
          "font-mono font-semibold tracking-tight text-ink tabular-nums",
          priceSizes[size].amount,
        )}
      >
        {formatAmount(amount)}
      </span>
      <span className={cn("font-medium text-ink-muted", priceSizes[size].unit)}>
        {currencyLabel(currency)}
      </span>
    </span>
  );
}

export function ListPrice({ amount, currency, className }: Omit<PriceProps, "size">) {
  return (
    <s className={cn("font-mono text-[13px] text-ink-muted tabular-nums", className)}>
      <span className="sr-only">Liste fiyatı: </span>
      {formatAmount(amount)} {currencyLabel(currency)}
    </s>
  );
}

export function DiscountBadge({ percent, className }: { percent: number; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded bg-drop px-2 py-1 text-xs font-semibold text-drop-foreground",
        className,
      )}
    >
      <ArrowDownRightIcon className="size-3" strokeWidth={2.5} aria-hidden />
      <span className="sr-only">İndirim </span>%{percent}
    </span>
  );
}
