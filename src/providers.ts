export interface ExpireOption {
  label: string;
  value: string;
}

export interface UploadProviderConfig {
  id: string;
  name: string;
  maxSizeBytes: number;
  maxSizeLabel: string;
  expiry: string;
  speed: string;
  features: string;
  isLargeFile: boolean;
  defaultExpire: string;
  expireOptions: ExpireOption[];
}

export interface UploadContext {
  expire: string;
  onProgress: (percent: number) => void;
  onStatus?: (message: string) => void;
  bindXhr: (xhr: XMLHttpRequest) => XMLHttpRequest;
  clearActiveXhr: (xhr?: XMLHttpRequest) => void;
  isCancelled: () => boolean;
}

export interface FallbackUploadResult {
  fileUrl: string;
  providerId: string;
  providerName: string;
}

const MB = 1024 * 1024;
const GB = 1024 * MB;
const WORKER_PROXY_SAFE_MAX = 95 * MB;

const STANDARD_EXPIRE_OPTIONS: ExpireOption[] = [
  { label: "30 sec", value: "30s" },
  { label: "5 min", value: "5p" },
  { label: "30 min", value: "30p" },
  { label: "1 hour", value: "1h" },
  { label: "12 hours", value: "12h" },
  { label: "24 hours", value: "24h" },
  { label: "48 hours", value: "48h" },
];

