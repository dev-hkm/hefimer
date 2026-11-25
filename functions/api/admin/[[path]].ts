import {
  getGlobalStats,
  issueAdminToken,
  updateGlobalStats,
  updateSecurityPasswords,
  verifyAdminPassword,
  verifyAdminToken,
  verifySecretPasscode,
  type AdminEnv,
} from "../../lib/admin-auth";
import { getR2ConfigSummary, saveR2ConfigOverride, type R2Env } from "../../lib/r2";
import { isRateLimited, isTrustedBrowserRequest, json } from "../../lib/request-guard";

interface Env extends AdminEnv, R2Env {}

function isAllowedClient(request: Request): boolean {
  const ua = request.headers.get("User-Agent") || "";
  if (ua.includes("Hefimer-Android")) return true;
  return isTrustedBrowserRequest(request);
}

export const onRequest: any = async ({
  request,
  env,
  params,
}: {
  request: Request;
  env: Env;
  params: { path?: string[] };
}) => {
  if (!isAllowedClient(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }
  if (isRateLimited(request)) {
    return json({ error: "Too many requests. Please try again shortly." }, 429);
  }

  const subpath = (params.path || []).join("/").toLowerCase();

  try {
    // Public read-only runtime stats (zero passwords or credentials)
    if (subpath === "public-stats" && request.method === "GET") {
      const stats = await getGlobalStats(env);
      return json({ ok: true, stats });
    }

    // Secret R2 passcode verification (server-side only)
    if (subpath === "verify-secret" && request.method === "POST") {
      const body = (await request.json().catch(() => ({}))) as { passcode?: string };
      const passcode = typeof body.passcode === "string" ? body.passcode : "";
      if (!passcode.trim()) return json({ ok: false, error: "Passcode is required" }, 400);
      const valid = await verifySecretPasscode(env, passcode);
      if (!valid) return json({ ok: false, error: "Invalid passcode" }, 401);
      return json({ ok: true });
    }

    // Admin login -> returns HMAC-signed 12h session token
    if (subpath === "login" && request.method === "POST") {
      const body = (await request.json().catch(() => ({}))) as { password?: string };
      const password = typeof body.password === "string" ? body.password : "";
      if (!password.trim()) return json({ ok: false, error: "Password is required" }, 400);
      const valid = await verifyAdminPassword(env, password);
      if (!valid) return json({ ok: false, error: "Invalid admin password" }, 401);
      const session = await issueAdminToken(env);
      return json({ ok: true, ...session });
    }

    // All endpoints below require a valid Admin Bearer token
    const authed = await verifyAdminToken(env, request.headers.get("Authorization"));
    if (!authed) {
      return json({ ok: false, error: "Unauthorized" }, 401);
    }

    if (subpath === "overview" && request.method === "GET") {
      const [stats, r2] = await Promise.all([getGlobalStats(env), getR2ConfigSummary(env)]);
      return json({ ok: true, stats, r2 });
    }

    if (subpath === "stats" && request.method === "POST") {
      const body = (await request.json().catch(() => ({}))) as Record<string, unknown>;
      const patch: Record<string, unknown> = {};
      if (typeof body.isFrozen === "boolean") patch.isFrozen = body.isFrozen;
      if (typeof body.developer === "string" && body.developer.trim()) patch.developer = body.developer.trim().slice(0, 64);
      if (typeof body.likes === "number" && Number.isFinite(body.likes) && body.likes >= 0) patch.likes = Math.floor(body.likes);
      if (typeof body.notice === "string") patch.notice = body.notice.trim().slice(0, 500);
      if (typeof body.noticeExpiresAt === "number") patch.noticeExpiresAt = body.noticeExpiresAt;
      if ("broadcast" in body) patch.broadcast = body.broadcast ?? null;
      const stats = await updateGlobalStats(env, patch as any);
      return json({ ok: true, stats });
    }

    if (subpath === "passwords" && request.method === "POST") {
      const body = (await request.json().catch(() => ({}))) as { adminPass?: string; secretPass?: string };
      const adminPass = typeof body.adminPass === "string" ? body.adminPass.trim() : "";
      const secretPass = typeof body.secretPass === "string" ? body.secretPass.trim() : "";
      if (!adminPass && !secretPass) {
        return json({ ok: false, error: "Provide at least one new password" }, 400);
      }
      if ((adminPass && adminPass.length < 4) || (secretPass && secretPass.length < 4)) {
        return json({ ok: false, error: "Passwords must be at least 4 characters" }, 400);
      }
      await updateSecurityPasswords(env, adminPass || undefined, secretPass || undefined);
      return json({ ok: true });
    }

    if (subpath === "r2-config" && request.method === "GET") {
      const r2 = await getR2ConfigSummary(env);
      return json({ ok: true, r2 });
    }

    if (subpath === "r2-config" && request.method === "POST") {
      const body = (await request.json().catch(() => ({}))) as {
        accountId?: string;
        accessKeyId?: string;
        secretAccessKey?: string;
        bucketName?: string;
      };
      const r2 = await saveR2ConfigOverride(env, {
        accountId: body.accountId || "",
        accessKeyId: body.accessKeyId || "",
        secretAccessKey: body.secretAccessKey || "",
        bucketName: body.bucketName || "",
      });
      return json({ ok: true, r2 });
    }

    return json({ error: "Not found" }, 404);
  } catch (err: any) {
    console.error("Admin API error:", err);
    return json({ ok: false, error: err?.message || "Internal server error" }, 500);
  }
};
