import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

test("storage.to proxy forwards the WAF-safe browser request context", async () => {
  const source = await readFile(new URL("../functions/api/proxy/storageto/[[path]].ts", import.meta.url), "utf8");

  for (const header of ["accept", "accept-language", "origin", "referer"]) {
    assert.match(source, new RegExp(`"${header}"`));
  }
  assert.match(source, /headers\.set\("user-agent", request\.headers\.get\("user-agent"\)/);
});

test("storage.to downloads use the same browser request context", async () => {
  const source = await readFile(new URL("../functions/api/download.ts", import.meta.url), "utf8");

  for (const header of ["Accept", "User-Agent", "Origin", "Referer"]) {
    assert.match(source, new RegExp(`upstreamHeaders\\.set\\("${header}"`));
  }
});
