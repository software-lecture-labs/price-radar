import { apiFetch } from "@/lib/api/client";

export interface PingResponse {
  service: string;
  apiVersion: string;
  timestamp: string;
}

export function getPing(): Promise<PingResponse> {
  return apiFetch<PingResponse>("/ping", { cache: "no-store" });
}

export interface HelloResponse {
  message: string;
  service: string;
}

export function getHello(): Promise<HelloResponse> {
  return apiFetch<HelloResponse>("/hello", { cache: "no-store" });
}
