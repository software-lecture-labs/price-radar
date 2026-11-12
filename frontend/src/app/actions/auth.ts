"use server";

import { redirect } from "next/navigation";

import * as authApi from "@/lib/api/auth";
import { isApiError } from "@/lib/api/problem";
import { safeRedirectPath } from "@/lib/auth/redirect";
import { endSession, startSession } from "@/lib/auth/session";
import {
  type AuthFormState,
  type FieldErrors,
  type LoginField,
  type RegisterField,
  validateLogin,
  validateRegistration,
} from "@/lib/auth/validation";
import { NEXT_PARAM, routes } from "@/lib/routes";

const REGISTER_FIELDS: readonly RegisterField[] = ["fullName", "email", "phone", "password"];

export async function login(
  _previous: AuthFormState<LoginField>,
  formData: FormData,
): Promise<AuthFormState<LoginField>> {
  const input = validateLogin({ email: formData.get("email"), password: formData.get("password") });
  if (!input.ok) {
    return { message: "Lütfen e-posta adresinizi ve şifrenizi girin.", fieldErrors: input.errors };
  }

  let tokens: authApi.AuthTokens;
  try {
    tokens = await authApi.login(input.data);
  } catch (error) {
    if (isApiError(error) && (error.status === 400 || error.status === 401)) {
      return { message: "E-posta adresi veya şifre hatalı." };
    }
    console.error("Sign-in failed", error);
    return { message: "Şu anda giriş yapılamıyor. Lütfen biraz sonra tekrar deneyin." };
  }

  await startSession(tokens);
  redirect(safeRedirectPath(formData.get(NEXT_PARAM)));
}

export async function register(
  _previous: AuthFormState<RegisterField>,
  formData: FormData,
): Promise<AuthFormState<RegisterField>> {
  const input = validateRegistration({
    fullName: formData.get("fullName"),
    email: formData.get("email"),
    phone: formData.get("phone"),
    password: formData.get("password"),
    confirmPassword: formData.get("confirmPassword"),
  });
  if (!input.ok) {
    return { message: "Lütfen işaretli alanları düzeltin.", fieldErrors: input.errors };
  }

  let tokens: authApi.AuthTokens;
  try {
    tokens = await authApi.register(input.data);
  } catch (error) {
    if (isApiError(error) && error.status === 409) {
      return {
        message: "Bu e-posta adresiyle kayıtlı bir hesap zaten var.",
        fieldErrors: { email: "Bu e-posta adresi zaten kayıtlı." },
      };
    }
    if (isApiError(error) && error.status === 400) {
      // The checks above mirror the backend's, so this only happens if the two drift apart.
      const fieldErrors: FieldErrors<RegisterField> = {};
      for (const field of REGISTER_FIELDS) {
        if (error.fieldErrors[field]) fieldErrors[field] = "Bu alanı kontrol edin.";
      }
      return { message: "Lütfen işaretli alanları düzeltin.", fieldErrors };
    }
    console.error("Registration failed", error);
    return { message: "Şu anda kayıt olunamıyor. Lütfen biraz sonra tekrar deneyin." };
  }

  await startSession(tokens);
  redirect(safeRedirectPath(formData.get(NEXT_PARAM), routes.account));
}

export async function logout(): Promise<void> {
  const refreshToken = await endSession();
  if (refreshToken) {
    try {
      await authApi.logout(refreshToken);
    } catch (error) {
      // The cookies are already gone; the token simply expires on its own.
      console.error("Could not revoke the refresh token", error);
    }
  }
  redirect(routes.home);
}
