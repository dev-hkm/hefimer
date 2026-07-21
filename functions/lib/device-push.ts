import { fromBase64Url, toBase64Url, type DeviceEnv } from "./device-auth";

const encoder = new TextEncoder();

export async function sendEmptyWebPush(env: DeviceEnv, endpoint: string) {
  if (!env.VAPID_PUBLIC_KEY || !env.VAPID_PRIVATE_JWK || !env.VAPID_SUBJECT) return false;
  const audience = new URL(endpoint).origin;
  const header = toBase64Url(encoder.encode(JSON.stringify({ typ: "JWT", alg: "ES256" })));
  const payload = toBase64Url(encoder.encode(JSON.stringify({
    aud: audience,
    exp: Math.floor(Date.now() / 1000) + 12 * 60 * 60,
    sub: env.VAPID_SUBJECT,
  })));
  const signingInput = `${header}.${payload}`;
  const privateKey = await crypto.subtle.importKey(
    "jwk",
    JSON.parse(env.VAPID_PRIVATE_JWK),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    { name: "ECDSA", hash: "SHA-256" },
    privateKey,
    encoder.encode(signingInput),
  );
  const token = `${signingInput}.${toBase64Url(signature)}`;
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Authorization: `vapid t=${token}, k=${env.VAPID_PUBLIC_KEY}`,
      TTL: "86400",
      Urgency: "high",
      "Content-Length": "0",
    },
  });
  return response.ok || response.status === 201;
}

export function vapidPublicKeyBytes(publicKey: string) {
  return fromBase64Url(publicKey);
}
