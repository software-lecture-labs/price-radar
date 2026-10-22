import type { NextConfig } from "next";
import { publicEnv, serverEnv } from "./src/lib/env";

const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
  poweredByHeader: false,

  images: {
    remotePatterns: [
      { protocol: "https", hostname: "cdn.dsmcdn.com" },
      { protocol: "https", hostname: "productimages.hepsiburada.net" },
      { protocol: "https", hostname: "n11scdn.akamaized.net" },
      { protocol: "https", hostname: "reimg-teknosa-cloud-prod.mncdn.com" },
    ],
  },

  rewrites() {
    return [
      {
        source: `${publicEnv.apiBasePath}/:path*`,
        destination: `${serverEnv.backendUrl}${serverEnv.apiBasePath}/:path*`,
      },
    ];
  },
};

export default nextConfig;
