import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "motion/react";
import {
  Archive,
  Box,
  Check,
  Clock,
  Code2,
  Copy,
  Download,
  Edit3,
  ExternalLink,
  Eye,
  File as FileIcon,
  FileUp,
  Grid,
  Image as ImageIcon,
  LayoutGrid,
  Link2,
  Loader2,
  LogOut,
  Map as MapIcon,
  Maximize2,
  Minimize2,
  Minus,
  Music,
  PanelRight,
  Plus,
  QrCode,
  Search,
  StickyNote,
  Trash2,
  Users,
  Video,
  X,
} from "lucide-react";
import { getDirectDownloadMode } from "../downloads";
import { getProviderConfig } from "../providers";
import {
  NOTE_COLOR_STYLES,
  computeFitView,
  computeSmartAlignment,
  detectPasteContent,
  formatBytes,
  formatCountdown,
  getMediaPreviewUrl,
  screenToCanvas,
  snapCoord,
  type SpaceCursor,
  type SpaceItem,
  type SpaceItemKind,
  type SpaceMember,
  type SpaceUploadTask,
} from "./types";

interface SpaceCanvasProps {
  spaceId: string;
  spaceName: string;
  expiresAt: number;
  items: SpaceItem[];
  members: SpaceMember[];
  cursors: SpaceCursor[];
  clientId: string;
  selectedProviderId: string;
  uploadTasks: SpaceUploadTask[];
  onLeaveSpace: () => void;
  onOpenShare: () => void;
  onOpenProviderPicker: () => void;
  onOpenComposer: (mode: "note" | "link" | "code", pos?: { x: number; y: number }, editing?: SpaceItem) => void;
  onOpenPreview: (item: SpaceItem) => void;
  onOpenRename: (item: SpaceItem) => void;
  onOpenDelete: (item: SpaceItem) => void;
  onUploadFiles: (files: File[], startPos: { x: number; y: number }) => void;
  onCancelUpload: () => void;
  onMoveItem: (item: SpaceItem, x: number, y: number, zIndex?: number, final?: boolean) => void;
  onCreatePastedItem: (payload: {
    kind: "note" | "link" | "code";
    name: string;
    content?: string;
    url?: string;
    x: number;
    y: number;
  }) => void;
  onAutoArrange: () => void;
  onDownloadItem: (item: SpaceItem) => void;
  onDownloadAllFiles: () => void;
  onCursorMove: (x: number, y: number) => void;
  showToast: (msg: string, type?: "success" | "error" | "info") => void;
}

const APPLE_GLASS =
  "bg-[#121214]/80 backdrop-blur-2xl backdrop-saturate-150 border border-white/[0.11] shadow-[inset_0_1px_0_rgba(255,255,255,0.12),0_12px_36px_rgba(0,0,0,0.5)]";

