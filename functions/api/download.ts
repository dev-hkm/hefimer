import { isTrustedBrowserRequest, json } from "../lib/request-guard";

const STORAGE_TO_BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

const ALLOWED_HOSTS = new Set([
  "storage.to",
  "tmpfiles.org",
  "litter.catbox.moe",
  "litterbox.catbox.moe",
  "filebin.net",
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

function tmpfilesDownloadUrl(html: string, source: URL) {
  const match = html.match(/<a\s+[^>]*class=["'][^"']*\bdownload\b[^"']*["'][^>]*href=["']([^"']+)["']/i)
    || html.match(/href=["']([^"']+)["'][^>]*class=["'][^"']*\bdownload\b[^"']*["']/i);
  if (!match?.[1]) return null;
  const resolved = new URL(match[1], source.origin);
  if (resolved.origin !== source.origin || !resolved.pathname.startsWith("/dl/")) return null;
  return resolved;
}

function verificationCookie(response: Response) {
  const header = response.headers.get("Set-Cookie") || "";
  const match = header.match(/(?:^|,\s*)verified=([^;]+)/i);
  return match ? `verified=${match[1]}` : null;
}

async function fetchFilebinFile(source: URL, headers: Headers) {
  const initial = await fetch(source.toString(), { headers, redirect: "manual" });
  let redirect = initial.headers.get("Location");

  if (!redirect) {
    const verified = verificationCookie(initial);
    if (!verified) return initial;
    const verifiedHeaders = new Headers(headers);
    verifiedHeaders.set("Cookie", verified);
    const verifiedResponse = await fetch(source.toString(), {
      headers: verifiedHeaders,
      redirect: "manual",
    });
    redirect = verifiedResponse.headers.get("Location");
    if (!redirect) return verifiedResponse;
  }

  const signedUrl = new URL(redirect);
  if (signedUrl.protocol !== "https:" || signedUrl.username || signedUrl.password || signedUrl.hostname !== "storage.filebin.net") {
    throw new Error("Filebin returned an invalid download location");
  }
  return fetch(signedUrl.toString(), { headers, redirect: "follow" });
}

async function fetchProviderFile(source: URL, headers: Headers) {
  if (source.hostname.toLowerCase() === "filebin.net") {
    return fetchFilebinFile(source, headers);
  }

  if (source.hostname.toLowerCase() === "tmpfiles.org") {
    const page = await fetch(source.toString(), {
      headers: { Accept: "text/html", "Cache-Control": "no-cache" },
      redirect: "follow",
    });
    if (!page.ok) return page;
    const contentType = page.headers.get("Content-Type") || "";
    if (!contentType.includes("text/html")) return page;
    const downloadUrl = tmpfilesDownloadUrl(await page.text(), source);
    if (!downloadUrl) throw new Error("tmpfiles.org did not provide a valid download link");
    return fetch(downloadUrl.toString(), { headers, redirect: "follow" });
  }

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
    if (source.hostname.toLowerCase() === "storage.to") {
      // Storage.to rejects the bare server-side fetch identity behind the download proxy.
      const appOrigin = new URL(request.url).origin;
      upstreamHeaders.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
      upstreamHeaders.set("User-Agent", request.headers.get("User-Agent") || STORAGE_TO_BROWSER_USER_AGENT);
      upstreamHeaders.set("Origin", appOrigin);
      upstreamHeaders.set("Referer", `${appOrigin}/`);
    }
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
