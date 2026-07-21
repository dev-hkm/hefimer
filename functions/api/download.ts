import { isTrustedBrowserRequest, json } from "../lib/request-guard";

const ALLOWED_HOSTS = new Set([
  "storage.to",
  "tmpfiles.org",
  "litter.catbox.moe",
  "litterbox.catbox.moe",
]);

function safeFileName(value: string) {
  return (value || "download").replace(/[\\/\r\n\0"]/g, "_").slice(0, 180) || "download";
}

function contentDisposition(value: string) {
  const original = safeFileName(value);
  const fallback = original.replace(/[^\x20-\x7E]/g, "_");
  return `attachment; filename="${fallback}"; filename*=UTF-8''${encodeURIComponent(original)}`;
}

export const onRequestGet: any = async ({ request }: { request: Request }) => {
  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  const requestUrl = new URL(request.url);
  const sourceValue = requestUrl.searchParams.get("url") || "";
  let source: URL;
  try {
    source = new URL(sourceValue);
  } catch {
    return json({ error: "Invalid download URL" }, 400);
  }

  if (source.protocol !== "https:" || source.username || source.password || !ALLOWED_HOSTS.has(source.hostname.toLowerCase())) {
    return json({ error: "Download provider is not allowed" }, 403);
  }

  try {
    const upstreamHeaders = new Headers();
    const range = request.headers.get("Range");
    if (range) upstreamHeaders.set("Range", range);
    const upstream = await fetch(source.toString(), { headers: upstreamHeaders, redirect: "follow" });
    if (!upstream.ok && upstream.status !== 206) {
      return json({ error: `Provider download failed (${upstream.status})` }, 502);
    }

    const headers = new Headers({
      "Cache-Control": "private, no-store",
      "Content-Disposition": contentDisposition(requestUrl.searchParams.get("filename") || "download"),
      "Content-Type": upstream.headers.get("Content-Type") || "application/octet-stream",
      "X-Content-Type-Options": "nosniff",
    });
    for (const name of ["Content-Length", "Content-Range", "Accept-Ranges", "ETag", "Last-Modified"]) {
      const value = upstream.headers.get(name);
      if (value) headers.set(name, value);
    }
    return new Response(upstream.body, { status: upstream.status, headers });
  } catch (error: any) {
    console.error("Provider download proxy failed:", error);
    return json({ error: "Could not start provider download" }, 502);
  }
};
