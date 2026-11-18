# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- deps stage
FROM node:24-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

# ---------------------------------------------------------------- build stage
FROM node:24-alpine AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# These must be present at BUILD time, not only at runtime:
# NEXT_PUBLIC_* values are inlined into the client bundle, and the /api/v1
# rewrite destination is baked into the routes manifest by `next build`.
ARG BACKEND_INTERNAL_URL=http://backend:8080
ARG NEXT_PUBLIC_API_BASE_PATH=/api/v1
ARG NEXT_PUBLIC_SITE_NAME="Price Radar"

ENV BACKEND_INTERNAL_URL=${BACKEND_INTERNAL_URL} \
    NEXT_PUBLIC_API_BASE_PATH=${NEXT_PUBLIC_API_BASE_PATH} \
    NEXT_PUBLIC_SITE_NAME=${NEXT_PUBLIC_SITE_NAME} \
    NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ---------------------------------------------------------------- run stage
FROM node:24-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

RUN addgroup -S -g 1001 nodejs && adduser -S -u 1001 -G nodejs nextjs

# `server.js` serves public/ and .next/static only if they are copied in.
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

CMD ["node", "server.js"]
