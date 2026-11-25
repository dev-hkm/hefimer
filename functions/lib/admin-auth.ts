export interface AdminSecurityKV {
  adminHash: string;
  adminSalt: string;
  secretHash: string;
  secretSalt: string;
  updatedAt: number;
}

export interface GlobalStatsKV {
  isFrozen: boolean;
  developer: string;
  likes: number;
  notice: string;
  noticeExpiresAt: number;
  broadcast: {
    id: string;
    title: string;
    message: string;
    icon: string;
    theme: string;
    timestamp: number;
    expiresAt: number;
  } | null;
}

export interface AdminEnv {
  PAIR_TOKENS?: KVNamespace;
  ADMIN_PASS_DEFAULT?: string;
  SECRET_PASS_DEFAULT?: string;
  ADMIN_SESSION_SECRET?: string;
}

const SECURITY_KV_KEY = "hefimer_admin_security_v1";
const STATS_KV_KEY = "hefimer_global_stats_v1";
const SESSION_SECRET_KV_KEY = "hefimer_admin_hmac_secret_v1";

function toHex(buf: ArrayBuffer | Uint8Array): string {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf);
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}

function fromHex(hex: string): Uint8Array {
  const clean = hex.trim();
  const out = new Uint8Array(clean.length / 2);
  for (let i = 0; i < out.length; i++) {
    out[i] = parseInt(clean.slice(i * 2, i * 2 + 2), 16);
  }
  return out;
}

function timingSafeEqualStr(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

function randomHex(byteLen = 16): string {
  const bytes = new Uint8Array(byteLen);
  crypto.getRandomValues(bytes);
  return toHex(bytes);
}

async function hashPassword(password: string, saltHex: string): Promise<string> {
  const enc = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    enc.encode(password),
    "PBKDF2",
    false,
    ["deriveBits"],
  );
  const derived = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      salt: fromHex(saltHex),
      iterations: 100_000,
      hash: "SHA-256",
    },
    keyMaterial,
    256,
  );
  return toHex(derived);
}

export async function getOrInitSecurity(env: AdminEnv): Promise<AdminSecurityKV> {
  if (env.PAIR_TOKENS) {
    const existing = await env.PAIR_TOKENS.get<AdminSecurityKV>(SECURITY_KV_KEY, "json");
    if (existing?.adminHash && existing?.secretHash) return existing;
  }

  const defaultAdmin = (env.ADMIN_PASS_DEFAULT || "meobinh95").trim();
  const defaultSecret = (env.SECRET_PASS_DEFAULT || "meobinh905").trim();
  const adminSalt = randomHex(16);
  const secretSalt = randomHex(16);
  const seeded: AdminSecurityKV = {
    adminHash: await hashPassword(defaultAdmin, adminSalt),
    adminSalt,
    secretHash: await hashPassword(defaultSecret, secretSalt),
    secretSalt,
    updatedAt: Date.now(),
  };

  if (env.PAIR_TOKENS) {
    await env.PAIR_TOKENS.put(SECURITY_KV_KEY, JSON.stringify(seeded));
  }
  return seeded;
}

export async function verifyAdminPassword(env: AdminEnv, candidate: string): Promise<boolean> {
  const sec = await getOrInitSecurity(env);
  const hash = await hashPassword(candidate.trim(), sec.adminSalt);
  return timingSafeEqualStr(hash, sec.adminHash);
}

export async function verifySecretPasscode(env: AdminEnv, candidate: string): Promise<boolean> {
  const sec = await getOrInitSecurity(env);
  const hash = await hashPassword(candidate.trim(), sec.secretSalt);
  return timingSafeEqualStr(hash, sec.secretHash);
}

export async function updateSecurityPasswords(
  env: AdminEnv,
  newAdminPass?: string,
  newSecretPass?: string,
): Promise<void> {
  if (!env.PAIR_TOKENS) throw new Error("PAIR_TOKENS KV is not bound");
  const current = await getOrInitSecurity(env);
  let { adminHash, adminSalt, secretHash, secretSalt } = current;

  if (newAdminPass && newAdminPass.trim().length > 0) {
    adminSalt = randomHex(16);
    adminHash = await hashPassword(newAdminPass.trim(), adminSalt);
  }
  if (newSecretPass && newSecretPass.trim().length > 0) {
    secretSalt = randomHex(16);
    secretHash = await hashPassword(newSecretPass.trim(), secretSalt);
  }

  const next: AdminSecurityKV = {
    adminHash,
    adminSalt,
    secretHash,
    secretSalt,
    updatedAt: Date.now(),
  };
  await env.PAIR_TOKENS.put(SECURITY_KV_KEY, JSON.stringify(next));
}

async function getSessionHmacSecret(env: AdminEnv): Promise<string> {
  if (env.ADMIN_SESSION_SECRET?.trim()) return env.ADMIN_SESSION_SECRET.trim();
  if (env.PAIR_TOKENS) {
    const stored = await env.PAIR_TOKENS.get(SESSION_SECRET_KV_KEY, "text");
    if (stored) return stored;
    const generated = randomHex(32);
    await env.PAIR_TOKENS.put(SESSION_SECRET_KV_KEY, generated);
    return generated;
  }
  return "hefimer-ephemeral-session-key";
}

async function signHmac(secret: string, payload: string): Promise<string> {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    enc.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, enc.encode(payload));
  return toHex(sig);
}

export async function issueAdminToken(env: AdminEnv, ttlMs = 12 * 3_600_000): Promise<{ token: string; expiresAt: number }> {
  const expiresAt = Date.now() + ttlMs;
  const nonce = randomHex(12);
  const payload = `${expiresAt}.${nonce}`;
  const secret = await getSessionHmacSecret(env);
  const sig = await signHmac(secret, payload);
  return { token: `adm1.${payload}.${sig}`, expiresAt };
}

export async function verifyAdminToken(env: AdminEnv, authHeader: string | null): Promise<boolean> {
  if (!authHeader || !authHeader.startsWith("Bearer ")) return false;
  const token = authHeader.slice(7).trim();
  const parts = token.split(".");
  if (parts.length !== 4 || parts[0] !== "adm1") return false;
  const expiresAt = Number(parts[1]);
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) return false;
  const payload = `${parts[1]}.${parts[2]}`;
  const secret = await getSessionHmacSecret(env);
  const expected = await signHmac(secret, payload);
  return timingSafeEqualStr(parts[3], expected);
}

const DEFAULT_STATS: GlobalStatsKV = {
  isFrozen: false,
  developer: "Khanh Minh",
  likes: 0,
  notice: "",
  noticeExpiresAt: 0,
  broadcast: null,
};

export async function getGlobalStats(env: AdminEnv): Promise<GlobalStatsKV> {
  if (!env.PAIR_TOKENS) return DEFAULT_STATS;
  const stored = await env.PAIR_TOKENS.get<Partial<GlobalStatsKV>>(STATS_KV_KEY, "json");
  return {
    ...DEFAULT_STATS,
    ...(stored || {}),
  };
}

export async function updateGlobalStats(env: AdminEnv, patch: Partial<GlobalStatsKV>): Promise<GlobalStatsKV> {
  const cur = await getGlobalStats(env);
  const next: GlobalStatsKV = {
    ...cur,
    ...patch,
  };
  if (env.PAIR_TOKENS) {
    await env.PAIR_TOKENS.put(STATS_KV_KEY, JSON.stringify(next));
  }
  return next;
}