export const UPLOAD_PROVIDERS: UploadProviderConfig[] = [
  {
    id: "storageto",
    name: "storage.to",
    maxSizeBytes: 25 * GB,
    maxSizeLabel: "25 GB",
    expiry: "Deletes after 3 days (1-7 days configurable)",
    speed: "Fast, powered by Cloudflare R2",
    features: "Direct R2 presigned upload up to 25 GB. Falls back automatically if blocked.",
    isLargeFile: true,
    defaultExpire: "72h",
    expireOptions: [
      { label: "1 day", value: "24h" },
      { label: "2 days", value: "48h" },
      { label: "3 days (Default)", value: "72h" },
      { label: "5 days", value: "120h" },
      { label: "7 days", value: "168h" },
    ],
  },
  {
    id: "stocare",
    name: "sto.care",
    maxSizeBytes: 25 * GB,
    maxSizeLabel: "25 GB",
    expiry: "Deletes after 7 days (fixed)",
    speed: "Very fast direct CORS upload & CDN download",
    features: "No-signup 25 GB direct stream upload with clean raw downloads via trystocare.com.",
    isLargeFile: true,
    defaultExpire: "168h",
    expireOptions: [{ label: "7 days (Fixed)", value: "168h" }],
  },
  {
    id: "gofile",
    name: "Gofile",
    maxSizeBytes: Number.POSITIVE_INFINITY,
    maxSizeLabel: "No stated limit",
    expiry: "Deletes after 10 days of inactivity",
    speed: "Fast multi-server direct upload",
    features: "Large-file external route with multi-server retry. Downloads open Gofile page.",
    isLargeFile: true,
    defaultExpire: "48h",
    expireOptions: STANDARD_EXPIRE_OPTIONS,
  },
  {
    id: "litterbox",
    name: "Litterbox",
    maxSizeBytes: 1 * GB,
    maxSizeLabel: "1 GB",
    expiry: "Deletes after 1, 12, 24, or 72 hours",
    speed: "Fast direct Catbox CDN",
    features: "Temporary Catbox host up to 1 GB with configurable retention and direct downloads.",
    isLargeFile: true,
    defaultExpire: "24h",
    expireOptions: [
      { label: "1 hour", value: "1h" },
      { label: "12 hours", value: "12h" },
      { label: "24 hours", value: "24h" },
      { label: "72 hours", value: "72h" },
    ],
  },
  {
    id: "vikingfile",
    name: "VikingFile",
    maxSizeBytes: Number.POSITIVE_INFINITY,
    maxSizeLabel: "No stated limit",
    expiry: "Inactive cleanup (long retention)",
    speed: "Fast S3 multipart direct upload",
    features: "Large-file S3 multipart fallback route. Downloads open VikingFile page.",
    isLargeFile: true,
    defaultExpire: "48h",
    expireOptions: STANDARD_EXPIRE_OPTIONS,
  },
  {
    id: "tmpfilelink",
    name: "tfLink",
    maxSizeBytes: WORKER_PROXY_SAFE_MAX,
    maxSizeLabel: "95 MB",
    expiry: "Deletes after 7 days (fixed)",
    speed: "Global CDN delivery",
    features: "tmpfile.link temporary hosting with 7-day retention and direct CDN downloads.",
    isLargeFile: false,
    defaultExpire: "168h",
    expireOptions: [{ label: "7 days (Fixed)", value: "168h" }],
  },
  {
    id: "tempfile",
    name: "TempFile",
    maxSizeBytes: 100 * MB,
    maxSizeLabel: "100 MB",
    expiry: "Deletes after 1, 6, 24, or 48 hours",
    speed: "Fast direct upload & CDN delivery",
    features: "Public API with direct downloads and configurable temporary retention.",
    isLargeFile: false,
    defaultExpire: "24h",
    expireOptions: [
      { label: "1 hour", value: "1h" },
      { label: "6 hours", value: "6h" },
      { label: "24 hours", value: "24h" },
      { label: "48 hours", value: "48h" },
    ],
  },
  {
    id: "filebin",
    name: "Filebin",
    maxSizeBytes: WORKER_PROXY_SAFE_MAX,
    maxSizeLabel: "95 MB (Web)",
    expiry: "Deletes after 6 days (fixed)",
    speed: "Standard streaming speed",
    features: "One private bin per share with verified direct file downloads.",
    isLargeFile: false,
    defaultExpire: "144h",
    expireOptions: [{ label: "6 days (Fixed)", value: "144h" }],
  },
  {
    id: "tempsh",
    name: "temp.sh",
    maxSizeBytes: WORKER_PROXY_SAFE_MAX,
    maxSizeLabel: "95 MB (Web)",
    expiry: "Deletes after 3 days (fixed)",
    speed: "Standard speed",
    features: "Minimalist temporary host with 3-day retention and proxied byte downloads.",
    isLargeFile: false,
    defaultExpire: "72h",
    expireOptions: [{ label: "3 days (Fixed)", value: "72h" }],
  },
  {
    id: "tmpfiles",
    name: "tmpfiles",
    maxSizeBytes: 100 * MB,
    maxSizeLabel: "100 MB",
    expiry: "Deletes after 60 minutes",
    speed: "Fast direct CORS upload",
    features: "Short-lived temporary hosting. Automatically deleted after 1 hour.",
    isLargeFile: false,
    defaultExpire: "1h",
    expireOptions: [{ label: "1 hour (Fixed)", value: "1h" }],
  },
  {
    id: "uguu",
    name: "Uguu.se",
    maxSizeBytes: WORKER_PROXY_SAFE_MAX,
    maxSizeLabel: "95 MB (Web)",
    expiry: "Deletes after 3 hours (fixed)",
    speed: "Fast lightweight host",
    features: "Pomf-style ephemeral file host with direct d.uguu.se downloads.",
    isLargeFile: false,
    defaultExpire: "3h",
    expireOptions: [{ label: "3 hours (Fixed)", value: "3h" }],
  },
  {
    id: "puticu",
    name: "Put.icu",
    maxSizeBytes: WORKER_PROXY_SAFE_MAX,
    maxSizeLabel: "95 MB (Web)",
    expiry: "Deletes after 5 min to 24 hours",
    speed: "Standard Linx server",
    features: "Configurable short-lived retention (5m, 1h, 12h, 24h) with direct /s/ download.",
    isLargeFile: false,
    defaultExpire: "24h",
    expireOptions: [
      { label: "5 minutes", value: "5p" },
      { label: "1 hour", value: "1h" },
      { label: "12 hours", value: "12h" },
      { label: "24 hours", value: "24h" },
    ],
  },
];

