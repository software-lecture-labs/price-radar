import type { NextConfig } from "next";

/**
 * Where the Spring Boot API lives as seen from the Next.js *server*.
 * In Docker Compose this is the service name; locally it is localhost.
 */
const backendUrl = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Self-contained server bundle for the Docker image.
  output: "standalone",
  reactStrictMode: true,
  poweredByHeader: false,

  images: {
    // Merchant logos and product photos are served from third-party hosts.
    // Add each host explicitly before using <Image> with its URLs.
    remotePatterns: [],
  },

  // Keeps the browser on a single origin: calls to /api/v1/* are proxied to
  // the backend, so CORS only matters for non-browser clients.
  //
  // NOTE: rewrites are resolved during `next build` and baked into the routes
  // manifest. BACKEND_INTERNAL_URL must therefore be supplied at BUILD time
  // (see the Dockerfile build arg); setting it only at runtime has no effect
  // on this rewrite. Server Components read it at runtime via serverEnv.
  rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: `${backendUrl}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
