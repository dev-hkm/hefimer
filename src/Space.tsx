import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "motion/react";
import { rtdb, auth } from "./firebase";
import { ref, set, get, child, serverTimestamp, onValue, remove, onDisconnect } from "firebase/database";
import { Users, Copy, Check, Clock, Plus, Download, Trash2, Edit3, Loader2, QrCode, File, Image as ImageIcon, Video, Music, Archive, ArrowLeft, ArrowUp, ArrowRight, Zap, Hash, LogIn, Link, X, Box, User } from "lucide-react";
import QRCode from "qrcode";
import { apiUrl as resolveApiUrl } from "./api-url";

export function Space({ showToast, addToHistory, state, setState, isAdmin, isFrozen }: any) {
  const {
    spaceId,
    spaceName,
    spaceNameInput,
    spaceExpire,
    userName,
    inSpace,
    showCreatedModal,
  } = state;

  const [loading, setLoading] = useState(false);
  const [files, setFiles] = useState<any[]>([]);
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [showQR, setShowQR] = useState(false);
  const [downloadQRFn, setDownloadQRFn] = useState<(() => void) | null>(null);
  const [memberCount, setMemberCount] = useState(0);
  
  const [customCode, setCustomCode] = useState("");
  const [checkingCode, setCheckingCode] = useState(false);
  const [codeStatus, setCodeStatus] = useState<"available" | "taken" | "idle">("idle");
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadPos, setUploadPos] = useState({ x: 100, y: 100 });
  const [uploadProgress, setUploadProgress] = useState(0);

  // Grid dragging state
  const containerRef = useRef<HTMLDivElement>(null);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState(false);
  const lastMousePos = useRef({ x: 0, y: 0 });
  const hasDragged = useRef(false);
  const draggingCardRef = useRef<string | null>(null);

  const vibrate = () => {}; // Stub for vibrate

  useEffect(() => {
    const initialCode = Math.floor(10000 + Math.random() * 90000).toString();
    setCustomCode(initialCode);
  }, []);

  useEffect(() => {
    if (customCode.length !== 5) {
      setCodeStatus("idle");
      return;
    }
    const checkCodeAvailability = async () => {
      setCheckingCode(true);
      try {
        const snapshot = await get(child(ref(rtdb), `spaces/${customCode}`));
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
      } catch (err) {
        setCodeStatus("available");
      } finally {
        setCheckingCode(false);
      }
    };
    const delayDebounce = setTimeout(checkCodeAvailability, 300);
    return () => clearTimeout(delayDebounce);
  }, [customCode]);

  const generateRandomCustomCode = () => {
    vibrate();
    const newCode = Math.floor(10000 + Math.random() * 90000).toString();
    setCustomCode(newCode);
  };

  const handleCustomCodeChange = (val: string) => {
    const cleaned = val.replace(/\D/g, "").slice(0, 5);
    setCustomCode(cleaned);
  };

  useEffect(() => {
    if (inSpace && spaceId) {
      const spaceRef = ref(rtdb, `spaces/${spaceId}`);
      const filesRef = ref(rtdb, `spaces/${spaceId}/files`);
      const membersRef = ref(rtdb, `spaces/${spaceId}/members`);
      const userMemberRef = ref(
        rtdb,
        `spaces/${spaceId}/members/${auth.currentUser?.uid || "anon" + Math.random().toString(36).substr(2, 9)}`,
      );

      const unsubscribeSpace = onValue(spaceRef, (snapshot) => {
        if (snapshot.exists()) {
          const data = snapshot.val();
          setState((prev: any) => ({ ...prev, spaceName: data.name || "" }));
          if (data.expiresAt && data.expiresAt < Date.now()) {
            showToast("Space has expired", "error");
            setState((prev: any) => ({ ...prev, inSpace: false }));
          }
        } else {
          showToast("Space not found", "error");
          setState((prev: any) => ({ ...prev, inSpace: false }));
        }
      });

      set(userMemberRef, { name: userName, joinedAt: serverTimestamp() });
      onDisconnect(userMemberRef).remove();

      const unsubscribeFiles = onValue(filesRef, (snapshot) => {
        const data = snapshot.val();
        if (data) {
          const list = Object.entries(data).map(([id, val]: any) => ({
            id,
            ...val,
          }));
          setFiles(list);
        } else {
          setFiles([]);
        }
      });

      const unsubscribeMembers = onValue(membersRef, (snapshot) => {
        setMemberCount(snapshot.size);
      });

      return () => {
        unsubscribeSpace();
        unsubscribeFiles();
        unsubscribeMembers();
        remove(userMemberRef);
      };
    }
  }, [inSpace, spaceId, userName, setState, showToast]);

  const handleCreateSpace = async (e?: React.FormEvent) => {
    e?.preventDefault();
    vibrate();
    if (isFrozen && !isAdmin) {
      showToast("System is currently frozen", "error");
      return;
    }
    if (!userName.trim()) {
      showToast("Please enter your name", "error");
      return;
    }
    if (!spaceNameInput.trim()) {
      showToast("Please enter a space name", "error");
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
          setLoading(false);
          return;
        }
      }

      let duration = 60 * 60 * 1000;
      const match = spaceExpire?.match(/^(\d+)([mh])$/);
      if (match) {
        const val = parseInt(match[1]);
        const unit = match[2];
        duration = unit === "m" ? val * 60 * 1000 : val * 60 * 60 * 1000;
      }
      const expiresAt = Date.now() + duration;

      await set(roomRef, {
        createdAt: serverTimestamp(),
        creator: userName,
        name: spaceNameInput.trim(),
        expiresAt,
      });

      setState((prev: any) => ({
        ...prev,
        spaceId: customCode,
        spaceName: spaceNameInput.trim(),
        showCreatedModal: true,
      }));

      if (addToHistory) {
        addToHistory({
          code: customCode,
          roomName: spaceNameInput.trim(),
          type: "space",
          action: "sent",
        });
      }

      showToast("Space created!", "success");
    } catch (err) {
      showToast("Failed to create space", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleJoinSpace = async (e?: React.FormEvent) => {
    e?.preventDefault();
    vibrate();
    if (!userName.trim() || !spaceId.trim()) {
      showToast("Please enter name and space code", "error");
      return;
    }
    if (spaceId.length !== 5) {
      showToast("Space code must be 5 digits", "error");
      return;
    }
    setLoading(true);
    try {
      const snapshot = await get(child(ref(rtdb), `spaces/${spaceId}`));
      if (!snapshot.exists()) {
        showToast("Space not found", "error");
        return;
      }
      const data = snapshot.val();
      if (data.expiresAt && data.expiresAt < Date.now()) {
        showToast("Space has expired", "error");
        return;
      }

      setState((prev: any) => ({
        ...prev,
        inSpace: true,
        spaceName: data.name,
      }));

      if (addToHistory) {
        addToHistory({
          code: spaceId,
          roomName: data.name,
          type: "space",
          action: "received",
        });
      }
      showToast("Joined space", "success");
    } catch (err) {
      showToast("Failed to join space", "error");
    } finally {
      setLoading(false);
    }
  };

  const generateQR = async () => {
    try {
      const shareUrl = `${window.location.origin}/space?code=${spaceId}`;
      const qrDataUrl = await QRCode.toDataURL(shareUrl, {
        width: 800,
        margin: 1,
        color: { dark: "#000000", light: "#ffffff" },
        errorCorrectionLevel: "H",
      });
      setShowQR(true);
      setTimeout(() => {
        const canvas = document.getElementById("space-qr-canvas") as HTMLCanvasElement;
        if (canvas) {
          const ctx = canvas.getContext("2d");
          const img = new Image();
          img.onload = () => {
            ctx?.drawImage(img, 0, 0, 200, 200);
          };
          img.src = qrDataUrl;
        }
      }, 100);
      setDownloadQRFn(() => () => {
        const link = document.createElement("a");
        link.download = `hefimer-space-${spaceId}.png`;
        link.href = qrDataUrl;
        link.click();
      });
    } catch (err) {
      showToast("Failed to generate QR code", "error");
    }
  };

  const copySpaceCode = () => {
    navigator.clipboard.writeText(spaceId);
    setCopiedCode(true);
    showToast("Code copied to clipboard!", "success");
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const copySpaceLink = () => {
    const shareUrl = `${window.location.origin}/space?code=${spaceId}`;
    navigator.clipboard.writeText(shareUrl);
    setCopiedLink(true);
    showToast("Link copied to clipboard!", "success");
    setTimeout(() => setCopiedLink(false), 2000);
  };

  // Upload Logic (Storage.to)
  const handleUpload = async (file: File) => {
    setUploading(true);
    setUploadProgress(0);
    try {
      const visitorToken = localStorage.getItem("h-token") || Math.random().toString(36).substring(2);
      
      const initRes = await fetch(resolveApiUrl("/api/proxy/storageto/upload/init"), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Visitor-Token": visitorToken,
        },
        body: JSON.stringify({
          filename: file.name,
          content_type: file.type || "application/octet-stream",
          size: file.size,
        }),
      });
      
      if (!initRes.ok) throw new Error(`Init upload failed`);
      const initData = await initRes.json();
      if (!initData.success) throw new Error(initData.error || "Init failed");

      const r2Key = initData.r2_key;
      const uploadUrl = initData.upload_url;
      if (!uploadUrl) throw new Error("Upload URL missing");

      await new Promise<void>((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) {
            setUploadProgress(Math.round((e.loaded / e.total) * 100));
          }
        };
        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) resolve();
          else reject(new Error(`Upload failed (status ${xhr.status})`));
        };
        xhr.onerror = () => reject(new Error("Network error"));
        xhr.open("PUT", uploadUrl);
        xhr.setRequestHeader("Content-Type", file.type || "application/octet-stream");
        xhr.send(file);
      });

      const confirmRes = await fetch(resolveApiUrl("/api/proxy/storageto/upload/confirm"), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Visitor-Token": visitorToken,
        },
        body: JSON.stringify({
          filename: file.name,
          size: file.size,
          content_type: file.type || "application/octet-stream",
          r2_key: r2Key,
        }),
      });
      if (!confirmRes.ok) throw new Error("Confirm failed");
      const confirmData = await confirmRes.json();
      const currentFileUrl = confirmData.file?.raw_url || confirmData.file?.url;
      if (!currentFileUrl) throw new Error("Confirm URL missing");

      const fileId = Math.random().toString(36).substr(2, 9);
      
      await set(ref(rtdb, `spaces/${spaceId}/files/${fileId}`), {
        name: file.name,
        size: file.size,
        type: file.type || "application/octet-stream",
        url: currentFileUrl,
        uploadedBy: userName,
        timestamp: serverTimestamp(),
        x: uploadPos.x,
        y: uploadPos.y
      });
      showToast("File uploaded!", "success");
    } catch (e: any) {
      showToast(e.message || "Upload failed", "error");
    } finally {
      setUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleUpload(e.target.files[0]);
    }
  };

  const handleCanvasClick = (e: React.MouseEvent) => {
    if (hasDragged.current) return;
    if ((e.target as HTMLElement).closest(".file-card")) return;
    const rect = containerRef.current?.getBoundingClientRect();
    if (rect) {
      const x = e.clientX - rect.left - pan.x;
      const y = e.clientY - rect.top - pan.y;
      setUploadPos({ x, y });
      fileInputRef.current?.click();
    }
  };

  const handleDragOver = (e: React.DragEvent) => { e.preventDefault(); };
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    if ((e.target as HTMLElement).closest(".file-card")) return;
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const rect = containerRef.current?.getBoundingClientRect();
      if (rect) {
        const x = e.clientX - rect.left - pan.x;
        const y = e.clientY - rect.top - pan.y;
        setUploadPos({ x, y });
        handleUpload(e.dataTransfer.files[0]);
      }
    }
  };

  const handleCanvasPointerDown = (e: React.PointerEvent) => {
    if ((e.target as HTMLElement).closest(".file-card")) return;
    const target = containerRef.current;
    if (!target) return;
    
    target.setPointerCapture(e.pointerId);
    hasDragged.current = false;
    
    const startX = e.clientX;
    const startY = e.clientY;
    const initialPanX = pan.x;
    const initialPanY = pan.y;
    
    let currentPanX = initialPanX;
    let currentPanY = initialPanY;
    let dragged = false;

    const childContainer = target.firstElementChild as HTMLElement;

    const onMove = (moveEvent: PointerEvent) => {
      dragged = true;
      hasDragged.current = true;
      const dx = moveEvent.clientX - startX;
      const dy = moveEvent.clientY - startY;
      currentPanX = initialPanX + dx;
      currentPanY = initialPanY + dy;
      
      target.style.backgroundPosition = `${currentPanX}px ${currentPanY}px`;
      if (childContainer) {
        childContainer.style.transform = `translate(${currentPanX}px, ${currentPanY}px)`;
      }
    };

    const onUp = (upEvent: PointerEvent) => {
      target.releasePointerCapture(upEvent.pointerId);
      target.removeEventListener('pointermove', onMove);
      target.removeEventListener('pointerup', onUp);
      
      if (dragged) {
        setPan({ x: currentPanX, y: currentPanY });
      }
      setTimeout(() => {
        if (!dragged) hasDragged.current = false;
      }, 50);
    };

    target.addEventListener('pointermove', onMove);
    target.addEventListener('pointerup', onUp);
  };

  const handleCardPointerDown = (e: React.PointerEvent, fileId: string, initialX: number, initialY: number) => {
    e.stopPropagation();
    const target = e.currentTarget as HTMLElement;
    target.setPointerCapture(e.pointerId);
    hasDragged.current = false;
    
    const startX = e.clientX;
    const startY = e.clientY;
    
    let currentLeft = initialX;
    let currentTop = initialY;
    let dragged = false;
    let lastSyncTime = 0;

    const onMove = (moveEvent: PointerEvent) => {
      dragged = true;
      hasDragged.current = true;
      const dx = moveEvent.clientX - startX;
      const dy = moveEvent.clientY - startY;
      currentLeft = initialX + dx;
      currentTop = initialY + dy;
      
      target.style.left = `${currentLeft}px`;
      target.style.top = `${currentTop}px`;

      const now = Date.now();
      if (now - lastSyncTime > 50) {
        set(ref(rtdb, `spaces/${spaceId}/files/${fileId}/x`), currentLeft);
        set(ref(rtdb, `spaces/${spaceId}/files/${fileId}/y`), currentTop);
        lastSyncTime = now;
      }
    };

    const onUp = (upEvent: PointerEvent) => {
      target.releasePointerCapture(upEvent.pointerId);
      target.removeEventListener('pointermove', onMove);
      target.removeEventListener('pointerup', onUp);
      
      if (dragged) {
        set(ref(rtdb, `spaces/${spaceId}/files/${fileId}/x`), currentLeft);
        set(ref(rtdb, `spaces/${spaceId}/files/${fileId}/y`), currentTop);
      } else {
        hasDragged.current = false;
      }
    };

    target.addEventListener('pointermove', onMove);
    target.addEventListener('pointerup', onUp);
  };

  const deleteFile = async (id: string, name: string) => {
    if (window.confirm(`Delete ${name}?`)) {
      await remove(ref(rtdb, `spaces/${spaceId}/files/${id}`));
      showToast("File deleted", "success");
    }
  };

  const renameFile = async (id: string, oldName: string) => {
    const newName = window.prompt("Rename file", oldName);
    if (newName && newName.trim() && newName !== oldName) {
      await set(ref(rtdb, `spaces/${spaceId}/files/${id}/name`), newName.trim());
      showToast("File renamed", "success");
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes === 0) return "0 B";
    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
  };

  const getIcon = (type: string) => {
    if (type.startsWith("image/")) return <ImageIcon size={20} className="text-blue-400" />;
    if (type.startsWith("video/")) return <Video size={20} className="text-purple-400" />;
    if (type.startsWith("audio/")) return <Music size={20} className="text-pink-400" />;
    if (type.includes("zip") || type.includes("tar") || type.includes("rar")) return <Archive size={20} className="text-amber-400" />;
    return <File size={20} className="text-white/60" />;
  };

  // ---------------- LOBBY & CREATED MODAL ---------------- //
  if (showCreatedModal && !inSpace) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="flex flex-col items-center py-6 space-y-6 relative w-full pt-14 text-center max-w-[440px] mx-auto"
      >
        <button
          onClick={() => setState((prev: any) => ({ ...prev, showCreatedModal: false }))}
          className="absolute top-0 left-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
        >
          <ArrowLeft size={10} /> Back
        </button>
        <button
          onClick={() => setState((prev: any) => ({ ...prev, showCreatedModal: false }))}
          className="absolute top-0 right-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
        >
          New Space <Plus size={10} />
        </button>

        <div className="flex flex-col items-center space-y-1.5">
          <h3 className="text-white/40 text-[10px] font-bold uppercase tracking-[0.2em]">
            Your Space Code
          </h3>
          <div className="h-0.5 w-6 bg-white/10 rounded-full" />
        </div>

        <div className="flex items-center justify-center gap-2.5">
          {spaceId.split("").map((digit: string, i: number) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, scale: 0.5, rotate: -15, y: 20 }}
              animate={{ opacity: 1, scale: 1, rotate: 0, y: 0 }}
              transition={{ type: "spring", stiffness: 400, damping: 15, delay: i * 0.08 + 0.2 }}
              whileHover={{ scale: 1.05, y: -5, transition: { duration: 0.2 } }}
              className="w-12 h-16 sm:w-14 sm:h-20 bg-white/[0.04] border-white/10 text-white border rounded-[28px] flex items-center justify-center text-3xl sm:text-4xl font-black font-sans relative group cursor-default shadow-lg"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-white/[0.05] to-transparent rounded-[28px] pointer-events-none" />
              <span className="relative z-10 tracking-tight drop-shadow-sm">{digit}</span>
              <div className="absolute inset-0 rounded-[28px] border border-white/0 group-hover:border-white/20 transition-all duration-300" />
            </motion.div>
          ))}
        </div>

        <div className="flex flex-col items-center space-y-3 w-full pt-2">
          <p className="text-white/30 text-[10px] font-medium tracking-wide leading-relaxed px-4">
            Share this 5-digit code or direct link with others to start collaborating in the space.
          </p>
        </div>

        <div className="flex gap-3 w-full pt-2">
          <button
            onClick={copySpaceLink}
            className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
              copiedLink ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"
            }`}
          >
            {copiedLink ? <Check size={16} /> : <Link size={16} />}
            {copiedLink ? "Copied!" : "Copy Link"}
          </button>
          <button
            onClick={copySpaceCode}
            className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
              copiedCode ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"
            }`}
          >
            {copiedCode ? <Check size={16} /> : <Copy size={16} />}
            {copiedCode ? "Copied!" : "Copy Code"}
          </button>
        </div>

        <div className="flex flex-col space-y-4 w-full pt-1">
          <button
            onClick={generateQR}
            className="w-full py-3.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/75 rounded-full font-bold text-xs transition-all flex items-center justify-center gap-2 active:scale-98 cursor-pointer"
          >
            <QrCode size={16} />
            Share via QR Code
          </button>

          <button
            onClick={() => {
              vibrate();
              setState((prev: any) => ({ ...prev, showCreatedModal: false, inSpace: true }));
            }}
            className="w-full bg-white text-black py-3.5 rounded-full font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-1.5 shadow-[0_0_20px_rgba(255,255,255,0.1)]"
          >
            Enter Space
            <ArrowRight size={16} />
          </button>
        </div>

        <AnimatePresence>
          {showQR && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md"
              onClick={() => setShowQR(false)}
            >
              <motion.div
                initial={{ scale: 0.9, opacity: 0, y: 20 }}
                animate={{ scale: 1, opacity: 1, y: 0 }}
                exit={{ scale: 0.9, opacity: 0, y: 20 }}
                transition={{ type: "spring", damping: 25, stiffness: 300 }}
                className="w-full max-w-sm bg-zinc-950 border border-white/10 rounded-[32px] p-6 flex flex-col items-center gap-5 shadow-2xl relative"
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  onClick={() => setShowQR(false)}
                  className="absolute top-4 right-4 text-white/40 hover:text-white/80 p-1.5 hover:bg-white/5 rounded-full transition-all cursor-pointer"
                >
                  <X size={16} />
                </button>

                <div className="absolute inset-0 rounded-[32px] bg-gradient-to-b from-indigo-500/[0.02] to-transparent pointer-events-none" />
                
                <h3 className="text-xl font-bold text-white tracking-tight mt-2">
                  {spaceName}
                </h3>
                
                <div className="bg-white p-5 rounded-[24px] shadow-[0_0_40px_rgba(255,255,255,0.1)] w-full flex items-center justify-center overflow-hidden">
                  <canvas id="space-qr-canvas" width="200" height="200" className="w-[200px] h-[200px]" style={{ imageRendering: "pixelated" }} />
                </div>
                
                <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-white/40">
                  Share Space Link
                </h4>

                <div className="flex gap-2 w-full mt-2">
                  <button
                    onClick={() => {
                      vibrate();
                      navigator.clipboard.writeText(`${window.location.origin}/space?code=${spaceId}`);
                      showToast("Link copied", "success");
                    }}
                    className="flex-1 bg-white/5 hover:bg-white/10 border border-white/10 text-white/80 py-3 rounded-2xl text-[10px] font-bold uppercase tracking-wider transition-all"
                  >
                    Copy Link
                  </button>
                  {downloadQRFn && (
                    <button
                      onClick={() => {
                        vibrate();
                        downloadQRFn();
                      }}
                      className="flex-1 bg-white text-black py-3 rounded-2xl text-[10px] font-bold uppercase tracking-wider transition-all hover:scale-102 active:scale-98"
                    >
                      Download
                    </button>
                  )}
                </div>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    );
  }

  if (!inSpace) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-[900px] mx-auto px-4"
      >
        <div className="space-y-10">
          <div className="max-w-[400px] mx-auto text-center space-y-2.5 bg-[#0a0a0a]/90 border border-white/10 rounded-3xl p-5 shadow-xl">
            <label className="text-[10px] uppercase tracking-[0.2em] font-bold text-white/40 block">
              Your Display Name
            </label>
            <div className="relative">
              <User className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={18} />
              <input
                value={userName}
                onChange={(e) => setState((prev: any) => ({ ...prev, userName: e.target.value }))}
                placeholder="Enter your name..."
                className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-11 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 text-center font-bold"
              />
            </div>
          </div>

          <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-10 md:gap-0 relative">
            <div className="hidden md:block absolute left-1/2 top-0 bottom-0 w-px bg-white/10 -translate-x-1/2 origin-top pointer-events-none" />

            <div className="space-y-6 md:pr-12 text-left flex flex-col justify-between">
              <div className="space-y-2">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-white/60">
                    <LogIn size={18} />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold tracking-tight text-white">Join Space</h3>
                    <p className="text-xs text-white/40">Access an existing space</p>
                  </div>
                </div>
                <p className="text-[11px] text-white/30 leading-relaxed pt-1">
                  Enter the 5-digit space ID shared by your friend to join their active session.
                </p>
              </div>

              <div className="space-y-4 pt-2">
                <div className="space-y-2">
                  <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                    Space Access Code
                  </label>
                  <div className="relative">
                    <Hash className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={16} />
                    <input
                      value={spaceId}
                      onChange={(e) =>
                        setState((prev: any) => ({
                          ...prev,
                          spaceId: e.target.value.replace(/\D/g, "").slice(0, 5),
                        }))
                      }
                      placeholder="5-digit Space ID"
                      className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-10 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold tracking-widest font-sans"
                    />
                  </div>
                </div>

                <button
                  onClick={handleJoinSpace}
                  disabled={loading}
                  className="w-full bg-white text-black py-3 rounded-2xl font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 shadow-md shadow-white/5 flex items-center justify-center gap-1.5"
                >
                  {loading ? <Loader2 size={16} className="animate-spin" /> : "Enter Space"}
                  <ArrowRight size={16} />
                </button>
              </div>
            </div>

            <div className="space-y-6 md:pl-12 text-left flex flex-col justify-between">
              <div className="space-y-2">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-white/60">
                    <Box size={18} />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold tracking-tight text-white">New Space</h3>
                    <p className="text-xs text-white/40">Create a collaborative canvas</p>
                  </div>
                </div>
                <p className="text-[11px] text-white/30 leading-relaxed pt-1">
                  Start a fresh, temporary workspace and invite friends to drop files and media.
                </p>
              </div>

              <div className="space-y-4 pt-2">
                <div className="space-y-2">
                  <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                    Space Title
                  </label>
                  <input
                    value={spaceNameInput}
                    onChange={(e) =>
                      setState((prev: any) => ({
                        ...prev,
                        spaceNameInput: e.target.value,
                      }))
                    }
                    placeholder="e.g. Design Assets, Project Files"
                    className="w-full bg-white/[0.04] border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-medium"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between ml-3">
                    <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block">
                      Custom Code
                    </label>
                    <button
                      onClick={generateRandomCustomCode}
                      className="text-[9px] uppercase tracking-wider font-bold text-white/30 hover:text-white/70 transition-colors"
                    >
                      Randomize
                    </button>
                  </div>
                  <div className="relative">
                    <Hash className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={16} />
                    <input
                      value={customCode}
                      onChange={(e) => handleCustomCodeChange(e.target.value)}
                      placeholder="5-digit ID"
                      maxLength={5}
                      className={`w-full bg-white/[0.04] border ${
                        customCode.length === 5
                          ? codeStatus === "available"
                            ? "border-green-500/30 focus:border-green-500/60"
                            : codeStatus === "taken"
                              ? "border-red-500/30 focus:border-red-500/60"
                              : "border-white/10"
                          : "border-white/10"
                      } rounded-2xl pl-10 pr-10 py-3 text-sm text-white focus:outline-none focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold tracking-widest font-sans`}
                    />
                    <div className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center">
                      {checkingCode ? (
                        <Loader2 size={14} className="text-white/30 animate-spin" />
                      ) : customCode.length === 5 ? (
                        codeStatus === "available" ? (
                          <Check size={16} className="text-green-400 drop-shadow-[0_0_8px_rgba(74,222,128,0.5)]" />
                        ) : codeStatus === "taken" ? (
                          <span className="text-[9px] font-bold text-red-400 uppercase tracking-wider bg-red-500/10 px-2 py-0.5 rounded-full">
                            Taken
                          </span>
                        ) : null
                      ) : null}
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                    Auto-Delete After
                  </label>
                  <div className="flex gap-2 p-1 bg-white/[0.02] border border-white/5 rounded-2xl">
                    {["1h", "24h", "72h"].map((opt) => (
                      <button
                        key={opt}
                        onClick={() => setState((prev: any) => ({ ...prev, spaceExpire: opt }))}
                        className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all ${
                          spaceExpire === opt || (!spaceExpire && opt === "1h")
                            ? "bg-white/10 text-white shadow-sm"
                            : "text-white/30 hover:text-white/60 hover:bg-white/[0.04]"
                        }`}
                      >
                        {opt === "1h" ? "1 Hour" : opt === "24h" ? "1 Day" : "3 Days"}
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  onClick={handleCreateSpace}
                  disabled={loading || !spaceNameInput.trim() || customCode.length !== 5 || codeStatus === "taken"}
                  className="w-full bg-white/5 border border-white/10 text-white py-3 rounded-2xl font-bold text-xs hover:bg-white/10 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {loading ? <Loader2 size={16} className="animate-spin" /> : <Plus size={16} />}
                  Create Space
                </button>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    );
  }

  // ---------------- MAIN CANVAS VIEW ---------------- //
  return (
    <div className="w-full h-[70vh] border border-white/10 rounded-3xl overflow-hidden relative flex flex-col bg-black">
      {/* Header */}
      <div className="absolute top-0 left-0 right-0 z-10 p-4 flex items-center justify-between bg-gradient-to-b from-black/80 to-transparent pointer-events-none">
        <div className="pointer-events-auto flex items-center gap-3">
          <div className="bg-white/10 backdrop-blur-md px-4 py-2 rounded-xl border border-white/10">
            <h2 className="text-white font-bold text-lg">{spaceName}</h2>
            <div className="flex items-center gap-3 text-xs text-white/50 mt-1">
              <span className="flex items-center gap-1 font-mono"><Users size={12}/> {memberCount}</span>
              <span className="flex items-center gap-1">Code: {spaceId}</span>
            </div>
          </div>
          <button onClick={() => setState((p:any)=>({...p, showCreatedModal: true}))} className="bg-white/10 backdrop-blur-md p-3 rounded-xl border border-white/10 hover:bg-white/20 transition-all text-white">
            <QrCode size={18} />
          </button>
        </div>
        <div className="pointer-events-auto px-4 py-2 bg-white/10 backdrop-blur-md rounded-xl border border-white/10 text-white/50 text-xs flex items-center gap-2">
          {uploading ? <><Loader2 size={14} className="animate-spin" /> Uploading {uploadProgress}%</> : "Click/Drag anywhere to drop files"}
        </div>
      </div>

      {/* Hidden File Input */}
      <input type="file" ref={fileInputRef} onChange={handleFileChange} className="hidden" />

      {/* Interactive Canvas Area */}
      <div 
        ref={containerRef}
        className="flex-1 w-full h-full cursor-grab active:cursor-grabbing relative overflow-hidden"
        style={{
          backgroundSize: '40px 40px',
          backgroundImage: 'linear-gradient(to right, rgba(255,255,255,0.03) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.03) 1px, transparent 1px)',
          backgroundPosition: `${pan.x}px ${pan.y}px`
        }}
        onPointerDown={handleCanvasPointerDown}
        onClick={handleCanvasClick}
        onDragOver={handleDragOver}
        onDrop={handleDrop}
      >
        <div style={{ transform: `translate(${pan.x}px, ${pan.y}px)` }} className="absolute inset-0 pointer-events-none">
          <AnimatePresence>
            {files.map(file => (
              <motion.div
                key={file.id}
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.8, opacity: 0 }}
                className="absolute pointer-events-auto file-card group cursor-grab active:cursor-grabbing"
                style={{ left: file.x, top: file.y }}
                onPointerDown={(e) => handleCardPointerDown(e, file.id, file.x, file.y)}
              >
                <div className="bg-zinc-900/90 backdrop-blur-xl border border-white/10 rounded-2xl p-4 min-w-[200px] shadow-2xl transition-all hover:border-white/30 hover:bg-zinc-800/90">
                  <div className="flex items-start gap-3">
                    <div className="p-3 bg-white/5 rounded-xl border border-white/10">
                      {getIcon(file.type)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-white truncate max-w-[120px]" title={file.name}>
                        {file.name}
                      </p>
                      <p className="text-xs text-white/40 mt-1">
                        {formatSize(file.size)}
                      </p>
                    </div>
                  </div>
                  
                  {/* Hover Actions */}
                  <div className="absolute inset-0 bg-black/60 backdrop-blur-sm rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                    <a href={file.url} target="_blank" rel="noreferrer" className="p-2 bg-white/10 hover:bg-white text-white hover:text-black rounded-lg transition-all" title="Download">
                      <Download size={16} />
                    </a>
                    <button onClick={(e) => { e.stopPropagation(); renameFile(file.id, file.name); }} className="p-2 bg-white/10 hover:bg-white text-white hover:text-black rounded-lg transition-all" title="Rename">
                      <Edit3 size={16} />
                    </button>
                    <button onClick={(e) => { e.stopPropagation(); deleteFile(file.id, file.name); }} className="p-2 bg-white/10 hover:bg-red-500 text-white rounded-lg transition-all" title="Delete">
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
                {/* Uploaded By Tag */}
                <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 whitespace-nowrap text-[10px] text-white/30 font-medium">
                  by {file.uploadedBy}
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
