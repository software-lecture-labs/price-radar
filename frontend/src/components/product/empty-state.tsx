import { SearchXIcon } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";

interface EmptyStateProps {
  title: string;
  text: string;
  /** Optional link shown as a button, for example back to the first page. */
  action?: { href: string; label: string };
}

export function EmptyState({ title, text, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-start gap-5 rounded-lg border border-border-subtle bg-card p-6 sm:flex-row sm:items-center sm:p-8">
      <SearchXIcon className="size-7 shrink-0 text-ink-muted" strokeWidth={1.5} aria-hidden />
      <div className="flex-1">
        <h2 className="text-base font-semibold text-ink">{title}</h2>
        <p className="mt-1.5 text-sm leading-6 text-ink-muted">{text}</p>
      </div>
      {action ? (
        <Button asChild variant="outline" size="lg">
          <Link href={action.href}>{action.label}</Link>
        </Button>
      ) : null}
    </div>
  );
}
