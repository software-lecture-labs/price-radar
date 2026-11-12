"use client";

import * as React from "react";
import { EyeIcon, EyeOffIcon, RadarIcon } from "lucide-react";
import Link from "next/link";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { publicEnv } from "@/lib/env";
import { routes } from "@/lib/routes";

export function LoginForm() {
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [showPassword, setShowPassword] = React.useState(false);
  const [isLoading, setIsLoading] = React.useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!email || !password) {
      toast.error("Lütfen e-posta adresinizi ve şifrenizi girin.");
      return;
    }

    setIsLoading(true);
    try {
      // Simüle edilen giriş işlemi
      await new Promise((resolve) => setTimeout(resolve, 800));
      toast.success("Giriş başarılı! Yönlendiriliyorsunuz...");
    } catch {
      toast.error("Giriş yapılırken bir hata oluştu. Lütfen tekrar deneyin.");
    } finally {
      setIsLoading(false);
    }
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

          {/* Hemen altında sadece 'Hoş geldiniz!' ana başlığı */}
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-ink sm:text-[26px]">
            Hoş geldiniz!
          </h1>

          {/* Başlığın altında 'Henüz hesabınız yok mu? Kayıt ol' metni */}
          <p className="mt-2 text-sm text-ink-muted">
            Henüz hesabınız yok mu?{" "}
            <Link
              href={routes.register}
              className="font-semibold text-ink underline decoration-radar-lime decoration-2 underline-offset-4 transition-colors hover:text-ink-secondary hover:decoration-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
            >
              Kayıt ol
            </Link>
          </p>
        </div>

        {/* Giriş Formu */}
        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          {/* E-posta adresi input */}
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-medium text-ink">
              E-posta adresi
            </Label>
            <Input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              placeholder="ornek@alanadi.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-11 rounded-lg border-border-strong/40 bg-white px-3.5 text-sm text-ink placeholder:text-ink-muted/70 transition-colors focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-radar-lime focus-visible:ring-offset-0"
            />
          </div>

          {/* Şifre input ve sağında görünürlük ikonu */}
          <div className="space-y-2">
            <Label htmlFor="password" className="text-sm font-medium text-ink">
              Şifre
            </Label>
            <div className="relative">
              <Input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                required
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="h-11 rounded-lg border-border-strong/40 bg-white pr-11 pl-3.5 text-sm text-ink placeholder:text-ink-muted/70 transition-colors focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-radar-lime focus-visible:ring-offset-0"
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
          </div>

          {/* Formun en altında genişliği kaplayan 'Giriş yap' butonu */}
          <Button
            type="submit"
            disabled={isLoading}
            className="mt-3 flex h-11 w-full items-center justify-center rounded-lg bg-radar-lime text-sm font-bold text-ink shadow-xs transition-all hover:bg-[#bde829] active:translate-y-px disabled:pointer-events-none disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink sm:text-base"
          >
            {isLoading ? "Giriş yapılıyor..." : "Giriş yap"}
          </Button>
        </form>
      </div>
    </div>
  );
}
