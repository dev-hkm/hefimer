import assert from "node:assert/strict";
import test from "node:test";
import worker from "../secondary-worker/src/index";

const FIREBASE_ORIGIN = "https://hefimer.web.app";
const env = {
  ALLOWED_ORIGINS: `${FIREBASE_ORIGIN},https://hefimer.firebaseapp.com`,
} as never;

test("secondary Worker answers health checks without touching application data", async () => {
  const response = await worker.fetch(new Request("https://api.example.test/health"), env);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    ok: true,
    service: "hefimer-secondary-api",
  });
});

test("secondary Worker allows exact Firebase CORS preflights", async () => {
  const response = await worker.fetch(new Request("https://api.example.test/api/devices/sync", {
    method: "OPTIONS",
    headers: {
      Origin: FIREBASE_ORIGIN,
      "Access-Control-Request-Method": "GET",
      "Access-Control-Request-Headers": "X-Hefimer-Device,X-Hefimer-Signature",
    },
  }), env);

  assert.equal(response.status, 204);
  assert.equal(response.headers.get("Access-Control-Allow-Origin"), FIREBASE_ORIGIN);
  assert.match(response.headers.get("Access-Control-Allow-Headers") || "", /X-Hefimer-Signature/);
});

test("secondary Worker rejects Pages and missing browser origins", async () => {
  const pagesResponse = await worker.fetch(new Request("https://api.example.test/api/download", {
    headers: { Origin: "https://hefimer.qzz.io" },
  }), env);
  const anonymousResponse = await worker.fetch(new Request("https://api.example.test/api/download"), env);

  assert.equal(pagesResponse.status, 403);
  assert.equal(anonymousResponse.status, 403);
  assert.equal(pagesResponse.headers.get("Access-Control-Allow-Origin"), null);
});

test("secondary Worker keeps secret R2 unavailable", async () => {
  const response = await worker.fetch(new Request("https://api.example.test/api/r2/download-url", {
    headers: { Origin: FIREBASE_ORIGIN },
  }), env);

  assert.equal(response.status, 503);
  assert.equal(response.headers.get("Access-Control-Allow-Origin"), FIREBASE_ORIGIN);
  assert.match(await response.text(), /not available/i);
});

test("secondary Worker never falls back to the production device database", async () => {
  const response = await worker.fetch(new Request("https://api.example.test/api/devices/sync", {
    headers: { Origin: FIREBASE_ORIGIN },
  }), env);

  assert.equal(response.status, 503);
  assert.match(await response.text(), /isolated mirror/i);
});

test("secondary Worker dispatches provider downloads with attachment CORS headers", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("worker bytes", {
    headers: {
      "Content-Length": "12",
      "Content-Type": "application/octet-stream",
    },
  });

  try {
    const source = encodeURIComponent("https://tmpfiles.org/example.bin");
    const response = await worker.fetch(new Request(
      `https://api.example.test/api/download?url=${source}&filename=example.bin`,
      { headers: { Referer: `${FIREBASE_ORIGIN}/` } },
    ), env);

    assert.equal(response.status, 200);
    assert.equal(await response.text(), "worker bytes");
    assert.equal(response.headers.get("Access-Control-Allow-Origin"), FIREBASE_ORIGIN);
    assert.equal(response.headers.get("Content-Length"), "12");
    assert.match(response.headers.get("Content-Disposition") || "", /example\.bin/);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
