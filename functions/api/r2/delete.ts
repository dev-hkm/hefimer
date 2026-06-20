import { deleteR2Object, isSafeObjectKey, type R2Env } from "../../lib/r2";
import { isRateLimited, isTrustedBrowserRequest, json } from "../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
  if (!isTrustedBrowserRequest(request)) return json({ error: "Cross-origin requests are not allowed" }, 403);
  if (isRateLimited(request)) return json({ error: "Too many requests. Please try again shortly." }, 429);
  const { objectKey } = await request.json().catch(() => ({}));
  if (typeof objectKey !== "string" || !isSafeObjectKey(objectKey)) return json({ error: "Invalid file reference" }, 400);
  try {
    await deleteR2Object(env, objectKey);
    return json({ success: true });
  } catch (error) {
    console.error("R2 delete error:", error);
    return json({ error: "Could not remove secure file" }, 503);
  }
};