const FALLBACK_PRIORITY = [
  "stocare",
  "storageto",
  "litterbox",
  "tmpfilelink",
  "tempfile",
  "tmpfiles",
  "tempsh",
  "filebin",
  "uguu",
  "puticu",
  "gofile",
  "vikingfile",
];

export function getProviderConfig(providerId: string): UploadProviderConfig {
  return UPLOAD_PROVIDERS.find((p) => p.id === providerId) || UPLOAD_PROVIDERS[0];
}

export function normalizeExpireForProvider(providerId: string, currentExpire: string): string {
  const config = getProviderConfig(providerId);
  if (config.expireOptions.some((opt) => opt.value === currentExpire)) {
    return currentExpire;
  }
  return config.defaultExpire;
}

export function getProviderNameFromUrl(url: string, hasObjectKey = false): string {
  if (!url) return "File";
  if (hasObjectKey) return "Cloudflare R2";
  if (url.includes("gofile.io")) return "Gofile";
  if (url.includes("catbox.moe")) return "Litterbox";
  if (url.includes("pixeldrain.com")) return "Pixeldrain";
  if (url.includes("storage.to")) return "storage.to";
  if (url.includes("trystocare.com") || url.includes("sto.care")) return "sto.care";
  if (url.includes("tmpfile.link")) return "tfLink";
  if (url.includes("tmpfiles.org")) return "tmpfiles.org";
  if (url.includes("filebin.net")) return "Filebin";
  if (url.includes("tempfile.org")) return "TempFile";
  if (url.includes("temp.sh")) return "temp.sh";
  if (url.includes("uguu.se")) return "Uguu.se";
  if (url.includes("put.icu")) return "Put.icu";
  if (url.includes("vikingfile.com")) return "VikingFile";
  try {
    const domain = new URL(url).hostname;
    return domain.replace("www.", "").split(".")[0].replace(/^\w/, (c) => c.toUpperCase());
  } catch {
    return "Host";
  }
}

function sendXhr(
  method: "POST" | "PUT",
  url: string,
  body: Document | XMLHttpRequestBodyInit | null,
  ctx: UploadContext,
  headers?: Record<string, string>,
): Promise<{ status: number; responseText: string; getHeader: (name: string) => string | null }> {
  return new Promise((resolve, reject) => {
    const xhr = ctx.bindXhr(new XMLHttpRequest());
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        ctx.onProgress(Math.round((event.loaded / event.total) * 100));
      }
    };
    xhr.onload = () => {
      ctx.clearActiveXhr(xhr);
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve({
          status: xhr.status,
          responseText: xhr.responseText,
          getHeader: (name: string) => xhr.getResponseHeader(name),
        });
        return;
      }
      let message = `Upload failed with status ${xhr.status}`;
      try {
        const parsed = JSON.parse(xhr.responseText);
        message = parsed.error || parsed.message || message;
      } catch {
        if (xhr.responseText && xhr.responseText.length < 300) {
          message = xhr.responseText;
        }
      }
      reject(new Error(message));
    };
    xhr.onabort = () => {
      ctx.clearActiveXhr(xhr);
      reject(new Error("UPLOAD_CANCELED"));
    };
    xhr.onerror = () => {
      ctx.clearActiveXhr(xhr);
      reject(new Error("Network error during upload"));
    };
    xhr.open(method, url);
    if (headers) {
      for (const [k, v] of Object.entries(headers)) {
        xhr.setRequestHeader(k, v);
      }
    }
    xhr.send(body);
  });
}

