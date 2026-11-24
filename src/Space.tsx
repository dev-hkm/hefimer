import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  child,
  get,
  onDisconnect,
  onValue,
  ref,
  remove,
  serverTimestamp,
  set,
  update,
} from "firebase/database";
import { rtdb } from "./firebase";
import { downloadFileInBrowser, getDirectDownloadMode } from "./downloads";
import { UPLOAD_PROVIDERS, getProviderConfig, uploadFileWithFallback } from "./providers";
import { SpaceCanvas } from "./space/SpaceCanvas";
import { SpaceCreatedView, SpaceLobby } from "./space/SpaceLobby";
import {
  SpaceComposerModal,
  SpaceDeleteModal,
  SpaceProviderModal,
  SpacePreviewModal,
  SpaceRenameModal,
  SpaceShareModal,
} from "./space/SpaceModals";
import {
  computeAutoArrangeLayout,
  getUserColor,
  normalizeLegacyFile,
  normalizeSpaceItem,
  parseSpaceDuration,
  stripUndefined,
  type NoteColor,
  type RecentSpaceEntry,
  type SpaceCursor,
  type SpaceItem,
  type SpaceMember,
  type SpaceProps,
  type SpaceUploadTask,
} from "./space/types";

const USERNAME_STORAGE_KEY = "hefimer_space_username";
const RECENT_SPACES_KEY = "hefimer_recent_spaces";
const SPACE_PROVIDER_KEY = "hefimer_space_provider";
const CLIENT_ID_KEY = "hefimer_space_client_id";

function getOrCreateClientId(): string {
  try {
    const existing = sessionStorage.getItem(CLIENT_ID_KEY);
    if (existing) return existing;
    const next = `sp_${crypto.randomUUID().replace(/-/g, "").slice(0, 14)}`;
    sessionStorage.setItem(CLIENT_ID_KEY, next);
    return next;
  } catch {
    return `sp_${Math.random().toString(36).slice(2, 12)}`;
  }
}

