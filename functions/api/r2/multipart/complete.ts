import { completeMultipartUpload, isSafeObjectKey, isSafeUploadId, type R2Env } from "../../../lib/r2";
import { isRateLimited, isTrustedBrowserRequest, json } from "../../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
  if (!isTrustedBrowserRequest(request)) return json({ error: "Cross-origin requests are not allowed" }, 403);
  if (isRateLimited(request)) return json({ error: "Too many requests. Please try again shortly." }, 429);
  const { objectKey, uploadId, parts } = await request.json().catch(() => ({}));
  const validParts = Array.isArray(parts) && parts.length > 0 && parts.length <= 10_000 && parts.every((part) => Number.isInteger(part?.PartNumber) && part.PartNumber >= 1 && part.PartNumber <= 10_000 && typeof part?.ETag === "string" && part.ETag.length > 0);
  if (typeof objectKey !== "string" || !isSafeObjectKey(objectKey) || typeof uploadId !== "string" || !isSafeUploadId(uploadId) || !validParts) return json({ error: "Invalid multipart upload reference" }, 400);
  try {
    await completeMultipartUpload(env, objectKey, uploadId, parts);
    return json({ success: true });
  } catch (error) {
    console.error("R2 multipart complete error:", error);
    return json({ error: "Could not finish secure upload" }, 503);
  }
};