function parseJsonSafe(text: string): any {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function parseExpireSeconds(expire: string): number {
  const match = expire.match(/^(\d+)([smhpdw])$/i);
  if (!match) return 86400;
  const val = parseInt(match[1], 10);
  const unit = match[2].toLowerCase();
  if (unit === "s") return val;
  if (unit === "m" || unit === "p") return val * 60;
  if (unit === "h") return val * 3600;
  if (unit === "d") return val * 86400;
  if (unit === "w") return val * 7 * 86400;
  return 86400;
}

async function uploadToStoCare(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via sto.care...");
  const res = await sendXhr(
    "PUT",
    `https://ul.sto.care/${encodeURIComponent(fileName)}`,
    blob,
    ctx,
    { "Content-Type": blob.type || "application/octet-stream" },
  );
  const data = parseJsonSafe(res.responseText);
  if (!data?.url) throw new Error(data?.error || "sto.care did not return a download URL");
  return data.url;
}

async function uploadToStorageTo(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Initializing upload with storage.to...");
  let visitorToken = localStorage.getItem("hefimer_storageto_visitor_token");
  if (!visitorToken) {
    visitorToken = Math.random().toString(36).substring(2, 18) + Math.random().toString(36).substring(2, 18);
    localStorage.setItem("hefimer_storageto_visitor_token", visitorToken);
  }

  const contentType = blob.type || "application/octet-stream";
  const initRes = await fetch("/api/proxy/storageto/upload/init", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Visitor-Token": visitorToken,
    },
    body: JSON.stringify({
      filename: fileName,
      content_type: contentType,
      size: blob.size,
    }),
  });

  if (!initRes.ok) {
    const errData = await initRes.json().catch(() => ({}));
    throw new Error(errData.error || `Failed to initialize storage.to upload (status ${initRes.status})`);
  }

  const initData = await initRes.json();
  if (!initData.success) {
    throw new Error(initData.error || "Failed to initialize storage.to upload");
  }

  const r2Key = initData.r2_key;
  let ownerToken = initData.owner_token || "";

  if (initData.type === "single") {
    const uploadUrl = initData.upload_url;
    if (!uploadUrl) throw new Error("Failed to retrieve upload URL from storage.to");
    ctx.onStatus?.("Uploading bytes to storage.to...");
    await sendXhr("PUT", uploadUrl, blob, ctx, { "Content-Type": contentType });
  } else if (initData.type === "multipart") {
    const uploadId = initData.upload_id;
    const partSize = initData.part_size;
    const totalParts = initData.total_parts;
    const initialUrls = initData.initial_urls || {};
    ctx.onStatus?.(`Uploading ${totalParts} parts to storage.to...`);
    const completedParts: { partNumber: number; etag: string }[] = [];

    for (let partNumber = 1; partNumber <= totalParts; partNumber++) {
      if (ctx.isCancelled()) throw new Error("UPLOAD_CANCELED");
      let partUploadUrl = initialUrls[partNumber.toString()];
      if (!partUploadUrl) {
        const partsRes = await fetch("/api/proxy/storageto/upload/parts", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Owner ${ownerToken}`,
          },
          body: JSON.stringify({ upload_id: uploadId, part_numbers: [partNumber] }),
        });
        if (!partsRes.ok) throw new Error(`Failed to fetch part ${partNumber} URL`);
        const partsData = await partsRes.json();
        const partObj = partsData.part_urls?.find((p: any) => p.partNumber === partNumber);
        if (!partObj) throw new Error(`Could not get URL for part ${partNumber}`);
        partUploadUrl = partObj.url;
      }

      const start = (partNumber - 1) * partSize;
      const end = Math.min(start + partSize, blob.size);
      const chunk = blob.slice(start, end);

      const partRes = await sendXhr("PUT", partUploadUrl, chunk, {
        ...ctx,
        onProgress: (chunkPct) => {
          const overall = Math.round(((partNumber - 1) / totalParts) * 100 + chunkPct / totalParts);
          ctx.onProgress(overall);
        },
      });
      const etag = partRes.getHeader("ETag") || partRes.getHeader("etag") || partRes.getHeader("ETAG");
      if (!etag) throw new Error(`ETag header missing in part ${partNumber} upload response`);
      completedParts.push({ partNumber, etag });
    }

    ctx.onStatus?.("Assembling parts...");
    const completeRes = await fetch("/api/proxy/storageto/upload/complete-multipart", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Owner ${ownerToken}`,
      },
      body: JSON.stringify({ upload_id: uploadId, parts: completedParts }),
    });
    if (!completeRes.ok) {
      const errData = await completeRes.json().catch(() => ({}));
      throw new Error(errData.error || "Failed to assemble multipart upload");
    }
  } else {
    throw new Error(`Unsupported upload type: ${initData.type}`);
  }

  ctx.onStatus?.("Finalizing upload with storage.to...");
  const confirmRes = await fetch("/api/proxy/storageto/upload/confirm", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Visitor-Token": visitorToken,
    },
    body: JSON.stringify({
      filename: fileName,
      size: blob.size,
      content_type: contentType,
      r2_key: r2Key,
    }),
  });
  if (!confirmRes.ok) {
    const errData = await confirmRes.json().catch(() => ({}));
    throw new Error(errData.error || `Failed to confirm storage.to upload (status ${confirmRes.status})`);
  }

  const confirmData = await confirmRes.json();
  if (!confirmData.success || !confirmData.file?.url) {
    throw new Error(confirmData.error || "Failed to confirm file with storage.to");
  }

  const fileUrl: string = confirmData.file.raw_url || confirmData.file.url;
  const fileId: string = confirmData.file.id;
  ownerToken = confirmData.owner_token || ownerToken;

  if (fileId && ownerToken && ctx.expire) {
    let days = 3;
    if (ctx.expire === "24h") days = 1;
    else if (ctx.expire === "48h") days = 2;
    else if (ctx.expire === "72h") days = 3;
    else if (ctx.expire === "120h") days = 5;
    else if (ctx.expire === "168h") days = 7;
    if (days !== 3) {
      fetch(`/api/proxy/storageto/file/${fileId}/expiry`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Owner ${ownerToken}`,
        },
        body: JSON.stringify({ days }),
      }).catch((err) => console.warn("Could not set custom expiry for storage.to:", err));
    }
  }

  return fileUrl;
}

async function uploadToGofile(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Getting Gofile server...");
  const candidateServers: string[] = [];
  const cachedServer = sessionStorage.getItem("gofile_server");
  try {
    const serverRes = await fetch("https://api.gofile.io/servers");
    if (serverRes.ok) {
      const serverData = await serverRes.json();
      if (serverData.status === "ok" && Array.isArray(serverData.data?.servers)) {
        for (const s of serverData.data.servers) {
          if (s?.name) candidateServers.push(s.name);
        }
      }
    }
  } catch (err) {
    console.warn("Failed to fetch Gofile server list:", err);
  }
  if (cachedServer && !candidateServers.includes(cachedServer)) {
    candidateServers.unshift(cachedServer);
  }
  for (const fallback of ["store-eu-par-3", "store1", "store3"]) {
    if (!candidateServers.includes(fallback)) candidateServers.push(fallback);
  }

  ctx.onStatus?.("Uploading via Gofile.io...");
  let lastError: any = null;
  for (const serverName of candidateServers.slice(0, 3)) {
    if (ctx.isCancelled()) throw new Error("UPLOAD_CANCELED");
    try {
      const formData = new FormData();
      formData.append("file", blob, fileName);
      const res = await sendXhr("POST", `https://${serverName}.gofile.io/contents/uploadfile`, formData, ctx);
      const uploadJson = parseJsonSafe(res.responseText);
      if (uploadJson?.status !== "ok") {
        throw new Error(uploadJson?.status || "Gofile upload failed");
      }
      sessionStorage.setItem("gofile_server", serverName);
      const gofileCode = uploadJson.data.parentFolderCode || uploadJson.data.id;
      const guestToken = uploadJson.data.guestToken;
      if (guestToken && gofileCode) {
        const expireTimestamp = Math.floor((Date.now() + parseExpireSeconds(ctx.expire) * 1000) / 1000);
        fetch("https://api.gofile.io/contents", {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${guestToken}`,
          },
          body: JSON.stringify({
            contentId: gofileCode,
            attribute: "expire",
            attributeValue: expireTimestamp,
          }),
        }).catch((err) => console.warn("Could not set Gofile expiration", err));
      }
      return `https://gofile.io/d/${gofileCode}`;
    } catch (err: any) {
      if (err?.message === "UPLOAD_CANCELED" || ctx.isCancelled()) throw err;
      lastError = err;
    }
  }
  throw lastError || new Error("Gofile upload failed");
}

async function uploadToLitterbox(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via Litterbox...");
  const allowedTimes = new Set(["1h", "12h", "24h", "72h"]);
  const timeVal = allowedTimes.has(ctx.expire) ? ctx.expire : "24h";
  const makeForm = () => {
    const fd = new FormData();
    fd.append("reqtype", "fileupload");
    fd.append("time", timeVal);
    fd.append("fileToUpload", blob, fileName);
    return fd;
  };

  let responseText = "";
  try {
    const res = await sendXhr("POST", "https://litterbox.catbox.moe/resources/internals/api.php", makeForm(), ctx);
    responseText = res.responseText.trim();
  } catch (err: any) {
    if (err?.message === "UPLOAD_CANCELED" || ctx.isCancelled() || blob.size > WORKER_PROXY_SAFE_MAX) {
      throw err;
    }
    const proxyRes = await sendXhr("POST", "/api/proxy/litterbox", makeForm(), ctx);
    responseText = proxyRes.responseText.trim();
  }

  if (!responseText.startsWith("https://")) {
    throw new Error(responseText || "Litterbox upload failed");
  }
  return responseText;
}

async function uploadToVikingFile(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Initializing upload with VikingFile...");
  const initRes = await fetch("/api/proxy/vikingfile/get-upload-url", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ size: String(blob.size) }).toString(),
  });
  if (!initRes.ok) {
    throw new Error(`VikingFile init failed (${initRes.status})`);
  }
  const initData = await initRes.json();
  const { uploadId, key, partSize, numberParts, urls } = initData;
  if (!uploadId || !key || !Array.isArray(urls) || urls.length === 0) {
    throw new Error("VikingFile returned invalid multipart metadata");
  }

  ctx.onStatus?.(numberParts > 1 ? `Uploading ${numberParts} parts to VikingFile...` : "Uploading via VikingFile...");
  const completedParts: { PartNumber: number; ETag: string }[] = [];
  for (let i = 0; i < urls.length; i++) {
    if (ctx.isCancelled()) throw new Error("UPLOAD_CANCELED");
    const partNumber = i + 1;
    const start = i * partSize;
    const end = Math.min(start + partSize, blob.size);
    const chunk = blob.slice(start, end);

    const putRes = await sendXhr("PUT", urls[i], chunk, {
      ...ctx,
      onProgress: (chunkPct) => {
        const overall = Math.round(((partNumber - 1) / urls.length) * 100 + chunkPct / urls.length);
        ctx.onProgress(overall);
      },
    });
    const etag = putRes.getHeader("ETag") || putRes.getHeader("etag");
    if (!etag) throw new Error(`VikingFile part ${partNumber} missing ETag`);
    completedParts.push({ PartNumber: partNumber, ETag: etag.replaceAll('"', "") });
  }

  ctx.onStatus?.("Finalizing VikingFile upload...");
  const completeParams = new URLSearchParams();
  completeParams.set("key", key);
  completeParams.set("uploadId", uploadId);
  completeParams.set("name", fileName);
  completeParams.set("user", "");
  completedParts.forEach((p, idx) => {
    completeParams.set(`parts[${idx}][PartNumber]`, String(p.PartNumber));
    completeParams.set(`parts[${idx}][ETag]`, p.ETag);
  });

  const completeRes = await fetch("/api/proxy/vikingfile/complete-upload", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: completeParams.toString(),
  });
  if (!completeRes.ok) {
    throw new Error(`VikingFile complete failed (${completeRes.status})`);
  }
  const completeData = await completeRes.json();
  if (!completeData?.url) {
    throw new Error(completeData?.error || "VikingFile did not return a download URL");
  }
  return completeData.url;
}

