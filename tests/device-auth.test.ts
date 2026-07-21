import assert from "node:assert/strict";
import test from "node:test";
import {
  buildSignaturePayload,
  fromBase64Url,
  sha256Base64Url,
  toBase64Url,
} from "../functions/lib/device-auth";

test("base64url helpers preserve binary values", () => {
  const original = Uint8Array.from([0, 1, 2, 127, 128, 250, 255]);
  assert.deepEqual(fromBase64Url(toBase64Url(original)), original);
});

test("device request payload is deterministic", async () => {
  const bodyHash = await sha256Base64Url('{"name":"Laptop"}');
  assert.equal(
    buildSignaturePayload("1721600000000", "nonce_123", "patch", "/api/devices/device", bodyHash),
    `1721600000000\nnonce_123\nPATCH\n/api/devices/device\n${bodyHash}`,
  );
});

test("browser-compatible ECDSA signatures verify", async () => {
  const pair = await crypto.subtle.generateKey(
    { name: "ECDSA", namedCurve: "P-256" },
    true,
    ["sign", "verify"],
  );
  const payload = buildSignaturePayload(
    Date.now().toString(),
    "test_nonce_123456",
    "POST",
    "/api/devices/groups",
    await sha256Base64Url('{"duration":"7d"}'),
  );
  const signature = await crypto.subtle.sign(
    { name: "ECDSA", hash: "SHA-256" },
    pair.privateKey,
    new TextEncoder().encode(payload),
  );
  const exported = await crypto.subtle.exportKey("spki", pair.publicKey);
  const imported = await crypto.subtle.importKey(
    "spki",
    fromBase64Url(toBase64Url(exported)),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["verify"],
  );
  assert.equal(
    await crypto.subtle.verify(
      { name: "ECDSA", hash: "SHA-256" },
      imported,
      fromBase64Url(toBase64Url(signature)),
      new TextEncoder().encode(payload),
    ),
    true,
  );
});
