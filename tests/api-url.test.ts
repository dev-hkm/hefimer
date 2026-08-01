import assert from "node:assert/strict";
import test from "node:test";
import { buildApiUrl, normalizeApiBase } from "../src/api-url";

test("Pages builds keep same-origin API paths", () => {
  assert.equal(normalizeApiBase(""), "");
  assert.equal(buildApiUrl("/api/download?code=123", ""), "/api/download?code=123");
});

test("Firebase builds resolve API paths against the Worker origin", () => {
  const base = normalizeApiBase("https://hefimer-secondary-api.example.workers.dev/");
  assert.equal(base, "https://hefimer-secondary-api.example.workers.dev");
  assert.equal(
    buildApiUrl("/api/devices/sync", base),
    "https://hefimer-secondary-api.example.workers.dev/api/devices/sync",
  );
});

test("configured API origins must be HTTPS", () => {
  assert.throws(() => normalizeApiBase("http://api.example.com"), /HTTPS/);
});