async function uploadToFilebin(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via Filebin...");
  const bin = `hefimer-${crypto.randomUUID().replaceAll("-", "").slice(0, 20)}`;
  const uploadUrl = `/api/proxy/filebin/${encodeURIComponent(bin)}/${encodeURIComponent(fileName)}`;
  await sendXhr("POST", uploadUrl, blob, ctx, {
    "Content-Type": blob.type || "application/octet-stream",
  });
  return `https://filebin.net/${bin}/${encodeURIComponent(fileName)}`;
}

async function uploadToTempFile(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via TempFile...");
  const hours = Number.parseInt(ctx.expire, 10);
  const validHours = [1, 6, 24, 48].includes(hours) ? hours : 24;
  const makeForm = () => {
    const fd = new FormData();
    fd.append("files", blob, fileName);
    fd.append("expiryHours", String(validHours));
    return fd;
  };

  let res;
  try {
    res = await sendXhr("POST", "https://tempfile.org/api/upload/local", makeForm(), ctx);
  } catch (err: any) {
    if (err?.message === "UPLOAD_CANCELED" || ctx.isCancelled()) throw err;
    res = await sendXhr("POST", "/api/proxy/tempfile", makeForm(), ctx);
  }
  const result = parseJsonSafe(res.responseText);
  const fileInfo = result?.files?.[0];
  if (!result?.success || !fileInfo?.url) {
    throw new Error(result?.message || "TempFile upload failed");
  }
  return `${fileInfo.url}download`;
}

