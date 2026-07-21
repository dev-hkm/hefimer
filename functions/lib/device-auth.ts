export interface D1PreparedStatementLike {
  bind(...values: unknown[]): D1PreparedStatementLike;
  first<T = Record<string, unknown>>(): Promise<T | null>;
  run(): Promise<{ success: boolean; meta?: { changes?: number } }>;
  all<T = Record<string, unknown>>(): Promise<{ results: T[] }>;
}

export interface D1DatabaseLike {
  prepare(query: string): D1PreparedStatementLike;
  batch<T = unknown>(statements: D1PreparedStatementLike[]): Promise<T[]>;
}

export interface KVNamespaceLike {
  get(key: string): Promise<string | null>;
  put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void>;
  delete(key: string): Promise<void>;
}

export interface DeviceEnv {
  HEFIMER_DB: D1DatabaseLike;
  PAIR_TOKENS: KVNamespaceLike;
  VAPID_PUBLIC_KEY?: string;
  VAPID_PRIVATE_JWK?: string;
  VAPID_SUBJECT?: string;
}

export interface AuthenticatedDevice {
  id: string;
  name: string;
}

const encoder = new TextEncoder();
const AUTH_WINDOW_MS = 5 * 60 * 1000;

export function toBase64Url(value: ArrayBuffer | Uint8Array) {
  const bytes = value instanceof Uint8Array ? value : new Uint8Array(value);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

export function fromBase64Url(value: string) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  const binary = atob(padded);
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

export async function sha256Base64Url(value: string) {
  return toBase64Url(await crypto.subtle.digest("SHA-256", encoder.encode(value)));
}

export function buildSignaturePayload(
  timestamp: string,
  nonce: string,
  method: string,
  pathAndQuery: string,
  bodyHash: string,
) {
  return [timestamp, nonce, method.toUpperCase(), pathAndQuery, bodyHash].join("\n");
}

export async function authenticateDevice(
  request: Request,
  env: DeviceEnv,
  bodyText: string,
): Promise<AuthenticatedDevice> {
  const deviceId = request.headers.get("X-Hefimer-Device") || "";
  const timestamp = request.headers.get("X-Hefimer-Time") || "";
  const nonce = request.headers.get("X-Hefimer-Nonce") || "";
  const signature = request.headers.get("X-Hefimer-Signature") || "";
  const parsedTime = Number(timestamp);

  if (!/^[A-Za-z0-9_-]{20,80}$/.test(deviceId) || !/^\d{10,16}$/.test(timestamp)) {
    throw new Response(JSON.stringify({ error: "Invalid device authentication" }), { status: 401 });
  }
  if (!/^[A-Za-z0-9_-]{16,80}$/.test(nonce) || !signature) {
    throw new Response(JSON.stringify({ error: "Incomplete device signature" }), { status: 401 });
  }
  if (Math.abs(Date.now() - parsedTime) > AUTH_WINDOW_MS) {
    throw new Response(JSON.stringify({ error: "Device signature expired" }), { status: 401 });
  }

  const device = await env.HEFIMER_DB.prepare(
    "SELECT id, name, public_key FROM devices WHERE id = ?",
  ).bind(deviceId).first<{ id: string; name: string; public_key: string }>();
  if (!device) {
    throw new Response(JSON.stringify({ error: "Unknown device" }), { status: 401 });
  }

  const url = new URL(request.url);
  const bodyHash = await sha256Base64Url(bodyText);
  const signedPayload = buildSignaturePayload(
    timestamp,
    nonce,
    request.method,
    `${url.pathname}${url.search}`,
    bodyHash,
  );
  let verified = false;
  try {
    const publicKey = await crypto.subtle.importKey(
      "spki",
      fromBase64Url(device.public_key),
      { name: "ECDSA", namedCurve: "P-256" },
      false,
      ["verify"],
    );
    verified = await crypto.subtle.verify(
      { name: "ECDSA", hash: "SHA-256" },
      publicKey,
      fromBase64Url(signature),
      encoder.encode(signedPayload),
    );
  } catch {
    verified = false;
  }
  if (!verified) {
    throw new Response(JSON.stringify({ error: "Invalid device signature" }), { status: 401 });
  }

  if (!["GET", "HEAD", "OPTIONS"].includes(request.method.toUpperCase())) {
    await env.HEFIMER_DB.prepare("DELETE FROM auth_nonces WHERE expires_at < ?")
      .bind(Date.now()).run();
    const nonceResult = await env.HEFIMER_DB.prepare(
      "INSERT OR IGNORE INTO auth_nonces (nonce, device_id, expires_at) VALUES (?, ?, ?)",
    ).bind(nonce, deviceId, Date.now() + AUTH_WINDOW_MS).run();
    if ((nonceResult.meta?.changes || 0) !== 1) {
      throw new Response(JSON.stringify({ error: "Request was already used" }), { status: 409 });
    }
  }

  await env.HEFIMER_DB.prepare("UPDATE devices SET last_seen_at = ? WHERE id = ?")
    .bind(Date.now(), deviceId).run();
  return { id: device.id, name: device.name };
}

export function randomId(prefix: string, bytes = 18) {
  const random = new Uint8Array(bytes);
  crypto.getRandomValues(random);
  return `${prefix}${toBase64Url(random)}`;
}

export function json(data: unknown, status = 200, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      ...headers,
    },
  });
}

