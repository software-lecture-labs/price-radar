#!/usr/bin/env node

import { existsSync } from "node:fs";
import { appendFile, mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { setTimeout as sleep } from "node:timers/promises";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(TOOL_DIR, "..", "..");
const PRODUCTS_FILE = path.join(TOOL_DIR, "products.json");
const DATA_DIR = path.join(REPO_ROOT, "data", "reefapi");
const RAW_DIR = path.join(DATA_DIR, "raw");
const LEDGER_FILE = path.join(DATA_DIR, "ledger.jsonl");

loadRootEnv();

const BASE_URL = (process.env.REEFAPI_BASE_URL ?? "https://api.reefapi.com").replace(/\/+$/, "");

const STORES = {
  trendyol: {
    idParam: "content_id",
    cost: { search: 2, "product/detail": 1, "product/variants": 1 },
    snapshot: ["product/detail"],
    once: ["product/variants"],
  },
  hepsiburada: {
    idParam: "sku",
    cost: { search: 3, "product/detail": 2 },
    snapshot: ["product/detail"],
    once: [],
  },
  n11: {
    idParam: "product_id",
    cost: { search: 2, "product/detail": 2 },
    snapshot: ["product/detail"],
    once: [],
  },
  teknosa: {
    idParam: "product_id",
    cost: { search: 1, "product/detail": 1 },
    snapshot: ["product/detail"],
    once: [],
  },
};

// These failures aren't billed.
const RETRYABLE = new Set(["RATE_LIMITED", "UPSTREAM_TIMEOUT", "TARGET_BLOCKED"]);
const FATAL = new Set(["AUTH_FAILED", "QUOTA_EXCEEDED"]);
const RETRY_DELAYS_MS = [2_000, 5_000];
const PAUSE_MS = 250;
const TIMEOUT_MS = 90_000;

const ONCE_FOLDER = "once";
const KEY_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const LABEL_PATTERN = /^[A-Za-z0-9][\w.-]*$/;

const USAGE = `Usage:
  node tools/reefapi/fetch.mjs credits
  node tools/reefapi/fetch.mjs search <store> "<query>" [--page N]
  node tools/reefapi/fetch.mjs snapshot [--label NAME] [--only STORE] [--product KEY]
                                        [--max-credits N] [--dry-run]

Stores: ${Object.keys(STORES).join(", ")}`;

class UsageError extends Error {}

async function main() {
  const { values, positionals } = parseArgs({
    allowPositionals: true,
    options: {
      page: { type: "string", default: "1" },
      label: { type: "string" },
      only: { type: "string" },
      product: { type: "string" },
      "max-credits": { type: "string", default: "250" },
      "dry-run": { type: "boolean", default: false },
      help: { type: "boolean", short: "h", default: false },
    },
  });
  const [command, ...args] = positionals;

  if (values.help || !command) {
    console.log(USAGE);
    return;
  }

  switch (command) {
    case "credits":
      return credits();
    case "search":
      return search(
        requireStore(args[0]),
        requireText(args[1], "query"),
        toPositiveInt(values.page, "--page"),
      );
    case "snapshot":
      return snapshot({
        label: requireLabel(values.label ?? today()),
        only: values.only === undefined ? undefined : requireStore(values.only),
        productKey: values.product,
        maxCredits: toPositiveInt(values["max-credits"], "--max-credits"),
        dryRun: values["dry-run"],
      });
    default:
      throw new UsageError(`Unknown command "${command}".`);
  }
}

async function credits() {
  const response = await fetch(`${BASE_URL}/account/v1/credits`, {
    headers: { "x-api-key": apiKey() },
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(`Balance check failed with HTTP ${response.status}: ${JSON.stringify(body)}`);
  }
  console.log(`ReefAPI balance: ${JSON.stringify(body?.data ?? body)}`);
  console.log(`Charged so far according to the local ledger: ${await ledgerTotal()} credits`);
}

async function search(store, query, page) {
  const request = { query, page };
  const file = rawPath(today(), store, "search", `${slug(query)}-p${page}`);
  let record = await readSaved(file);

  if (record) {
    console.log(`Already fetched today, nothing charged: ${relative(file)}`);
  } else {
    apiKey();
    const result = await call(store, "search", request);
    if (!isPaid(result)) {
      throw new Error(`Search failed: ${describe(result)}`);
    }
    record = await save(file, store, "search", request, result);
    console.log(`Saved ${relative(file)}: ${describe(result)}`);
  }

  printRows(store, record.response.data);
}

async function snapshot({ label, only, productKey, maxCredits, dryRun }) {
  const products = await loadProducts();
  if (products.length === 0) {
    console.log(`${relative(PRODUCTS_FILE)} is empty. Find products with "search" and add them.`);
    return;
  }

  const tasks = planSnapshot(products, label, only, productKey);
  const pending = tasks.filter((task) => !existsSync(task.file));
  const estimate = pending.reduce((total, task) => total + task.cost, 0);
  console.log(
    `Snapshot "${label}": ${pending.length} calls to make (~${estimate} credits), ` +
      `${tasks.length - pending.length} already saved.`,
  );

  if (dryRun) {
    for (const task of pending) {
      console.log(`  ${task.store} ${task.action} ${task.productKey} (~${task.cost} cr)`);
    }
    return;
  }
  if (pending.length > 0) {
    apiKey();
  }

  let spent = 0;
  let saved = 0;
  let failed = 0;
  for (const task of pending) {
    if (spent + task.cost > maxCredits) {
      console.log(`Stopping: the next call would go past --max-credits ${maxCredits}.`);
      break;
    }

    const result = await call(task.store, task.action, task.params);
    spent += result.charged;
    if (isPaid(result)) {
      await save(task.file, task.store, task.action, task.params, result);
      saved++;
    } else {
      failed++;
    }
    console.log(`  ${task.store} ${task.action} ${task.productKey}: ${describe(result)}`);

    if (FATAL.has(result.code)) {
      console.log("Stopping: every following call would fail the same way.");
      break;
    }
    await sleep(PAUSE_MS);
  }

  console.log(`Saved ${saved}, failed ${failed}, charged ${spent} credits in this run.`);
  if (failed > 0) {
    console.log("Failed calls were not saved. Run the same command again to retry only those.");
  }
}

function planSnapshot(products, label, only, productKey) {
  const selected = productKey ? products.filter((product) => product.key === productKey) : products;
  if (productKey && selected.length === 0) {
    throw new UsageError(`No product with key "${productKey}" in products.json.`);
  }

  const tasks = [];
  for (const product of selected) {
    for (const [store, params] of Object.entries(product.stores ?? {})) {
      if (only && store !== only) {
        continue;
      }
      const config = STORES[store];
      const groups = [
        [ONCE_FOLDER, config.once],
        [label, config.snapshot],
      ];
      for (const [folder, actions] of groups) {
        for (const action of actions) {
          tasks.push({
            store,
            action,
            params,
            productKey: product.key,
            cost: config.cost[action],
            file: rawPath(folder, store, action, product.key),
          });
        }
      }
    }
  }
  return tasks;
}

async function call(store, action, params) {
  for (let attempt = 1; ; attempt++) {
    const result = await post(store, action, params);
    await appendLedger({
      at: result.fetchedAt,
      store,
      action,
      params,
      attempt,
      httpStatus: result.httpStatus,
      ok: result.body?.ok === true,
      code: result.code,
      charged: result.charged,
      remaining: result.remaining,
    });

    const delay = RETRY_DELAYS_MS[attempt - 1];
    if (!RETRYABLE.has(result.code) || delay === undefined) {
      return result;
    }
    console.log(`  ${store} ${action}: ${result.code}, retrying in ${delay / 1000}s (free)`);
    await sleep(delay);
  }
}

async function post(store, action, params) {
  const key = apiKey();
  let response;
  try {
    response = await fetch(`${BASE_URL}/${store}/v1/${action}`, {
      method: "POST",
      headers: { "x-api-key": key, "content-type": "application/json" },
      body: JSON.stringify(params),
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
  } catch (error) {
    return {
      fetchedAt: new Date().toISOString(),
      httpStatus: null,
      body: null,
      code: "NETWORK",
      message: error.message,
      charged: 0,
      remaining: null,
    };
  }

  const fetchedAt = new Date().toISOString();
  const body = await response.json().catch(() => null);
  return {
    fetchedAt,
    httpStatus: response.status,
    body,
    code: body?.ok === true ? null : errorCode(body, response.status),
    message: typeof body?.error?.message === "string" ? body.error.message : null,
    charged: Number(body?.meta?.charged_credits ?? 0),
    remaining: toNumberOrNull(response.headers.get("x-credits-remaining")),
  };
}

function errorCode(body, httpStatus) {
  const error = body?.error;
  if (typeof error === "string") {
    return error;
  }
  return error?.code ?? `HTTP_${httpStatus}`;
}

function isPaid(result) {
  return result.body !== null && (result.body.ok === true || result.charged > 0);
}

function describe(result) {
  const parts = [result.body?.ok === true ? "ok" : result.code, `${result.charged} cr`];
  if (result.remaining !== null) {
    parts.push(`${result.remaining} left`);
  }
  if (result.message) {
    parts.push(result.message);
  }
  return parts.join(", ");
}

function apiKey() {
  const key = process.env.REEFAPI_API_KEY?.trim();
  if (!key) {
    throw new Error(
      "REEFAPI_API_KEY is not set. Add it to the root .env (git-ignored) or set it in the shell.",
    );
  }
  return key;
}

async function loadProducts() {
  let parsed;
  try {
    parsed = JSON.parse(await readFile(PRODUCTS_FILE, "utf8"));
  } catch (error) {
    throw new Error(`Cannot read ${relative(PRODUCTS_FILE)}: ${error.message}`);
  }

  const products = parsed?.products;
  if (!Array.isArray(products)) {
    throw new Error(`${relative(PRODUCTS_FILE)} must contain a "products" array.`);
  }

  const keys = new Set();
  for (const product of products) {
    const where = `products.json, product "${product?.key}"`;
    if (typeof product?.key !== "string" || !KEY_PATTERN.test(product.key)) {
      throw new Error(`${where}: "key" must be lowercase words joined by "-".`);
    }
    if (keys.has(product.key)) {
      throw new Error(`${where}: duplicate key.`);
    }
    keys.add(product.key);

    for (const [store, params] of Object.entries(product.stores ?? {})) {
      const config = STORES[store];
      if (!config) {
        throw new Error(`${where}: unknown store "${store}".`);
      }
      if (params?.[config.idParam] === undefined && params?.url === undefined) {
        throw new Error(`${where}: ${store} needs "${config.idParam}" or "url".`);
      }
    }
  }
  return products;
}

function rawPath(folder, store, action, name) {
  return path.join(RAW_DIR, folder, store, action.replace("/", "-"), `${name}.json`);
}

async function readSaved(file) {
  return existsSync(file) ? JSON.parse(await readFile(file, "utf8")) : null;
}

async function save(file, store, action, request, result) {
  const record = {
    fetchedAt: result.fetchedAt,
    store,
    action,
    request,
    httpStatus: result.httpStatus,
    chargedCredits: result.charged,
    response: result.body,
  };
  await mkdir(path.dirname(file), { recursive: true });
  // Never overwrite a paid response.
  await writeFile(file, `${JSON.stringify(record, null, 2)}\n`, { flag: "wx" });
  return record;
}

async function appendLedger(entry) {
  await mkdir(DATA_DIR, { recursive: true });
  await appendFile(LEDGER_FILE, `${JSON.stringify(entry)}\n`);
}

async function ledgerTotal() {
  if (!existsSync(LEDGER_FILE)) {
    return 0;
  }
  const lines = (await readFile(LEDGER_FILE, "utf8")).split("\n").filter(Boolean);
  return lines.reduce((total, line) => total + (JSON.parse(line).charged ?? 0), 0);
}

function loadRootEnv() {
  try {
    process.loadEnvFile(path.join(REPO_ROOT, ".env"));
  } catch (error) {
    if (error.code !== "ENOENT") {
      throw error;
    }
  }
}

function printRows(store, data) {
  const rows = findRows(data);
  if (rows.length === 0) {
    console.log("No result rows found in the response. Check the saved file.");
    return;
  }

  const { idParam } = STORES[store];
  console.log(`${rows.length} results. Columns: ${idParam}, price, barcode, title.`);
  for (const row of rows) {
    const id = row[idParam] ?? row.id ?? "?";
    const barcode = row.barcode ?? row.gtin ?? "-";
    const title = String(row.title ?? row.name ?? "").slice(0, 70);
    console.log(`  ${id}\t${priceOf(row)}\t${barcode}\t${title}`);
  }
}

function findRows(data) {
  if (Array.isArray(data)) {
    return data;
  }
  if (data === null || typeof data !== "object") {
    return [];
  }
  for (const key of ["results", "products", "items"]) {
    if (Array.isArray(data[key])) {
      return data[key];
    }
  }
  const list = Object.values(data).find(
    (value) => Array.isArray(value) && typeof value[0] === "object",
  );
  return list ?? [];
}

function priceOf(row) {
  const price = row.price_value ?? row.price;
  if (price !== null && typeof price === "object") {
    return price.current_value ?? price.current ?? "?";
  }
  return price ?? "?";
}

function requireStore(name) {
  if (!Object.hasOwn(STORES, name ?? "")) {
    throw new UsageError(`Unknown store "${name ?? ""}".`);
  }
  return name;
}

function requireText(value, name) {
  if (!value?.trim()) {
    throw new UsageError(`Missing ${name}.`);
  }
  return value.trim();
}

function requireLabel(label) {
  if (!LABEL_PATTERN.test(label) || label === ONCE_FOLDER) {
    throw new UsageError(`Invalid --label "${label}". Use letters, digits, ".", "_" or "-".`);
  }
  return label;
}

function toPositiveInt(value, flag) {
  const number = Number(value);
  if (!Number.isInteger(number) || number < 1) {
    throw new UsageError(`${flag} must be a positive whole number.`);
  }
  return number;
}

function toNumberOrNull(value) {
  const number = Number(value);
  return value === null || Number.isNaN(number) ? null : number;
}

function today() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function slug(text) {
  const folded = text
    .toLocaleLowerCase("tr")
    .replaceAll("ı", "i")
    .normalize("NFKD")
    .replace(/[̀-ͯ]/g, "");
  return folded.replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "query";
}

function relative(file) {
  return path.relative(process.cwd(), file);
}

main().catch((error) => {
  console.error(
    error instanceof UsageError ? `${error.message}\n\n${USAGE}` : `Error: ${error.message}`,
  );
  process.exitCode = 1;
});