async function uploadToTmpfiles(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via tmpfiles.org...");
  const makeForm = () => {
    const fd = new FormData();
    fd.append("file", blob, fileName);
    return fd;
  };

  let res;
  try {
    res = await sendXhr("POST", "https://tmpfiles.org/api/v1/upload", makeForm(), ctx);
  } catch (err: any) {
    if (err?.message === "UPLOAD_CANCELED" || ctx.isCancelled()) throw err;
    res = await sendXhr("POST", "/api/proxy/tmpfiles", makeForm(), ctx);
  }
  const result = parseJsonSafe(res.responseText);
  if (result?.status === "success" && result?.data?.url) {
    return result.data.url;
  }
  throw new Error(result?.error || result?.message || "tmpfiles upload failed");
}

async function uploadToTmpfileLink(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via tfLink...");
  const fd = new FormData();
  fd.append("file", blob, fileName);
  const res = await sendXhr("POST", "/api/proxy/tmpfilelink", fd, ctx);
  const data = parseJsonSafe(res.responseText);
  const url = data?.downloadLink || data?.downloadLinkEncoded;
  if (!url) throw new Error(data?.error || "tfLink upload failed");
  return url;
}

async function uploadToTempSh(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via temp.sh...");
  const fd = new FormData();
  fd.append("file", blob, fileName);
  const res = await sendXhr("POST", "/api/proxy/tempsh", fd, ctx);
  const text = res.responseText.trim();
  if (!text.startsWith("https://temp.sh/")) {
    throw new Error(text || "temp.sh upload failed");
  }
  return text;
}

