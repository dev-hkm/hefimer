import { createUploadUrl, getSafeFilename, isSafeContentType, type R2Env } from "../../lib/r2";
import { isRateLimited, isTrustedBrowserRequest, json } from "../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "GET") return json({ error: "Method not allowed" }, 405);
  if (!isTrustedBrowserRequest(request)) return json({ error: "Cross-origin requests are not allowed" }, 403);
  if (isRateLimited(request)) return json({ error: "Too many requests. Please try again shortly." }, 429);

  const url = new URL(request.url);
  const filename = getSafeFilename(url.searchParams.get("filename") || "");
  const contentType = (url.searchParams.get("contentType") || "application/octet-stream").trim();
  if (!filename || !isSafeContentType(contentType)) return json({ error: "Invalid filename or content type" }, 400);

  try {
    const objectKey = `hefimer/${crypto.randomUUID()}/${filename}`;
    const url = `/api/r2/upload?objectKey=${encodeURIComponent(objectKey)}&contentType=${encodeURIComponent(contentType)}`;
    return json({ objectKey, url });
  } catch (error) {
    console.error("R2 upload URL error:", error);
    return json({ error: error instanceof Error ? error.message : "Could not prepare secure upload" }, 503);
  }
};
