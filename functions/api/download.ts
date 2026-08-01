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

function storageToShareId(source: URL) {
  if (source.hostname.toLowerCase() !== "storage.to") return null;
  const match = source.pathname.match(/^\/([A-Za-z0-9_-]{5,64})\/?$/);
  return match?.[1] || null;
}

function storageToDownloadUrl(html: string, source: URL, shareId: string) {
  const escapedId = shareId.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = html.match(new RegExp(
    `(?:https:\\/\\/storage\\.to)?\\/${escapedId}\\/download\\?expires=\\d+(?:&amp;|&)signature=[A-Za-z0-9_-]+`,
  ));
  if (!match) return null;
  const resolved = new URL(match[0].replace(/&amp;/g, "&"), source.origin);
  if (resolved.origin !== source.origin || resolved.pathname !== `/${shareId}/download`) return null;
  return resolved;
}

function storageToMintProof(html: string) {
  const match = html.match(/\\?["']mint_proof\\?["']\s*,\s*\\?["']([0-9]+\.[a-f0-9]{32,128})\\?["']/i);
  return match?.[1] || null;
}

async function storageToCurrentDownloadUrl(html: string, source: URL, shareId: string) {
  const mintProof = storageToMintProof(html);
  if (!mintProof) return null;
  const response = await fetch(new URL(`/${shareId}/download`, source.origin), {
    headers: {
      Accept: "application/json",
      "x-mint-proof": mintProof,
    },
    redirect: "manual",
  });
  if (!response.ok) return null;
  const payload = await response.json() as { url?: unknown };
  if (typeof payload.url !== "string") return null;
  const resolved = new URL(payload.url);
  if (resolved.protocol !== "https:" || resolved.username || resolved.password || resolved.hostname !== "stusercontent.com") {
    return null;
  }
  return resolved;
}

async function fetchProviderFile(source: URL, headers: Headers) {
  const shareId = storageToShareId(source);
  if (!shareId) return fetch(source.toString(), { headers, redirect: "follow" });

  const page = await fetch(source.toString(), {
    headers: { Accept: "text/html", "Cache-Control": "no-cache" },
    redirect: "follow",
  });
  if (!page.ok) return page;
  const html = await page.text();
  const downloadUrl = storageToDownloadUrl(html, source, shareId)
    || await storageToCurrentDownloadUrl(html, source, shareId);
  if (!downloadUrl) throw new Error("storage.to did not provide a valid download link");
  return fetch(downloadUrl.toString(), { headers, redirect: "follow" });
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
    const upstream = await fetchProviderFile(source, upstreamHeaders);
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