async function uploadToUguu(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via Uguu.se...");
  const fd = new FormData();
  fd.append("files[]", blob, fileName);
  const res = await sendXhr("POST", "/api/proxy/uguu", fd, ctx);
  const data = parseJsonSafe(res.responseText);
  const url = data?.files?.[0]?.url;
  if (!data?.success || !url) {
    throw new Error(data?.description || data?.error || "Uguu.se upload failed");
  }
  return url;
}

async function uploadToPutIcu(blob: Blob, fileName: string, ctx: UploadContext): Promise<string> {
  ctx.onStatus?.("Uploading via Put.icu...");
  const expirySec = Math.min(Math.max(parseExpireSeconds(ctx.expire), 60), 86400);
  const res = await sendXhr(
    "PUT",
    `/api/proxy/puticu/${encodeURIComponent(fileName)}`,
    blob,
    ctx,
    {
      Accept: "application/json",
      "Content-Type": blob.type || "application/octet-stream",
      "Linx-Expiry": String(expirySec),
    },
  );
  const data = parseJsonSafe(res.responseText);
  const url = data?.direct_url || data?.url;
  if (!url) throw new Error(data?.error || "Put.icu upload failed");
  return url;
}

async function uploadSingleProvider(
  providerId: string,
  blob: Blob,
  fileName: string,
  ctx: UploadContext,
): Promise<string> {
  switch (providerId) {
    case "stocare":
      return uploadToStoCare(blob, fileName, ctx);
    case "storageto":
      return uploadToStorageTo(blob, fileName, ctx);
    case "gofile":
      return uploadToGofile(blob, fileName, ctx);
    case "litterbox":
      return uploadToLitterbox(blob, fileName, ctx);
    case "vikingfile":
      return uploadToVikingFile(blob, fileName, ctx);
    case "tmpfilelink":
      return uploadToTmpfileLink(blob, fileName, ctx);
    case "tempfile":
      return uploadToTempFile(blob, fileName, ctx);
    case "filebin":
      return uploadToFilebin(blob, fileName, ctx);
    case "tempsh":
      return uploadToTempSh(blob, fileName, ctx);
    case "tmpfiles":
      return uploadToTmpfiles(blob, fileName, ctx);
    case "uguu":
      return uploadToUguu(blob, fileName, ctx);
    case "puticu":
      return uploadToPutIcu(blob, fileName, ctx);
    default:
      throw new Error(`Unsupported upload provider: ${providerId}`);
  }
}

