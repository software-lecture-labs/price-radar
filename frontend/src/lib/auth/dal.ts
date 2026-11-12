import "server-only";

import { notFound, redirect } from "next/navigation";
import { cache } from "react";

import { getCurrentUser as fetchCurrentUser, type UserProfile } from "@/lib/api/auth";
import { isApiError } from "@/lib/api/problem";
import { readAccessToken } from "@/lib/auth/session";
import { routes } from "@/lib/routes";

/**
 * The signed-in user, verified by the backend at most once per request. `null` when signed out or the token
 * is no longer accepted; other backend failures are thrown.
 */
export const getCurrentUser = cache(async (): Promise<UserProfile | null> => {
  const token = await readAccessToken();
  if (!token) {
    return null;
  }
  try {
    return await fetchCurrentUser(token);
  } catch (error) {
    if (isApiError(error) && error.status === 401) {
      return null;
    }
    throw error;
  }
});

/** For UI that only changes with the session, such as the header: a backend outage reads as signed out. */
export async function findCurrentUser(): Promise<UserProfile | null> {
  try {
    return await getCurrentUser();
  } catch (error) {
    console.error("Could not load the signed-in user", error);
    return null;
  }
}

/**
 * Call from every page that needs a session. Layouts are not re-rendered on client navigation, so a check
 * there would not run for each page.
 */
export async function requireUser(returnTo: string): Promise<UserProfile> {
  const user = await getCurrentUser();
  if (!user) {
    redirect(routes.login(returnTo));
  }
  return user;
}

/** Non-admins get a 404 rather than a hint that the page exists. */
export async function requireAdmin(returnTo: string): Promise<UserProfile> {
  const user = await requireUser(returnTo);
  if (user.role !== "ADMIN") {
    notFound();
  }
  return user;
}