function loadRecentSpaces(): RecentSpaceEntry[] {
  try {
    const raw = localStorage.getItem(RECENT_SPACES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((x) => x && typeof x.code === "string" && x.code.length === 5)
      .slice(0, 5);
  } catch {
    return [];
  }
}

function saveRecentSpace(code: string, name: string): RecentSpaceEntry[] {
  const existing = loadRecentSpaces().filter((x) => x.code !== code);
  const updated = [{ code, name: name || `Space ${code}`, visitedAt: Date.now() }, ...existing].slice(0, 5);
  try {
    localStorage.setItem(RECENT_SPACES_KEY, JSON.stringify(updated));
  } catch {
    // ignore storage quota errors
  }
  return updated;
}

export function Space({
  showToast,
  addToHistory,
  state,
  setState,
  isAdmin,
  isFrozen,
}: SpaceProps) {
  const { spaceId, spaceName, spaceNameInput, spaceExpire, userName, inSpace, showCreatedModal } = state;

  const clientIdRef = useRef<string>(getOrCreateClientId());
  const showToastRef = useRef(showToast);
  showToastRef.current = showToast;
  const userNameRef = useRef(userName);
  userNameRef.current = userName;

  const [loading, setLoading] = useState(false);
  const [spaceExpiresAt, setSpaceExpiresAt] = useState(0);
  const [spaceItems, setSpaceItems] = useState<SpaceItem[]>([]);
  const [legacyFiles, setLegacyFiles] = useState<SpaceItem[]>([]);
  const [members, setMembers] = useState<SpaceMember[]>([]);
  const [cursors, setCursors] = useState<SpaceCursor[]>([]);
  const [recentSpaces, setRecentSpaces] = useState<RecentSpaceEntry[]>(() => loadRecentSpaces());

  // Create & Join UI state
  const [customCode, setCustomCode] = useState(() =>
    Math.floor(10000 + Math.random() * 90000).toString(),
  );
  const [checkingCode, setCheckingCode] = useState(false);
  const [codeStatus, setCodeStatus] = useState<"available" | "taken" | "idle">("idle");
  const [createPassword, setCreatePassword] = useState("");
  const [joinPassword, setJoinPassword] = useState("");
  const [joinRequiresPassword, setJoinRequiresPassword] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  // Provider & Upload Queue state
  const [selectedProvider, setSelectedProvider] = useState<string>(() => {
    try {
      const saved = localStorage.getItem(SPACE_PROVIDER_KEY);
      if (saved && UPLOAD_PROVIDERS.some((p) => p.id === saved)) return saved;
    } catch {
      // ignore
    }
    return "stocare";
  });
  const [uploadTasks, setUploadTasks] = useState<SpaceUploadTask[]>([]);
  const activeXhrRef = useRef<XMLHttpRequest | null>(null);
  const uploadCancelledRef = useRef(false);

  // Modals state
  const [showShareModal, setShowShareModal] = useState(false);
  const [showProviderModal, setShowProviderModal] = useState(false);
  const [composerState, setComposerState] = useState<{
    open: boolean;
    mode: "note" | "link" | "code";
    pos: { x: number; y: number };
    editing: SpaceItem | null;
  }>({
    open: false,
    mode: "note",
    pos: { x: 160, y: 160 },
    editing: null,
  });
  const [previewItem, setPreviewItem] = useState<SpaceItem | null>(null);
  const [renameItem, setRenameItem] = useState<SpaceItem | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<SpaceItem | null>(null);

  const lastCursorSentRef = useRef(0);

  // Load saved username once if empty
  useEffect(() => {
    if (userName) return;
    try {
      const saved = localStorage.getItem(USERNAME_STORAGE_KEY);
      if (saved) {
        setState((prev) => (prev.userName ? prev : { ...prev, userName: saved }));
      }
    } catch {
      // ignore
    }
  }, [userName, setState]);

  // Persist username changes
  useEffect(() => {
    if (!userName.trim()) return;
    try {
      localStorage.setItem(USERNAME_STORAGE_KEY, userName.trim());
    } catch {
      // ignore
    }
  }, [userName]);

  // Persist selected provider
  useEffect(() => {
    try {
      localStorage.setItem(SPACE_PROVIDER_KEY, selectedProvider);
    } catch {
      // ignore
    }
  }, [selectedProvider]);

  // Check custom 5-digit code availability
  useEffect(() => {
    if (inSpace || showCreatedModal) return;
    if (customCode.length !== 5) {
      setCodeStatus("idle");
      return;
    }
    let cancelled = false;
    const timer = setTimeout(async () => {
      setCheckingCode(true);
      try {
        const snapshot = await get(child(ref(rtdb), `spaces/${customCode}`));
        if (cancelled) return;
        if (snapshot.exists()) {
          const data = snapshot.val();
          if (data.expiresAt && data.expiresAt < Date.now()) {
            setCodeStatus("available");
          } else {
            setCodeStatus("taken");
          }
        } else {
          setCodeStatus("available");
        }
      } catch {
        if (!cancelled) setCodeStatus("available");
      } finally {
        if (!cancelled) setCheckingCode(false);
      }
    }, 280);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [customCode, inSpace, showCreatedModal]);

  // Reset join password prompt when code changes
  useEffect(() => {
    setJoinRequiresPassword(false);
    setJoinPassword("");
  }, [spaceId]);

  // Live RTDB subscriptions while in Space (depends ONLY on inSpace & spaceId)
  useEffect(() => {
    if (!inSpace || !spaceId) return;

    const clientId = clientIdRef.current;
    const displayName = userNameRef.current.trim() || "Guest";
    const userColor = getUserColor(displayName + clientId);

    const spaceRef = ref(rtdb, `spaces/${spaceId}`);
    const itemsRef = ref(rtdb, `spaces/${spaceId}/items`);
    const legacyFilesRef = ref(rtdb, `spaces/${spaceId}/files`);
    const membersRef = ref(rtdb, `spaces/${spaceId}/members`);
    const myMemberRef = ref(rtdb, `spaces/${spaceId}/members/${clientId}`);
    const cursorsRef = ref(rtdb, `spaces/${spaceId}/cursors`);
    const myCursorRef = ref(rtdb, `spaces/${spaceId}/cursors/${clientId}`);

    const unsubSpace = onValue(spaceRef, (snapshot) => {
      if (!snapshot.exists()) {
        showToastRef.current("Space not found or deleted", "error");
        setState((prev) => ({ ...prev, inSpace: false }));
        return;
      }
      const data = snapshot.val();
      setState((prev) => (prev.spaceName === (data.name || "") ? prev : { ...prev, spaceName: data.name || "" }));
      if (data.expiresAt) {
        setSpaceExpiresAt(Number(data.expiresAt));
        if (Number(data.expiresAt) < Date.now()) {
          showToastRef.current("Space has expired", "error");
          setState((prev) => ({ ...prev, inSpace: false }));
        }
      }
    });

    set(myMemberRef, {
      name: displayName,
      color: userColor,
      joinedAt: serverTimestamp(),
    }).catch(() => {});
    onDisconnect(myMemberRef).remove().catch(() => {});
    onDisconnect(myCursorRef).remove().catch(() => {});

    const unsubItems = onValue(itemsRef, (snapshot) => {
      const data = snapshot.val();
      if (!data) {
        setSpaceItems([]);
        return;
      }
      setSpaceItems(
        Object.entries(data).map(([id, val]) => normalizeSpaceItem(id, val)),
      );
    });

    const unsubLegacyFiles = onValue(legacyFilesRef, (snapshot) => {
      const data = snapshot.val();
      if (!data) {
        setLegacyFiles([]);
        return;
      }
      setLegacyFiles(
        Object.entries(data).map(([id, val]) => normalizeLegacyFile(id, val)),
      );
    });

    const unsubMembers = onValue(membersRef, (snapshot) => {
      const data = snapshot.val();
      if (!data) {
        setMembers([]);
        return;
      }
      const list: SpaceMember[] = Object.entries(data).map(([id, val]: [string, any]) => ({
        id,
        name: typeof val?.name === "string" && val.name ? val.name : "Guest",
        color: typeof val?.color === "string" ? val.color : getUserColor(id),
        joinedAt: Number(val?.joinedAt) || 0,
      }));
      setMembers(list);
    });

    const unsubCursors = onValue(cursorsRef, (snapshot) => {
      const data = snapshot.val();
      if (!data) {
        setCursors([]);
        return;
      }
      const list: SpaceCursor[] = Object.entries(data).map(([id, val]: [string, any]) => ({
        id,
        name: typeof val?.name === "string" ? val.name : "Guest",
        color: typeof val?.color === "string" ? val.color : getUserColor(id),
        x: Number(val?.x) || 0,
        y: Number(val?.y) || 0,
        updatedAt: Number(val?.updatedAt) || 0,
      }));
      setCursors(list);
    });

    return () => {
      unsubSpace();
      unsubItems();
      unsubLegacyFiles();
      unsubMembers();
      unsubCursors();
      remove(myMemberRef).catch(() => {});
      remove(myCursorRef).catch(() => {});
    };
  }, [inSpace, spaceId, setState]);

  // Keep member display name in sync if changed
  useEffect(() => {
    if (!inSpace || !spaceId) return;
    const displayName = userName.trim() || "Guest";
    const clientId = clientIdRef.current;
    update(ref(rtdb, `spaces/${spaceId}/members/${clientId}`), {
      name: displayName,
      color: getUserColor(displayName + clientId),
    }).catch(() => {});
  }, [inSpace, spaceId, userName]);

  // Active expiry monitor
  useEffect(() => {
    if (!inSpace || !spaceExpiresAt) return;
    const timer = setInterval(() => {
      if (Date.now() >= spaceExpiresAt) {
        showToastRef.current("Space has expired", "error");
        setState((prev) => ({ ...prev, inSpace: false }));
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [inSpace, spaceExpiresAt, setState]);

  const mergedItems = useMemo(() => {
    const byId = new Map<string, SpaceItem>();
    for (const f of legacyFiles) byId.set(f.id, f);
    for (const item of spaceItems) byId.set(item.id, item);
    return Array.from(byId.values());
  }, [legacyFiles, spaceItems]);

  const handleLeaveSpace = useCallback(() => {
    const clientId = clientIdRef.current;
    if (spaceId) {
      remove(ref(rtdb, `spaces/${spaceId}/members/${clientId}`)).catch(() => {});
      remove(ref(rtdb, `spaces/${spaceId}/cursors/${clientId}`)).catch(() => {});
    }
    setSpaceItems([]);
    setLegacyFiles([]);
    setMembers([]);
    setCursors([]);
    setSpaceExpiresAt(0);
    setState((prev) => ({ ...prev, inSpace: false, showCreatedModal: false }));
  }, [spaceId, setState]);

  const handleCreateSpace = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (isFrozen && !isAdmin) {
      showToast("System is currently frozen", "error");
      return;
    }
    if (!userName.trim()) {
      showToast("Please enter your display name", "error");
      return;
    }
    if (!spaceNameInput.trim()) {
      showToast("Please enter a space title", "error");
      return;
    }
    if (customCode.length !== 5) {
      showToast("Space code must be exactly 5 digits", "error");
      return;
    }
    if (codeStatus === "taken") {
      showToast("Space code is already in use", "error");
      return;
    }

    setLoading(true);
    try {
      const roomRef = ref(rtdb, `spaces/${customCode}`);
      const snapshot = await get(roomRef);
      if (snapshot.exists()) {
        const data = snapshot.val();
        if (data.expiresAt && data.expiresAt >= Date.now()) {
          showToast("Space code is already taken", "error");
          setCodeStatus("taken");
          return;
        }
      }

      const duration = parseSpaceDuration(spaceExpire);
      const expiresAt = Date.now() + duration;
      const trimmedPass = createPassword.trim();

      await set(
        roomRef,
        stripUndefined({
          createdAt: serverTimestamp(),
          creator: userName.trim(),
          name: spaceNameInput.trim(),
          expiresAt,
          password: trimmedPass || undefined,
        }),
      );

      setSpaceExpiresAt(expiresAt);
      setRecentSpaces(saveRecentSpace(customCode, spaceNameInput.trim()));
      setState((prev) => ({
        ...prev,
        spaceId: customCode,
        spaceName: spaceNameInput.trim(),
        showCreatedModal: true,
      }));

      addToHistory?.({
        code: customCode,
        roomName: spaceNameInput.trim(),
        type: "space",
        action: "created",
      });

      showToast("Space created!", "success");
    } catch {
      showToast("Failed to create space", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleJoinSpace = async (e?: React.FormEvent, codeOverride?: string) => {
    e?.preventDefault();
    const targetCode = (codeOverride || spaceId).trim();
    if (!userName.trim()) {
      showToast("Please enter your display name first", "error");
      return;
    }
    if (targetCode.length !== 5) {
      showToast("Space code must be 5 digits", "error");
      return;
    }

    setLoading(true);
    try {
      const snapshot = await get(child(ref(rtdb), `spaces/${targetCode}`));
      if (!snapshot.exists()) {
        showToast("Space not found", "error");
        return;
      }
      const data = snapshot.val();
      if (data.expiresAt && Number(data.expiresAt) < Date.now()) {
        showToast("Space has expired", "error");
        return;
      }

      if (data.password && !isAdmin) {
        if (!joinRequiresPassword || !joinPassword) {
          setJoinRequiresPassword(true);
          showToast("This space requires a passcode", "info");
          return;
        }
        if (joinPassword !== data.password) {
          showToast("Incorrect space passcode", "error");
          return;
        }
      }

      setSpaceExpiresAt(Number(data.expiresAt) || 0);
      setRecentSpaces(saveRecentSpace(targetCode, data.name || `Space ${targetCode}`));
      setState((prev) => ({
        ...prev,
        spaceId: targetCode,
        inSpace: true,
        showCreatedModal: false,
        spaceName: data.name || "",
      }));

      addToHistory?.({
        code: targetCode,
        roomName: data.name || "",
        type: "space",
        action: "joined",
      });
      showToast("Joined space", "success");
    } catch {
      showToast("Failed to join space", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleUploadFiles = useCallback(
    async (filesToUpload: File[], startPos: { x: number; y: number }) => {
      if (!spaceId || filesToUpload.length === 0) return;

      uploadCancelledRef.current = false;
      const queued: SpaceUploadTask[] = filesToUpload.map((file, idx) => ({
        id: `up_${Date.now()}_${idx}`,
        fileName: file.name,
        progress: 0,
        status: idx === 0 ? "Starting upload..." : "Queued...",
        providerName: getProviderConfig(selectedProvider).name,
      }));
      setUploadTasks((prev) => [...prev, ...queued]);

      for (let i = 0; i < filesToUpload.length; i++) {
        if (uploadCancelledRef.current) break;
        const file = filesToUpload[i];
        const taskId = queued[i].id;

        const updateTask = (patch: Partial<SpaceUploadTask>) => {
          setUploadTasks((prev) =>
            prev.map((t) => (t.id === taskId ? { ...t, ...patch } : t)),
          );
        };

        try {
          const result = await uploadFileWithFallback(
            selectedProvider,
            file,
            file.name,
            {
              expire: spaceExpire || "24h",
              onProgress: (pct) => updateTask({ progress: pct }),
              onStatus: (msg) => updateTask({ status: msg }),
              bindXhr: (xhr) => {
                activeXhrRef.current = xhr;
                return xhr;
              },
              clearActiveXhr: (xhr) => {
                if (!xhr || activeXhrRef.current === xhr) {
                  activeXhrRef.current = null;
                }
              },
              isCancelled: () => uploadCancelledRef.current,
            },
            (fromName, toName) => {
              updateTask({ providerName: toName, status: `Switching from ${fromName} to ${toName}...` });
              showToastRef.current(`${fromName} unavailable, switching to ${toName}...`, "info");
            },
          );

          const itemId = crypto.randomUUID().replace(/-/g, "").slice(0, 12);
          const nextZ = mergedItems.reduce((m, it) => Math.max(m, it.zIndex || 1), 1) + 1 + i;

          await set(
            ref(rtdb, `spaces/${spaceId}/items/${itemId}`),
            stripUndefined({
              kind: "file",
              name: file.name,
              size: file.size,
              mimeType: file.type || "application/octet-stream",
              url: result.fileUrl,
              providerName: result.providerName,
              uploadedBy: userNameRef.current.trim() || "Guest",
              timestamp: Date.now(),
              x: startPos.x + i * 36,
              y: startPos.y + i * 36,
              zIndex: nextZ,
            }),
          );
          showToastRef.current(`Uploaded ${file.name} via ${result.providerName}`, "success");
        } catch (err: any) {
          if (err?.message === "UPLOAD_CANCELED" || uploadCancelledRef.current) {
            showToastRef.current("Upload canceled", "info");
            break;
          }
          showToastRef.current(err?.message || `Failed to upload ${file.name}`, "error");
        } finally {
          setUploadTasks((prev) => prev.filter((t) => t.id !== taskId));
        }
      }
      setUploadTasks([]);
    },
    [spaceId, selectedProvider, spaceExpire, mergedItems],
  );

  const handleCancelUpload = useCallback(() => {
    uploadCancelledRef.current = true;
    if (activeXhrRef.current) {
      activeXhrRef.current.abort();
      activeXhrRef.current = null;
    }
    setUploadTasks([]);
  }, []);

  const handleMoveItem = useCallback(
    (item: SpaceItem, x: number, y: number, zIndex?: number, final = false) => {
      if (!spaceId) return;
      const node = item.sourceNode === "files" ? "files" : "items";
      const patch = stripUndefined({ x, y, zIndex });
      if (final || Object.keys(patch).length > 0) {
        update(ref(rtdb, `spaces/${spaceId}/${node}/${item.id}`), patch).catch(() => {});
      }
    },
    [spaceId],
  );

  const handleComposerSubmit = async (payload: {
    kind: "note" | "link" | "code";
    name: string;
    content?: string;
    url?: string;
    noteColor?: NoteColor;
  }) => {
    if (!spaceId) return;
    const editing = composerState.editing;
    if (editing) {
      await update(
        ref(rtdb, `spaces/${spaceId}/${editing.sourceNode}/${editing.id}`),
        stripUndefined({
          name: payload.name,
          content: payload.content,
          url: payload.url,
          noteColor: payload.noteColor,
        }),
      );
      showToast("Updated item", "success");
      return;
    }

    const itemId = crypto.randomUUID().replace(/-/g, "").slice(0, 12);
    const nextZ = mergedItems.reduce((m, it) => Math.max(m, it.zIndex || 1), 1) + 1;
    await set(
      ref(rtdb, `spaces/${spaceId}/items/${itemId}`),
      stripUndefined({
        kind: payload.kind,
        name: payload.name,
        content: payload.content,
        url: payload.url,
        noteColor: payload.noteColor,
        uploadedBy: userName.trim() || "Guest",
        timestamp: Date.now(),
        x: composerState.pos.x,
        y: composerState.pos.y,
        zIndex: nextZ,
      }),
    );
    showToast("Placed on canvas", "success");
  };

  const handleCreatePastedItem = useCallback(
    async (payload: {
      kind: "note" | "link" | "code";
      name: string;
      content?: string;
      url?: string;
      x: number;
      y: number;
    }) => {
      if (!spaceId) return;
      const itemId = crypto.randomUUID().replace(/-/g, "").slice(0, 12);
      const nextZ = mergedItems.reduce((m, it) => Math.max(m, it.zIndex || 1), 1) + 1;
      await set(
        ref(rtdb, `spaces/${spaceId}/items/${itemId}`),
        stripUndefined({
          kind: payload.kind,
          name: payload.name,
          content: payload.content,
          url: payload.url,
          noteColor: payload.kind === "note" ? "zinc" : undefined,
          uploadedBy: userNameRef.current.trim() || "Guest",
          timestamp: Date.now(),
          x: payload.x,
          y: payload.y,
          zIndex: nextZ,
        }),
      );
      showToastRef.current(`Pasted ${payload.kind} onto canvas`, "success");
    },
    [spaceId, mergedItems],
  );

  const handleRenameConfirm = async (item: SpaceItem, newName: string) => {
    if (!spaceId) return;
    await update(ref(rtdb, `spaces/${spaceId}/${item.sourceNode}/${item.id}`), {
      name: newName,
    });
    showToast("Renamed", "success");
  };

  const handleDeleteConfirm = async (item: SpaceItem) => {
    if (!spaceId) return;
    await remove(ref(rtdb, `spaces/${spaceId}/${item.sourceNode}/${item.id}`));
    showToast("Removed from space", "success");
  };

  const handleAutoArrange = async () => {
    if (!spaceId || mergedItems.length === 0) return;
    const layout = computeAutoArrangeLayout(mergedItems);
    const updates: Record<string, any> = {};
    for (const entry of layout) {
      updates[`spaces/${spaceId}/${entry.sourceNode}/${entry.id}/x`] = entry.x;
      updates[`spaces/${spaceId}/${entry.sourceNode}/${entry.id}/y`] = entry.y;
    }
    await update(ref(rtdb), updates);
    showToast("Canvas auto-arranged", "success");
  };

  const handleDownloadItem = async (item: SpaceItem) => {
    if (!item.url) return;
    const mode = getDirectDownloadMode(item.url);
    showToast(mode === "tab" ? "Opening provider page..." : `Downloading ${item.name}...`, "info");
    await downloadFileInBrowser(item.url, item.name);
  };

  const handleDownloadAllFiles = async () => {
    const fileItems = mergedItems.filter((i) => i.kind === "file" && i.url);
    if (fileItems.length === 0) return;
    showToast(`Starting download for ${fileItems.length} file(s)...`, "info");
    for (let i = 0; i < fileItems.length; i++) {
      await downloadFileInBrowser(fileItems[i].url!, fileItems[i].name);
      if (i < fileItems.length - 1) {
        await new Promise((r) => setTimeout(r, 550));
      }
    }
  };

  const handleCursorMove = useCallback(
    (x: number, y: number) => {
      if (!inSpace || !spaceId) return;
      const now = Date.now();
      if (now - lastCursorSentRef.current < 60) return;
      lastCursorSentRef.current = now;
      const clientId = clientIdRef.current;
      const displayName = userNameRef.current.trim() || "Guest";
      set(ref(rtdb, `spaces/${spaceId}/cursors/${clientId}`), {
        name: displayName,
        color: getUserColor(displayName + clientId),
        x,
        y,
        updatedAt: now,
      }).catch(() => {});
    },
    [inSpace, spaceId],
  );

  const copyCode = () => {
    navigator.clipboard.writeText(spaceId);
    setCopiedCode(true);
    showToast("Code copied to clipboard!", "success");
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const copyLink = () => {
    navigator.clipboard.writeText(`${window.location.origin}/space?code=${spaceId}`);
    setCopiedLink(true);
    showToast("Link copied to clipboard!", "success");
    setTimeout(() => setCopiedLink(false), 2000);
  };

  return (
    <>
      {showCreatedModal && !inSpace ? (
        <SpaceCreatedView
          spaceId={spaceId}
          spaceName={spaceName}
          copiedCode={copiedCode}
          copiedLink={copiedLink}
          onCopyCode={copyCode}
          onCopyLink={copyLink}
          onOpenQR={() => setShowShareModal(true)}
          onEnterSpace={() =>
            setState((prev) => ({ ...prev, showCreatedModal: false, inSpace: true }))
          }
          onBack={() => setState((prev) => ({ ...prev, showCreatedModal: false }))}
        />
      ) : !inSpace ? (
        <SpaceLobby
          state={state}
          setState={setState}
          customCode={customCode}
          onCustomCodeChange={(val) => setCustomCode(val.replace(/\D/g, "").slice(0, 5))}
          onRandomizeCode={() =>
            setCustomCode(Math.floor(10000 + Math.random() * 90000).toString())
          }
          checkingCode={checkingCode}
          codeStatus={codeStatus}
          loading={loading}
          recentSpaces={recentSpaces}
          createPassword={createPassword}
          onCreatePasswordChange={setCreatePassword}
          joinPassword={joinPassword}
          onJoinPasswordChange={setJoinPassword}
          joinRequiresPassword={joinRequiresPassword}
          onCreateSpace={handleCreateSpace}
          onJoinSpace={handleJoinSpace}
          onSelectRecent={(entry) => {
            setState((prev) => ({ ...prev, spaceId: entry.code }));
            if (userName.trim()) {
              handleJoinSpace(undefined, entry.code);
            }
          }}
        />
      ) : (
        <SpaceCanvas
          spaceId={spaceId}
          spaceName={spaceName}
          expiresAt={spaceExpiresAt}
          items={mergedItems}
          members={members}
          cursors={cursors}
          clientId={clientIdRef.current}
          selectedProviderId={selectedProvider}
          uploadTasks={uploadTasks}
          onLeaveSpace={handleLeaveSpace}
          onOpenShare={() => setShowShareModal(true)}
          onOpenProviderPicker={() => setShowProviderModal(true)}
          onOpenComposer={(mode, pos, editing) =>
            setComposerState({
              open: true,
              mode,
              pos: pos || { x: 180, y: 180 },
              editing: editing || null,
            })
          }
          onOpenPreview={(item) => setPreviewItem(item)}
          onOpenRename={(item) => setRenameItem(item)}
          onOpenDelete={(item) => setDeleteTarget(item)}
          onUploadFiles={handleUploadFiles}
          onCancelUpload={handleCancelUpload}
          onMoveItem={handleMoveItem}
          onCreatePastedItem={handleCreatePastedItem}
          onAutoArrange={handleAutoArrange}
          onDownloadItem={handleDownloadItem}
          onDownloadAllFiles={handleDownloadAllFiles}
          onCursorMove={handleCursorMove}
          showToast={showToast}
        />
      )}

      {/* Modals rendered globally regardless of Lobby / Active Canvas */}
      <SpaceShareModal
        open={showShareModal}
        onClose={() => setShowShareModal(false)}
        spaceId={spaceId}
        spaceName={spaceName}
        expiresAt={spaceExpiresAt}
        showToast={showToast}
      />

      <SpaceComposerModal
        open={composerState.open}
        mode={composerState.mode}
        editingItem={composerState.editing}
        onClose={() => setComposerState((prev) => ({ ...prev, open: false, editing: null }))}
        onSubmit={handleComposerSubmit}
      />

      <SpacePreviewModal
        item={previewItem}
        onClose={() => setPreviewItem(null)}
        onDownload={handleDownloadItem}
        onCopyText={(text, label) => {
          navigator.clipboard.writeText(text);
          showToast(label || "Copied!", "success");
        }}
        onEdit={(item) => {
          if (item.kind === "note" || item.kind === "link" || item.kind === "code") {
            setComposerState({
              open: true,
              mode: item.kind,
              pos: { x: item.x, y: item.y },
              editing: item,
            });
          }
        }}
        onRename={(item) => setRenameItem(item)}
        onDelete={(item) => setDeleteTarget(item)}
      />

      <SpaceRenameModal
        item={renameItem}
        onClose={() => setRenameItem(null)}
        onConfirm={handleRenameConfirm}
      />

      <SpaceDeleteModal
        item={deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
      />

      <SpaceProviderModal
        open={showProviderModal}
        selectedProvider={selectedProvider}
        onSelect={(provId) => setSelectedProvider(provId)}
        onClose={() => setShowProviderModal(false)}
      />
    </>
  );
}
