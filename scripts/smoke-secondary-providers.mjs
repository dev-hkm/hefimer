import assert from "node:assert/strict";

const apiBase = process.env.HEFIMER_SECONDARY_API
  || "https://hefimer-secondary-api.hoangkhanhminh2005-vl.workers.dev";
const origin = process.env.HEFIMER_FRONTEND_ORIGIN || "https://hefimer.web.app";
const bytes = new TextEncoder().encode(`Hefimer secondary smoke ${Date.now()}`);
const filename = `hefimer-secondary-smoke-${Date.now()}.txt`;
const results = {};
const fetch = (input, init = {}) => globalThis.fetch(input, {
  ...init,
  signal: AbortSignal.timeout(30_000),
});

async function checked(response, label) {
  if (!response.ok) {
    throw new Error(`${label} failed (${response.status}): ${await response.text()}`);
  }
  return response;
}

async function verifyProxyDownload(url, label) {
  const query = new URLSearchParams({ url, filename });
  const response = await checked(await fetch(`${apiBase}/api/download?${query}`, {
    headers: { Origin: origin },
  }), `${label} download`);
  assert.equal(response.headers.get("access-control-allow-origin"), origin);
  assert.match(response.headers.get("content-disposition") || "", /attachment/i);
  const downloaded = new Uint8Array(await response.arrayBuffer());
  if (!Buffer.from(downloaded).equals(Buffer.from(bytes))) {
    throw new Error(`${label} returned ${downloaded.byteLength} bytes as ${response.headers.get("content-type") || "unknown"}`);
  }
}

async function smokeLitterbox() {
  const form = new FormData();
  form.append("reqtype", "fileupload");
  form.append("time", "1h");
  form.append("fileToUpload", new Blob([bytes], { type: "text/plain" }), filename);
  const response = await checked(await fetch(`${apiBase}/api/proxy/litterbox`, {
    method: "POST",
    headers: { Origin: origin },
    body: form,
  }), "Litterbox upload");
  const url = (await response.text()).trim();
  assert.match(url, /^https:\/\/litterbox\.catbox\.moe\/files\//);
  await verifyProxyDownload(url, "Litterbox");
  results.litterbox = "upload and proxy download passed";
}

async function smokeTmpfiles() {
  const form = new FormData();
  form.append("file", new Blob([bytes], { type: "text/plain" }), filename);
  const response = await checked(await fetch(`${apiBase}/api/proxy/tmpfiles`, {
    method: "POST",
    headers: { Origin: origin },
    body: form,
  }), "tmpfiles upload");
  const payload = await response.json();
  assert.equal(payload.status, "success");
  const url = String(payload.data?.url || "").replace("tmpfiles.org/", "tmpfiles.org/dl/");
  console.log(`tmpfiles URL: ${url}`);
  assert.match(url, /^https:\/\/tmpfiles\.org\/dl\//);
  await verifyProxyDownload(url, "tmpfiles");
  results.tmpfiles = "upload and proxy download passed";
}

async function smokeStorageTo() {
  const visitor = crypto.randomUUID().replaceAll("-", "");
  const headers = {
    Origin: origin,
    "Content-Type": "application/json",
    "X-Visitor-Token": visitor,
  };
  const init = await checked(await fetch(`${apiBase}/api/proxy/storageto/upload/init`, {
    method: "POST",
    headers,
    body: JSON.stringify({ filename, content_type: "text/plain", size: bytes.byteLength }),
  }), "storage.to init");
  const initialized = await init.json();
  assert.equal(initialized.success, true);
  assert.equal(initialized.type, "single");

  await checked(await fetch(initialized.upload_url, {
    method: "PUT",
    headers: { "Content-Type": "text/plain" },
    body: bytes,
  }), "storage.to byte upload");

  const confirm = await checked(await fetch(`${apiBase}/api/proxy/storageto/upload/confirm`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      filename,
      size: bytes.byteLength,
      content_type: "text/plain",
      r2_key: initialized.r2_key,
    }),
  }), "storage.to confirm");
  const confirmed = await confirm.json();
  assert.equal(confirmed.success, true);
  const url = confirmed.file?.raw_url || confirmed.file?.url;
  console.log(`storage.to URL: ${url}`);
  assert.match(String(url), /^https:\/\/storage\.to\//);
  await verifyProxyDownload(url, "storage.to");
  results.storageTo = "upload and proxy download passed";
}

async function smokeGofile() {
  const servers = await checked(await fetch("https://api.gofile.io/servers"), "Gofile server lookup");
  const serverPayload = await servers.json();
  const server = serverPayload.data?.servers?.[0]?.name;
  console.log(`Gofile server: ${server || "none"}`);
  assert.ok(server, "Gofile did not return an upload server");
  const form = new FormData();
  form.append("file", new Blob([bytes], { type: "text/plain" }), filename);
  const upload = await checked(await fetch(`https://${server}.gofile.io/contents/uploadfile`, {
    method: "POST",
    body: form,
  }), "Gofile upload");
  const payload = await upload.json();
  assert.equal(payload.status, "ok");
  assert.ok(payload.data?.parentFolderCode || payload.data?.id);
  results.gofile = "direct browser-compatible upload passed";
}

const failures = {};
async function run(name, smoke) {
  console.log(`Checking ${name}...`);
  try {
    await smoke();
    console.log(`${name}: passed`);
  } catch (error) {
    failures[name] = error instanceof Error ? error.message : String(error);
    console.log(`${name}: failed`);
  }
}

await run("litterbox", smokeLitterbox);
await run("tmpfiles", smokeTmpfiles);
await run("storageTo", smokeStorageTo);
await run("gofile", smokeGofile);
console.log(JSON.stringify({ ok: Object.keys(failures).length === 0, results, failures }, null, 2));
if (Object.keys(failures).length) process.exitCode = 1;
