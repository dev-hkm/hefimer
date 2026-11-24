import assert from "node:assert/strict";
import test from "node:test";
import { buildDownloadProxyUrl, getDirectDownloadMode } from "../src/downloads";

test("temporary providers download through the same-origin proxy", () => {
  assert.equal(getDirectDownloadMode("https://storage.to/raw/example"), "proxy");
  assert.equal(getDirectDownloadMode("https://tmpfiles.org/dl/123/file.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://litter.catbox.moe/example.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://filebin.net/example-bin/file.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://trystocare.com/example123"), "proxy");
  assert.equal(getDirectDownloadMode("https://d.tmpfile.link/public/2026-11-24/id/file.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://tempfile.org/abc123/download"), "proxy");
  assert.equal(getDirectDownloadMode("https://temp.sh/abc/file.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://d.uguu.se/abc.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://put.icu/s/abc.bin"), "proxy");
  assert.equal(getDirectDownloadMode("https://gofile.io/d/example"), "tab");
  assert.equal(getDirectDownloadMode("https://vikingfile.com/f/example"), "tab");
});

test("download proxy URL preserves the complete source URL and filename", () => {
  const source = "https://storage.to/raw/example?signature=a%2Bb&part=1";
  const proxy = new URL(buildDownloadProxyUrl(source, "report final.pdf"), "https://hefimer.qzz.io");

  assert.equal(proxy.pathname, "/api/download");
  assert.equal(proxy.searchParams.get("url"), source);
  assert.equal(proxy.searchParams.get("filename"), "report final.pdf");
});
