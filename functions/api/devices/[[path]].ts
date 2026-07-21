import {
  authenticateDevice,
  json,
  randomId,
  sha256Base64Url,
  type AuthenticatedDevice,
  type DeviceEnv,
} from "../../lib/device-auth";

interface PagesContext {
  request: Request;
  env: DeviceEnv;
}

interface MembershipRow {
  group_id: string;
  group_name: string;
  owner_device_id: string;
  group_expires_at: number | null;
  auto_approve: number;
  joined_at: number;
}

const INVITE_TTL_SECONDS = 10 * 60;
const GROUP_DURATIONS: Record<string, number | null> = {
  "12h": 12 * 60 * 60 * 1000,
  "24h": 24 * 60 * 60 * 1000,
  "7d": 7 * 24 * 60 * 60 * 1000,
  "30d": 30 * 24 * 60 * 60 * 1000,
  never: null,
};
const ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

function cleanText(value: unknown, fallback: string, maxLength: number) {
  if (typeof value !== "string") return fallback;
  const cleaned = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
  return cleaned.slice(0, maxLength) || fallback;
}

function token36() {
  const bytes = new Uint8Array(36);
  crypto.getRandomValues(bytes);
  let value = "hfm-";
  for (const byte of bytes) value += ALPHANUMERIC[byte % ALPHANUMERIC.length];
  return value;
}

async function bodyJson(bodyText: string) {
  if (!bodyText) return {} as Record<string, unknown>;
  try {
    return JSON.parse(bodyText) as Record<string, unknown>;
  } catch {
    throw json({ error: "Invalid JSON body" }, 400);
  }
}

async function activeMembership(env: DeviceEnv, deviceId: string) {
  return env.HEFIMER_DB.prepare(`
    SELECT g.id AS group_id, g.name AS group_name, g.owner_device_id,
           g.expires_at AS group_expires_at, m.auto_approve, m.joined_at
    FROM group_memberships m
    JOIN device_groups g ON g.id = m.group_id
    WHERE m.device_id = ? AND (g.expires_at IS NULL OR g.expires_at > ?)
    ORDER BY m.joined_at DESC LIMIT 1
  `).bind(deviceId, Date.now()).first<MembershipRow>();
}

async function requireMembership(env: DeviceEnv, deviceId: string) {
  const membership = await activeMembership(env, deviceId);
  if (!membership) throw json({ error: "This device is not in an active group" }, 409);
  return membership;
}

async function requireOwner(env: DeviceEnv, device: AuthenticatedDevice) {
  const membership = await requireMembership(env, device.id);
  if (membership.owner_device_id !== device.id) {
    throw json({ error: "Only the group owner can do this" }, 403);
  }
  return membership;
}

function publicTransfer(row: Record<string, unknown>, includePayload: boolean) {
  const result: Record<string, unknown> = { ...row };
  if (includePayload && typeof row.payload_json === "string") {
    try {
      result.payload = JSON.parse(row.payload_json);
    } catch {
      result.payload = null;
    }
  }
  delete result.payload_json;
  return result;
}

