const API_ROOT = "/api/devices";
const DB_NAME = "hefimer-device-identity";
const STORE_NAME = "keys";
const IDENTITY_KEY = "primary";

export interface LocalDeviceIdentity {
  id: string;
  name: string;
  platform: string;
  publicKey: string;
  privateKey: CryptoKey;
}

export interface GroupMember {
  id: string;
  name: string;
  platform: string;
  last_seen_at: number;
  auto_approve: boolean;
  is_owner: boolean;
  online: boolean;
}

export interface TransferRecipient {
  device_id: string;
  device_name: string;
  status: "pending" | "approved" | "declined" | "downloading" | "received" | "failed";
  updated_at: number;
}

export interface DeviceTransfer {
  id: string;
  kind: "file" | "text";
  name: string;
  provider: string;
  size_bytes: number;
  drop_code: string;
  expires_at: number;
  created_at: number;
  transfer_status: "active" | "cancelled" | "expired";
  recipient_status?: TransferRecipient["status"];
  sender_name?: string;
  sender_device_id?: string;
  payload?: Record<string, unknown> | null;
  recipients?: TransferRecipient[];
}

export interface DeviceSync {
  device: { id: string; name: string };
  group: null | {
    id: string;
    name: string;
    ownerDeviceId: string;
    expiresAt: number | null;
    autoApprove: boolean;
  };
  members: GroupMember[];
  incoming: DeviceTransfer[];
  outgoing: DeviceTransfer[];
}

export interface DropCreatedDetail {
  kind: "file" | "text";
  name: string;
  provider: string;
  sizeBytes: number;
  dropCode: string;
  expiresAt: number;
  payload: Record<string, unknown>;
}

interface StoredIdentity {
  id: string;
  publicKey: string;
  privateKey: CryptoKey;
}

const encoder = new TextEncoder();
let identityPromise: Promise<LocalDeviceIdentity> | null = null;

function toBase64Url(value: ArrayBuffer | Uint8Array) {
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

async function sha256(value: string) {
  return toBase64Url(await crypto.subtle.digest("SHA-256", encoder.encode(value)));
}

function randomValue(bytes = 24) {
  const value = new Uint8Array(bytes);
  crypto.getRandomValues(value);
  return toBase64Url(value);
}

function openIdentityDb() {
  return new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(STORE_NAME)) {
        request.result.createObjectStore(STORE_NAME);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error("Could not open device identity storage"));
  });
}

async function readStoredIdentity() {
  const db = await openIdentityDb();
  try {
    return await new Promise<StoredIdentity | null>((resolve, reject) => {
      const request = db.transaction(STORE_NAME, "readonly").objectStore(STORE_NAME).get(IDENTITY_KEY);
      request.onsuccess = () => resolve((request.result as StoredIdentity | undefined) || null);
      request.onerror = () => reject(request.error);
    });
  } finally {
    db.close();
  }
}

async function saveStoredIdentity(identity: StoredIdentity) {
  const db = await openIdentityDb();
  try {
    await new Promise<void>((resolve, reject) => {
      const transaction = db.transaction(STORE_NAME, "readwrite");
      transaction.objectStore(STORE_NAME).put(identity, IDENTITY_KEY);
      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
    });
  } finally {
    db.close();
  }
}

function detectPlatform() {
  const ua = navigator.userAgent;
  if (/Android/i.test(ua)) return "Android";
  if (/iPhone|iPad|iPod/i.test(ua)) return "iPhone / iPad";
  if (/Windows/i.test(ua)) return "Windows";
  if (/Macintosh|Mac OS X/i.test(ua)) return "Mac";
  if (/Linux/i.test(ua)) return "Linux";
  return "Web";
}

function defaultDeviceName(platform: string) {
  const ua = navigator.userAgent;
  const browser = /Edg\//.test(ua) ? "Edge" : /Firefox\//.test(ua) ? "Firefox" : /Chrome\//.test(ua) ? "Chrome" : /Safari\//.test(ua) ? "Safari" : "Browser";
  return `${browser} on ${platform}`;
}

async function createIdentity() {
  const keyPair = await crypto.subtle.generateKey(
    { name: "ECDSA", namedCurve: "P-256" },
    true,
    ["sign", "verify"],
  );
  const stored: StoredIdentity = {
    id: randomValue(24),
    publicKey: toBase64Url(await crypto.subtle.exportKey("spki", keyPair.publicKey)),
    privateKey: keyPair.privateKey,
  };
  await saveStoredIdentity(stored);
  return stored;
}

