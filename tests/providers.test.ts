import assert from "node:assert/strict";
import test from "node:test";
import { getProviderNameFromUrl, normalizeExpireForProvider, UPLOAD_PROVIDERS } from "../src/providers";

test("UPLOAD_PROVIDERS has unique ids and valid expiry defaults", () => {
  const ids = new Set<string>();
  for (const prov of UPLOAD_PROVIDERS) {
    assert.equal(ids.has(prov.id), false, `Duplicate provider id: ${prov.id}`);
    ids.add(prov.id);
    assert.ok(prov.expireOptions.length > 0, `${prov.id} missing expireOptions`);
    assert.ok(
      prov.expireOptions.some((opt) => opt.value === prov.defaultExpire),
      `${prov.id} defaultExpire ${prov.defaultExpire} not in expireOptions`,
    );
  }
  assert.ok(UPLOAD_PROVIDERS.length >= 12);
});

test("normalizeExpireForProvider preserves valid choices and falls back to default", () => {
  assert.equal(normalizeExpireForProvider("litterbox", "72h"), "72h");
  assert.equal(normalizeExpireForProvider("litterbox", "168h"), "24h");
  assert.equal(normalizeExpireForProvider("tmpfiles", "24h"), "1h");
  assert.equal(normalizeExpireForProvider("puticu", "5p"), "5p");
});

test("getProviderNameFromUrl identifies all supported hosts", () => {
  assert.equal(getProviderNameFromUrl("https://trystocare.com/abc"), "sto.care");
  assert.equal(getProviderNameFromUrl("https://storage.to/abc"), "storage.to");
  assert.equal(getProviderNameFromUrl("https://gofile.io/d/abc"), "Gofile");
  assert.equal(getProviderNameFromUrl("https://litter.catbox.moe/abc.txt"), "Litterbox");
  assert.equal(getProviderNameFromUrl("https://vikingfile.com/f/abc"), "VikingFile");
  assert.equal(getProviderNameFromUrl("https://d.tmpfile.link/public/x/y.txt"), "tfLink");
  assert.equal(getProviderNameFromUrl("https://tempfile.org/abc/download"), "TempFile");
  assert.equal(getProviderNameFromUrl("https://filebin.net/bin/a.txt"), "Filebin");
  assert.equal(getProviderNameFromUrl("https://temp.sh/abc/a.txt"), "temp.sh");
  assert.equal(getProviderNameFromUrl("https://tmpfiles.org/123/a.txt"), "tmpfiles.org");
  assert.equal(getProviderNameFromUrl("https://d.uguu.se/abc.txt"), "Uguu.se");
  assert.equal(getProviderNameFromUrl("https://put.icu/s/abc.txt"), "Put.icu");
  assert.equal(getProviderNameFromUrl("https://example.com/a", true), "Cloudflare R2");
});
