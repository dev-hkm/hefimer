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

test("storage.to mint proofs resolve through the current download API", async () => {
  const originalFetch = globalThis.fetch;
  const requests: string[] = [];
  globalThis.fetch = async (input, init) => {
    const url = String(input);
    requests.push(url);
    if (url === "https://storage.to/mCn2TaULN") {
      return new Response('<script>stream.enqueue("[\\\"mint_proof\\\",\\\"1785590600.96d372c4e11c0693324b255cec905bd5e862df1a71bef54c66d1c1c9d4f64274\\\"]")</script>');
    }
    if (url === "https://storage.to/mCn2TaULN/download") {
      assert.equal(new Headers(init?.headers).get("x-mint-proof"), "1785590600.96d372c4e11c0693324b255cec905bd5e862df1a71bef54c66d1c1c9d4f64274");
      return Response.json({
        url: "https://stusercontent.com/object-id?expires=123&sig=abc",
      });
    }
    if (url === "https://stusercontent.com/object-id?expires=123&sig=abc") {
      return new Response("current storage bytes", {
        headers: { "Content-Type": "text/plain", "Content-Length": "21" },
      });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  };

  try {
    const source = encodeURIComponent("https://storage.to/mCn2TaULN");
    const request = new Request(`https://hefimer.qzz.io/api/download?url=${source}&filename=current.txt`, {
      headers: { Referer: "https://hefimer.qzz.io/" },
    });
    const response = await onRequestGet({ request });

    assert.equal(response.status, 200);
    assert.equal(await response.text(), "current storage bytes");
    assert.equal(response.headers.get("Content-Type"), "text/plain");
    assert.deepEqual(requests, [
      "https://storage.to/mCn2TaULN",
      "https://storage.to/mCn2TaULN/download",
      "https://stusercontent.com/object-id?expires=123&sig=abc",
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
