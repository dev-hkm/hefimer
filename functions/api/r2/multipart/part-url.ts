import { createMultipartPartUrl, isSafeObjectKey, isSafeUploadId, type R2Env } from "../../../lib/r2";
import { isTrustedBrowserRequest, json } from "../../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
  if (!isTrustedBrowserRequest(request)) return json({ error: "Cross-origin requests are not allowed" }, 403);
  const { objectKey, uploadId, partNumber } = await request.json().catch(() => ({}));
  if (typeof objectKey !== "string" || !isSafeObjectKey(objectKey) || typeof uploadId !== "string" || !isSafeUploadId(uploadId) || !Number.isInteger(partNumber) || partNumber < 1 || partNumber > 10_000) {
    return json({ error: "Invalid multipart upload reference" }, 400);
  }
  try {
    const partUrl = `/api/r2/multipart/upload-part?objectKey=${encodeURIComponent(objectKey)}&uploadId=${encodeURIComponent(uploadId)}&partNumber=${partNumber}`;
    return json({ url: partUrl });
  } catch (error) {
    console.error("R2 multipart part URL error:", error);
    return json({ error: "Could not prepare upload part" }, 503);
  }
};