export function SpaceCanvas({
  spaceId,
  spaceName,
  expiresAt,
  items,
  members,
  cursors,
  clientId,
  selectedProviderId,
  uploadTasks,
  onLeaveSpace,
  onOpenShare,
  onOpenProviderPicker,
  onOpenComposer,
  onOpenPreview,
  onOpenRename,
  onOpenDelete,
  onUploadFiles,
  onCancelUpload,
  onMoveItem,
  onCreatePastedItem,
  onAutoArrange,
  onDownloadItem,
  onDownloadAllFiles,
  onCursorMove,
  showToast,
}: SpaceCanvasProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [snapToGrid, setSnapToGrid] = useState(true);
  const [showMinimap, setShowMinimap] = useState(true);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showMembersPopover, setShowMembersPopover] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);
  const [drawerQuery, setDrawerQuery] = useState("");
  const [drawerFilter, setDrawerFilter] = useState<"all" | SpaceItemKind>("all");
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [copiedCodeBadge, setCopiedCodeBadge] = useState(false);
  const [nowTick, setNowTick] = useState(Date.now());
  const [brokenThumbs, setBrokenThumbs] = useState<Record<string, boolean>>({});
  const [isPanning, setIsPanning] = useState(false);

  // Optimistic card drag + magnetic alignment guides
  const [activeDrag, setActiveDrag] = useState<{
    id: string;
    x: number;
    y: number;
    guideX: number | null;
    guideY: number | null;
  } | null>(null);

  // Latest camera & item refs for RAF loops and native event listeners
  const panRef = useRef(pan);
  const zoomRef = useRef(zoom);
  const itemsRef = useRef(items);
  panRef.current = pan;
  zoomRef.current = zoom;
  itemsRef.current = items;

  const cameraAnimRef = useRef<number | null>(null);
  const inertiaRafRef = useRef<number | null>(null);

  // Unified multi-pointer gesture state
  const activePointersRef = useRef<Map<number, { x: number; y: number }>>(new Map());
  const gestureRef = useRef<{
    mode: "none" | "pan" | "pinch";
    panAnchorPointerId: number | null;
    startX: number;
    startY: number;
    initialPanX: number;
    initialPanY: number;
    passedThreshold: boolean;
    lastMoveTime: number;
    lastX: number;
    lastY: number;
    vx: number;
    vy: number;
    pinchDist: number;
    pinchZoom: number;
    pinchMidX: number;
    pinchMidY: number;
    pinchPanX: number;
    pinchPanY: number;
  }>({
    mode: "none",
    panAnchorPointerId: null,
    startX: 0,
    startY: 0,
    initialPanX: 0,
    initialPanY: 0,
    passedThreshold: false,
    lastMoveTime: 0,
    lastX: 0,
    lastY: 0,
    vx: 0,
    vy: 0,
    pinchDist: 1,
    pinchZoom: 1,
    pinchMidX: 0,
    pinchMidY: 0,
    pinchPanX: 0,
    pinchPanY: 0,
  });

  const providerConfig = getProviderConfig(selectedProviderId);

  const stopCameraMotion = useCallback(() => {
    if (cameraAnimRef.current !== null) {
      cancelAnimationFrame(cameraAnimRef.current);
      cameraAnimRef.current = null;
    }
    if (inertiaRafRef.current !== null) {
      cancelAnimationFrame(inertiaRafRef.current);
      inertiaRafRef.current = null;
    }
  }, []);

  useEffect(() => stopCameraMotion, [stopCameraMotion]);

  useEffect(() => {
    const timer = setInterval(() => setNowTick(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const animateCameraTo = useCallback(
    (targetPan: { x: number; y: number }, targetZoom: number, durationMs = 280) => {
      stopCameraMotion();
      const startPan = { ...panRef.current };
      const startZoom = zoomRef.current;
      const startTime = performance.now();

      const step = (now: number) => {
        const t = Math.min(1, (now - startTime) / durationMs);
        const eased = 1 - Math.pow(1 - t, 3);
        const nextZoom = startZoom + (targetZoom - startZoom) * eased;
        const nextPan = {
          x: startPan.x + (targetPan.x - startPan.x) * eased,
          y: startPan.y + (targetPan.y - startPan.y) * eased,
        };
        zoomRef.current = nextZoom;
        panRef.current = nextPan;
        setZoom(nextZoom);
        setPan(nextPan);
        if (t < 1) {
          cameraAnimRef.current = requestAnimationFrame(step);
        } else {
          cameraAnimRef.current = null;
        }
      };

      cameraAnimRef.current = requestAnimationFrame(step);
    },
    [stopCameraMotion],
  );

  const startInertia = useCallback(
    (initialVx: number, initialVy: number) => {
      stopCameraMotion();
      let vx = initialVx;
      let vy = initialVy;
      let lastTime = performance.now();

      const step = (now: number) => {
        const dt = Math.min(32, Math.max(1, now - lastTime));
        lastTime = now;

        const decay = Math.pow(0.92, dt / 16.67);
        vx *= decay;
        vy *= decay;

        if (Math.hypot(vx, vy) < 0.02) {
          inertiaRafRef.current = null;
          return;
        }

        const nextPan = {
          x: panRef.current.x + vx * dt,
          y: panRef.current.y + vy * dt,
        };
        panRef.current = nextPan;
        setPan(nextPan);
        inertiaRafRef.current = requestAnimationFrame(step);
      };

      inertiaRafRef.current = requestAnimationFrame(step);
    },
    [stopCameraMotion],
  );

  const maxZIndex = useMemo(() => {
    let max = 1;
    for (const item of items) {
      if ((item.zIndex || 1) > max) max = item.zIndex || 1;
    }
    return max;
  }, [items]);

  const getViewportCenter = useCallback(() => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return { x: 240, y: 180 };
    return screenToCanvas(
      rect.left + rect.width / 2,
      rect.top + rect.height / 2,
      rect,
      panRef.current,
      zoomRef.current,
    );
  }, []);

  const handleFitView = useCallback(() => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    const next = computeFitView(items, rect.width, rect.height);
    animateCameraTo(next.pan, next.zoom);
  }, [items, animateCameraTo]);

  const handleResetView = useCallback(() => {
    animateCameraTo({ x: 0, y: 0 }, 1);
  }, [animateCameraTo]);

  const handleZoomBy = useCallback(
    (factor: number) => {
      const rect = containerRef.current?.getBoundingClientRect();
      const prevZoom = zoomRef.current;
      const nextZoom = Math.min(2.0, Math.max(0.25, Number((prevZoom * factor).toFixed(3))));
      if (!rect) {
        animateCameraTo(panRef.current, nextZoom, 200);
        return;
      }
      const cx = rect.width / 2;
      const cy = rect.height / 2;
      const worldX = (cx - panRef.current.x) / prevZoom;
      const worldY = (cy - panRef.current.y) / prevZoom;
      animateCameraTo(
        {
          x: cx - worldX * nextZoom,
          y: cy - worldY * nextZoom,
        },
        nextZoom,
        200,
      );
    },
    [animateCameraTo],
  );

  // Smart trackpad & mouse wheel handler (supports pinch-zoom, 2-finger trackpad pan, & wheel zoom)
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      stopCameraMotion();
      const rect = el.getBoundingClientRect();
      const prevZoom = zoomRef.current;

      // Trackpad pinch (ctrlKey/metaKey) or standard mouse vertical wheel -> continuous exponential zoom
      if (e.ctrlKey || e.metaKey || Math.abs(e.deltaX) === 0) {
        const clampedDelta = Math.max(-120, Math.min(120, e.deltaY));
        const sensitivity = e.ctrlKey || e.metaKey ? 0.008 : 0.0022;
        const factor = Math.exp(-clampedDelta * sensitivity);
        const nextZoom = Math.min(2.0, Math.max(0.25, Number((prevZoom * factor).toFixed(4))));
        if (nextZoom === prevZoom) return;

        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;
        const worldX = (mouseX - panRef.current.x) / prevZoom;
        const worldY = (mouseY - panRef.current.y) / prevZoom;
        const nextPan = {
          x: mouseX - worldX * nextZoom,
          y: mouseY - worldY * nextZoom,
        };
        zoomRef.current = nextZoom;
        panRef.current = nextPan;
        setZoom(nextZoom);
        setPan(nextPan);
        return;
      }

      // Two-finger trackpad 2D pan
      const nextPan = {
        x: panRef.current.x - e.deltaX,
        y: panRef.current.y - e.deltaY,
      };
      panRef.current = nextPan;
      setPan(nextPan);
    };
    el.addEventListener("wheel", onWheel, { passive: false });
    return () => el.removeEventListener("wheel", onWheel);
  }, [stopCameraMotion, isFullscreen]);

  // Keyboard shortcuts
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable)
      ) {
        return;
      }

      if (e.key === "=" || e.key === "+") {
        e.preventDefault();
        handleZoomBy(1.15);
      } else if (e.key === "-" || e.key === "_") {
        e.preventDefault();
        handleZoomBy(0.87);
      } else if (e.key === "0") {
        e.preventDefault();
        handleResetView();
      } else if (e.key.toLowerCase() === "f" && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        handleFitView();
      } else if (e.key.toLowerCase() === "g" && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        setSnapToGrid((v) => !v);
      } else if ((e.key === "Delete" || e.key === "Backspace") && selectedItemId) {
        const found = items.find((i) => i.id === selectedItemId);
        if (found) {
          e.preventDefault();
          onOpenDelete(found);
        }
      } else if (e.key === "Escape") {
        setSelectedItemId(null);
        setShowMembersPopover(false);
        if (isFullscreen) setIsFullscreen(false);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [handleZoomBy, handleResetView, handleFitView, selectedItemId, items, onOpenDelete, isFullscreen]);

  // Clipboard paste (images / files / links / code / notes)
  useEffect(() => {
    const onPaste = (e: ClipboardEvent) => {
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable)
      ) {
        return;
      }

      const center = getViewportCenter();
      const pastedFiles = Array.from(e.clipboardData?.files || []);
      if (pastedFiles.length > 0) {
        e.preventDefault();
        onUploadFiles(pastedFiles, {
          x: snapCoord(center.x - 120, snapToGrid),
          y: snapCoord(center.y - 60, snapToGrid),
        });
        return;
      }

      const text = e.clipboardData?.getData("text/plain") || "";
      if (text.trim()) {
        e.preventDefault();
        const detected = detectPasteContent(text);
        onCreatePastedItem({
          ...detected,
          x: snapCoord(center.x - 120, snapToGrid),
          y: snapCoord(center.y - 60, snapToGrid),
        });
      }
    };
    window.addEventListener("paste", onPaste);
    return () => window.removeEventListener("paste", onPaste);
  }, [getViewportCenter, onUploadFiles, onCreatePastedItem, snapToGrid]);

  // Unified multi-pointer pan + pinch-to-zoom + momentum inertia
  const handleCanvasPointerDown = (e: React.PointerEvent) => {
    if ((e.target as HTMLElement).closest(".space-hud, .space-card")) return;
    const el = containerRef.current;
    if (!el) return;
    if (e.pointerType === "mouse" && e.button !== 0) return;

    stopCameraMotion();
    setSelectedItemId(null);
    setShowMembersPopover(false);

    try {
      el.setPointerCapture(e.pointerId);
    } catch {
      // ignore if capture rejected
    }

    activePointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    const g = gestureRef.current;

    if (activePointersRef.current.size === 2) {
      const pts = Array.from(activePointersRef.current.values());
      const dist = Math.hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y);
      g.mode = "pinch";
      g.pinchDist = Math.max(1, dist);
      g.pinchZoom = zoomRef.current;
      g.pinchMidX = (pts[0].x + pts[1].x) / 2;
      g.pinchMidY = (pts[0].y + pts[1].y) / 2;
      g.pinchPanX = panRef.current.x;
      g.pinchPanY = panRef.current.y;
      g.vx = 0;
      g.vy = 0;
      setIsPanning(true);
      return;
    }

    if (activePointersRef.current.size === 1) {
      const now = performance.now();
      g.mode = "pan";
      g.panAnchorPointerId = e.pointerId;
      g.startX = e.clientX;
      g.startY = e.clientY;
      g.initialPanX = panRef.current.x;
      g.initialPanY = panRef.current.y;
      g.passedThreshold = false;
      g.lastMoveTime = now;
      g.lastX = e.clientX;
      g.lastY = e.clientY;
      g.vx = 0;
      g.vy = 0;
    }
  };

  const handleCanvasPointerMove = (e: React.PointerEvent) => {
    const el = containerRef.current;
    const rect = el?.getBoundingClientRect();
    if (rect) {
      const pos = screenToCanvas(e.clientX, e.clientY, rect, panRef.current, zoomRef.current);
      onCursorMove(Math.round(pos.x), Math.round(pos.y));
    }

    if (!activePointersRef.current.has(e.pointerId)) return;
    activePointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    const g = gestureRef.current;

    if (g.mode === "pinch" && activePointersRef.current.size >= 2 && rect) {
      const pts = Array.from(activePointersRef.current.values());
      const dist = Math.hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y);
      const midX = (pts[0].x + pts[1].x) / 2;
      const midY = (pts[0].y + pts[1].y) / 2;
      const scale = dist / g.pinchDist;
      const nextZoom = Math.min(2.0, Math.max(0.25, g.pinchZoom * scale));

      const worldX = (g.pinchMidX - rect.left - g.pinchPanX) / g.pinchZoom;
      const worldY = (g.pinchMidY - rect.top - g.pinchPanY) / g.pinchZoom;
      const nextPan = {
        x: midX - rect.left - worldX * nextZoom,
        y: midY - rect.top - worldY * nextZoom,
      };
      zoomRef.current = nextZoom;
      panRef.current = nextPan;
      setZoom(nextZoom);
      setPan(nextPan);
      return;
    }

    if (g.mode === "pan" && g.panAnchorPointerId === e.pointerId) {
      const dx = e.clientX - g.startX;
      const dy = e.clientY - g.startY;
      if (!g.passedThreshold && Math.hypot(dx, dy) < 5) return;
      if (!g.passedThreshold) {
        g.passedThreshold = true;
        setIsPanning(true);
      }

      const now = performance.now();
      const dt = Math.max(1, now - g.lastMoveTime);
      const instVx = (e.clientX - g.lastX) / dt;
      const instVy = (e.clientY - g.lastY) / dt;
      g.vx = g.vx * 0.65 + instVx * 0.35;
      g.vy = g.vy * 0.65 + instVy * 0.35;
      g.lastMoveTime = now;
      g.lastX = e.clientX;
      g.lastY = e.clientY;

      const nextPan = { x: g.initialPanX + dx, y: g.initialPanY + dy };
      panRef.current = nextPan;
      setPan(nextPan);
    }
  };

  const handleCanvasPointerUpOrCancel = (e: React.PointerEvent) => {
    const el = containerRef.current;
    if (el) {
      try {
        el.releasePointerCapture(e.pointerId);
      } catch {
        // ignore
      }
    }

    if (!activePointersRef.current.has(e.pointerId)) return;
    activePointersRef.current.delete(e.pointerId);
    const g = gestureRef.current;

    if (activePointersRef.current.size === 1) {
      // Seamlessly transition back from 2-finger pinch to 1-finger pan without jump
      const [remainingId, pos] = Array.from(activePointersRef.current.entries())[0];
      g.mode = "pan";
      g.panAnchorPointerId = remainingId;
      g.startX = pos.x;
      g.startY = pos.y;
      g.initialPanX = panRef.current.x;
      g.initialPanY = panRef.current.y;
      g.passedThreshold = true;
      g.lastMoveTime = performance.now();
      g.lastX = pos.x;
      g.lastY = pos.y;
      g.vx = 0;
      g.vy = 0;
      return;
    }

    if (activePointersRef.current.size === 0) {
      const wasPanning = g.mode === "pan" && g.passedThreshold;
      const staleMs = performance.now() - g.lastMoveTime;
      const releaseVx = g.vx;
      const releaseVy = g.vy;
      g.mode = "none";
      g.panAnchorPointerId = null;
      g.passedThreshold = false;
      setIsPanning(false);

      if (wasPanning && staleMs < 80 && Math.hypot(releaseVx, releaseVy) > 0.08) {
        startInertia(
          Math.max(-2.5, Math.min(2.5, releaseVx)),
          Math.max(-2.5, Math.min(2.5, releaseVy)),
        );
      }
    }
  };

  const handleCanvasDoubleClick = (e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest(".space-hud, .space-card")) return;
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    const pos = screenToCanvas(e.clientX, e.clientY, rect, panRef.current, zoomRef.current);
    onOpenComposer("note", {
      x: snapCoord(pos.x, snapToGrid),
      y: snapCoord(pos.y, snapToGrid),
    });
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const droppedFiles = Array.from(e.dataTransfer.files || []);
    if (droppedFiles.length === 0) return;
    const rect = containerRef.current?.getBoundingClientRect();
    const pos = rect
      ? screenToCanvas(e.clientX, e.clientY, rect, panRef.current, zoomRef.current)
      : { x: 160, y: 160 };
    onUploadFiles(droppedFiles, {
      x: snapCoord(pos.x, snapToGrid),
      y: snapCoord(pos.y, snapToGrid),
    });
  };

  // GPU-accelerated 120fps card drag with Apple physical lift + smart magnetic alignment guides
  const handleCardPointerDown = (e: React.PointerEvent, item: SpaceItem) => {
    if (e.button !== 0 && e.pointerType === "mouse") return;
    e.stopPropagation();
    stopCameraMotion();
    setSelectedItemId(item.id);
    const target = e.currentTarget as HTMLElement;
    try {
      target.setPointerCapture(e.pointerId);
    } catch {
      // ignore
    }

    const startX = e.clientX;
    const startY = e.clientY;
    const baseX = item.x;
    const baseY = item.y;
    const nextZ = maxZIndex + 1;
    let passedThreshold = false;
    let lastX = baseX;
    let lastY = baseY;
    let lastSync = 0;

    const onMove = (moveEvt: PointerEvent) => {
      if (!passedThreshold && Math.hypot(moveEvt.clientX - startX, moveEvt.clientY - startY) < 5) {
        return;
      }
      passedThreshold = true;
      const rawX = baseX + (moveEvt.clientX - startX) / zoomRef.current;
      const rawY = baseY + (moveEvt.clientY - startY) / zoomRef.current;
      const aligned = computeSmartAlignment(rawX, rawY, item.id, itemsRef.current, 8);
      lastX = aligned.x;
      lastY = aligned.y;

      setActiveDrag({
        id: item.id,
        x: aligned.x,
        y: aligned.y,
        guideX: aligned.guideX,
        guideY: aligned.guideY,
      });

      const now = Date.now();
      if (now - lastSync > 60) {
        onMoveItem(item, aligned.x, aligned.y, nextZ, false);
        lastSync = now;
      }
    };

    const onUp = (upEvt: PointerEvent) => {
      try {
        target.releasePointerCapture(upEvt.pointerId);
      } catch {
        // ignore
      }
      target.removeEventListener("pointermove", onMove);
      target.removeEventListener("pointerup", onUp);
      target.removeEventListener("pointercancel", onUp);
      setActiveDrag(null);
      if (passedThreshold) {
        const finalX = snapCoord(lastX, snapToGrid);
        const finalY = snapCoord(lastY, snapToGrid);
        onMoveItem(item, finalX, finalY, nextZ, true);
      } else {
        onMoveItem(item, baseX, baseY, nextZ, true);
      }
    };

    target.addEventListener("pointermove", onMove);
    target.addEventListener("pointerup", onUp);
    target.addEventListener("pointercancel", onUp);
  };

  const getFileIcon = (mimeType?: string) => {
    const mime = (mimeType || "").toLowerCase();
    if (mime.startsWith("image/")) return <ImageIcon size={18} className="text-sky-400" />;
    if (mime.startsWith("video/")) return <Video size={18} className="text-purple-400" />;
    if (mime.startsWith("audio/")) return <Music size={18} className="text-pink-400" />;
    if (mime.includes("zip") || mime.includes("tar") || mime.includes("rar")) {
      return <Archive size={18} className="text-amber-400" />;
    }
    return <FileIcon size={18} className="text-white/65" />;
  };

  const filteredDrawerItems = useMemo(() => {
    const q = drawerQuery.trim().toLowerCase();
    return items
      .filter((item) => {
        if (drawerFilter !== "all" && item.kind !== drawerFilter) return false;
        if (!q) return true;
        return (
          item.name.toLowerCase().includes(q) ||
          item.uploadedBy.toLowerCase().includes(q) ||
          (item.content || "").toLowerCase().includes(q) ||
          (item.url || "").toLowerCase().includes(q)
        );
      })
      .sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
  }, [items, drawerFilter, drawerQuery]);

  const focusItemOnCanvas = (item: SpaceItem) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    const targetZoom = Math.max(zoomRef.current, 0.92);
    animateCameraTo(
      {
        x: Math.round(rect.width / 2 - (item.x + 125) * targetZoom),
        y: Math.round(rect.height / 2 - (item.y + 80) * targetZoom),
      },
      targetZoom,
      280,
    );
    setSelectedItemId(item.id);
    setShowDrawer(false);
  };

  const activeCursors = useMemo(
    () =>
      cursors.filter(
        (c) => c.id !== clientId && nowTick - (c.updatedAt || 0) < 15_000,
      ),
    [cursors, clientId, nowTick],
  );

  const countdownLabel = expiresAt > 0 ? formatCountdown(expiresAt, nowTick) : "";
  const isExpiringSoon = expiresAt > 0 && expiresAt - nowTick < 10 * 60 * 1000;
  const fileCount = items.filter((i) => i.kind === "file" && i.url).length;

  const canvasViewport = (
    <div
      className={
        isFullscreen
          ? "fixed inset-0 z-[400] w-screen h-dvh bg-[#050505] flex flex-col overflow-hidden"
          : "w-full h-[calc(100dvh-108px)] min-h-[560px] border border-white/[0.11] rounded-[28px] overflow-hidden relative flex flex-col bg-[#050505] shadow-[0_24px_80px_rgba(0,0,0,0.65)]"
      }
    >
      {/* Hidden Multi-File Input */}
      <input
        type="file"
        multiple
        ref={fileInputRef}
        onChange={(e) => {
          const picked = Array.from(e.target.files || []);
          if (picked.length > 0) {
            const center = getViewportCenter();
            onUploadFiles(picked, {
              x: snapCoord(center.x - 120, snapToGrid),
              y: snapCoord(center.y - 60, snapToGrid),
            });
          }
          e.target.value = "";
        }}
        className="hidden"
      />

      {/* Top Floating HUD Bar */}
      <div className="space-hud absolute top-0 left-0 right-0 z-30 p-3 sm:p-4 flex flex-wrap items-center justify-between gap-2 pointer-events-none">
        <div className="pointer-events-auto flex items-center gap-2 flex-wrap">
          <button
            type="button"
            onClick={onLeaveSpace}
            title="Leave Space"
            className={`h-10 px-3 rounded-2xl ${APPLE_GLASS} hover:border-white/25 hover:bg-white/10 text-white/80 hover:text-white flex items-center gap-1.5 text-xs font-bold transition-all active:scale-[0.96]`}
          >
            <LogOut size={14} />
            <span className="hidden sm:inline">Leave</span>
          </button>

          <div className={`h-10 px-3.5 rounded-2xl ${APPLE_GLASS} flex items-center gap-2.5`}>
            <h2 className="text-white font-bold text-sm truncate max-w-[130px] sm:max-w-[220px]">
              {spaceName || "Space"}
            </h2>
            <button
              type="button"
              onClick={() => {
                navigator.clipboard.writeText(spaceId);
                setCopiedCodeBadge(true);
                showToast("Space code copied!", "success");
                setTimeout(() => setCopiedCodeBadge(false), 1600);
              }}
              title="Copy 5-digit Space code"
              className="px-2 py-0.5 rounded-lg bg-white/10 hover:bg-white/20 text-[11px] font-mono font-bold text-emerald-300 flex items-center gap-1 transition-all active:scale-[0.95]"
            >
              {spaceId}
              {copiedCodeBadge ? <Check size={10} /> : <Copy size={10} />}
            </button>
          </div>

          {countdownLabel && (
            <div
              className={`h-10 px-3 rounded-2xl backdrop-blur-2xl backdrop-saturate-150 border flex items-center gap-1.5 text-[11px] font-mono font-bold shadow-[inset_0_1px_0_rgba(255,255,255,0.1)] ${
                isExpiringSoon
                  ? "bg-amber-500/15 border-amber-400/35 text-amber-300"
                  : "bg-[#121214]/80 border-white/[0.11] text-white/65"
              }`}
              title="Time until auto-delete"
            >
              <Clock size={13} />
              <span>{countdownLabel}</span>
            </div>
          )}

          {/* Members Presence Pill */}
          <div className="relative">
            <button
              type="button"
              onClick={() => setShowMembersPopover((v) => !v)}
              className={`h-10 px-3 rounded-2xl ${APPLE_GLASS} hover:border-white/25 text-white/85 flex items-center gap-1.5 text-xs font-bold transition-all active:scale-[0.96]`}
            >
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <Users size={13} />
              <span className="font-mono">{members.length}</span>
            </button>
            <AnimatePresence>
              {showMembersPopover && (
                <motion.div
                  initial={{ opacity: 0, y: 6, scale: 0.96 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 6, scale: 0.96 }}
                  transition={{ type: "spring", damping: 24, stiffness: 360 }}
                  className="absolute right-0 sm:left-0 top-12 w-56 rounded-2xl bg-[#111113]/95 backdrop-blur-2xl backdrop-saturate-150 border border-white/15 p-3 shadow-2xl space-y-2 z-50"
                >
                  <p className="text-[9px] font-black uppercase tracking-[0.18em] text-white/40">
                    Online Members ({members.length})
                  </p>
                  <div className="max-h-48 overflow-y-auto space-y-1.5 custom-scrollbar">
                    {members.map((m) => (
                      <div key={m.id} className="flex items-center gap-2 text-xs text-white/85">
                        <span
                          className="w-2.5 h-2.5 rounded-full shrink-0"
                          style={{ backgroundColor: m.color }}
                        />
                        <span className="truncate flex-1">{m.name}</span>
                        {m.id === clientId && (
                          <span className="text-[9px] font-mono text-white/35">(you)</span>
                        )}
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>

        <div className="pointer-events-auto flex items-center gap-2">
          <button
            type="button"
            onClick={onOpenShare}
            title="Share QR & Link"
            className={`h-10 px-3 rounded-2xl ${APPLE_GLASS} hover:border-white/25 hover:bg-white/10 text-white flex items-center gap-1.5 text-xs font-bold transition-all active:scale-[0.96]`}
          >
            <QrCode size={15} />
            <span className="hidden sm:inline">Share</span>
          </button>

          <button
            type="button"
            onClick={() => setShowDrawer((v) => !v)}
            title="Items & Search Drawer"
            className={`h-10 px-3 rounded-2xl backdrop-blur-2xl backdrop-saturate-150 border flex items-center gap-1.5 text-xs font-bold transition-all active:scale-[0.96] ${
              showDrawer
                ? "bg-white text-black border-white shadow-lg"
                : `${APPLE_GLASS} hover:border-white/25 text-white/85`
            }`}
          >
            <PanelRight size={15} />
            <span className="font-mono">{items.length}</span>
          </button>

          <button
            type="button"
            onClick={() => setIsFullscreen((v) => !v)}
            title={isFullscreen ? "Exit Fullscreen (Esc)" : "Fullscreen"}
            className={`h-10 w-10 rounded-2xl ${APPLE_GLASS} hover:border-white/25 hover:bg-white/10 text-white/85 flex items-center justify-center transition-all active:scale-[0.96]`}
          >
            {isFullscreen ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
          </button>
        </div>
      </div>

      {/* Interactive Spatial Canvas Surface */}
      <div
        ref={containerRef}
        className={`flex-1 w-full h-full relative overflow-hidden select-none ${
          isPanning ? "cursor-grabbing" : "cursor-grab"
        }`}
        style={{
          touchAction: "none",
          backgroundImage: "radial-gradient(rgba(255,255,255,0.15) 1px, transparent 1px)",
          backgroundSize: `${24 * zoom}px ${24 * zoom}px`,
          backgroundPosition: `${pan.x}px ${pan.y}px`,
        }}
        onPointerDown={handleCanvasPointerDown}
        onPointerMove={handleCanvasPointerMove}
        onPointerUp={handleCanvasPointerUpOrCancel}
        onPointerCancel={handleCanvasPointerUpOrCancel}
        onDoubleClick={handleCanvasDoubleClick}
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragOver(true);
        }}
        onDragLeave={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget as Node)) {
            setIsDragOver(false);
          }
        }}
        onDrop={handleDrop}
      >
        {/* Empty State Prompt */}
        {items.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none px-4">
            <div className={`text-center space-y-2 max-w-sm rounded-3xl px-6 py-5 ${APPLE_GLASS}`}>
              <p className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-400">
                Infinite Canvas Ready
              </p>
              <h3 className="text-sm font-bold text-white">
                Drop files, paste screenshots/text, or double-click anywhere
              </h3>
              <p className="text-[11px] text-white/45 leading-relaxed">
                Drag background to glide • Scroll or pinch to zoom • Use the bottom dock to add Notes, Links, or Code.
              </p>
            </div>
          </div>
        )}

        {/* Drag-and-Drop Overlay */}
        <AnimatePresence>
          {isDragOver && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-3 z-40 rounded-3xl border-2 border-dashed border-emerald-400/70 bg-emerald-500/10 backdrop-blur-sm flex items-center justify-center pointer-events-none"
            >
              <div className="px-6 py-4 rounded-2xl bg-black/85 border border-emerald-400/40 text-center space-y-1">
                <FileUp size={24} className="mx-auto text-emerald-300" />
                <p className="text-sm font-bold text-white">Drop files to place on canvas</p>
                <p className="text-[11px] text-white/50">
                  Uploading via {providerConfig.name} (auto-fallback active)
                </p>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Transformed World Container */}
        <div
          style={{
            transform: `translate3d(${pan.x}px, ${pan.y}px, 0) scale(${zoom})`,
            transformOrigin: "0 0",
          }}
          className="absolute top-0 left-0 w-0 h-0 pointer-events-none"
        >
          {/* Magnetic Smart Alignment Guides */}
          {activeDrag?.guideX !== null && activeDrag?.guideX !== undefined && (
            <div
              style={{
                transform: `translate3d(${activeDrag.guideX}px, -4000px, 0)`,
              }}
              className="absolute top-0 left-0 w-px h-[8000px] bg-emerald-400/45 pointer-events-none"
            />
          )}
          {activeDrag?.guideY !== null && activeDrag?.guideY !== undefined && (
            <div
              style={{
                transform: `translate3d(-4000px, ${activeDrag.guideY}px, 0)`,
              }}
              className="absolute top-0 left-0 h-px w-[8000px] bg-emerald-400/45 pointer-events-none"
            />
          )}

          {/* Spatial Items */}
          {items.map((item) => {
            const isDraggingThis = activeDrag?.id === item.id;
            const x = isDraggingThis ? activeDrag.x : item.x;
            const y = isDraggingThis ? activeDrag.y : item.y;
            const isSelected = selectedItemId === item.id;
            const noteStyle = NOTE_COLOR_STYLES[item.noteColor || "zinc"];
            const previewUrl =
              item.kind === "file" && !brokenThumbs[item.id]
                ? getMediaPreviewUrl(item.url, item.mimeType)
                : null;
            const isImageThumb =
              Boolean(previewUrl) && (item.mimeType || "").toLowerCase().startsWith("image/");

            return (
              <div
                key={`${item.sourceNode}-${item.id}`}
                onPointerDown={(e) => handleCardPointerDown(e, item)}
                onDoubleClick={(e) => {
                  e.stopPropagation();
                  onOpenPreview(item);
                }}
                style={{
                  transform: `translate3d(${x}px, ${y}px, 0)`,
                  zIndex: isDraggingThis ? maxZIndex + 10 : item.zIndex || 1,
                  willChange: isDraggingThis ? "transform" : undefined,
                }}
                className={`space-card absolute top-0 left-0 pointer-events-auto group cursor-grab active:cursor-grabbing ${
                  isDraggingThis ? "" : "transition-transform duration-150 ease-out"
                }`}
              >
                <div
                  className={`rounded-2xl transition-all duration-150 ${
                    isDraggingThis
                      ? "scale-[1.03] shadow-[0_28px_72px_rgba(0,0,0,0.85)] ring-1 ring-white/40"
                      : isSelected
                        ? "ring-2 ring-white/80 shadow-[0_20px_50px_rgba(0,0,0,0.75)]"
                        : "shadow-[0_14px_38px_rgba(0,0,0,0.55)]"
                  }`}
                >
                  {item.kind === "file" && (
                    <div className="w-[250px] bg-[#141416]/90 backdrop-blur-2xl backdrop-saturate-150 border border-white/[0.12] group-hover:border-white/30 rounded-2xl overflow-hidden shadow-[inset_0_1px_0_rgba(255,255,255,0.1)]">
                      {isImageThumb && previewUrl && (
                        <div className="w-full h-32 bg-black/50 border-b border-white/10 overflow-hidden relative">
                          <img
                            src={previewUrl}
                            alt={item.name}
                            draggable={false}
                            onError={() =>
                              setBrokenThumbs((prev) => ({ ...prev, [item.id]: true }))
                            }
                            className="w-full h-full object-cover"
                          />
                        </div>
                      )}
                      <div className="p-3.5">
                        <div className="flex items-start gap-3">
                          <div className="w-10 h-10 shrink-0 rounded-xl bg-white/[0.06] border border-white/10 flex items-center justify-center">
                            {getFileIcon(item.mimeType)}
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="text-xs font-bold text-white truncate" title={item.name}>
                              {item.name}
                            </p>
                            <p className="text-[10px] text-white/45 mt-0.5 flex items-center gap-1.5">
                              <span>{formatBytes(item.size)}</span>
                              {item.providerName && (
                                <>
                                  <span>•</span>
                                  <span className="text-emerald-300/85">{item.providerName}</span>
                                </>
                              )}
                            </p>
                          </div>
                        </div>

                        <div
                          onPointerDown={(e) => e.stopPropagation()}
                          className="mt-3 pt-2.5 border-t border-white/10 flex items-center justify-between gap-1"
                        >
                          <span className="text-[10px] text-white/35 truncate max-w-[85px]">
                            by {item.uploadedBy}
                          </span>
                          <div className="flex items-center gap-1">
                            <button
                              type="button"
                              onClick={() => onOpenPreview(item)}
                              title="Preview"
                              className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                            >
                              <Eye size={13} />
                            </button>
                            <button
                              type="button"
                              onClick={() => onDownloadItem(item)}
                              title={
                                item.url && getDirectDownloadMode(item.url) === "tab"
                                  ? "Open provider page"
                                  : "Download"
                              }
                              className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                            >
                              <Download size={13} />
                            </button>
                            {item.url && (
                              <button
                                type="button"
                                onClick={() => {
                                  navigator.clipboard.writeText(item.url!);
                                  showToast("File link copied!", "success");
                                }}
                                title="Copy link"
                                className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                              >
                                <Copy size={13} />
                              </button>
                            )}
                            <button
                              type="button"
                              onClick={() => onOpenRename(item)}
                              title="Rename"
                              className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                            >
                              <Edit3 size={13} />
                            </button>
                            <button
                              type="button"
                              onClick={() => onOpenDelete(item)}
                              title="Delete"
                              className="p-1.5 rounded-lg bg-white/5 hover:bg-red-500 text-white/75 hover:text-white transition-all active:scale-90"
                            >
                              <Trash2 size={13} />
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  {item.kind === "note" && (
                    <div
                      className={`w-[240px] ${noteStyle.bg} ${noteStyle.border} backdrop-blur-2xl backdrop-saturate-150 border rounded-2xl p-3.5 space-y-2 shadow-[inset_0_1px_0_rgba(255,255,255,0.1)]`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-1.5 min-w-0">
                          <StickyNote size={13} className="text-white/60 shrink-0" />
                          <span className="text-xs font-bold text-white truncate">{item.name}</span>
                        </div>
                      </div>
                      <p className="text-xs text-white/80 leading-relaxed whitespace-pre-wrap break-words line-clamp-6">
                        {item.content || ""}
                      </p>
                      <div
                        onPointerDown={(e) => e.stopPropagation()}
                        className="pt-2 border-t border-white/10 flex items-center justify-between"
                      >
                        <span className="text-[10px] text-white/35 truncate max-w-[95px]">
                          by {item.uploadedBy}
                        </span>
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => onOpenPreview(item)}
                            title="Expand"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Eye size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              navigator.clipboard.writeText(item.content || "");
                              showToast("Note copied!", "success");
                            }}
                            title="Copy text"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Copy size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => onOpenComposer("note", undefined, item)}
                            title="Edit"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Edit3 size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => onOpenDelete(item)}
                            title="Delete"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-red-500 text-white/75 hover:text-white transition-all active:scale-90"
                          >
                            <Trash2 size={12} />
                          </button>
                        </div>
                      </div>
                    </div>
                  )}

                  {item.kind === "link" && (
                    <div className="w-[260px] bg-[#141416]/90 border border-sky-400/25 backdrop-blur-2xl backdrop-saturate-150 rounded-2xl p-3.5 space-y-2 shadow-[inset_0_1px_0_rgba(255,255,255,0.1)]">
                      <div className="flex items-center gap-2">
                        <span className="w-8 h-8 rounded-xl bg-sky-400/10 border border-sky-400/20 flex items-center justify-center text-sky-300 shrink-0">
                          <Link2 size={15} />
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-bold text-white truncate">{item.name}</p>
                          <p className="text-[10px] font-mono text-sky-300/75 truncate">{item.url}</p>
                        </div>
                      </div>
                      {item.content && (
                        <p className="text-[11px] text-white/60 line-clamp-2">{item.content}</p>
                      )}
                      <div
                        onPointerDown={(e) => e.stopPropagation()}
                        className="pt-2 border-t border-white/10 flex items-center justify-between"
                      >
                        <span className="text-[10px] text-white/35 truncate max-w-[90px]">
                          by {item.uploadedBy}
                        </span>
                        <div className="flex items-center gap-1">
                          {item.url && (
                            <a
                              href={item.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              title="Open link"
                              className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                            >
                              <ExternalLink size={12} />
                            </a>
                          )}
                          <button
                            type="button"
                            onClick={() => {
                              navigator.clipboard.writeText(item.url || "");
                              showToast("URL copied!", "success");
                            }}
                            title="Copy URL"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Copy size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => onOpenComposer("link", undefined, item)}
                            title="Edit"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Edit3 size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => onOpenDelete(item)}
                            title="Delete"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-red-500 text-white/75 hover:text-white transition-all active:scale-90"
                          >
                            <Trash2 size={12} />
                          </button>
                        </div>
                      </div>
                    </div>
                  )}

                  {item.kind === "code" && (
                    <div className="w-[290px] bg-[#0e0e11]/95 border border-emerald-400/25 backdrop-blur-2xl backdrop-saturate-150 rounded-2xl p-3.5 space-y-2 shadow-[inset_0_1px_0_rgba(255,255,255,0.1)]">
                      <div className="flex items-center gap-2">
                        <Code2 size={14} className="text-emerald-400 shrink-0" />
                        <span className="text-xs font-bold text-white truncate">{item.name}</span>
                      </div>
                      <pre className="rounded-xl bg-black/70 border border-white/10 p-2.5 text-[10px] font-mono text-emerald-200/85 overflow-hidden line-clamp-6 whitespace-pre-wrap break-all">
                        {item.content || ""}
                      </pre>
                      <div
                        onPointerDown={(e) => e.stopPropagation()}
                        className="pt-2 border-t border-white/10 flex items-center justify-between"
                      >
                        <span className="text-[10px] text-white/35 truncate max-w-[95px]">
                          by {item.uploadedBy}
                        </span>
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => onOpenPreview(item)}
                            title="Expand"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Eye size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              navigator.clipboard.writeText(item.content || "");
                              showToast("Code copied!", "success");
                            }}
                            title="Copy code"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Copy size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => onOpenComposer("code", undefined, item)}
                            title="Edit"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/75 hover:text-black transition-all active:scale-90"
                          >
                            <Edit3 size={12} />
                          </button>
                          <button
                            type="button"
                            onClick={() => onOpenDelete(item)}
                            title="Delete"
                            className="p-1.5 rounded-lg bg-white/5 hover:bg-red-500 text-white/75 hover:text-white transition-all active:scale-90"
                          >
                            <Trash2 size={12} />
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          {/* Live Multiplayer Cursors */}
          {activeCursors.map((cursor) => (
            <div
              key={cursor.id}
              style={{
                transform: `translate3d(${cursor.x}px, ${cursor.y}px, 0)`,
                zIndex: 9999,
              }}
              className="absolute top-0 left-0 pointer-events-none transition-transform duration-75 ease-linear"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path
                  d="M4 3L20 11L13 13.5L10.5 20.5L4 3Z"
                  fill={cursor.color}
                  stroke="#000000"
                  strokeWidth="1.5"
                />
              </svg>
              <span
                className="ml-3 -mt-1 inline-block px-2 py-0.5 rounded-full text-[10px] font-bold text-black whitespace-nowrap shadow-lg"
                style={{ backgroundColor: cursor.color }}
              >
                {cursor.name}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Bottom-Left Viewport Controls & Corner Minimap */}
      <div className="space-hud absolute bottom-3 left-3 z-30 hidden sm:flex flex-col gap-2 pointer-events-auto">
        {showMinimap && (
          <div
            onClick={(e) => {
              const box = e.currentTarget.getBoundingClientRect();
              const rect = containerRef.current?.getBoundingClientRect();
              if (!rect) return;
              const relX = (e.clientX - box.left) / box.width;
              const relY = (e.clientY - box.top) / box.height;
              const targetX = (relX - 0.15) * 2400;
              const targetY = (relY - 0.15) * 1600;
              animateCameraTo(
                {
                  x: Math.round(rect.width / 2 - targetX * zoomRef.current),
                  y: Math.round(rect.height / 2 - targetY * zoomRef.current),
                },
                zoomRef.current,
                260,
              );
            }}
            title="Click minimap to jump"
            className={`w-40 h-28 rounded-2xl ${APPLE_GLASS} relative overflow-hidden cursor-pointer`}
          >
            {items.map((item) => {
              const nx = ((item.x + 360) / 2400) * 100;
              const ny = ((item.y + 240) / 1600) * 100;
              return (
                <span
                  key={`mini-${item.id}`}
                  style={{
                    left: `${Math.min(94, Math.max(4, nx))}%`,
                    top: `${Math.min(90, Math.max(6, ny))}%`,
                  }}
                  className={`absolute w-2 h-1.5 rounded-sm ${
                    item.id === selectedItemId
                      ? "bg-white"
                      : item.kind === "file"
                        ? "bg-emerald-400/80"
                        : item.kind === "link"
                          ? "bg-sky-400/80"
                          : "bg-amber-400/80"
                  }`}
                />
              );
            })}
            <span className="absolute bottom-1 right-2 text-[8px] font-mono uppercase tracking-widest text-white/30">
              Map
            </span>
          </div>
        )}

        <div className={`flex items-center gap-1 p-1 rounded-2xl ${APPLE_GLASS} text-white/75`}>
          <button
            type="button"
            onClick={() => handleZoomBy(0.85)}
            title="Zoom Out (-)"
            className="w-8 h-8 rounded-xl hover:bg-white/10 hover:text-white flex items-center justify-center transition-all active:scale-90"
          >
            <Minus size={14} />
          </button>
          <button
            type="button"
            onClick={handleResetView}
            title="Reset Zoom (0)"
            className="px-2 h-8 rounded-xl hover:bg-white/10 text-[11px] font-mono font-bold text-white transition-all active:scale-95"
          >
            {Math.round(zoom * 100)}%
          </button>
          <button
            type="button"
            onClick={() => handleZoomBy(1.15)}
            title="Zoom In (+)"
            className="w-8 h-8 rounded-xl hover:bg-white/10 hover:text-white flex items-center justify-center transition-all active:scale-90"
          >
            <Plus size={14} />
          </button>
          <div className="w-px h-4 bg-white/10 mx-0.5" />
          <button
            type="button"
            onClick={handleFitView}
            title="Fit All Items (F)"
            className="px-2.5 h-8 rounded-xl hover:bg-white/10 hover:text-white text-[10px] font-bold uppercase tracking-wider transition-all active:scale-95"
          >
            Fit
          </button>
          <button
            type="button"
            onClick={() => setSnapToGrid((v) => !v)}
            title="Toggle Snap to Grid (G)"
            className={`w-8 h-8 rounded-xl flex items-center justify-center transition-all active:scale-90 ${
              snapToGrid ? "bg-white text-black" : "hover:bg-white/10 text-white/60"
            }`}
          >
            <Grid size={13} />
          </button>
          <button
            type="button"
            onClick={() => setShowMinimap((v) => !v)}
            title="Toggle Minimap"
            className={`w-8 h-8 rounded-xl flex items-center justify-center transition-all active:scale-90 ${
              showMinimap ? "bg-white/15 text-white" : "hover:bg-white/10 text-white/60"
            }`}
          >
            <MapIcon size={13} />
          </button>
        </div>
      </div>

      {/* Compact Mobile Zoom / Fit Strip (< sm) */}
      <div className="space-hud absolute bottom-16 left-3 z-30 flex sm:hidden items-center gap-1 p-1 rounded-full bg-[#121214]/85 backdrop-blur-2xl border border-white/15 text-white/80 pointer-events-auto">
        <button
          type="button"
          onClick={() => handleZoomBy(0.85)}
          aria-label="Zoom out"
          className="w-7 h-7 rounded-full hover:bg-white/10 flex items-center justify-center active:scale-90"
        >
          <Minus size={12} />
        </button>
        <button
          type="button"
          onClick={handleFitView}
          className="px-2 h-7 rounded-full text-[10px] font-bold uppercase tracking-wider text-white"
        >
          Fit
        </button>
        <button
          type="button"
          onClick={() => handleZoomBy(1.15)}
          aria-label="Zoom in"
          className="w-7 h-7 rounded-full hover:bg-white/10 flex items-center justify-center active:scale-90"
        >
          <Plus size={12} />
        </button>
      </div>

      {/* Bottom-Center Floating Creation Dock */}
      <div className="space-hud absolute bottom-3 left-1/2 -translate-x-1/2 z-30 pointer-events-auto max-w-[95vw]">
        <div
          className={`flex items-center gap-1.5 p-1.5 rounded-full ${APPLE_GLASS} shadow-[inset_0_1px_0_rgba(255,255,255,0.14),0_16px_48px_rgba(0,0,0,0.75)] overflow-x-auto no-scrollbar`}
        >
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="px-3.5 py-2 rounded-full bg-white text-black hover:bg-white/90 text-xs font-bold flex items-center gap-1.5 shrink-0 transition-all active:scale-[0.96]"
          >
            <FileUp size={14} />
            <span>Upload</span>
          </button>

          <button
            type="button"
            onClick={() => {
              const c = getViewportCenter();
              onOpenComposer("note", {
                x: snapCoord(c.x - 120, snapToGrid),
                y: snapCoord(c.y - 60, snapToGrid),
              });
            }}
            className="px-3 py-2 rounded-full bg-white/5 hover:bg-white/15 text-white/85 text-xs font-semibold flex items-center gap-1.5 shrink-0 transition-all active:scale-[0.96]"
          >
            <StickyNote size={14} className="text-amber-300" />
            <span className="hidden sm:inline">Note</span>
          </button>

          <button
            type="button"
            onClick={() => {
              const c = getViewportCenter();
              onOpenComposer("link", {
                x: snapCoord(c.x - 120, snapToGrid),
                y: snapCoord(c.y - 60, snapToGrid),
              });
            }}
            className="px-3 py-2 rounded-full bg-white/5 hover:bg-white/15 text-white/85 text-xs font-semibold flex items-center gap-1.5 shrink-0 transition-all active:scale-[0.96]"
          >
            <Link2 size={14} className="text-sky-300" />
            <span className="hidden sm:inline">Link</span>
          </button>

          <button
            type="button"
            onClick={() => {
              const c = getViewportCenter();
              onOpenComposer("code", {
                x: snapCoord(c.x - 120, snapToGrid),
                y: snapCoord(c.y - 60, snapToGrid),
              });
            }}
            className="px-3 py-2 rounded-full bg-white/5 hover:bg-white/15 text-white/85 text-xs font-semibold flex items-center gap-1.5 shrink-0 transition-all active:scale-[0.96]"
          >
            <Code2 size={14} className="text-emerald-300" />
            <span className="hidden sm:inline">Code</span>
          </button>

          <div className="w-px h-5 bg-white/10 mx-0.5 shrink-0" />

          <button
            type="button"
            onClick={onOpenProviderPicker}
            title="Select upload provider"
            className="px-3 py-2 rounded-full bg-emerald-400/10 hover:bg-emerald-400/20 border border-emerald-400/25 text-emerald-300 text-[11px] font-bold flex items-center gap-1.5 shrink-0 transition-all active:scale-[0.96]"
          >
            <Box size={13} />
            <span>{providerConfig.name}</span>
          </button>

          <button
            type="button"
            onClick={onAutoArrange}
            disabled={items.length === 0}
            title="Auto-arrange all cards cleanly"
            className="px-3 py-2 rounded-full bg-white/5 hover:bg-white/15 disabled:opacity-35 text-white/80 text-xs font-semibold flex items-center gap-1.5 shrink-0 transition-all active:scale-[0.96]"
          >
            <LayoutGrid size={14} />
            <span className="hidden md:inline">Arrange</span>
          </button>
        </div>
      </div>

      {/* Bottom-Right Upload Queue Tray */}
      <AnimatePresence>
        {uploadTasks.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 16 }}
            className={`space-hud absolute bottom-16 sm:bottom-3 right-3 z-30 w-72 rounded-2xl ${APPLE_GLASS} p-3.5 space-y-2.5`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-bold text-white">
                <Loader2 size={14} className="animate-spin text-emerald-400" />
                <span>Uploading ({uploadTasks.length})</span>
              </div>
              <button
                type="button"
                onClick={onCancelUpload}
                className="text-[10px] font-bold uppercase tracking-wider text-red-400 hover:text-red-300"
              >
                Cancel
              </button>
            </div>
            <div className="space-y-2">
              {uploadTasks.slice(0, 3).map((task) => (
                <div key={task.id} className="space-y-1">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-white/80 truncate max-w-[160px]">{task.fileName}</span>
                    <span className="font-mono text-emerald-300">{task.progress}%</span>
                  </div>
                  <div className="h-1.5 w-full rounded-full bg-white/10 overflow-hidden">
                    <div
                      className="h-full bg-emerald-400 transition-all duration-150"
                      style={{ width: `${task.progress}%` }}
                    />
                  </div>
                  <p className="text-[10px] text-white/40 truncate">{task.status}</p>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Right Slide-Over Items & Search Drawer */}
      <AnimatePresence>
        {showDrawer && (
          <motion.aside
            initial={{ x: "100%" }}
            animate={{ x: 0 }}
            exit={{ x: "100%" }}
            transition={{ type: "spring", damping: 28, stiffness: 320 }}
            className="space-hud absolute top-0 right-0 bottom-0 z-40 w-80 max-w-[90vw] bg-[#101012]/95 backdrop-blur-2xl backdrop-saturate-150 border-l border-white/15 flex flex-col shadow-2xl"
          >
            <div className="p-4 border-b border-white/10 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-white">Space Items ({items.length})</h3>
                <p className="text-[10px] text-white/40">Click any item to jump to it on canvas</p>
              </div>
              <button
                type="button"
                aria-label="Close items drawer"
                onClick={() => setShowDrawer(false)}
                className="p-2 rounded-full text-white/55 hover:text-white hover:bg-white/10 transition-colors"
              >
                <X size={16} />
              </button>
            </div>

            <div className="p-3 space-y-2.5 border-b border-white/10">
              <div className="relative">
                <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/35" />
                <input
                  value={drawerQuery}
                  onChange={(e) => setDrawerQuery(e.target.value)}
                  placeholder="Search files, notes, links..."
                  className="w-full bg-white/[0.04] border border-white/10 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder:text-white/30 focus:outline-none focus:border-white/30"
                />
              </div>

              <div className="flex gap-1 overflow-x-auto no-scrollbar">
                {(["all", "file", "note", "link", "code"] as const).map((kind) => (
                  <button
                    key={kind}
                    type="button"
                    onClick={() => setDrawerFilter(kind)}
                    className={`px-2.5 py-1 rounded-lg text-[10px] font-bold uppercase tracking-wider shrink-0 transition-colors ${
                      drawerFilter === kind
                        ? "bg-white text-black"
                        : "bg-white/5 text-white/50 hover:text-white"
                    }`}
                  >
                    {kind}
                  </button>
                ))}
              </div>

              {fileCount > 0 && (
                <button
                  type="button"
                  onClick={onDownloadAllFiles}
                  className="w-full py-2 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-xs font-bold text-white flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Download size={13} />
                  Download All Files ({fileCount})
                </button>
              )}
            </div>

            <div className="flex-1 overflow-y-auto p-3 space-y-2 custom-scrollbar">
              {filteredDrawerItems.length === 0 ? (
                <p className="text-xs text-white/35 text-center py-8">No matching items</p>
              ) : (
                filteredDrawerItems.map((item) => (
                  <div
                    key={`drawer-${item.id}`}
                    onClick={() => focusItemOnCanvas(item)}
                    className="p-2.5 rounded-xl bg-white/[0.03] hover:bg-white/[0.08] border border-white/10 cursor-pointer flex items-center gap-2.5 transition-colors"
                  >
                    <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center shrink-0">
                      {item.kind === "file" ? (
                        getFileIcon(item.mimeType)
                      ) : item.kind === "note" ? (
                        <StickyNote size={15} className="text-amber-300" />
                      ) : item.kind === "link" ? (
                        <Link2 size={15} className="text-sky-300" />
                      ) : (
                        <Code2 size={15} className="text-emerald-300" />
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-semibold text-white truncate">{item.name}</p>
                      <p className="text-[10px] text-white/40 truncate">
                        by {item.uploadedBy}
                        {item.size ? ` • ${formatBytes(item.size)}` : ""}
                      </p>
                    </div>
                    <div
                      onClick={(e) => e.stopPropagation()}
                      className="flex items-center gap-1 shrink-0"
                    >
                      {item.kind === "file" && item.url && (
                        <button
                          type="button"
                          onClick={() => onDownloadItem(item)}
                          title="Download"
                          className="p-1.5 rounded-lg bg-white/5 hover:bg-white text-white/70 hover:text-black"
                        >
                          <Download size={12} />
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => onOpenDelete(item)}
                        title="Delete"
                        className="p-1.5 rounded-lg bg-white/5 hover:bg-red-500 text-white/70 hover:text-white"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </motion.aside>
        )}
      </AnimatePresence>
    </div>
  );

  if (isFullscreen && typeof document !== "undefined") {
    return createPortal(canvasViewport, document.body);
  }
  return canvasViewport;
}