async function requestJson<T>(path: string, init: RequestInit = {}, signed = true): Promise<T> {
  const method = (init.method || "GET").toUpperCase();
  const body = typeof init.body === "string" ? init.body : "";
  const headers = new Headers(init.headers);
  if (body) headers.set("Content-Type", "application/json");

  if (signed) {
    const identity = await getDeviceIdentity();
    const timestamp = Date.now().toString();
    const nonce = randomValue(18);
    const url = new URL(`${API_ROOT}${path}`, window.location.origin);
    const payload = [timestamp, nonce, method, `${url.pathname}${url.search}`, await sha256(body)].join("\n");
    const signature = await crypto.subtle.sign(
      { name: "ECDSA", hash: "SHA-256" },
      identity.privateKey,
      encoder.encode(payload),
    );
    headers.set("X-Hefimer-Device", identity.id);
    headers.set("X-Hefimer-Time", timestamp);
    headers.set("X-Hefimer-Nonce", nonce);
    headers.set("X-Hefimer-Signature", toBase64Url(signature));
  }

  const response = await fetch(`${API_ROOT}${path}`, { ...init, method, body: body || undefined, headers });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || `Device service returned ${response.status}`);
  return data as T;
}

export async function getDeviceIdentity(): Promise<LocalDeviceIdentity> {
  if (!identityPromise) {
    identityPromise = (async () => {
      let stored = await readStoredIdentity();
      if (!stored?.privateKey || !stored?.publicKey || !stored?.id) stored = await createIdentity();
      const platform = detectPlatform();
      const name = localStorage.getItem("hefimer_device_name") || defaultDeviceName(platform);
      await requestJson("/register", {
        method: "POST",
        body: JSON.stringify({ id: stored.id, publicKey: stored.publicKey, name, platform }),
      }, false);
      return { ...stored, name, platform };
    })();
  }
  return identityPromise;
}

export const deviceApi = {
  sync: () => requestJson<DeviceSync>("/sync"),
  rename: async (name: string) => {
    const result = await requestJson<{ name: string }>("/device", { method: "PATCH", body: JSON.stringify({ name }) });
    localStorage.setItem("hefimer_device_name", result.name);
    identityPromise = null;
    return result;
  },
  createGroup: (name: string, duration: string) => requestJson("/groups", { method: "POST", body: JSON.stringify({ name, duration }) }),
  createInvite: () => requestJson<{ token: string; expiresAt: number }>("/groups/invite", { method: "POST", body: "{}" }),
  joinGroup: (token: string) => requestJson("/groups/join", { method: "POST", body: JSON.stringify({ token }) }),
  setAutoApprove: (autoApprove: boolean) => requestJson("/groups/settings", { method: "PATCH", body: JSON.stringify({ autoApprove }) }),
  removeDevice: (deviceId: string) => requestJson("/groups/remove", { method: "POST", body: JSON.stringify({ deviceId }) }),
  leaveGroup: () => requestJson<{ groupDeleted: boolean }>("/groups/leave", { method: "POST", body: "{}" }),
  createTransfer: (detail: DropCreatedDetail) => requestJson<{ transferId: string }>("/transfers", { method: "POST", body: JSON.stringify(detail) }),
  decideTransfer: (id: string, decision: "approve" | "decline") => requestJson(`/transfers/${id}/decision`, { method: "POST", body: JSON.stringify({ decision }) }),
  updateTransfer: (id: string, status: "downloading" | "received" | "failed") => requestJson(`/transfers/${id}/status`, { method: "POST", body: JSON.stringify({ status }) }),
  cancelTransfer: (id: string) => requestJson(`/transfers/${id}/cancel`, { method: "POST", body: "{}" }),
};

export async function getPushSubscription() {
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) return null;
  const registration = await navigator.serviceWorker.ready;
  return registration.pushManager.getSubscription();
}

export async function enableDevicePush() {
  if (!("Notification" in window) || !("serviceWorker" in navigator) || !("PushManager" in window)) {
    throw new Error("System notifications are not supported on this browser");
  }
  const permission = Notification.permission === "granted" ? "granted" : await Notification.requestPermission();
  if (permission !== "granted") throw new Error("Notification permission was not granted");
  const keyResult = await requestJson<{ publicKey: string | null; enabled: boolean }>("/push-key", {}, false);
  if (!keyResult.enabled || !keyResult.publicKey) throw new Error("Push notifications are not configured yet");
  const registration = await navigator.serviceWorker.ready;
  let subscription = await registration.pushManager.getSubscription();
  if (!subscription) {
    subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: fromBase64Url(keyResult.publicKey) as BufferSource,
    });
  }
  await requestJson("/push-subscriptions", {
    method: "POST",
    body: JSON.stringify({ subscription: subscription.toJSON() }),
  });
  return subscription;
}

export async function disableDevicePush() {
  const subscription = await getPushSubscription();
  if (!subscription) return;
  await requestJson("/push-subscriptions", {
    method: "DELETE",
    body: JSON.stringify({ endpoint: subscription.endpoint }),
  });
  await subscription.unsubscribe();
}

