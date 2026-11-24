import type React from "react";

export type SpaceItemKind = "file" | "note" | "link" | "code";

export type NoteColor = "zinc" | "emerald" | "amber" | "sky" | "rose" | "violet";

export interface SpaceItem {
  id: string;
  kind: SpaceItemKind;
  name: string;
  x: number;
  y: number;
  zIndex?: number;
  uploadedBy: string;
  timestamp: number;
  sourceNode: "items" | "files";
  url?: string;
  size?: number;
  mimeType?: string;
  providerName?: string;
  content?: string;
  noteColor?: NoteColor;
  language?: string;
}

export interface SpaceMember {
  id: string;
  name: string;
  color: string;
  joinedAt: number;
}

export interface SpaceCursor {
  id: string;
  name: string;
  color: string;
  x: number;
  y: number;
  updatedAt: number;
}

export interface SpaceUploadTask {
  id: string;
  fileName: string;
  progress: number;
  status: string;
  providerName: string;
}

export interface SpaceState {
  spaceId: string;
  spaceName: string;
  spaceNameInput: string;
  spaceExpire: string;
  userName: string;
  inSpace: boolean;
  showCreatedModal: boolean;
  spaceMode: "create" | "join";
}

export interface SpaceProps {
  showToast: (msg: string, type?: "success" | "error" | "info") => void;
  addToHistory?: (item: any) => void;
  state: SpaceState;
  setState: React.Dispatch<React.SetStateAction<SpaceState>>;
  isAdmin?: boolean;
  isFrozen?: boolean;
}

export interface RecentSpaceEntry {
  code: string;
  name: string;
  visitedAt: number;
}

const USER_COLORS = [
  "#34d399", // emerald
  "#38bdf8", // sky
  "#fbbf24", // amber
  "#f472b6", // pink
  "#a78bfa", // violet
  "#fb7185", // rose
  "#2dd4bf", // teal
  "#facc15", // yellow
];

export const NOTE_COLOR_STYLES: Record<
  NoteColor,
  { bg: string; border: string; dot: string; label: string }
> = {
  zinc: {
    bg: "bg-zinc-900/90",
    border: "border-white/15",
    dot: "#a1a1aa",
    label: "Monochrome",
  },
  emerald: {
    bg: "bg-emerald-950/75",
    border: "border-emerald-400/30",
    dot: "#34d399",
    label: "Emerald",
  },
  amber: {
    bg: "bg-amber-950/75",
    border: "border-amber-400/30",
    dot: "#fbbf24",
    label: "Amber",
  },
  sky: {
    bg: "bg-sky-950/75",
    border: "border-sky-400/30",
    dot: "#38bdf8",
    label: "Sky",
  },
  rose: {
    bg: "bg-rose-950/75",
    border: "border-rose-400/30",
    dot: "#fb7185",
    label: "Rose",
  },
  violet: {
    bg: "bg-violet-950/75",
    border: "border-violet-400/30",
    dot: "#a78bfa",
    label: "Violet",
  },
};

export function getUserColor(seed: string): string {
  const value = (seed || "anon").trim();
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) >>> 0;
  }
  return USER_COLORS[hash % USER_COLORS.length];
}

export function screenToCanvas(
  clientX: number,
  clientY: number,
  rect: { left: number; top: number },
  pan: { x: number; y: number },
  zoom: number,
): { x: number; y: number } {
  const safeZoom = zoom > 0 ? zoom : 1;
  return {
    x: (clientX - rect.left - pan.x) / safeZoom,
    y: (clientY - rect.top - pan.y) / safeZoom,
  };
}

export function snapCoord(value: number, enabled: boolean, gridSize = 20): number {
  if (!enabled || gridSize <= 0) return Math.round(value);
  return Math.round(value / gridSize) * gridSize;
}

export function computeFitView(
  items: Array<{ x: number; y: number }>,
  viewportWidth: number,
  viewportHeight: number,
): { pan: { x: number; y: number }; zoom: number } {
  if (!items || items.length === 0 || viewportWidth <= 0 || viewportHeight <= 0) {
    return { pan: { x: 0, y: 0 }, zoom: 1 };
  }

  const cardWidth = 260;
  const cardHeight = 160;
  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;

  for (const item of items) {
    const x = Number.isFinite(item.x) ? item.x : 0;
    const y = Number.isFinite(item.y) ? item.y : 0;
    if (x < minX) minX = x;
    if (y < minY) minY = y;
    if (x + cardWidth > maxX) maxX = x + cardWidth;
    if (y + cardHeight > maxY) maxY = y + cardHeight;
  }

  const padding = 96;
  const boundsW = Math.max(cardWidth, maxX - minX);
  const boundsH = Math.max(cardHeight, maxY - minY);
  const availW = Math.max(120, viewportWidth - padding * 2);
  const availH = Math.max(120, viewportHeight - padding * 2);

  const rawZoom = Math.min(availW / boundsW, availH / boundsH);
  const zoom = Math.min(1.2, Math.max(0.35, rawZoom));

  const centerX = (minX + maxX) / 2;
  const centerY = (minY + maxY) / 2;

  return {
    zoom,
    pan: {
      x: Math.round(viewportWidth / 2 - centerX * zoom),
      y: Math.round(viewportHeight / 2 - centerY * zoom),
    },
  };
}

