import assert from "node:assert/strict";
import test from "node:test";
import { onRequestGet } from "../functions/api/download";

test("download function streams provider bytes as an attachment", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("file bytes", {
    headers: {
      "Content-Length": "10",
      "Content-Type": "application/octet-stream",
    },
  });

  try {
    const source = encodeURIComponent("https://storage.to/raw/example");
    const filename = encodeURIComponent("báo cáo.txt");
    const request = new Request(`https://hefimer.qzz.io/api/download?url=${source}&filename=${filename}`, {
      headers: { Referer: "https://hefimer.qzz.io/" },
    });
    const response = await onRequestGet({ request });

    assert.equal(response.status, 200);
    assert.equal(await response.text(), "file bytes");
    assert.match(response.headers.get("Content-Disposition") || "", /^attachment;/);
    assert.match(response.headers.get("Content-Disposition") || "", /filename\*=UTF-8''/);
    assert.equal(response.headers.get("Content-Length"), "10");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