async function syncDevice(env: DeviceEnv, device: AuthenticatedDevice) {
  const now = Date.now();
  await env.HEFIMER_DB.batch([
    env.HEFIMER_DB.prepare("UPDATE transfers SET status = 'expired' WHERE status = 'active' AND expires_at <= ?").bind(now),
    env.HEFIMER_DB.prepare("DELETE FROM auth_nonces WHERE expires_at <= ?").bind(now),
    env.HEFIMER_DB.prepare("DELETE FROM pair_invites WHERE expires_at <= ? OR consumed_at IS NOT NULL AND consumed_at <= ?").bind(now, now - 24 * 60 * 60 * 1000),
  ]);

  const membership = await activeMembership(env, device.id);
  if (!membership) return { device, group: null, members: [], incoming: [], outgoing: [] };

  const members = await env.HEFIMER_DB.prepare(`
    SELECT d.id, d.name, d.platform, d.last_seen_at, m.auto_approve, m.joined_at,
           CASE WHEN d.id = g.owner_device_id THEN 1 ELSE 0 END AS is_owner
    FROM group_memberships m
    JOIN devices d ON d.id = m.device_id
    JOIN device_groups g ON g.id = m.group_id
    WHERE m.group_id = ? ORDER BY m.joined_at ASC
  `).bind(membership.group_id).all<Record<string, unknown>>();

  const incomingRows = await env.HEFIMER_DB.prepare(`
    SELECT t.id, t.kind, t.name, t.provider, t.size_bytes, t.drop_code,
           t.expires_at, t.created_at, t.status AS transfer_status,
           tr.status AS recipient_status, tr.updated_at, t.payload_json,
           d.name AS sender_name, d.id AS sender_device_id
    FROM transfer_recipients tr
    JOIN transfers t ON t.id = tr.transfer_id
    JOIN devices d ON d.id = t.sender_device_id
    WHERE tr.device_id = ? AND t.group_id = ? AND t.status = 'active' AND t.expires_at > ?
    ORDER BY t.created_at DESC LIMIT 40
  `).bind(device.id, membership.group_id, now).all<Record<string, unknown>>();

  const outgoingRows = await env.HEFIMER_DB.prepare(`
    SELECT id, kind, name, provider, size_bytes, drop_code, expires_at,
           created_at, status AS transfer_status, payload_json
    FROM transfers WHERE sender_device_id = ? AND group_id = ?
    ORDER BY created_at DESC LIMIT 30
  `).bind(device.id, membership.group_id).all<Record<string, unknown>>();

  const outgoing = await Promise.all(outgoingRows.results.map(async (row) => {
    const recipients = await env.HEFIMER_DB.prepare(`
      SELECT d.id AS device_id, d.name AS device_name, r.status, r.updated_at
      FROM transfer_recipients r JOIN devices d ON d.id = r.device_id
      WHERE r.transfer_id = ? ORDER BY d.name
    `).bind(row.id).all<Record<string, unknown>>();
    return { ...publicTransfer(row, true), recipients: recipients.results };
  }));

  return {
    device,
    group: {
      id: membership.group_id,
      name: membership.group_name,
      ownerDeviceId: membership.owner_device_id,
      expiresAt: membership.group_expires_at,
      autoApprove: Boolean(membership.auto_approve),
    },
    members: members.results.map((member) => ({
      ...member,
      auto_approve: Boolean(member.auto_approve),
      is_owner: Boolean(member.is_owner),
      online: now - Number(member.last_seen_at || 0) < 90_000,
    })),
    incoming: incomingRows.results.map((row) => publicTransfer(
      row,
      ["approved", "downloading", "received"].includes(String(row.recipient_status)),
    )),
    outgoing,
  };
}

async function handleRegister(context: PagesContext, bodyText: string) {
  const body = await bodyJson(bodyText);
  const id = typeof body.id === "string" ? body.id : "";
  const publicKey = typeof body.publicKey === "string" ? body.publicKey : "";
  const name = cleanText(body.name, "My device", 48);
  const platform = cleanText(body.platform, "Web", 48);
  if (!/^[A-Za-z0-9_-]{20,80}$/.test(id) || !/^[A-Za-z0-9_-]{80,180}$/.test(publicKey)) {
    return json({ error: "Invalid device identity" }, 400);
  }
  try {
    await crypto.subtle.importKey(
      "spki",
      Uint8Array.from(atob(publicKey.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(publicKey.length / 4) * 4, "=")), (char) => char.charCodeAt(0)),
      { name: "ECDSA", namedCurve: "P-256" },
      false,
      ["verify"],
    );
  } catch {
    return json({ error: "Unsupported device public key" }, 400);
  }

  const existing = await context.env.HEFIMER_DB.prepare("SELECT public_key FROM devices WHERE id = ?")
    .bind(id).first<{ public_key: string }>();
  if (existing && existing.public_key !== publicKey) return json({ error: "Device identity conflict" }, 409);
  const now = Date.now();
  await context.env.HEFIMER_DB.prepare(`
    INSERT INTO devices (id, public_key, name, platform, created_at, last_seen_at)
    VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET name = excluded.name, platform = excluded.platform, last_seen_at = excluded.last_seen_at
  `).bind(id, publicKey, name, platform, now, now).run();
  return json({ ok: true, device: { id, name, platform } });
}

