"use client";

import * as React from "react";
import { EyeIcon, EyeOffIcon, RadarIcon } from "lucide-react";
import Link from "next/link";
import { toast } from "sonner";

import { register } from "@/app/actions/auth";
import { errorProps, FieldError } from "@/components/auth/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PASSWORD_MIN_LENGTH } from "@/lib/auth/validation";
import { publicEnv } from "@/lib/env";
import { NEXT_PARAM, routes } from "@/lib/routes";

/** @param next where to go after signing up; already checked with `safeRedirectPath` */
export function RegisterForm({ next }: { next: string }) {
  const [fullName, setFullName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [phone, setPhone] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [confirmPassword, setConfirmPassword] = React.useState("");
  const [showPassword, setShowPassword] = React.useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = React.useState(false);
  const [state, formAction, isLoading] = React.useActionState(register, undefined);
  const errors = state?.fieldErrors;

  React.useEffect(() => {
    if (state?.message) toast.error(state.message);
  }, [state]);

  // Dispatched by hand rather than with <form action>, which would clear the fields after a failed attempt.
  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    React.startTransition(() => formAction(formData));
  };

  return (
    <div className="flex min-h-[calc(100vh-280px)] w-full items-center justify-center py-6 sm:py-12">
      <div className="w-full max-w-[420px] rounded-2xl border border-border-subtle bg-card p-7 shadow-xs sm:p-9">
        {/* En üstte ortalanmış site logosu */}
        <div className="flex flex-col items-center text-center">
          <Link
            href={routes.home}
            className="inline-flex items-center gap-2.5 rounded-md text-[22px] font-extrabold tracking-[-0.055em] text-ink focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
            aria-label={`${publicEnv.siteName} ana sayfa`}
          >
            <span className="flex size-10 items-center justify-center rounded-xl bg-radar-lime shadow-xs">
              <RadarIcon className="size-6 text-ink" strokeWidth={1.8} aria-hidden />
            </span>
            <span>{publicEnv.siteName}</span>
          </Link>

          {/* Ana başlık */}
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-ink sm:text-[26px]">
            Hesap oluşturun
          </h1>

          {/* Giriş yap bağlantısı */}
          <p className="mt-2 text-sm text-ink-muted">
            Zaten hesabınız var mı?{" "}
            <Link
              href={routes.login(next === routes.account ? undefined : next)}
              className="font-semibold text-ink underline decoration-radar-lime decoration-2 underline-offset-4 transition-colors hover:text-ink-secondary hover:decoration-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
            >
              Giriş yap
            </Link>
          </p>
        </div>

        {/* Kayıt Formu */}
        <form onSubmit={handleSubmit} className="mt-8 space-y-4">
          <input type="hidden" name={NEXT_PARAM} value={next} />

          {/* Ad Soyad input */}
          <div className="space-y-2">
            <Label htmlFor="fullName" className="text-sm font-medium text-ink">
              Ad Soyad
            </Label>
            <Input
              id="fullName"
              name="fullName"
              type="text"
              autoComplete="name"
              required
              maxLength={100}
              placeholder="Ahmet Yılmaz"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              {...errorProps("fullName-error", errors?.fullName)}
              className="h-11 rounded-lg border-border-strong/40 bg-white px-3.5 text-sm text-ink transition-colors placeholder:text-ink-muted/70 focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-radar-lime focus-visible:ring-offset-0"
            />
            <FieldError id="fullName-error" message={errors?.fullName} />
          </div>

          {/* E-posta adresi input */}
          <div className="space-y-2">
            <Label htmlFor="register-email" className="text-sm font-medium text-ink">
              E-posta adresi
            </Label>
            <Input
              id="register-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              placeholder="ornek@alanadi.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              {...errorProps("register-email-error", errors?.email)}
              className="h-11 rounded-lg border-border-strong/40 bg-white px-3.5 text-sm text-ink transition-colors placeholder:text-ink-muted/70 focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-radar-lime focus-visible:ring-offset-0"
            />
            <FieldError id="register-email-error" message={errors?.email} />
          </div>

          {/* Telefon numarası input */}
          <div className="space-y-2">
            <Label htmlFor="register-phone" className="text-sm font-medium text-ink">
              Telefon numarası
            </Label>
            <Input
              id="register-phone"
              name="phone"
              type="tel"
              autoComplete="tel"
              required
              placeholder="05XX XXX XX XX"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              {...errorProps("register-phone-error", errors?.phone)}
              className="h-11 rounded-lg border-border-strong/40 bg-white px-3.5 text-sm text-ink transition-colors placeholder:text-ink-muted/70 focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-radar-lime focus-visible:ring-offset-0"
            />
            <FieldError id="register-phone-error" message={errors?.phone} />
          </div>

          {/* Şifre input ve sağında görünürlük ikonu */}
          <div className="space-y-2">
            <Label htmlFor="register-password" className="text-sm font-medium text-ink">
              Şifre
            </Label>
            <div className="relative">
              <Input
                id="register-password"
                name="password"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                required
                minLength={PASSWORD_MIN_LENGTH}
                placeholder={`En az ${PASSWORD_MIN_LENGTH} karakter`}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                {...errorProps("register-password-error", errors?.password)}
                className="h-11 rounded-lg border-border-strong/40 bg-white pr-11 pl-3.5 text-sm text-ink transition-colors placeholder:text-ink-muted/70 focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-radar-lime focus-visible:ring-offset-0"
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-ink-muted transition-colors hover:text-ink focus-visible:rounded-r-lg focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring"
                aria-label={showPassword ? "Şifreyi gizle" : "Şifreyi göster"}
              >
                {showPassword ? (
                  <EyeOffIcon className="size-5" aria-hidden />
                ) : (
                  <EyeIcon className="size-5" aria-hidden />
                )}
              </button>
            </div>
            <FieldError id="register-password-error" message={errors?.password} />
          </div>

          {/* Şifre Tekrarı input ve sağında görünürlük ikonu */}
          <div className="space-y-2">
            <Label htmlFor="confirm-password" className="text-sm font-medium text-ink">
              Şifre tekrarı
            </Label>
            <div className="relative">
              <Input
                id="confirm-password"
                name="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                autoComplete="new-password"
                required
                placeholder="Şifrenizi tekrar girin"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                {...errorProps("confirm-password-error", errors?.confirmPassword)}
                className="h-11 rounded-lg border-border-strong/40 bg-white pr-11 pl-3.5 text-sm text-ink transition-colors placeholder:text-ink-muted/70 focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-radar-lime focus-visible:ring-offset-0"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-ink-muted transition-colors hover:text-ink focus-visible:rounded-r-lg focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring"
                aria-label={
                  showConfirmPassword ? "Şifre tekrarını gizle" : "Şifre tekrarını göster"
                }
              >
                {showConfirmPassword ? (
                  <EyeOffIcon className="size-5" aria-hidden />
                ) : (
                  <EyeIcon className="size-5" aria-hidden />
                )}
              </button>
            </div>
            <FieldError id="confirm-password-error" message={errors?.confirmPassword} />
          </div>

          {/* Formun en altında genişliği kaplayan 'Kayıt ol' butonu */}
          <Button
            type="submit"
            disabled={isLoading}
            className="mt-4 flex h-11 w-full items-center justify-center rounded-lg bg-radar-lime text-sm font-bold text-ink shadow-xs transition-all hover:bg-[#bde829] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink active:translate-y-px disabled:pointer-events-none disabled:opacity-60 sm:text-base"
          >
            {isLoading ? "Hesap oluşturuluyor..." : "Kayıt ol"}
          </Button>
        </form>
      </div>
    </div>
  );
}