const KIND_ORDER: Record<SpaceItemKind, number> = {
  file: 0,
  note: 1,
  link: 2,
  code: 3,
};

export function estimateCardHeight(item: SpaceItem): number {
  if (item.kind === "file") {
    const isImg =
      (item.mimeType || "").toLowerCase().startsWith("image/") &&
      Boolean(getMediaPreviewUrl(item.url, item.mimeType));
    return isImg ? 276 : 176;
  }
  if (item.kind === "code") return 240;
  if (item.kind === "note") return 210;
  return 176;
}

export function computeAutoArrangeLayout(
  items: SpaceItem[],
): Array<{ id: string; sourceNode: "items" | "files"; x: number; y: number }> {
  const sorted = [...items].sort((a, b) => {
    const kindDiff = (KIND_ORDER[a.kind] ?? 9) - (KIND_ORDER[b.kind] ?? 9);
    if (kindDiff !== 0) return kindDiff;
    return (a.timestamp || 0) - (b.timestamp || 0);
  });

  const cols = Math.min(4, Math.max(1, Math.ceil(Math.sqrt(sorted.length))));
  const colStep = 296;
  const rowGap = 36;
  const startX = 72;
  const startY = 104;

  const result: Array<{ id: string; sourceNode: "items" | "files"; x: number; y: number }> = [];
  let currentY = startY;

  for (let rowStart = 0; rowStart < sorted.length; rowStart += cols) {
    const rowItems = sorted.slice(rowStart, rowStart + cols);
    let rowMaxHeight = 0;

    rowItems.forEach((item, colIdx) => {
      result.push({
        id: item.id,
        sourceNode: item.sourceNode,
        x: startX + colIdx * colStep,
        y: currentY,
      });
      rowMaxHeight = Math.max(rowMaxHeight, estimateCardHeight(item));
    });

    currentY += rowMaxHeight + rowGap;
  }

  return result;
}

export function computeSmartAlignment(
  dragX: number,
  dragY: number,
  dragId: string,
  items: SpaceItem[],
  threshold = 8,
): { x: number; y: number; guideX: number | null; guideY: number | null } {
  let snappedX = dragX;
  let snappedY = dragY;
  let guideX: number | null = null;
  let guideY: number | null = null;
  let bestDx = threshold + 1;
  let bestDy = threshold + 1;

  for (const other of items) {
    if (other.id === dragId) continue;
    const dx = Math.abs(dragX - other.x);
    if (dx <= threshold && dx < bestDx) {
      bestDx = dx;
      snappedX = other.x;
      guideX = other.x;
    }
    const dy = Math.abs(dragY - other.y);
    if (dy <= threshold && dy < bestDy) {
      bestDy = dy;
      snappedY = other.y;
      guideY = other.y;
    }
  }

  return { x: snappedX, y: snappedY, guideX, guideY };
}

export function parseSpaceDuration(expireStr?: string): number {
  const match = expireStr?.trim().match(/^(\d+)([mh])$/i);
  if (!match) return 60 * 60 * 1000;
  const val = parseInt(match[1], 10);
  const unit = match[2].toLowerCase();
  return unit === "m" ? val * 60 * 1000 : val * 60 * 60 * 1000;
}

export function formatCountdown(expiresAt: number, now = Date.now()): string {
  const diff = expiresAt - now;
  if (!Number.isFinite(diff) || diff <= 0) return "Expired";

  const totalSec = Math.floor(diff / 1000);
  const days = Math.floor(totalSec / 86400);
  const hours = Math.floor((totalSec % 86400) / 3600);
  const minutes = Math.floor((totalSec % 3600) / 60);
  const seconds = totalSec % 60;

  if (days > 0) return `${days}d ${hours}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m ${seconds}s`;
}

