import { cn } from "@/lib/utils";

const WIDTH = 200;
const HEIGHT = 40;
const PADDING = 5;

export function Sparkline({ prices, className }: { prices: number[]; className?: string }) {
  if (prices.length < 2) {
    return null;
  }

  const low = Math.min(...prices);
  const range = Math.max(...prices) - low || 1;
  const points = prices.map((price, index) => ({
    x: PADDING + (index / (prices.length - 1)) * (WIDTH - 2 * PADDING),
    y: PADDING + (1 - (price - low) / range) * (HEIGHT - 2 * PADDING),
  }));
  const path = points
    .map((point, index) => `${index === 0 ? "M" : "L"}${point.x.toFixed(1)},${point.y.toFixed(1)}`)
    .join(" ");
  const lowest = points[prices.lastIndexOf(low)];

  return (
    <svg
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      className={cn("overflow-visible", className)}
      aria-hidden
    >
      <path
        d={path}
        fill="none"
        stroke="currentColor"
        strokeWidth={1.5}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle
        cx={lowest.x}
        cy={lowest.y}
        r={4}
        fill="var(--color-radar-lime)"
        stroke="currentColor"
        strokeWidth={1.5}
      />
    </svg>
  );
}
