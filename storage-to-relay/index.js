import { onRequest } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";

const STORAGE_TO_API = "https://storage.to/api/";
const STORAGE_TO_BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";
const MAX_BODY_BYTES = 64 * 1024;
const RATE_WINDOW_MS = 60_000;
const RATE_MAX_REQUESTS = 30;
const requestBuckets = new Map();

const CONTROL_PATHS = new Set([
  "upload/init",
  "upload/confirm",
  "upload/parts",
  "upload/complete-multipart",
]);

function isAllowedOrigin(origin) {
  if (!origin) return false;
  if (["https://hefimer.qzz.io", "http://127.0.0.1:3000", "http://localhost:3000"].includes(origin)) {
    return true;
  }
  return /^https:\/\/[a-z0-9-]+\.hefimer\.pages\.dev$/i.test(origin);
}

function setCors(res, origin) {
  res.set("Vary", "Origin");
  res.set("Access-Control-Allow-Origin", origin);
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, X-Visitor-Token, X-Owner-Token, Authorization");
  res.set("Access-Control-Max-Age", "600");
}

function isAllowedControlPath(value) {
  if (CONTROL_PATHS.has(value)) return true;
  return /^file\/[A-Za-z0-9_-]{1,128}\/expiry$/.test(value);
}

function isRateLimited(req) {
  const key = req.get("x-forwarded-for")?.split(",")[0]?.trim() || "unknown";
  const now = Date.now();
  const bucket = requestBuckets.get(key);
  if (!bucket || bucket.resetAt <= now) {
    requestBuckets.set(key, { count: 1, resetAt: now + RATE_WINDOW_MS });
    return false;
  }
  bucket.count += 1;
  return bucket.count > RATE_MAX_REQUESTS;
}

function upstreamHeaders(req, origin) {
  const headers = new Headers({
    "Content-Type": "application/json",
    Accept: "application/json, text/plain, */*",
    "User-Agent": STORAGE_TO_BROWSER_USER_AGENT,
    Origin: origin,
    Referer: `${origin}/`,
  });
  for (const key of ["x-visitor-token", "x-owner-token", "authorization"]) {
    const value = req.get(key);
    if (value) headers.set(key, value);
  }
  return headers;
}

export const storageToRelay = onRequest({
  region: "asia-southeast1",
  timeoutSeconds: 30,
  memory: "256MiB",
  maxInstances: 3,
  invoker: "public",
}, async (req, res) => {
  const origin = req.get("origin") || "";
  if (!isAllowedOrigin(origin)) {
    res.status(403).json({ error: "Origin is not allowed" });
    return;
  }
  setCors(res, origin);

  if (req.method === "OPTIONS") {
    res.status(204).send();
    return;
  }
  if (req.method !== "POST") {
    res.status(405).json({ error: "Only POST is allowed" });
    return;
  }
  if (isRateLimited(req)) {
    res.status(429).json({ error: "Too many relay requests" });
    return;
  }
  if (Number(req.get("content-length") || 0) > MAX_BODY_BYTES) {
    res.status(413).json({ error: "Relay accepts metadata only" });
    return;
  }

  const controlPath = typeof req.query.path === "string" ? req.query.path : "";
  if (!isAllowedControlPath(controlPath)) {
    res.status(400).json({ error: "Unsupported Storage.to control path" });
    return;
  }

  try {
    const upstream = await fetch(new URL(controlPath, STORAGE_TO_API), {
      method: "POST",
      headers: upstreamHeaders(req, origin),
      body: JSON.stringify(req.body || {}),
    });
    const payload = await upstream.text();
    res.status(upstream.status);
    res.set("Cache-Control", "no-store");
    res.type(upstream.headers.get("content-type") || "application/json");
    res.send(payload);
  } catch (error) {
    logger.error("Storage.to relay request failed", { controlPath, message: error instanceof Error ? error.message : String(error) });
    res.status(502).json({ error: "Storage.to relay is temporarily unavailable" });
  }
});