export function formatBytes(bytes?: number): string {
  if (!bytes || !Number.isFinite(bytes) || bytes <= 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.min(sizes.length - 1, Math.floor(Math.log(bytes) / Math.log(k)));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}

export function normalizeLegacyFile(id: string, raw: any): SpaceItem {
  return {
    id,
    kind: "file",
    name: typeof raw?.name === "string" && raw.name ? raw.name : "Untitled file",
    x: Number.isFinite(Number(raw?.x)) ? Number(raw.x) : 100,
    y: Number.isFinite(Number(raw?.y)) ? Number(raw.y) : 100,
    zIndex: Number.isFinite(Number(raw?.zIndex)) ? Number(raw.zIndex) : 1,
    uploadedBy: typeof raw?.uploadedBy === "string" && raw.uploadedBy ? raw.uploadedBy : "Anonymous",
    timestamp: Number.isFinite(Number(raw?.timestamp)) ? Number(raw.timestamp) : 0,
    sourceNode: "files",
    url: typeof raw?.url === "string" ? raw.url : "",
    size: Number.isFinite(Number(raw?.size)) ? Number(raw.size) : 0,
    mimeType: typeof raw?.type === "string" ? raw.type : typeof raw?.mimeType === "string" ? raw.mimeType : "application/octet-stream",
    providerName: typeof raw?.providerName === "string" ? raw.providerName : undefined,
  };
}

export function normalizeSpaceItem(id: string, raw: any): SpaceItem {
  const kind: SpaceItemKind =
    raw?.kind === "note" || raw?.kind === "link" || raw?.kind === "code" || raw?.kind === "file"
      ? raw.kind
      : "file";
  const noteColor: NoteColor =
    raw?.noteColor && raw.noteColor in NOTE_COLOR_STYLES ? raw.noteColor : "zinc";

  return {
    id,
    kind,
    name: typeof raw?.name === "string" && raw.name ? raw.name : kind === "note" ? "Sticky Note" : "Untitled",
    x: Number.isFinite(Number(raw?.x)) ? Number(raw.x) : 100,
    y: Number.isFinite(Number(raw?.y)) ? Number(raw.y) : 100,
    zIndex: Number.isFinite(Number(raw?.zIndex)) ? Number(raw.zIndex) : 1,
    uploadedBy: typeof raw?.uploadedBy === "string" && raw.uploadedBy ? raw.uploadedBy : "Anonymous",
    timestamp: Number.isFinite(Number(raw?.timestamp)) ? Number(raw.timestamp) : 0,
    sourceNode: "items",
    url: typeof raw?.url === "string" ? raw.url : undefined,
    size: Number.isFinite(Number(raw?.size)) ? Number(raw.size) : undefined,
    mimeType: typeof raw?.mimeType === "string" ? raw.mimeType : undefined,
    providerName: typeof raw?.providerName === "string" ? raw.providerName : undefined,
    content: typeof raw?.content === "string" ? raw.content : undefined,
    noteColor,
    language: typeof raw?.language === "string" ? raw.language : undefined,
  };
}

export function detectPasteContent(text: string): {
  kind: "link" | "code" | "note";
  name: string;
  url?: string;
  content?: string;
} {
  const trimmed = (text || "").trim();
  if (!trimmed.includes("\n") && /^https?:\/\/\S+$/i.test(trimmed)) {
    let host = "Link";
    try {
      host = new URL(trimmed).hostname.replace(/^www\./, "");
    } catch {
      host = "Link";
    }
    return {
      kind: "link",
      name: host,
      url: trimmed,
    };
  }

  const hasNewline = trimmed.includes("\n");
  const looksLikeCode =
    hasNewline &&
    /(\{|\}|=>|\bfunction\s+|\bimport\s+|\bconst\s+|\blet\s+|\bdef\s+|\bclass\s+|<\w+[^>]*>)/.test(trimmed);

  if (looksLikeCode) {
    const firstLine = trimmed.split("\n")[0].trim().slice(0, 42);
    return {
      kind: "code",
      name: firstLine || "Code Snippet",
      content: trimmed,
    };
  }

  return {
    kind: "note",
    name: "Quick Note",
    content: trimmed,
  };
}

const SKIP_INLINE_PREVIEW_HOSTS = new Set([
  "gofile.io",
  "vikingfile.com",
  "storage.to",
]);

export function getMediaPreviewUrl(url?: string, mimeType?: string): string | null {
  if (!url) return null;
  const normalizedMime = (mimeType || "").toLowerCase();
  const isPreviewableMime =
    normalizedMime.startsWith("image/") ||
    normalizedMime.startsWith("video/") ||
    normalizedMime.startsWith("audio/");

  if (!isPreviewableMime || normalizedMime.includes("svg")) {
    return null;
  }

  try {
    const parsed = new URL(url);
    if (parsed.protocol !== "https:") return null;
    const host = parsed.hostname.toLowerCase();
    if (SKIP_INLINE_PREVIEW_HOSTS.has(host)) return null;
    return `/api/download?url=${encodeURIComponent(url)}&filename=preview&inline=1`;
  } catch {
    return null;
  }
}

export function stripUndefined<T extends Record<string, any>>(obj: T): T {
  const clean: Record<string, any> = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value !== undefined) {
      clean[key] = value;
    }
  }
  return clean as T;
}
