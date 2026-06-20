import { createMultipartUpload, getSafeFilename, isSafeContentType, type R2Env } from "../../../lib/r2";
import { isRateLimited, isTrustedBrowserRequest, json } from "../../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
  if (!isTrustedBrowserRequest(request)) return json({ error: "Cross-origin requests are not allowed" }, 403);
  if (isRateLimited(request)) return json({ error: "Too many requests. Please try again shortly." }, 429);
  const { filename, contentType } = await request.json().catch(() => ({}));
  const safeFilename = typeof filename === "string" ? getSafeFilename(filename) : null;
  const safeType = typeof contentType === "string" ? contentType.trim() : "";
  if (!safeFilename || !isSafeContentType(safeType)) return json({ error: "Invalid filename or content type" }, 400);
  try {
    return json(await createMultipartUpload(env, safeFilename, safeType));
  } catch (error) {
    console.error("R2 multipart init error:", error);
    return json({ error: "Could not start secure upload" }, 503);
  }
};
