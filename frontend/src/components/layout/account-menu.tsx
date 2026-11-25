"use client";

import {
  BellIcon,
  ChevronDownIcon,
  HeartIcon,
  LogOutIcon,
  ShieldIcon,
  UserIcon,
} from "lucide-react";
import Link from "next/link";
import { type ComponentProps, useTransition } from "react";

import { logout } from "@/app/actions/auth";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { routes } from "@/lib/routes";

/** Only what the menu shows; the full profile stays on the server. */
export interface AccountMenuUser {
  fullName: string;
  email: string;
  isAdmin: boolean;
}

export function AccountMenu({ user, className }: { user: AccountMenuUser; className?: string }) {
  const [isSigningOut, startTransition] = useTransition();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className={className}>
          <UserIcon className="size-[18px]" strokeWidth={1.7} aria-hidden />
          <span className="max-w-32 truncate">{user.fullName.split(/\s+/)[0]}</span>
          <ChevronDownIcon className="size-4 text-muted-foreground" aria-hidden />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-60">
        <DropdownMenuLabel className="font-normal">
          <p className="truncate text-sm font-semibold">{user.fullName}</p>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link href={routes.account}>
            <UserIcon aria-hidden />
            Hesabım
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link href={routes.favorites}>
            <HeartIcon aria-hidden />
            Favoriler
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link href={routes.alerts}>
            <BellIcon aria-hidden />
            Fiyat alarmları
          </Link>
        </DropdownMenuItem>
        {user.isAdmin && (
          <DropdownMenuItem asChild>
            <Link href={routes.admin}>
              <ShieldIcon aria-hidden />
              Yönetim paneli
            </Link>
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="destructive"
          disabled={isSigningOut}
          onSelect={() => startTransition(() => logout())}
        >
          <LogOutIcon aria-hidden />
          {isSigningOut ? "Çıkış yapılıyor..." : "Çıkış yap"}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/**
 * Signs out without a form, so it also works inside a closing sheet. Keeps an `onClick` passed in by a
 * wrapper such as `SheetClose asChild`.
 */
export function SignOutButton({ onClick, ...props }: ComponentProps<typeof Button>) {
  const [isSigningOut, startTransition] = useTransition();

  return (
    <Button
      type="button"
      disabled={isSigningOut}
      onClick={(event) => {
        onClick?.(event);
        startTransition(() => logout());
      }}
      {...props}
    >
      {isSigningOut ? "Çıkış yapılıyor..." : "Çıkış yap"}
    </Button>
  );
}
