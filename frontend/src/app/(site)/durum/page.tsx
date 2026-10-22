import type { Metadata } from "next";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { getHello, getPing, type PingResponse } from "@/lib/api/meta";
import { publicEnv } from "@/lib/env";

type BackendStatus = { ok: true; data: PingResponse } | { ok: false; reason: string };

async function readBackendStatus(): Promise<BackendStatus> {
  try {
    return { ok: true, data: await getPing() };
  } catch (error) {
    return { ok: false, reason: error instanceof Error ? error.message : "Unknown error" };
  }
}

async function readGreeting(): Promise<{ message: string; source: "backend" | "frontend" }> {
  try {
    return { message: (await getHello()).message, source: "backend" };
  } catch {
    return { message: "Hello World", source: "frontend" };
  }
}

const stack = [
  { layer: "Backend", value: "Java 25 · Spring Boot 4.1.1" },
  { layer: "Frontend", value: "Next.js 16.3.8 · React 19.2.8 · TypeScript 5" },
  { layer: "UI", value: "Tailwind CSS 4 · shadcn/ui" },
  { layer: "Database", value: "PostgreSQL · Flyway" },
  { layer: "Auth", value: "Spring Security · JWT" },
  { layer: "API docs", value: "springdoc-openapi 3.1.1" },
  { layer: "Testing", value: "JUnit 5 · Mockito" },
  { layer: "Code quality", value: "SonarQube · JaCoCo" },
];

export const metadata: Metadata = { title: "Sistem durumu" };

export default async function StatusPage() {
  const [status, greeting] = await Promise.all([readBackendStatus(), readGreeting()]);

  return (
    <div className="mx-auto w-full max-w-3xl">
      <header className="space-y-2">
        <p className="font-mono text-xs tracking-widest text-muted-foreground uppercase">
          project setup
        </p>
        <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">{publicEnv.siteName}</h1>
        <p className="text-muted-foreground">
          Stack is installed and wired up. No application features yet.
        </p>
      </header>

      <Separator className="my-8" />

      <section className="rounded-lg border bg-card px-6 py-10 text-center">
        <p className="text-4xl font-semibold tracking-tight sm:text-5xl">{greeting.message}</p>
        <p className="mt-3 font-mono text-xs text-muted-foreground">
          {greeting.source === "backend"
            ? `served by ${publicEnv.apiBasePath}/hello`
            : "backend unreachable — rendered locally"}
        </p>
      </section>

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1">
              <CardTitle>Backend connection</CardTitle>
              <CardDescription className="font-mono text-xs">
                GET {publicEnv.apiBasePath}/ping
              </CardDescription>
            </div>
            <Badge variant={status.ok ? "default" : "destructive"}>
              {status.ok ? "connected" : "unreachable"}
            </Badge>
          </div>
        </CardHeader>
        <CardContent>
          {status.ok ? (
            <dl className="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-[auto_1fr]">
              <dt className="text-muted-foreground">Service</dt>
              <dd className="font-mono">{status.data.service}</dd>
              <dt className="text-muted-foreground">API version</dt>
              <dd className="font-mono">{status.data.apiVersion}</dd>
              <dt className="text-muted-foreground">Server time</dt>
              <dd className="font-mono">{status.data.timestamp}</dd>
            </dl>
          ) : (
            <div className="space-y-2 text-sm">
              <p className="text-muted-foreground">
                Check the PostgreSQL connection and make sure the backend is running.
              </p>
              <p className="font-mono text-xs text-destructive">{status.reason}</p>
            </div>
          )}
        </CardContent>
      </Card>

      <section className="mt-8">
        <h2 className="mb-3 text-sm font-medium text-muted-foreground">Installed layers</h2>
        <dl className="divide-y rounded-lg border">
          {stack.map((item) => (
            <div
              key={item.layer}
              className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-baseline sm:justify-between"
            >
              <dt className="text-sm font-medium">{item.layer}</dt>
              <dd className="font-mono text-xs text-muted-foreground">{item.value}</dd>
            </div>
          ))}
        </dl>
      </section>
    </div>
  );
}
