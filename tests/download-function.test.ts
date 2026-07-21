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

test("storage.to share pages resolve to the signed file download", async () => {
  const originalFetch = globalThis.fetch;
  const requests: string[] = [];
  globalThis.fetch = async (input) => {
    const url = String(input);
    requests.push(url);
    if (url === "https://storage.to/mtUasBVgi") {
      return new Response(`
        <button data-url="https://storage.to/mtUasBVgi/download?expires=123&amp;signature=abc123">
          Download
        </button>
      `, { headers: { "Content-Type": "text/html; charset=utf-8" } });
    }
    if (url === "https://storage.to/mtUasBVgi/download?expires=123&signature=abc123") {
      return new Response("markdown bytes", {
        headers: {
          "Content-Length": "14",
          "Content-Type": "text/markdown",
        },
      });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  };

  try {
    const source = encodeURIComponent("https://storage.to/mtUasBVgi");
    const request = new Request(`https://hefimer.qzz.io/api/download?url=${source}&filename=perplexity.md`, {
      headers: { Referer: "https://hefimer.qzz.io/" },
    });
    const response = await onRequestGet({ request });

    assert.equal(response.status, 200);
    assert.equal(await response.text(), "markdown bytes");
    assert.equal(response.headers.get("Content-Type"), "text/markdown");
    assert.match(response.headers.get("Content-Disposition") || "", /filename="perplexity\.md"/);
    assert.deepEqual(requests, [
      "https://storage.to/mtUasBVgi",
      "https://storage.to/mtUasBVgi/download?expires=123&signature=abc123",
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
