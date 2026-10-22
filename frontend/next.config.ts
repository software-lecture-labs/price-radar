import type { NextConfig } from "next";
import { publicEnv, serverEnv } from "./src/lib/env";

const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
  poweredByHeader: false,

  images: {
    remotePatterns: [],
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
