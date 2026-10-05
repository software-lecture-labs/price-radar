import { apiFetch } from "@/lib/api/client";

export interface PingResponse {
  service: string;
  apiVersion: string;
  timestamp: string;
}

/** Mirrors `GET /api/v1/ping` on the backend. */
export function getPing(): Promise<PingResponse> {
  return apiFetch<PingResponse>("/ping", { cache: "no-store" });
}
