import { abortMultipartUpload, isSafeObjectKey, isSafeUploadId, type R2Env } from "../../../lib/r2";
import { isRateLimited, isTrustedBrowserRequest, json } from "../../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
  if (!isTrustedBrowserRequest(request)) return json({ error: "Cross-origin requests are not allowed" }, 403);
  if (isRateLimited(request)) return json({ error: "Too many requests. Please try again shortly." }, 429);
  const { objectKey, uploadId } = await request.json().catch(() => ({}));
  if (typeof objectKey !== "string" || !isSafeObjectKey(objectKey) || typeof uploadId !== "string" || !isSafeUploadId(uploadId)) return json({ error: "Invalid multipart upload reference" }, 400);
  try {
    await abortMultipartUpload(env, objectKey, uploadId);
    return json({ success: true });
  } catch (error) {
    console.error("R2 multipart abort error:", error);
    return json({ error: "Could not cancel secure upload" }, 503);
  }
};
