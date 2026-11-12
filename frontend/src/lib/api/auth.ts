import { apiFetch } from "@/lib/api/client";
import type { LoginInput, RegisterInput } from "@/lib/auth/validation";

export type Role = "USER" | "ADMIN";

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  /** E.164, for example +905321234567. */
  phone: string;
  role: Role;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  user: UserProfile;
}

export interface AccessToken {
  accessToken: string;
  accessTokenExpiresAt: string;
}

// Called only from the server (Server Actions, the DAL and the proxy): tokens never reach browser JavaScript.

export function login(input: LoginInput): Promise<AuthTokens> {
  return apiFetch<AuthTokens>("/auth/login", { method: "POST", body: input, cache: "no-store" });
}

export function register(input: RegisterInput): Promise<AuthTokens> {
  return apiFetch<AuthTokens>("/auth/register", { method: "POST", body: input, cache: "no-store" });
}

export function refreshAccessToken(refreshToken: string): Promise<AccessToken> {
  return apiFetch<AccessToken>("/auth/refresh", {
    method: "POST",
    body: { refreshToken },
    cache: "no-store",
  });
}

export function logout(refreshToken: string): Promise<void> {
  return apiFetch<void>("/auth/logout", {
    method: "POST",
    body: { refreshToken },
    cache: "no-store",
  });
}

export function getCurrentUser(accessToken: string): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/me", { token: accessToken, cache: "no-store" });
}
