import { createDownloadUrl, filenameFromObjectKey, isSafeObjectKey, type R2Env } from "../../lib/r2";
import { isRateLimited, isTrustedBrowserRequest, json } from "../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "GET") return json({ error: "Method not allowed" }, 405);
  if (!isTrustedBrowserRequest(request)) return json({ error: "Cross-origin requests are not allowed" }, 403);
  if (isRateLimited(request)) return json({ error: "Too many requests. Please try again shortly." }, 429);
  const objectKey = new URL(request.url).searchParams.get("objectKey") || "";
  const filename = filenameFromObjectKey(objectKey);
  if (!filename || !isSafeObjectKey(objectKey)) return json({ error: "Invalid file reference" }, 400);
  try {
    return json({ url: await createDownloadUrl(env, objectKey, filename) });
  } catch (error) {
    console.error("R2 download URL error:", error);
    return json({ error: "Could not prepare secure download" }, 503);
  }
};
