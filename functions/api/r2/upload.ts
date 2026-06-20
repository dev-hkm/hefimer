import { uploadObjectProxy, isSafeObjectKey, type R2Env } from "../../lib/r2";
import { isTrustedBrowserRequest, json } from "../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "PUT") {
    return json({ error: "Method not allowed" }, 405);
  }

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  const url = new URL(request.url);
  const objectKey = url.searchParams.get("objectKey") || "";
  const contentType = (url.searchParams.get("contentType") || "application/octet-stream").trim();

  if (!objectKey || !isSafeObjectKey(objectKey)) {
    return json({ error: "Invalid object key" }, 400);
  }

  if (!env.R2_ACCOUNT_ID || !env.R2_ACCESS_KEY_ID || !env.R2_SECRET_ACCESS_KEY || !env.R2_BUCKET_NAME) {
    return json({ error: "R2 storage is not configured." }, 503);
  }

  try {
    const contentLength = request.headers.get("content-length");
    const contentLengthNum = contentLength ? parseInt(contentLength, 10) : undefined;
    const bodyBuffer = await request.arrayBuffer();

    await uploadObjectProxy(
      env,
      objectKey,
      new Uint8Array(bodyBuffer),
      contentType,
      contentLengthNum
    );

    return json({ success: true, objectKey });
  } catch (error: any) {
    console.error("R2 proxy upload error:", error);
    return json({ error: error.message || "Failed to upload file to secure storage" }, 500);
  }
};