export async function uploadFileWithFallback(
  primaryProviderId: string,
  blob: Blob,
  fileName: string,
  ctx: UploadContext,
  onFallback?: (fromName: string, toName: string) => void,
): Promise<FallbackUploadResult> {
  const orderedIds = [
    primaryProviderId,
    ...FALLBACK_PRIORITY.filter((id) => id !== primaryProviderId),
  ];

  const eligibleIds = orderedIds.filter((id) => {
    const cfg = getProviderConfig(id);
    return blob.size <= cfg.maxSizeBytes;
  });

  if (eligibleIds.length === 0) {
    const primary = getProviderConfig(primaryProviderId);
    throw new Error(`File size exceeds limit of ${primary.maxSizeLabel} for ${primary.name}`);
  }

  let lastError: any = null;
  for (let i = 0; i < eligibleIds.length; i++) {
    const providerId = eligibleIds[i];
    const providerCfg = getProviderConfig(providerId);
    if (ctx.isCancelled()) {
      throw new Error("UPLOAD_CANCELED");
    }

    if (i > 0) {
      const prevCfg = getProviderConfig(eligibleIds[i - 1]);
      onFallback?.(prevCfg.name, providerCfg.name);
      ctx.onProgress(0);
    }

    try {
      const effectiveExpire = i === 0 ? ctx.expire : providerCfg.defaultExpire;
      const fileUrl = await uploadSingleProvider(providerId, blob, fileName, {
        ...ctx,
        expire: effectiveExpire,
      });
      return {
        fileUrl,
        providerId,
        providerName: providerCfg.name,
      };
    } catch (err: any) {
      if (err?.message === "UPLOAD_CANCELED" || ctx.isCancelled()) {
        throw new Error("UPLOAD_CANCELED");
      }
      console.warn(`Provider ${providerCfg.name} failed, attempting fallback:`, err);
      lastError = err;
    }
  }

  throw lastError || new Error("All upload providers failed");
}
