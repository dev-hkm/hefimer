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

test("tmpfiles share pages resolve the current timestamped download link", async () => {
  const originalFetch = globalThis.fetch;
  const requests: string[] = [];
  globalThis.fetch = async (input) => {
    const url = String(input);
    requests.push(url);
    if (url === "https://tmpfiles.org/abc123/report.txt") {
      return new Response(`
        <a class="download" href="https://tmpfiles.org/dl/1785852038.token/abc123/report.txt">Download</a>
      `, { headers: { "Content-Type": "text/html; charset=utf-8" } });
    }
    if (url === "https://tmpfiles.org/dl/1785852038.token/abc123/report.txt") {
      return new Response("tmpfiles bytes", {
        headers: { "Content-Type": "text/plain", "Content-Length": "14" },
      });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  };

  try {
    const source = encodeURIComponent("https://tmpfiles.org/abc123/report.txt");
    const request = new Request(`https://hefimer.qzz.io/api/download?url=${source}&filename=report.txt`, {
      headers: { Referer: "https://hefimer.qzz.io/" },
    });
    const response = await onRequestGet({ request });

    assert.equal(response.status, 200);
    assert.equal(await response.text(), "tmpfiles bytes");
    assert.equal(response.headers.get("Content-Type"), "text/plain");
    assert.deepEqual(requests, [
      "https://tmpfiles.org/abc123/report.txt",
      "https://tmpfiles.org/dl/1785852038.token/abc123/report.txt",
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("Filebin verification pages resolve to the provider download", async () => {
  const originalFetch = globalThis.fetch;
  const requests: string[] = [];
  let filebinVisits = 0;
  globalThis.fetch = async (input, init) => {
    const url = String(input);
    requests.push(url);
    if (url === "https://filebin.net/hefimer-bin/report.txt") {
      filebinVisits += 1;
      if (filebinVisits === 1) {
        return new Response("verification", {
          headers: {
            "Content-Type": "text/html; charset=utf-8",
            "Set-Cookie": "verified=2026-08-04; Path=/; HttpOnly; Secure",
          },
        });
      }
      assert.equal(new Headers(init?.headers).get("cookie"), "verified=2026-08-04");
      return new Response(null, {
        status: 302,
        headers: { Location: "https://storage.filebin.net/object?signature=abc" },
      });
    }
    if (url === "https://storage.filebin.net/object?signature=abc") {
      return new Response("filebin bytes", {
        headers: { "Content-Type": "text/plain", "Content-Length": "13" },
      });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  };

  try {
    const source = encodeURIComponent("https://filebin.net/hefimer-bin/report.txt");
    const request = new Request(`https://hefimer.qzz.io/api/download?url=${source}&filename=report.txt`, {
      headers: { Referer: "https://hefimer.qzz.io/" },
    });
    const response = await onRequestGet({ request });

    assert.equal(response.status, 200);
    assert.equal(await response.text(), "filebin bytes");
    assert.equal(response.headers.get("Content-Type"), "text/plain");
    assert.deepEqual(requests, [
      "https://filebin.net/hefimer-bin/report.txt",
      "https://filebin.net/hefimer-bin/report.txt",
      "https://storage.filebin.net/object?signature=abc",
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
