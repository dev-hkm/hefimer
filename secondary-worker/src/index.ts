import { onRequest as handleDevices } from "../../functions/api/devices/[[path]]";
import { onRequestGet as handleDownload } from "../../functions/api/download";
import { onRequest as handleLitterbox } from "../../functions/api/proxy/litterbox";
import { onRequest as handleTmpfiles } from "../../functions/api/proxy/tmpfiles";
import { onRequest as handleStorageTo } from "../../functions/api/proxy/storageto/[[path]]";
import type { DeviceEnv } from "../../functions/lib/device-auth";

interface WorkerEnv extends DeviceEnv {
  ALLOWED_ORIGINS?: string;
}

const DEFAULT_ALLOWED_ORIGINS = [
  "https://hefimer.web.app",
  "https://hefimer.firebaseapp.com",
  "http://127.0.0.1:4176",
];

const ALLOWED_METHODS = "GET, POST, PATCH, DELETE, OPTIONS";
const ALLOWED_HEADERS = [
  "Content-Type",
  "Range",
  "Authorization",
  "X-Visitor-Token",
  "X-Owner-Token",
  "X-Hefimer-Device",
  "X-Hefimer-Time",
  "X-Hefimer-Nonce",
  "X-Hefimer-Signature",
].join(", ");
const EXPOSED_HEADERS = [
  "Content-Disposition",
  "Content-Length",
  "Content-Range",
  "Accept-Ranges",
  "ETag",
  "Last-Modified",
].join(", ");

function json(body: Record<string, unknown>, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
    },
  });
}

function allowedOrigins(env: WorkerEnv) {
  const configured = env.ALLOWED_ORIGINS?.split(",").map((value) => value.trim()).filter(Boolean);
  return new Set(configured?.length ? configured : DEFAULT_ALLOWED_ORIGINS);
}

function callerOrigin(request: Request, env: WorkerEnv) {
  const candidate = request.headers.get("Origin") || request.headers.get("Referer");
  if (!candidate) return null;
  try {
    const origin = new URL(candidate).origin;
    return allowedOrigins(env).has(origin) ? origin : null;
  } catch {
    return null;
  }
}

function trustedRequest(request: Request, origin: string) {
  const url = new URL(request.url);
  const frontend = new URL(origin);
  url.protocol = frontend.protocol;
  url.host = frontend.host;

  const trusted = new Request(url.toString(), request);
  trusted.headers.set("Origin", origin);
  trusted.headers.set("Referer", `${origin}/`);
  trusted.headers.set("Sec-Fetch-Site", "same-origin");
  return trusted;
}

function appendVary(headers: Headers, value: string) {
  const existing = headers.get("Vary");
  const values = new Set((existing ? existing.split(",") : []).map((item) => item.trim()).filter(Boolean));
  values.add(value);
  headers.set("Vary", [...values].join(", "));
}

function withCors(response: Response, origin: string) {
  const headers = new Headers(response.headers);
  headers.set("Access-Control-Allow-Origin", origin);
  headers.set("Access-Control-Expose-Headers", EXPOSED_HEADERS);
  appendVary(headers, "Origin");
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}

function preflight(origin: string) {
  return new Response(null, {
    status: 204,
    headers: {
      "Access-Control-Allow-Origin": origin,
      "Access-Control-Allow-Methods": ALLOWED_METHODS,
      "Access-Control-Allow-Headers": ALLOWED_HEADERS,
      "Access-Control-Max-Age": "86400",
      Vary: "Origin, Access-Control-Request-Method, Access-Control-Request-Headers",
    },
  });
}

async function dispatch(request: Request, env: WorkerEnv) {
  const url = new URL(request.url);
  const { pathname } = url;

  if (pathname.startsWith("/api/r2/")) {
    return json({ error: "Secret R2 mode is not available on this mirror" }, 503);
  }
  if (pathname === "/api/download") {
    if (request.method !== "GET") return json({ error: "Method not allowed" }, 405);
    return handleDownload({ request });
  }
  if (pathname === "/api/proxy/litterbox") return handleLitterbox({ request, env });
  if (pathname === "/api/proxy/tmpfiles") return handleTmpfiles({ request, env });
  if (pathname === "/api/devices" || pathname.startsWith("/api/devices/")) {
    return handleDevices({ request, env });
  }
  if (pathname === "/api/proxy/storageto" || pathname.startsWith("/api/proxy/storageto/")) {
    const path = pathname.replace(/^\/api\/proxy\/storageto\/?/, "").split("/").filter(Boolean);
    return handleStorageTo({ request, env, params: { path } });
  }
  return json({ error: "Route not found" }, 404);
}

export default {
  async fetch(request: Request, env: WorkerEnv) {
    const url = new URL(request.url);
    if (url.pathname === "/health") {
      if (request.method !== "GET") return json({ error: "Method not allowed" }, 405);
      return json({ ok: true, service: "hefimer-secondary-api" }, 200);
    }

    const origin = callerOrigin(request, env);
    if (!origin) return json({ error: "Origin is not allowed" }, 403);
    if (request.method === "OPTIONS") return preflight(origin);

    try {
      return withCors(await dispatch(trustedRequest(request, origin), env), origin);
    } catch (error) {
      console.error("Secondary Worker request failed", error);
      return withCors(json({ error: "Secondary backend unavailable" }, 500), origin);
    }
  },
};
