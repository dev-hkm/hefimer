import React, { useEffect, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import QRCode from "qrcode";
import {
  Box,
  Check,
  ChevronDown,
  Code2,
  Copy,
  Download,
  Edit3,
  ExternalLink,
  File as FileIcon,
  FileUp,
  Link2,
  StickyNote,
  Trash2,
  X,
} from "lucide-react";
import { UPLOAD_PROVIDERS } from "../providers";
import {
  NOTE_COLOR_STYLES,
  formatBytes,
  formatCountdown,
  getMediaPreviewUrl,
  type NoteColor,
  type SpaceItem,
} from "./types";

/* ---------------- Share / QR Modal ---------------- */
interface SpaceShareModalProps {
  open: boolean;
  onClose: () => void;
  spaceId: string;
  spaceName: string;
  expiresAt: number;
  showToast: (msg: string, type?: "success" | "error" | "info") => void;
}

export function SpaceShareModal({
  open,
  onClose,
  spaceId,
  spaceName,
  expiresAt,
  showToast,
}: SpaceShareModalProps) {
  const [qrDataUrl, setQrDataUrl] = useState("");
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  const shareUrl =
    typeof window !== "undefined" ? `${window.location.origin}/space?code=${spaceId}` : "";

  useEffect(() => {
    if (!open || !spaceId) return;
    let cancelled = false;
    QRCode.toDataURL(shareUrl, {
      width: 640,
      margin: 1,
      color: { dark: "#000000", light: "#ffffff" },
      errorCorrectionLevel: "H",
    })
      .then((url) => {
        if (!cancelled) setQrDataUrl(url);
      })
      .catch(() => {
        if (!cancelled) showToast("Failed to generate QR code", "error");
      });
    return () => {
      cancelled = true;
    };
  }, [open, spaceId, shareUrl, showToast]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(spaceId);
    setCopiedCode(true);
    showToast("Space code copied!", "success");
    setTimeout(() => setCopiedCode(false), 1800);
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(shareUrl);
    setCopiedLink(true);
    showToast("Space link copied!", "success");
    setTimeout(() => setCopiedLink(false), 1800);
  };

  const handleDownloadQR = () => {
    if (!qrDataUrl) return;
    const a = document.createElement("a");
    a.href = qrDataUrl;
    a.download = `hefimer-space-${spaceId}.png`;
    a.click();
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md"
          onMouseDown={onClose}
        >
          <motion.div
            initial={{ scale: 0.94, opacity: 0, y: 16 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.94, opacity: 0, y: 16 }}
            transition={{ type: "spring", damping: 26, stiffness: 320 }}
            onMouseDown={(e) => e.stopPropagation()}
            className="w-full max-w-sm bg-[#0a0a0a] border border-white/15 rounded-[28px] p-6 flex flex-col items-center gap-5 shadow-2xl relative"
          >
            <button
              type="button"
              onClick={onClose}
              aria-label="Close share modal"
              className="absolute top-4 right-4 text-white/40 hover:text-white p-1.5 hover:bg-white/10 rounded-full transition-all"
            >
              <X size={16} />
            </button>

            <div className="text-center space-y-1 mt-1">
              <p className="text-[9px] font-black uppercase tracking-[0.22em] text-emerald-400">
                Invite Collaborators
              </p>
              <h3 className="text-xl font-bold text-white tracking-tight">
                {spaceName || `Space ${spaceId}`}
              </h3>
              {expiresAt > 0 && (
                <p className="text-[11px] text-white/40 font-mono">
                  Expires in {formatCountdown(expiresAt)}
                </p>
              )}
            </div>

            <div className="bg-white p-4 rounded-[22px] shadow-[0_0_40px_rgba(255,255,255,0.08)] flex items-center justify-center">
              {qrDataUrl ? (
                <img
                  src={qrDataUrl}
                  alt={`QR Code for Space ${spaceId}`}
                  className="w-[188px] h-[188px] block rounded-lg"
                />
              ) : (
                <div className="w-[188px] h-[188px] flex items-center justify-center text-black/40 text-xs font-mono">
                  Generating QR...
                </div>
              )}
            </div>

            <div className="w-full rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-2.5 flex items-center justify-between">
              <span className="text-[10px] font-bold uppercase tracking-[0.18em] text-white/40">
                Access Code
              </span>
              <button
                type="button"
                onClick={handleCopyCode}
                className="flex items-center gap-2 font-mono text-base font-bold tracking-[0.2em] text-white hover:text-emerald-300 transition-colors"
              >
                {spaceId}
                {copiedCode ? <Check size={14} className="text-emerald-400" /> : <Copy size={14} className="text-white/50" />}
              </button>
            </div>

            <div className="flex gap-2 w-full">
              <button
                type="button"
                onClick={handleCopyLink}
                className={`flex-1 py-3 rounded-2xl text-[11px] font-bold uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 border ${
                  copiedLink
                    ? "bg-emerald-400 text-black border-emerald-400"
                    : "bg-white/5 hover:bg-white/10 border-white/10 text-white/85"
                }`}
              >
                {copiedLink ? <Check size={14} /> : <Link2 size={14} />}
                {copiedLink ? "Copied" : "Copy Link"}
              </button>
              <button
                type="button"
                onClick={handleDownloadQR}
                disabled={!qrDataUrl}
                className="flex-1 bg-white text-black py-3 rounded-2xl text-[11px] font-bold uppercase tracking-wider transition-all hover:bg-white/90 disabled:opacity-40 flex items-center justify-center gap-1.5"
              >
                <Download size={14} />
                Save QR
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ---------------- Composer Modal (Note / Link / Code) ---------------- */
interface SpaceComposerModalProps {
  open: boolean;
  mode: "note" | "link" | "code";
  editingItem?: SpaceItem | null;
  onClose: () => void;
  onSubmit: (payload: {
    kind: "note" | "link" | "code";
    name: string;
    content?: string;
    url?: string;
    noteColor?: NoteColor;
  }) => void;
}

export function SpaceComposerModal({
  open,
  mode,
  editingItem,
  onClose,
  onSubmit,
}: SpaceComposerModalProps) {
  const activeMode =
    editingItem && (editingItem.kind === "note" || editingItem.kind === "link" || editingItem.kind === "code")
      ? editingItem.kind
      : mode;

  const [name, setName] = useState("");
  const [content, setContent] = useState("");
  const [url, setUrl] = useState("");
  const [noteColor, setNoteColor] = useState<NoteColor>("zinc");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) return;
    setError("");
    if (editingItem) {
      setName(editingItem.name || "");
      setContent(editingItem.content || "");
      setUrl(editingItem.url || "");
      setNoteColor(editingItem.noteColor || "zinc");
    } else {
      setName("");
      setContent("");
      setUrl("");
      setNoteColor("zinc");
    }
  }, [open, editingItem, mode]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (activeMode === "link") {
      let normalizedUrl = url.trim();
      if (!normalizedUrl) {
        setError("Please enter a URL");
        return;
      }
      if (!/^https?:\/\//i.test(normalizedUrl)) {
        normalizedUrl = `https://${normalizedUrl}`;
      }
      try {
        const parsed = new URL(normalizedUrl);
        const title = name.trim() || parsed.hostname.replace(/^www\./, "");
        onSubmit({
          kind: "link",
          name: title,
          url: parsed.toString(),
          content: content.trim() || undefined,
        });
        onClose();
      } catch {
        setError("Please enter a valid URL");
      }
      return;
    }

    if (!content.trim()) {
      setError(activeMode === "code" ? "Please paste or type your code snippet" : "Please write a note");
      return;
    }

    const fallbackTitle = activeMode === "code" ? "Code Snippet" : "Sticky Note";
    onSubmit({
      kind: activeMode,
      name: name.trim() || fallbackTitle,
      content: content,
      noteColor: activeMode === "note" ? noteColor : undefined,
    });
    onClose();
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/75 backdrop-blur-md"
          onMouseDown={onClose}
        >
          <motion.form
            initial={{ scale: 0.95, opacity: 0, y: 12 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.95, opacity: 0, y: 12 }}
            onSubmit={handleSubmit}
            onMouseDown={(e) => e.stopPropagation()}
            className="w-full max-w-md bg-[#0a0a0a] border border-white/15 rounded-[28px] p-6 space-y-4 shadow-2xl"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span className="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white/80">
                  {activeMode === "note" ? (
                    <StickyNote size={16} />
                  ) : activeMode === "link" ? (
                    <Link2 size={16} />
                  ) : (
                    <Code2 size={16} />
                  )}
                </span>
                <div>
                  <h3 className="text-base font-bold text-white">
                    {editingItem ? "Edit" : "Add"}{" "}
                    {activeMode === "note" ? "Sticky Note" : activeMode === "link" ? "Link Bookmark" : "Code Snippet"}
                  </h3>
                  <p className="text-[11px] text-white/40">Placed directly onto the shared canvas</p>
                </div>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="p-1.5 rounded-full text-white/40 hover:text-white hover:bg-white/10 transition-colors"
              >
                <X size={16} />
              </button>
            </div>

            {activeMode === "link" && (
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold uppercase tracking-widest text-white/40 ml-1">
                  URL
                </label>
                <input
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  placeholder="https://example.com"
                  autoFocus
                  className="w-full bg-white/[0.04] border border-white/10 rounded-2xl px-4 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-white/30"
                />
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-[10px] font-bold uppercase tracking-widest text-white/40 ml-1">
                {activeMode === "code" ? "Snippet Title / Filename" : "Title (Optional)"}
              </label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={
                  activeMode === "note"
                    ? "e.g. Design Feedback"
                    : activeMode === "link"
                      ? "e.g. Figma Prototype"
                      : "e.g. auth-helper.ts"
                }
                autoFocus={activeMode !== "link"}
                className="w-full bg-white/[0.04] border border-white/10 rounded-2xl px-4 py-2.5 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-white/30"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-[10px] font-bold uppercase tracking-widest text-white/40 ml-1">
                {activeMode === "link"
                  ? "Description (Optional)"
                  : activeMode === "code"
                    ? "Code"
                    : "Note Content"}
              </label>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={activeMode === "link" ? 3 : 6}
                placeholder={
                  activeMode === "code"
                    ? "Paste your code snippet here..."
                    : activeMode === "link"
                      ? "Short context for teammates..."
                      : "Write something for everyone in this space..."
                }
                className={`w-full bg-white/[0.04] border border-white/10 rounded-2xl px-4 py-3 text-sm text-white placeholder:text-white/25 focus:outline-none focus:border-white/30 resize-y ${
                  activeMode === "code" ? "font-mono text-xs leading-relaxed" : ""
                }`}
              />
            </div>

            {activeMode === "note" && (
              <div className="flex items-center justify-between px-1">
                <span className="text-[10px] font-bold uppercase tracking-widest text-white/40">
                  Card Tint
                </span>
                <div className="flex items-center gap-2">
                  {(Object.keys(NOTE_COLOR_STYLES) as NoteColor[]).map((colorKey) => (
                    <button
                      key={colorKey}
                      type="button"
                      onClick={() => setNoteColor(colorKey)}
                      title={NOTE_COLOR_STYLES[colorKey].label}
                      className={`w-6 h-6 rounded-full border transition-transform ${
                        noteColor === colorKey ? "scale-125 ring-2 ring-white/80" : "opacity-65 hover:opacity-100"
                      }`}
                      style={{
                        backgroundColor: NOTE_COLOR_STYLES[colorKey].dot,
                        borderColor: "rgba(255,255,255,0.2)",
                      }}
                    />
                  ))}
                </div>
              </div>
            )}

            {error && (
              <p className="text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2">
                {error}
              </p>
            )}

            <div className="flex gap-2 pt-1">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-2xl border border-white/10 bg-white/5 hover:bg-white/10 text-white/70 text-xs font-bold transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="flex-1 py-2.5 rounded-2xl bg-white text-black hover:bg-white/90 text-xs font-bold transition-colors"
              >
                {editingItem ? "Save Changes" : "Place on Canvas"}
              </button>
            </div>
          </motion.form>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ---------------- Preview Lightbox Modal ---------------- */
interface SpacePreviewModalProps {
  item: SpaceItem | null;
  onClose: () => void;
  onDownload: (item: SpaceItem) => void;
  onCopyText: (text: string, label?: string) => void;
  onEdit: (item: SpaceItem) => void;
  onRename: (item: SpaceItem) => void;
  onDelete: (item: SpaceItem) => void;
}

export function SpacePreviewModal({
  item,
  onClose,
  onDownload,
  onCopyText,
  onEdit,
  onRename,
  onDelete,
}: SpacePreviewModalProps) {
  const [mediaError, setMediaError] = useState(false);

  useEffect(() => {
    setMediaError(false);
  }, [item?.id]);

  useEffect(() => {
    if (!item) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [item, onClose]);

  const previewUrl = item?.kind === "file" ? getMediaPreviewUrl(item.url, item.mimeType) : null;
  const mime = (item?.mimeType || "").toLowerCase();

  return (
    <AnimatePresence>
      {item && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/85 backdrop-blur-md"
          onMouseDown={onClose}
        >
          <motion.div
            initial={{ scale: 0.95, opacity: 0, y: 12 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.95, opacity: 0, y: 12 }}
            onMouseDown={(e) => e.stopPropagation()}
            className="w-full max-w-3xl max-h-[88vh] bg-[#0d0d0f]/90 backdrop-blur-2xl backdrop-saturate-150 border border-white/15 rounded-[28px] overflow-hidden flex flex-col shadow-[inset_0_1px_0_rgba(255,255,255,0.12),0_28px_90px_rgba(0,0,0,0.75)]"
          >
            {/* Header */}
            <div className="flex items-center justify-between gap-3 px-5 py-4 border-b border-white/10">
              <div className="min-w-0">
                <h3 className="text-sm font-bold text-white truncate">{item.name}</h3>
                <p className="text-[11px] text-white/40 flex items-center gap-2 mt-0.5">
                  <span>by {item.uploadedBy}</span>
                  {item.size ? <span>• {formatBytes(item.size)}</span> : null}
                  {item.providerName ? <span>• {item.providerName}</span> : null}
                </p>
              </div>
              <button
                type="button"
                onClick={onClose}
                aria-label="Close preview"
                className="p-2 rounded-full text-white/50 hover:text-white hover:bg-white/10 transition-colors"
              >
                <X size={16} />
              </button>
            </div>

            {/* Body */}
            <div className="flex-1 overflow-auto p-5 flex items-center justify-center bg-black/50 min-h-[220px]">
              {item.kind === "file" && previewUrl && !mediaError && mime.startsWith("image/") ? (
                <div
                  className="min-w-[160px] min-h-[120px] p-4 rounded-2xl border border-white/10 flex items-center justify-center shadow-inner"
                  style={{
                    backgroundImage:
                      "linear-gradient(45deg, rgba(255,255,255,0.03) 25%, transparent 25%), linear-gradient(-45deg, rgba(255,255,255,0.03) 25%, transparent 25%), linear-gradient(45deg, transparent 75%, rgba(255,255,255,0.03) 75%), linear-gradient(-45deg, transparent 75%, rgba(255,255,255,0.03) 75%)",
                    backgroundSize: "16px 16px",
                    backgroundPosition: "0 0, 0 8px, 8px -8px, -8px 0",
                    backgroundColor: "#0b0b0d",
                  }}
                >
                  <img
                    src={previewUrl}
                    alt={item.name}
                    onError={() => setMediaError(true)}
                    className="max-h-[58vh] max-w-full min-w-[48px] min-h-[48px] rounded-lg object-contain"
                  />
                </div>
              ) : item.kind === "file" && previewUrl && !mediaError && mime.startsWith("video/") ? (
                <video
                  src={previewUrl}
                  controls
                  onError={() => setMediaError(true)}
                  className="max-h-[60vh] max-w-full rounded-xl border border-white/10 bg-black"
                />
              ) : item.kind === "file" && previewUrl && !mediaError && mime.startsWith("audio/") ? (
                <div className="w-full max-w-md p-6 rounded-2xl bg-white/[0.04] border border-white/10 space-y-4 text-center">
                  <p className="text-xs font-bold text-white/70 truncate">{item.name}</p>
                  <audio
                    src={previewUrl}
                    controls
                    onError={() => setMediaError(true)}
                    className="w-full"
                  />
                </div>
              ) : item.kind === "code" ? (
                <pre className="w-full max-h-[58vh] overflow-auto rounded-2xl bg-zinc-950 border border-white/10 p-4 text-xs font-mono text-emerald-200/90 leading-relaxed whitespace-pre-wrap break-words">
                  {item.content || ""}
                </pre>
              ) : item.kind === "note" ? (
                <div className="w-full max-w-xl rounded-2xl bg-white/[0.04] border border-white/10 p-5 text-sm text-white/90 leading-relaxed whitespace-pre-wrap break-words">
                  {item.content || ""}
                </div>
              ) : item.kind === "link" ? (
                <div className="w-full max-w-xl rounded-2xl bg-white/[0.04] border border-white/10 p-6 space-y-3 text-center">
                  <Link2 size={28} className="mx-auto text-sky-400" />
                  <p className="text-sm font-mono text-sky-300 break-all">{item.url}</p>
                  {item.content && <p className="text-xs text-white/60">{item.content}</p>}
                  <a
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-white text-black text-xs font-bold hover:bg-white/90 transition-colors"
                  >
                    Open External Link
                    <ExternalLink size={13} />
                  </a>
                </div>
              ) : (
                <div className="text-center space-y-3 py-8">
                  <div className="w-14 h-14 mx-auto rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-white/50">
                    <FileIcon size={24} />
                  </div>
                  <p className="text-xs text-white/50">
                    Inline preview is unavailable for this file type or provider.
                  </p>
                  <button
                    type="button"
                    onClick={() => onDownload(item)}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-white text-black text-xs font-bold hover:bg-white/90 transition-colors"
                  >
                    <Download size={14} />
                    Download File
                  </button>
                </div>
              )}
            </div>

            {/* Footer Actions */}
            <div className="flex flex-wrap items-center justify-between gap-2 px-5 py-3.5 border-t border-white/10 bg-[#0a0a0a]">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    if (item.kind === "file") onRename(item);
                    else onEdit(item);
                  }}
                  className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-white/80 flex items-center gap-1.5 transition-colors"
                >
                  <Edit3 size={13} />
                  {item.kind === "file" ? "Rename" : "Edit"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    onDelete(item);
                  }}
                  className="px-3.5 py-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 text-xs font-semibold text-red-400 flex items-center gap-1.5 transition-colors"
                >
                  <Trash2 size={13} />
                  Delete
                </button>
              </div>
              <div className="flex items-center gap-2">
                {(item.url || item.content) && (
                  <button
                    type="button"
                    onClick={() =>
                      onCopyText(
                        item.url || item.content || "",
                        item.kind === "file" || item.kind === "link" ? "Link copied!" : "Copied to clipboard!",
                      )
                    }
                    className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-white/80 flex items-center gap-1.5 transition-colors"
                  >
                    <Copy size={13} />
                    {item.kind === "file" || item.kind === "link" ? "Copy Link" : "Copy Text"}
                  </button>
                )}
                {item.kind === "file" && item.url && (
                  <button
                    type="button"
                    onClick={() => onDownload(item)}
                    className="px-4 py-2 rounded-xl bg-white text-black hover:bg-white/90 text-xs font-bold flex items-center gap-1.5 transition-colors"
                  >
                    <Download size={13} />
                    Download
                  </button>
                )}
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ---------------- Rename Modal ---------------- */
interface SpaceRenameModalProps {
  item: SpaceItem | null;
  onClose: () => void;
  onConfirm: (item: SpaceItem, newName: string) => void;
}

export function SpaceRenameModal({ item, onClose, onConfirm }: SpaceRenameModalProps) {
  const [value, setValue] = useState("");

  useEffect(() => {
    if (item) setValue(item.name);
  }, [item]);

  useEffect(() => {
    if (!item) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [item, onClose]);

  return (
    <AnimatePresence>
      {item && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[510] flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm"
          onMouseDown={onClose}
        >
          <motion.form
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.95, opacity: 0 }}
            onSubmit={(e) => {
              e.preventDefault();
              if (value.trim() && value.trim() !== item.name) {
                onConfirm(item, value.trim());
              }
              onClose();
            }}
            onMouseDown={(e) => e.stopPropagation()}
            className="w-full max-w-sm bg-[#0a0a0a] border border-white/15 rounded-[24px] p-5 space-y-4 shadow-2xl"
          >
            <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-white flex items-center gap-2">
              <Edit3 size={14} className="text-emerald-400" />
              Rename Item
            </h3>
            <input
              value={value}
              onChange={(e) => setValue(e.target.value)}
              autoFocus
              className="w-full bg-white/[0.05] border border-white/15 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:border-white/40"
            />
            <div className="flex gap-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-white/70"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!value.trim()}
                className="flex-1 py-2.5 rounded-xl bg-white text-black hover:bg-white/90 text-xs font-bold disabled:opacity-40"
              >
                Save
              </button>
            </div>
          </motion.form>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ---------------- Delete Confirmation Modal ---------------- */
interface SpaceDeleteModalProps {
  item: SpaceItem | null;
  onClose: () => void;
  onConfirm: (item: SpaceItem) => void;
}

export function SpaceDeleteModal({ item, onClose, onConfirm }: SpaceDeleteModalProps) {
  useEffect(() => {
    if (!item) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [item, onClose]);

  return (
    <AnimatePresence>
      {item && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[510] flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm"
          onMouseDown={onClose}
        >
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.95, opacity: 0 }}
            onMouseDown={(e) => e.stopPropagation()}
            className="w-full max-w-sm bg-[#0a0a0a] border border-white/15 rounded-[24px] p-5 space-y-4 shadow-2xl"
          >
            <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-red-400 flex items-center gap-2">
              <Trash2 size={14} />
              Remove from Space
            </h3>
            <p className="text-xs text-white/65 leading-relaxed">
              Delete <span className="text-white font-semibold">{item.name}</span> from this collaborative canvas for all members?
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-white/70"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onConfirm(item);
                  onClose();
                }}
                className="flex-1 py-2.5 rounded-xl bg-red-500 text-white hover:bg-red-400 text-xs font-bold"
              >
                Delete
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ---------------- Upload Provider Picker Modal ---------------- */
interface SpaceProviderModalProps {
  open: boolean;
  selectedProvider: string;
  onSelect: (providerId: string) => void;
  onClose: () => void;
}

export function SpaceProviderModal({
  open,
  selectedProvider,
  onSelect,
  onClose,
}: SpaceProviderModalProps) {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[500] flex items-end justify-center bg-black/75 p-3 backdrop-blur-sm sm:items-center"
          onMouseDown={onClose}
        >
          <motion.section
            initial={{ opacity: 0, y: 24, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 18, scale: 0.98 }}
            transition={{ type: "spring", damping: 25, stiffness: 330 }}
            onMouseDown={(e) => e.stopPropagation()}
            className="max-h-[84vh] w-full max-w-[600px] overflow-hidden rounded-[28px] border border-white/15 bg-[#0a0a0a] shadow-2xl"
          >
            <div className="flex items-start justify-between gap-4 border-b border-white/10 px-5 py-4">
              <div>
                <p className="text-[9px] font-black uppercase tracking-[0.22em] text-emerald-300">
                  Space Transfer Route
                </p>
                <h3 className="mt-1 text-lg font-bold tracking-tight text-white">
                  Choose an upload provider
                </h3>
                <p className="mt-1 text-[11px] text-white/45">
                  Auto-fallback is active across all eligible routes if a host blocks or throttles.
                </p>
              </div>
              <button
                type="button"
                onClick={onClose}
                aria-label="Close provider picker"
                className="grid h-9 w-9 shrink-0 place-items-center rounded-full border border-white/10 text-white/60 hover:border-white/30 hover:bg-white/10 hover:text-white"
              >
                <X size={16} />
              </button>
            </div>

            <div className="max-h-[calc(84vh-106px)] space-y-2 overflow-y-auto p-3 sm:p-4 custom-scrollbar">
              {UPLOAD_PROVIDERS.map((prov) => {
                const isSelected = selectedProvider === prov.id;
                const isExpanded = expandedId === prov.id;
                return (
                  <motion.div
                    layout
                    key={prov.id}
                    className={`overflow-hidden rounded-[19px] border transition-colors ${
                      isSelected
                        ? "border-white bg-white text-black"
                        : "border-white/10 bg-white/[0.025] text-white"
                    }`}
                  >
                    <div className="flex items-center gap-2 p-2">
                      <button
                        type="button"
                        onClick={() => {
                          onSelect(prov.id);
                          onClose();
                        }}
                        className="flex min-w-0 flex-1 items-center gap-3 rounded-[14px] px-2 py-2 text-left"
                      >
                        <span
                          className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl border ${
                            isSelected ? "border-black/15 bg-black/5" : "border-white/10 bg-black/30"
                          }`}
                        >
                          {prov.isLargeFile ? <Box size={16} /> : <FileUp size={16} />}
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="flex items-center gap-2">
                            <span className="truncate text-sm font-bold">{prov.name}</span>
                            {prov.isLargeFile && (
                              <span
                                className={`rounded-full px-1.5 py-0.5 text-[7px] font-black uppercase tracking-[0.12em] ${
                                  isSelected ? "bg-black/10 text-black/60" : "bg-emerald-400/10 text-emerald-300"
                                }`}
                              >
                                Large files
                              </span>
                            )}
                          </span>
                          <span
                            className={`mt-0.5 block font-mono text-[10px] ${
                              isSelected ? "text-black/55" : "text-white/40"
                            }`}
                          >
                            {prov.maxSizeLabel}
                          </span>
                        </span>
                        {isSelected && (
                          <span className="grid h-5 w-5 place-items-center rounded-full bg-black text-white">
                            <Check size={11} strokeWidth={3} />
                          </span>
                        )}
                      </button>
                      <button
                        type="button"
                        onClick={() => setExpandedId(isExpanded ? null : prov.id)}
                        aria-label={`Show ${prov.name} details`}
                        className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl border transition-colors ${
                          isSelected
                            ? "border-black/15 text-black/60 hover:bg-black/10"
                            : "border-white/10 text-white/50 hover:bg-white/10 hover:text-white"
                        }`}
                      >
                        <motion.span animate={{ rotate: isExpanded ? 180 : 0 }}>
                          <ChevronDown size={15} />
                        </motion.span>
                      </button>
                    </div>
                    <AnimatePresence initial={false}>
                      {isExpanded && (
                        <motion.div
                          initial={{ height: 0, opacity: 0 }}
                          animate={{ height: "auto", opacity: 1 }}
                          exit={{ height: 0, opacity: 0 }}
                          transition={{ duration: 0.16 }}
                          className={`overflow-hidden border-t ${
                            isSelected ? "border-black/10" : "border-white/10"
                          }`}
                        >
                          <div
                            className={`grid gap-2 px-4 py-3 text-[10px] leading-relaxed sm:grid-cols-[94px_1fr] ${
                              isSelected ? "text-black/65" : "text-white/55"
                            }`}
                          >
                            <span className="font-black uppercase tracking-[0.13em] opacity-60">Expiry</span>
                            <span>{prov.expiry}</span>
                            <span className="font-black uppercase tracking-[0.13em] opacity-60">Delivery</span>
                            <span>{prov.speed}</span>
                            <span className="font-black uppercase tracking-[0.13em] opacity-60">Notes</span>
                            <span>{prov.features}</span>
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </motion.div>
                );
              })}
            </div>
          </motion.section>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
