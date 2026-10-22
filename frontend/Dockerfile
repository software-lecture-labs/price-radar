# syntax=docker/dockerfile:1

FROM node:24-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

FROM node:24-alpine AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Public env values and rewrites are fixed at build time.
ARG BACKEND_INTERNAL_URL=http://backend:8080
ARG NEXT_PUBLIC_API_BASE_PATH=/api/v1
ARG NEXT_PUBLIC_SITE_NAME="Price Radar"

ENV BACKEND_INTERNAL_URL=${BACKEND_INTERNAL_URL} \
    NEXT_PUBLIC_API_BASE_PATH=${NEXT_PUBLIC_API_BASE_PATH} \
    NEXT_PUBLIC_SITE_NAME=${NEXT_PUBLIC_SITE_NAME} \
    NEXT_TELEMETRY_DISABLED=1

RUN npm run build

FROM node:24-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

RUN addgroup -S -g 1001 nodejs && adduser -S -u 1001 -G nodejs nextjs

COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

CMD ["node", "server.js"]
