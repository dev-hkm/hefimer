import { apiUrl } from "./api-url";

export type DirectDownloadMode = "proxy" | "fetch" | "tab";

const PROXIED_HOSTS = new Set([
  "storage.to",
  "tmpfiles.org",
  "litter.catbox.moe",
  "litterbox.catbox.moe",
]);

export function getDirectDownloadMode(url: string): DirectDownloadMode {
  try {
    const host = new URL(url).hostname.toLowerCase();
    if (PROXIED_HOSTS.has(host)) return "proxy";
    if (host === "gofile.io") return "tab";
  } catch {
    return "fetch";
  }
  return "fetch";
}

export function buildDownloadProxyUrl(url: string, fileName: string) {
  const params = new URLSearchParams({ url, filename: fileName || "download" });
  return apiUrl(`/api/download?${params.toString()}`);
}

export function triggerAnchorDownload(url: string, fileName: string) {
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName || "download";
  anchor.rel = "noopener noreferrer";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
}

export async function downloadFileInBrowser(url: string, fileName: string) {
  const mode = getDirectDownloadMode(url);
  if (mode === "tab") {
    window.open(url, "_blank", "noopener,noreferrer");
    return "tab" as const;
  }
  if (mode === "proxy") {
    triggerAnchorDownload(buildDownloadProxyUrl(url, fileName), fileName);
    return "download" as const;
  }

  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Download failed (${response.status})`);
    const blobUrl = URL.createObjectURL(await response.blob());
    triggerAnchorDownload(blobUrl, fileName);
    window.setTimeout(() => URL.revokeObjectURL(blobUrl), 1_000);
    return "download" as const;
  } catch (error) {
    console.warn("Direct fetch download failed, falling back to provider URL:", error);
    triggerAnchorDownload(url, fileName);
    return "download" as const;
  }
}
