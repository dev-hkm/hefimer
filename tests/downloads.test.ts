import assert from "node:assert/strict";
import test from "node:test";
import { buildDownloadProxyUrl, getDirectDownloadMode } from "../src/downloads";

test("temporary providers download through the same-origin proxy", () => {
  assert.equal(getDirectDownloadMode("https://storage.to/raw/example"), "proxy");
  assert.equal(getDirectDownloadMode("https://tmpfiles.org/dl/123/file.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://litter.catbox.moe/example.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://gofile.io/d/example"), "tab");
});

test("download proxy URL preserves the complete source URL and filename", () => {
  const source = "https://storage.to/raw/example?signature=a%2Bb&part=1";
  const proxy = new URL(buildDownloadProxyUrl(source, "report final.pdf"), "https://hefimer.qzz.io");

  assert.equal(proxy.pathname, "/api/download");
  assert.equal(proxy.searchParams.get("url"), source);
  assert.equal(proxy.searchParams.get("filename"), "report final.pdf");
});
