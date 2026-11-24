import assert from "node:assert/strict";
import test from "node:test";
import {
  computeAutoArrangeLayout,
  computeFitView,
  computeSmartAlignment,
  detectPasteContent,
  formatCountdown,
  getMediaPreviewUrl,
  normalizeLegacyFile,
  parseSpaceDuration,
  screenToCanvas,
  snapCoord,
} from "../src/space/types";

test("screenToCanvas converts screen coordinates with pan and zoom", () => {
  const pt = screenToCanvas(500, 300, { left: 100, top: 50 }, { x: 40, y: 50 }, 2);
  assert.deepEqual(pt, { x: 180, y: 100 });
});

test("snapCoord snaps to grid when enabled and rounds when disabled", () => {
  assert.equal(snapCoord(112, true, 20), 120);
  assert.equal(snapCoord(107, true, 20), 100);
  assert.equal(snapCoord(112.6, false, 20), 113);
});

test("computeFitView returns default view when empty and bounded view for items", () => {
  assert.deepEqual(computeFitView([], 1200, 800), { pan: { x: 0, y: 0 }, zoom: 1 });

  const fit = computeFitView(
    [
      { x: 0, y: 0 },
      { x: 1000, y: 600 },
    ],
    1200,
    800,
  );
  assert.ok(fit.zoom >= 0.35 && fit.zoom <= 1.2);
  assert.ok(Number.isFinite(fit.pan.x) && Number.isFinite(fit.pan.y));
});

test("computeAutoArrangeLayout groups items into deterministic grid coordinates", () => {
  const layout = computeAutoArrangeLayout([
    {
      id: "n1",
      kind: "note",
      name: "Note 1",
      x: 500,
      y: 500,
      uploadedBy: "A",
      timestamp: 10,
      sourceNode: "items",
    },
    {
      id: "f1",
      kind: "file",
      name: "File 1",
      x: 900,
      y: 200,
      uploadedBy: "A",
      timestamp: 20,
      sourceNode: "files",
    },
  ]);

  assert.equal(layout[0].id, "f1");
  assert.equal(layout[1].id, "n1");
  assert.deepEqual({ x: layout[0].x, y: layout[0].y }, { x: 72, y: 104 });
  assert.deepEqual({ x: layout[1].x, y: layout[1].y }, { x: 368, y: 104 });

  // Verify tall image cards in row 1 push row 2 down cleanly without overlap
  const twoRowLayout = computeAutoArrangeLayout([
    {
      id: "img1",
      kind: "file",
      name: "photo.png",
      mimeType: "image/png",
      url: "https://trystocare.com/photo",
      x: 0,
      y: 0,
      uploadedBy: "A",
      timestamp: 1,
      sourceNode: "items",
    },
    {
      id: "n2",
      kind: "note",
      name: "Note 2",
      x: 0,
      y: 0,
      uploadedBy: "A",
      timestamp: 2,
      sourceNode: "items",
    },
    {
      id: "n3",
      kind: "link",
      name: "Link 3",
      x: 0,
      y: 0,
      uploadedBy: "A",
      timestamp: 3,
      sourceNode: "items",
    },
  ]);
  const row1Y = twoRowLayout[0].y;
  const row2Y = twoRowLayout[2].y;
  assert.ok(row2Y >= row1Y + 276 + 36, `Expected row2Y (${row2Y}) >= ${row1Y + 276 + 36}`);
});

test("computeSmartAlignment snaps coordinates within threshold and emits guides", () => {
  const aligned = computeSmartAlignment(205, 144, "drag", [
    {
      id: "drag",
      kind: "note",
      name: "Dragged",
      x: 0,
      y: 0,
      uploadedBy: "A",
      timestamp: 1,
      sourceNode: "items",
    },
    {
      id: "target",
      kind: "note",
      name: "Target",
      x: 200,
      y: 140,
      uploadedBy: "A",
      timestamp: 2,
      sourceNode: "items",
    },
  ]);
  assert.deepEqual(aligned, { x: 200, y: 140, guideX: 200, guideY: 140 });
});

test("parseSpaceDuration and formatCountdown handle durations and expiry", () => {
  assert.equal(parseSpaceDuration("30m"), 30 * 60 * 1000);
  assert.equal(parseSpaceDuration("6h"), 6 * 3600 * 1000);
  assert.equal(parseSpaceDuration("72h"), 72 * 3600 * 1000);
  assert.equal(parseSpaceDuration("invalid"), 3600 * 1000);

  const now = 1_000_000;
  assert.equal(formatCountdown(now - 1, now), "Expired");
  assert.equal(formatCountdown(now + 2 * 86400 * 1000 + 3 * 3600 * 1000, now), "2d 3h");
  assert.equal(formatCountdown(now + 125 * 1000, now), "2m 5s");
});

test("normalizeLegacyFile normalizes old RTDB file entries", () => {
  const item = normalizeLegacyFile("abc", {
    name: "deck.pdf",
    size: 2048,
    type: "application/pdf",
    url: "https://trystocare.com/xyz",
    uploadedBy: "Minh",
    x: 240,
    y: 180,
  });

  assert.equal(item.id, "abc");
  assert.equal(item.kind, "file");
  assert.equal(item.sourceNode, "files");
  assert.equal(item.mimeType, "application/pdf");
  assert.equal(item.x, 240);
  assert.equal(item.y, 180);
});

test("detectPasteContent distinguishes URLs, code snippets, and plain notes", () => {
  const link = detectPasteContent("https://hefimer.qzz.io/space");
  assert.equal(link.kind, "link");
  assert.equal(link.name, "hefimer.qzz.io");

  const code = detectPasteContent("const greet = (name: string) => {\n  return name;\n};");
  assert.equal(code.kind, "code");

  const note = detectPasteContent("Remember to export dark mode assets");
  assert.equal(note.kind, "note");
});

test("getMediaPreviewUrl enables inline preview only for safe media on supported hosts", () => {
  assert.equal(
    getMediaPreviewUrl("https://trystocare.com/abc123", "image/png"),
    "/api/download?url=https%3A%2F%2Ftrystocare.com%2Fabc123&filename=preview&inline=1",
  );
  assert.equal(getMediaPreviewUrl("https://trystocare.com/abc123", "image/svg+xml"), null);
  assert.equal(getMediaPreviewUrl("https://storage.to/abc123", "image/png"), null);
  assert.equal(getMediaPreviewUrl("https://gofile.io/d/abc123", "image/png"), null);
  assert.equal(getMediaPreviewUrl("https://trystocare.com/abc123", "application/zip"), null);
});