async function handleRoute(context: PagesContext) {
  const { request, env } = context;
  const url = new URL(request.url);
  const route = url.pathname.replace(/^\/api\/devices\/?/, "").replace(/\/$/, "");
  const method = request.method.toUpperCase();
  const bodyText = ["GET", "HEAD"].includes(method) ? "" : await request.text();

  if (method === "OPTIONS") return new Response(null, { status: 204 });
  if (route === "register" && method === "POST") return handleRegister(context, bodyText);
  const device = await authenticateDevice(request, env, bodyText);
  const body = await bodyJson(bodyText);

  if (route === "sync" && method === "GET") return json(await syncDevice(env, device));

  if (route === "device" && method === "PATCH") {
    const name = cleanText(body.name, "My device", 48);
    await env.HEFIMER_DB.prepare("UPDATE devices SET name = ? WHERE id = ?").bind(name, device.id).run();
    return json({ ok: true, name });
  }

  if (route === "groups" && method === "POST") {
    if (await activeMembership(env, device.id)) return json({ error: "Leave the current group first" }, 409);
    const duration = typeof body.duration === "string" ? body.duration : "7d";
    if (!(duration in GROUP_DURATIONS)) return json({ error: "Invalid group lifetime" }, 400);
    const now = Date.now();
    const groupId = randomId("grp_");
    const expiresAt = GROUP_DURATIONS[duration] === null ? null : now + Number(GROUP_DURATIONS[duration]);
    const name = cleanText(body.name, "My Hefimer Group", 64);
    await env.HEFIMER_DB.batch([
      env.HEFIMER_DB.prepare("INSERT INTO device_groups (id, name, owner_device_id, expires_at, created_at) VALUES (?, ?, ?, ?, ?)")
        .bind(groupId, name, device.id, expiresAt, now),
      env.HEFIMER_DB.prepare("INSERT INTO group_memberships (group_id, device_id, auto_approve, joined_at) VALUES (?, ?, 0, ?)")
        .bind(groupId, device.id, now),
    ]);
    return json({ ok: true, groupId, expiresAt }, 201);
  }

  if (route === "groups/invite" && method === "POST") {
    const membership = await requireOwner(env, device);
    const token = token36();
    const tokenHash = await sha256Base64Url(token);
    const now = Date.now();
    await env.HEFIMER_DB.prepare(`
      INSERT INTO pair_invites (token_hash, group_id, created_by, expires_at, created_at)
      VALUES (?, ?, ?, ?, ?)
    `).bind(tokenHash, membership.group_id, device.id, now + INVITE_TTL_SECONDS * 1000, now).run();
    await env.PAIR_TOKENS.put(`invite:${tokenHash}`, membership.group_id, { expirationTtl: INVITE_TTL_SECONDS });
    return json({ token, expiresAt: now + INVITE_TTL_SECONDS * 1000 });
  }

  if (route === "groups/join" && method === "POST") {
    if (await activeMembership(env, device.id)) return json({ error: "This device already belongs to a group" }, 409);
    const token = typeof body.token === "string" ? body.token.trim() : "";
    if (!/^hfm-[A-Za-z0-9]{36}$/.test(token)) return json({ error: "Invalid pairing token" }, 400);
    const tokenHash = await sha256Base64Url(token);
    const now = Date.now();
    const invite = await env.HEFIMER_DB.prepare(`
      SELECT group_id FROM pair_invites WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > ?
    `).bind(tokenHash, now).first<{ group_id: string }>();
    if (!invite) return json({ error: "Pairing token is expired or already used" }, 410);
    const results = await env.HEFIMER_DB.batch<any>([
      env.HEFIMER_DB.prepare(`
        INSERT OR IGNORE INTO group_memberships (group_id, device_id, auto_approve, joined_at)
        SELECT group_id, ?, 0, ? FROM pair_invites
        WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > ?
      `).bind(device.id, now, tokenHash, now),
      env.HEFIMER_DB.prepare(`
        UPDATE pair_invites SET consumed_at = ?, consumed_by = ?
        WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > ?
      `).bind(now, device.id, tokenHash, now),
    ]);
    const inserted = Number(results?.[0]?.meta?.changes || 0);
    if (inserted !== 1) return json({ error: "Pairing token was just used by another device" }, 409);
    await env.PAIR_TOKENS.delete(`invite:${tokenHash}`);
    return json({ ok: true, groupId: invite.group_id });
  }

  if (route === "groups/settings" && method === "PATCH") {
    const membership = await requireMembership(env, device.id);
    const autoApprove = body.autoApprove === true ? 1 : 0;
    await env.HEFIMER_DB.prepare("UPDATE group_memberships SET auto_approve = ? WHERE group_id = ? AND device_id = ?")
      .bind(autoApprove, membership.group_id, device.id).run();
    return json({ ok: true, autoApprove: Boolean(autoApprove) });
  }

  if (route === "groups/remove" && method === "POST") {
    const membership = await requireOwner(env, device);
    const targetId = typeof body.deviceId === "string" ? body.deviceId : "";
    if (!targetId || targetId === device.id) return json({ error: "Use Leave group for this device" }, 400);
    await env.HEFIMER_DB.prepare("DELETE FROM group_memberships WHERE group_id = ? AND device_id = ?")
      .bind(membership.group_id, targetId).run();
    return json({ ok: true });
  }

  if (route === "groups/leave" && method === "POST") {
    const membership = await requireMembership(env, device.id);
    if (membership.owner_device_id === device.id) {
      await env.HEFIMER_DB.prepare("DELETE FROM device_groups WHERE id = ?").bind(membership.group_id).run();
      return json({ ok: true, groupDeleted: true });
    }
    await env.HEFIMER_DB.prepare("DELETE FROM group_memberships WHERE group_id = ? AND device_id = ?")
      .bind(membership.group_id, device.id).run();
    return json({ ok: true, groupDeleted: false });
  }

  if (route === "transfers" && method === "POST") {
    const membership = await requireMembership(env, device.id);
    const kind = body.kind === "text" ? "text" : body.kind === "file" ? "file" : "";
    const dropCode = typeof body.dropCode === "string" ? body.dropCode : "";
    const expiresAt = Number(body.expiresAt);
    const payload = body.payload;
    if (!kind || !/^\d{5}$/.test(dropCode) || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      return json({ error: "Invalid transfer metadata" }, 400);
    }
    const payloadJson = JSON.stringify(payload ?? {});
    if (payloadJson.length > 512_000) return json({ error: "Transfer metadata is too large" }, 413);
    const transferId = randomId("trn_");
    const now = Date.now();
    await env.HEFIMER_DB.batch([
      env.HEFIMER_DB.prepare(`
        INSERT INTO transfers (id, group_id, sender_device_id, kind, name, provider, size_bytes, drop_code, payload_json, expires_at, status, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?)
      `).bind(
        transferId,
        membership.group_id,
        device.id,
        kind,
        cleanText(body.name, kind === "file" ? "Shared file" : "Shared text", 160),
        cleanText(body.provider, kind === "file" ? "Provider" : "Hefimer", 48),
        Math.max(0, Math.floor(Number(body.sizeBytes) || 0)),
        dropCode,
        payloadJson,
        Math.min(expiresAt, membership.group_expires_at || expiresAt),
        now,
      ),
      env.HEFIMER_DB.prepare(`
        INSERT INTO transfer_recipients (transfer_id, device_id, status, decision_at, updated_at)
        SELECT ?, device_id, CASE WHEN auto_approve = 1 THEN 'approved' ELSE 'pending' END,
               CASE WHEN auto_approve = 1 THEN ? ELSE NULL END, ?
        FROM group_memberships WHERE group_id = ? AND device_id <> ?
      `).bind(transferId, now, now, membership.group_id, device.id),
    ]);
    return json({ ok: true, transferId }, 201);
  }

  const transferMatch = route.match(/^transfers\/([A-Za-z0-9_-]+)\/(decision|status|cancel)$/);
  if (transferMatch && method === "POST") {
    const [, transferId, action] = transferMatch;
    const now = Date.now();
    if (action === "cancel") {
      const result = await env.HEFIMER_DB.prepare(`
        UPDATE transfers SET status = 'cancelled', cancelled_at = ?
        WHERE id = ? AND sender_device_id = ? AND status = 'active'
      `).bind(now, transferId, device.id).run();
      if ((result.meta?.changes || 0) !== 1) return json({ error: "Transfer cannot be cancelled" }, 409);
      return json({ ok: true });
    }
    if (action === "decision") {
      const decision = body.decision === "approve" ? "approved" : body.decision === "decline" ? "declined" : "";
      if (!decision) return json({ error: "Invalid decision" }, 400);
      const result = await env.HEFIMER_DB.prepare(`
        UPDATE transfer_recipients SET status = ?, decision_at = ?, updated_at = ?
        WHERE transfer_id = ? AND device_id = ? AND status = 'pending'
      `).bind(decision, now, now, transferId, device.id).run();
      if ((result.meta?.changes || 0) !== 1) return json({ error: "Transfer was already handled" }, 409);
      return json({ ok: true, status: decision });
    }
    const status = typeof body.status === "string" ? body.status : "";
    if (!["downloading", "received", "failed"].includes(status)) return json({ error: "Invalid transfer status" }, 400);
    const allowedCurrent = status === "downloading" ? "approved" : "downloading";
    const result = await env.HEFIMER_DB.prepare(`
      UPDATE transfer_recipients SET status = ?, updated_at = ?
      WHERE transfer_id = ? AND device_id = ? AND status IN (?, ?)
    `).bind(status, now, transferId, device.id, allowedCurrent, status).run();
    if ((result.meta?.changes || 0) !== 1) return json({ error: "Invalid transfer state change" }, 409);
    return json({ ok: true, status });
  }

  return json({ error: "Device API route not found" }, 404);
}

export const onRequest = async (context: PagesContext) => {
  try {
    return await handleRoute(context);
  } catch (error) {
    if (error instanceof Response) {
      const headers = new Headers(error.headers);
      if (!headers.has("Content-Type")) headers.set("Content-Type", "application/json; charset=utf-8");
      headers.set("Cache-Control", "no-store");
      return new Response(error.body, { status: error.status, headers });
    }
    console.error("Device API error", error);
    return json({ error: error instanceof Error ? error.message : "Device service unavailable" }, 500);
  }
};
