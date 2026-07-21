import React, { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import {
  Bell,
  BellOff,
  Check,
  ChevronRight,
  CircleOff,
  Clock3,
  Copy,
  Download,
  FileText,
  Laptop,
  Link2,
  Loader2,
  LogOut,
  MonitorSmartphone,
  PackageCheck,
  QrCode,
  Radio,
  RefreshCw,
  Send,
  Settings2,
  ShieldCheck,
  Smartphone,
  Trash2,
  UserRoundPlus,
  Users,
  X,
} from "lucide-react";
import QRCode from "qrcode";
import {
  deviceApi,
  disableDevicePush,
  enableDevicePush,
  getDeviceIdentity,
  getPushSubscription,
  type DeviceSync,
  type DeviceTransfer,
  type DropCreatedDetail,
  type GroupMember,
} from "./device-api";

interface DeviceHubProps {
  showToast: (message: string, type?: "success" | "error" | "info") => void;
  onOpenDrop: (code: string) => void;
}

const EMPTY_SYNC: DeviceSync = {
  device: { id: "", name: "This device" },
  group: null,
  members: [],
  incoming: [],
  outgoing: [],
};

const STATUS_LABELS: Record<string, string> = {
  pending: "Waiting",
  approved: "Ready",
  declined: "Declined",
  downloading: "Opening",
  received: "Received",
  failed: "Failed",
  cancelled: "Cancelled",
  expired: "Expired",
};

function formatBytes(bytes: number) {
  if (!bytes) return "Text";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** index).toFixed(index > 1 ? 1 : 0)} ${units[index]}`;
}

function formatRemaining(expiresAt: number | null) {
  if (!expiresAt) return "No expiry";
  const ms = expiresAt - Date.now();
  if (ms <= 0) return "Expired";
  const hours = Math.floor(ms / 3_600_000);
  if (hours >= 48) return `${Math.ceil(hours / 24)} days left`;
  if (hours >= 1) return `${hours}h left`;
  return `${Math.max(1, Math.ceil(ms / 60_000))}m left`;
}

function deviceIcon(member: Pick<GroupMember, "platform">, size = 18) {
  if (/Android|iPhone|iPad/i.test(member.platform)) return <Smartphone size={size} />;
  return <Laptop size={size} />;
}

function StatusPill({ status }: { status: string }) {
  const active = status === "approved" || status === "received";
  const waiting = status === "pending" || status === "downloading";
  return (
    <span className={`rounded-full border px-2.5 py-1 text-[9px] font-black uppercase tracking-[0.16em] ${
      active
        ? "border-white/25 bg-white/10 text-white"
        : waiting
          ? "border-white/15 bg-white/[0.06] text-white/70"
          : "border-white/10 bg-white/[0.04] text-white/40"
    }`}>
      {STATUS_LABELS[status] || status}
    </span>
  );
}

export function DeviceHub({ showToast, onOpenDrop }: DeviceHubProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [sync, setSync] = useState<DeviceSync>(EMPTY_SYNC);
  const syncRef = useRef(sync);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const [setupMode, setSetupMode] = useState<"create" | "join">("create");
  const [groupName, setGroupName] = useState("My Hefimer Group");
  const [duration, setDuration] = useState("7d");
  const [pairToken, setPairToken] = useState("");
  const [invite, setInvite] = useState<{ token: string; expiresAt: number } | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState("");
  const [notificationsEnabled, setNotificationsEnabled] = useState(false);
  const [copiedValue, setCopiedValue] = useState("");
  const [deviceName, setDeviceName] = useState("");
  const [section, setSection] = useState<"devices" | "activity">("devices");
  const refreshing = useRef(false);
  const firstSync = useRef(true);
  const deviceFeatureEnabled = () => localStorage.getItem("hefimer_device_enabled") === "1";

  const refresh = async (silent = true) => {
    if (refreshing.current) return;
    refreshing.current = true;
    if (!silent) setBusy("refresh");
    try {
      await getDeviceIdentity();
      const next = await deviceApi.sync();
      syncRef.current = next;
      setSync(next);
      setDeviceName(next.device.name);
      setError("");
      firstSync.current = false;
    } catch (err: any) {
      setError(err.message || "Device service is unavailable");
    } finally {
      refreshing.current = false;
      setLoading(false);
      if (!silent) setBusy("");
    }
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("pair");
    const openDevices = params.get("devices");
    if (token) {
      localStorage.setItem("hefimer_device_enabled", "1");
      setPairToken(token);
      setSetupMode("join");
      setIsOpen(true);
    }
    if (openDevices === "1") {
      localStorage.setItem("hefimer_device_enabled", "1");
      setIsOpen(true);
    }

    if (deviceFeatureEnabled() || token || openDevices === "1") refresh(false);
    else setLoading(false);
    const interval = window.setInterval(() => deviceFeatureEnabled() && refresh(true), 4000);
    const handleFocus = () => deviceFeatureEnabled() && refresh(true);
    const handleVisibility = () => deviceFeatureEnabled() && !document.hidden && refresh(true);
    const handleSwMessage = (event: MessageEvent) => {
      if (deviceFeatureEnabled() && event.data?.type === "hefimer-transfer") refresh(true);
    };
    window.addEventListener("focus", handleFocus);
    document.addEventListener("visibilitychange", handleVisibility);
    navigator.serviceWorker?.addEventListener("message", handleSwMessage);
    if (deviceFeatureEnabled()) getPushSubscription().then((subscription) => setNotificationsEnabled(Boolean(subscription))).catch(() => {});
    if (deviceFeatureEnabled() && "Notification" in window && Notification.permission === "granted") {
      enableDevicePush().then(() => setNotificationsEnabled(true)).catch(() => {});
    }
    return () => {
      window.clearInterval(interval);
      window.removeEventListener("focus", handleFocus);
      document.removeEventListener("visibilitychange", handleVisibility);
      navigator.serviceWorker?.removeEventListener("message", handleSwMessage);
    };
  }, []);

  useEffect(() => {
    const handleDropCreated = async (event: Event) => {
      const detail = (event as CustomEvent<DropCreatedDetail>).detail;
      if (!detail || !deviceFeatureEnabled()) return;
      try {
        const current = await deviceApi.sync();
        syncRef.current = current;
        setSync(current);
        if (!current.group) return;
        await deviceApi.createTransfer(detail);
        const recipients = Math.max(0, current.members.length - 1);
        showToast(
          recipients ? `Offered to ${recipients} paired device${recipients === 1 ? "" : "s"}` : "Saved to your device group",
          "success",
        );
        await refresh(true);
      } catch (err: any) {
        console.warn("Paired-device offer failed without affecting the drop:", err);
        showToast("Drop created, but paired-device delivery is temporarily unavailable", "info");
      }
    };
    const handleDropOpened = async (event: Event) => {
      const code = (event as CustomEvent<{ code: string }>).detail?.code;
      const transfer = syncRef.current.incoming.find((item) => item.drop_code === code);
      if (!transfer || !["approved", "downloading"].includes(transfer.recipient_status || "")) return;
      try {
        if (transfer.recipient_status === "approved") await deviceApi.updateTransfer(transfer.id, "downloading");
        await deviceApi.updateTransfer(transfer.id, "received");
        await refresh(true);
      } catch {}
    };
    window.addEventListener("hefimer:drop-created", handleDropCreated);
    window.addEventListener("hefimer:drop-opened", handleDropOpened);
    return () => {
      window.removeEventListener("hefimer:drop-created", handleDropCreated);
      window.removeEventListener("hefimer:drop-opened", handleDropOpened);
    };
  }, []);

  useEffect(() => {
    if (!invite) {
      setQrDataUrl("");
      return;
    }
    const url = `${window.location.origin}/?pair=${encodeURIComponent(invite.token)}`;
    QRCode.toDataURL(url, { width: 320, margin: 2, color: { dark: "#050505", light: "#ffffff" } })
      .then(setQrDataUrl)
      .catch(() => setQrDataUrl(""));
  }, [invite]);

  const run = async (key: string, task: () => Promise<unknown>, success?: string) => {
    setBusy(key);
    try {
      await task();
      if (success) showToast(success, "success");
      await refresh(true);
      return true;
    } catch (err: any) {
      showToast(err.message || "Something went wrong", "error");
      return false;
    } finally {
      setBusy("");
    }
  };

  const createGroup = () => run("create", async () => {
    await deviceApi.createGroup(groupName, duration);
    const nextInvite = await deviceApi.createInvite();
    setInvite(nextInvite);
  }, "Device group created");

  const joinGroup = async () => {
    const joined = await run("join", () => deviceApi.joinGroup(pairToken), "Devices paired successfully");
    if (joined) {
      setPairToken("");
      const url = new URL(window.location.href);
      url.searchParams.delete("pair");
      window.history.replaceState({}, document.title, `${url.pathname}${url.search}`);
    }
  };

  const createInvite = async () => {
    setBusy("invite");
    try {
      setInvite(await deviceApi.createInvite());
    } catch (err: any) {
      showToast(err.message || "Could not create pairing token", "error");
    } finally {
      setBusy("");
    }
  };

  const copyValue = async (value: string, message: string) => {
    const area = document.createElement("textarea");
    area.value = value;
    area.setAttribute("readonly", "");
    area.style.position = "fixed";
    area.style.left = "-9999px";
    area.style.opacity = "0";
    document.body.appendChild(area);
    area.focus();
    area.select();
    area.setSelectionRange(0, value.length);
    let copied = false;
    try {
      copied = document.execCommand("copy");
    } catch {}
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(value);
        copied = true;
      }
    } catch {}
    area.remove();
    if (!copied) {
      showToast("Clipboard access was blocked. Select the token and try again.", "error");
      return;
    }
    setCopiedValue(value);
    window.setTimeout(() => setCopiedValue((current) => current === value ? "" : current), 1800);
    showToast(message, "success");
  };

  const toggleNotifications = async () => {
    setBusy("push");
    try {
      if (notificationsEnabled) {
        await disableDevicePush();
        setNotificationsEnabled(false);
        showToast("Device notifications disabled", "success");
      } else {
        await enableDevicePush();
        setNotificationsEnabled(true);
        showToast("You will be notified when a device sends", "success");
      }
    } catch (err: any) {
      showToast(err.message || "Could not update notifications", "error");
    } finally {
      setBusy("");
    }
  };

  const decide = async (transfer: DeviceTransfer, decision: "approve" | "decline") => {
    const accepted = await run(`${decision}:${transfer.id}`, () => deviceApi.decideTransfer(transfer.id, decision));
    if (accepted && decision === "approve") {
      onOpenDrop(transfer.drop_code);
      setIsOpen(false);
    }
  };

  const openTransfer = async (transfer: DeviceTransfer) => {
    if (transfer.recipient_status === "approved") {
      await run(`open:${transfer.id}`, () => deviceApi.updateTransfer(transfer.id, "downloading"));
    }
    onOpenDrop(transfer.drop_code);
    setIsOpen(false);
  };

  const pending = sync.incoming.filter((item) => item.recipient_status === "pending");
  const ready = sync.incoming.filter((item) => ["approved", "downloading"].includes(item.recipient_status || ""));
  const badgeCount = pending.length + ready.length;
  const incomingPrompt = !isOpen ? pending[0] : null;
  const isOwner = sync.group?.ownerDeviceId === sync.device.id;
  const openHub = () => {
    localStorage.setItem("hefimer_device_enabled", "1");
    setLoading(true);
    setIsOpen(true);
    refresh(false);
  };

  return (
    <div className="device-hub-theme">
      <motion.button
        type="button"
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        onClick={openHub}
        className="fixed right-[4.15rem] top-4 z-50 flex h-9 w-9 items-center justify-center rounded-full border border-white/10 bg-black/60 text-white/70 shadow-xl backdrop-blur-xl transition hover:border-white/25 hover:text-white sm:h-10 sm:w-10"
        title="Paired devices"
      >
        <MonitorSmartphone size={16} />
        {sync.group && <span className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-black bg-white" />}
        {badgeCount > 0 && (
          <span className="absolute -right-1.5 -top-1.5 min-w-4 rounded-full bg-white px-1 py-0.5 text-[8px] font-black text-black">
            {badgeCount > 9 ? "9+" : badgeCount}
          </span>
        )}
      </motion.button>

      <AnimatePresence>
        {incomingPrompt && (
          <motion.div
            initial={{ opacity: 0, y: 40, scale: 0.94 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 40, scale: 0.94 }}
            className="fixed bottom-4 left-4 right-4 z-[360] mx-auto max-w-md overflow-hidden rounded-[30px] border border-white/15 bg-[#0a0a0a]/95 p-5 shadow-[0_30px_100px_rgba(0,0,0,.75)] backdrop-blur-2xl sm:bottom-7"
          >
            <div className="absolute inset-x-8 top-0 h-px bg-gradient-to-r from-transparent via-white/70 to-transparent" />
            <div className="flex items-start gap-4">
              <div className="relative flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-white/20 bg-white/10 text-white">
                {incomingPrompt.kind === "file" ? <Download size={21} /> : <FileText size={21} />}
                <span className="absolute -right-1 -top-1 h-2.5 w-2.5 animate-pulse rounded-full bg-white" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[9px] font-black uppercase tracking-[0.26em] text-white/55">Incoming from {incomingPrompt.sender_name}</p>
                <h3 className="mt-1 truncate text-lg font-black tracking-tight text-white">{incomingPrompt.name}</h3>
                <p className="mt-1 text-xs text-white/40">{formatBytes(incomingPrompt.size_bytes)} · {incomingPrompt.provider} · {formatRemaining(incomingPrompt.expires_at)}</p>
              </div>
            </div>
            <div className="mt-5 grid grid-cols-2 gap-3">
              <button onClick={() => decide(incomingPrompt, "decline")} disabled={Boolean(busy)} className="rounded-full border border-white/10 bg-white/[0.04] py-3 text-[10px] font-black uppercase tracking-[0.18em] text-white/55 transition hover:bg-white/10 disabled:opacity-40">
                Decline
              </button>
              <button onClick={() => decide(incomingPrompt, "approve")} disabled={Boolean(busy)} className="flex items-center justify-center gap-2 rounded-full bg-white py-3 text-[10px] font-black uppercase tracking-[0.18em] text-black transition hover:scale-[1.02] disabled:opacity-40">
                {busy === `approve:${incomingPrompt.id}` ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />} Approve
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {isOpen && (
          <motion.div className="fixed inset-0 z-[340] flex items-end justify-center p-0 sm:items-center sm:p-5">
            <motion.button
              aria-label="Close paired devices"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              onClick={() => setIsOpen(false)}
              className="absolute inset-0 cursor-default bg-black/80 backdrop-blur-lg"
            />
            <motion.section
              initial={{ opacity: 0, y: 45, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 45, scale: 0.97 }}
              transition={{ type: "spring", stiffness: 330, damping: 32 }}
              className="device-hub-surface relative flex max-h-[94vh] w-full max-w-4xl flex-col overflow-hidden rounded-t-[38px] border border-white/12 bg-[#070707] shadow-[0_35px_140px_rgba(0,0,0,.85)] sm:max-h-[88vh] sm:rounded-[38px]"
            >
              <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_12%_5%,rgba(255,255,255,.075),transparent_28%),radial-gradient(circle_at_88%_90%,rgba(255,255,255,.035),transparent_30%)]" />
              <header className="relative flex items-center justify-between border-b border-white/8 px-5 py-4 sm:px-7">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-2xl border border-white/12 bg-white/[0.06]">
                    <MonitorSmartphone size={19} />
                  </div>
                  <div>
                    <p className="text-[9px] font-black uppercase tracking-[0.26em] text-white/35">Hefimer link</p>
                    <h2 className="text-lg font-black tracking-tight">Paired devices</h2>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => refresh(false)} disabled={busy === "refresh"} className="flex h-9 w-9 items-center justify-center rounded-full border border-white/10 text-white/40 transition hover:text-white">
                    <RefreshCw size={15} className={busy === "refresh" ? "animate-spin" : ""} />
                  </button>
                  <button onClick={() => setIsOpen(false)} className="flex h-9 w-9 items-center justify-center rounded-full border border-white/10 text-white/40 transition hover:bg-white/10 hover:text-white">
                    <X size={16} />
                  </button>
                </div>
              </header>

              <div className="relative overflow-y-auto p-5 sm:p-7">
                {loading ? (
                  <div className="flex min-h-[420px] flex-col items-center justify-center gap-4 text-white/35">
                    <Loader2 size={28} className="animate-spin" />
                    <span className="text-[10px] font-black uppercase tracking-[0.24em]">Securing this device</span>
                  </div>
                ) : error && !sync.device.id ? (
                  <div className="flex min-h-[420px] flex-col items-center justify-center text-center">
                    <CircleOff size={34} className="mb-4 text-white/70" />
                    <h3 className="text-xl font-black">Device service unavailable</h3>
                    <p className="mt-2 max-w-sm text-sm leading-relaxed text-white/40">{error}</p>
                    <button onClick={() => refresh(false)} className="mt-6 rounded-full bg-white px-6 py-3 text-[10px] font-black uppercase tracking-widest text-black">Try again</button>
                  </div>
                ) : !sync.group ? (
                  <div className="mx-auto max-w-2xl">
                    <div className="grid gap-4 sm:grid-cols-[1fr_auto_1fr] sm:items-center">
                      <div className="rounded-[30px] border border-white/10 bg-white/[0.035] p-6">
                        <ShieldCheck className="mb-10 text-white" size={26} />
                        <p className="text-[9px] font-black uppercase tracking-[0.25em] text-white/50">Accountless identity</p>
                        <h3 className="mt-2 text-2xl font-black tracking-tight">This browser is now a device.</h3>
                        <p className="mt-3 text-sm leading-relaxed text-white/40">Its private key stays here. Pair once, then send through Hefimer providers whenever the group is active.</p>
                      </div>
                      <div className="hidden h-16 w-px bg-gradient-to-b from-transparent via-white/15 to-transparent sm:block" />
                      <div className="rounded-[30px] border border-white/10 bg-black/30 p-6">
                        <p className="text-[9px] font-black uppercase tracking-[0.25em] text-white/30">Device name</p>
                        <div className="mt-3 flex items-center gap-3">
                          <Laptop size={18} className="text-white/40" />
                          <span className="truncate text-sm font-bold">{sync.device.name}</span>
                        </div>
                        <p className="mt-4 text-xs leading-relaxed text-white/30">No email, password, or Hefimer account is required.</p>
                      </div>
                    </div>

                    <div className="mt-6 rounded-[32px] border border-white/10 bg-white/[0.025] p-2">
                      <div className="grid grid-cols-2 gap-2">
                        <button onClick={() => setSetupMode("create")} className={`rounded-full py-3 text-[10px] font-black uppercase tracking-[0.18em] transition ${setupMode === "create" ? "bg-white text-black" : "text-white/35 hover:text-white"}`}>Create group</button>
                        <button onClick={() => setSetupMode("join")} className={`rounded-full py-3 text-[10px] font-black uppercase tracking-[0.18em] transition ${setupMode === "join" ? "bg-white text-black" : "text-white/35 hover:text-white"}`}>Enter token</button>
                      </div>
                      <div className="overflow-hidden p-4 sm:p-6">
                        <AnimatePresence mode="wait" initial={false}>
                        {setupMode === "create" ? (
                          <motion.div key="create" initial={{ opacity: 0, x: -12, filter: "blur(4px)" }} animate={{ opacity: 1, x: 0, filter: "blur(0px)" }} exit={{ opacity: 0, x: 12, filter: "blur(4px)" }} transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }} className="space-y-5">
                            <label className="block">
                              <span className="text-[9px] font-black uppercase tracking-[0.22em] text-white/35">Group name</span>
                              <input value={groupName} onChange={(event) => setGroupName(event.target.value)} maxLength={64} className="mt-2 w-full rounded-2xl border border-white/10 bg-black/40 px-5 py-4 text-sm font-bold outline-none transition focus:border-white/30" />
                            </label>
                            <div>
                              <span className="text-[9px] font-black uppercase tracking-[0.22em] text-white/35">Link lifetime</span>
                              <div className="mt-2 grid grid-cols-3 gap-2 sm:grid-cols-5">
                                {[['12h','12 hours'],['24h','24 hours'],['7d','7 days'],['30d','30 days'],['never','Forever']].map(([value, label]) => (
                                  <button key={value} onClick={() => setDuration(value)} className={`rounded-2xl border px-2 py-3 text-[10px] font-bold transition ${duration === value ? "border-white bg-white text-black" : "border-white/8 text-white/35 hover:border-white/20"}`}>{label}</button>
                                ))}
                              </div>
                            </div>
                            <button onClick={createGroup} disabled={Boolean(busy)} className="flex w-full items-center justify-center gap-2 rounded-full bg-white py-4 text-[10px] font-black uppercase tracking-[0.2em] text-black transition hover:scale-[1.01] disabled:opacity-40">
                              {busy === "create" ? <Loader2 size={15} className="animate-spin" /> : <Users size={15} />} Create secure group
                            </button>
                          </motion.div>
                        ) : (
                          <motion.div key="join" initial={{ opacity: 0, x: 12, filter: "blur(4px)" }} animate={{ opacity: 1, x: 0, filter: "blur(0px)" }} exit={{ opacity: 0, x: -12, filter: "blur(4px)" }} transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }} className="space-y-5">
                            <div className="rounded-3xl border border-white/12 bg-white/[0.045] p-5">
                              <QrCode size={22} className="text-white" />
                              <h3 className="mt-5 text-lg font-black">Scan the QR with your camera, or enter its token here.</h3>
                              <p className="mt-2 text-xs leading-relaxed text-white/35">Every token pairs one device and expires after ten minutes.</p>
                            </div>
                            <input value={pairToken} onChange={(event) => setPairToken(event.target.value.trim())} placeholder="hfm-••••••••••••••••••••••••••••••••••••" className="w-full rounded-2xl border border-white/10 bg-black/40 px-5 py-4 font-mono text-xs outline-none transition placeholder:text-white/15 focus:border-white/30" />
                            <button onClick={joinGroup} disabled={Boolean(busy) || !/^hfm-[A-Za-z0-9]{36}$/.test(pairToken)} className="flex w-full items-center justify-center gap-2 rounded-full bg-white py-4 text-[10px] font-black uppercase tracking-[0.2em] text-black transition hover:scale-[1.01] disabled:opacity-30">
                              {busy === "join" ? <Loader2 size={15} className="animate-spin" /> : <Link2 size={15} />} Pair this device
                            </button>
                          </motion.div>
                        )}
                        </AnimatePresence>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div>
                    <div className="grid gap-4 lg:grid-cols-[1.4fr_.8fr]">
                      <div className="relative overflow-hidden rounded-[32px] border border-white/10 bg-white/[0.04] p-6 sm:p-7">
                        <div className="absolute -right-14 -top-14 h-48 w-48 rounded-full border border-white/12" />
                        <div className="absolute -right-5 -top-5 h-28 w-28 rounded-full border border-white/7" />
                        <div className="relative flex items-start justify-between gap-4">
                          <div>
                            <div className="flex items-center gap-2 text-[9px] font-black uppercase tracking-[0.24em] text-white/55"><Radio size={12} /> Active group</div>
                            <h3 className="mt-3 text-3xl font-black tracking-[-0.04em] sm:text-4xl">{sync.group.name}</h3>
                            <p className="mt-3 text-sm text-white/35">{sync.members.length} device{sync.members.length === 1 ? "" : "s"} · {formatRemaining(sync.group.expiresAt)}</p>
                          </div>
                          <div className="flex -space-x-2">
                            {sync.members.slice(0, 4).map((member, index) => (
                              <div key={member.id} title={member.name} style={{ zIndex: 5 - index }} className={`flex h-10 w-10 items-center justify-center rounded-full border-2 border-[#111] ${member.online ? "bg-white text-black" : "bg-zinc-800 text-white/40"}`}>{deviceIcon(member, 15)}</div>
                            ))}
                          </div>
                        </div>
                      </div>
                      <button onClick={toggleNotifications} disabled={busy === "push"} className={`group rounded-[32px] border p-6 text-left transition ${notificationsEnabled ? "border-white/25 bg-white/[0.09]" : "border-white/10 bg-white/[0.025] hover:border-white/20"}`}>
                        <div className="flex items-start justify-between">
                          <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${notificationsEnabled ? "bg-white text-black" : "bg-white/8 text-white/50"}`}>{busy === "push" ? <Loader2 size={18} className="animate-spin" /> : notificationsEnabled ? <Bell size={18} /> : <BellOff size={18} />}</div>
                          <Settings2 size={15} className="text-white/20 transition group-hover:rotate-45" />
                        </div>
                        <p className="mt-6 text-sm font-black">Background alerts</p>
                        <p className="mt-1 text-xs leading-relaxed text-white/35">{notificationsEnabled ? "On · alerts arrive when Hefimer is closed" : "Off · tap to receive system notifications"}</p>
                      </button>
                    </div>

                    <div className="mt-5 flex gap-2 rounded-full border border-white/8 bg-white/[0.02] p-1.5">
                      <button onClick={() => setSection("devices")} className={`flex-1 rounded-full py-3 text-[10px] font-black uppercase tracking-[0.18em] transition ${section === "devices" ? "bg-white text-black" : "text-white/35"}`}>Devices</button>
                      <button onClick={() => setSection("activity")} className={`relative flex-1 rounded-full py-3 text-[10px] font-black uppercase tracking-[0.18em] transition ${section === "activity" ? "bg-white text-black" : "text-white/35"}`}>Activity {badgeCount > 0 && <span className="ml-1 opacity-60">{badgeCount}</span>}</button>
                    </div>

                    <AnimatePresence mode="wait" initial={false}>
                    {section === "devices" ? (
                      <motion.div key="devices" initial={{ opacity: 0, y: 12, filter: "blur(4px)" }} animate={{ opacity: 1, y: 0, filter: "blur(0px)" }} exit={{ opacity: 0, y: -8, filter: "blur(4px)" }} transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }} className="mt-5 grid gap-5 lg:grid-cols-[1.05fr_.95fr]">
                        <div className="rounded-[30px] border border-white/10 bg-white/[0.025] p-5">
                          <div className="flex items-center justify-between">
                            <div><p className="text-[9px] font-black uppercase tracking-[0.24em] text-white/30">Trusted lane</p><h4 className="mt-1 text-lg font-black">Connected devices</h4></div>
                            <span className="rounded-full border border-white/10 px-3 py-1 text-[9px] font-bold text-white/35">{sync.members.filter((member) => member.online).length} online</span>
                          </div>
                          <div className="mt-5 space-y-2">
                            {sync.members.map((member) => (
                              <div key={member.id} className="device-hub-row flex items-center gap-3 rounded-2xl border border-white/7 bg-black/25 p-3.5">
                                <div className={`relative flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl ${member.online ? "bg-white text-black" : "bg-white/5 text-white/35"}`}>
                                  {deviceIcon(member)}
                                  <span className={`absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-[#0a0a0a] ${member.online ? "bg-white" : "bg-zinc-600"}`} />
                                </div>
                                <div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{member.name}</p><p className="mt-0.5 text-[10px] text-white/30">{member.platform}{member.id === sync.device.id ? " · This device" : ""}{member.is_owner ? " · Owner" : ""}</p></div>
                                {isOwner && member.id !== sync.device.id && <button onClick={() => run(`remove:${member.id}`, () => deviceApi.removeDevice(member.id), "Device removed")} className="flex h-8 w-8 items-center justify-center rounded-full text-white/20 transition hover:bg-white/10 hover:text-white"><Trash2 size={14} /></button>}
                              </div>
                            ))}
                          </div>

                          <div className="mt-5 rounded-2xl border border-white/8 bg-black/25 p-4">
                            <label className="text-[9px] font-black uppercase tracking-[0.22em] text-white/30">This device name</label>
                            <div className="mt-2 flex gap-2"><input value={deviceName} onChange={(event) => setDeviceName(event.target.value)} maxLength={48} className="min-w-0 flex-1 rounded-xl border border-white/8 bg-black/40 px-4 py-3 text-xs font-bold outline-none focus:border-white/25" /><button onClick={() => run("rename", () => deviceApi.rename(deviceName), "Device renamed")} className="rounded-xl border border-white/10 px-4 text-[9px] font-black uppercase tracking-wider hover:bg-white/10">Save</button></div>
                          </div>
                        </div>

                        <div className="space-y-4">
                          {isOwner && (
                            <div className="rounded-[30px] border border-white/10 bg-gradient-to-br from-white/[0.08] to-transparent p-5">
                              <div className="flex items-center justify-between"><div><p className="text-[9px] font-black uppercase tracking-[0.24em] text-white/45">One-time entrance</p><h4 className="mt-1 text-lg font-black">Pair another device</h4></div><UserRoundPlus size={20} className="text-white/55" /></div>
                              {!invite ? (
                                <button onClick={createInvite} disabled={busy === "invite"} className="mt-5 flex w-full items-center justify-center gap-2 rounded-full bg-white py-3.5 text-[10px] font-black uppercase tracking-[0.18em] text-black disabled:opacity-40">{busy === "invite" ? <Loader2 size={14} className="animate-spin" /> : <QrCode size={14} />} Generate QR + token</button>
                              ) : (
                                <div className="mt-5">
                                  {qrDataUrl && <div className="mx-auto w-fit rounded-[24px] bg-white p-3"><img src={qrDataUrl} alt="Hefimer pairing QR code" className="h-40 w-40" /></div>}
                                  <button aria-label="Copy pairing token" onClick={() => copyValue(invite.token, "Pairing token copied")} className="mt-4 flex w-full items-center gap-3 rounded-2xl border border-white/10 bg-black/35 p-3 text-left hover:border-white/25 hover:bg-white/[0.06]"><span className="min-w-0 flex-1 truncate font-mono text-[10px] text-white/60">{invite.token}</span><AnimatePresence mode="wait" initial={false}>{copiedValue === invite.token ? <motion.span key="copied" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -4 }} className="flex shrink-0 items-center gap-1.5 text-[9px] font-black uppercase tracking-wider text-white"><Check size={13} /> Copied</motion.span> : <motion.span key="copy" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -4 }} className="flex shrink-0 items-center gap-1.5 text-[9px] font-black uppercase tracking-wider text-white/55"><Copy size={13} /> Copy token</motion.span>}</AnimatePresence></button>
                                  <div className="mt-3 flex items-center justify-between text-[9px] font-bold uppercase tracking-wider text-white/25"><span>Single use</span><span>{formatRemaining(invite.expiresAt)}</span></div>
                                  <button onClick={createInvite} className="mt-3 w-full py-2 text-[9px] font-black uppercase tracking-widest text-white/35 hover:text-white">Replace token</button>
                                </div>
                              )}
                            </div>
                          )}
                          <button onClick={() => run("auto", () => deviceApi.setAutoApprove(!sync.group!.autoApprove), sync.group!.autoApprove ? "Approval required again" : "Auto-approve enabled")} className={`flex w-full items-center gap-4 rounded-[26px] border p-4 text-left transition ${sync.group.autoApprove ? "border-white/25 bg-white/[0.09]" : "border-white/10 bg-white/[0.025]"}`}>
                            <div className={`flex h-10 w-10 items-center justify-center rounded-2xl ${sync.group.autoApprove ? "bg-white text-black" : "bg-white/5 text-white/35"}`}><PackageCheck size={18} /></div>
                            <div className="flex-1"><p className="text-sm font-black">Always approve</p><p className="mt-0.5 text-[10px] text-white/30">Accept offers from this group automatically</p></div>
                            <div className={`h-6 w-11 rounded-full p-1 transition ${sync.group.autoApprove ? "bg-white" : "bg-white/10"}`}><div className={`h-4 w-4 rounded-full transition ${sync.group.autoApprove ? "translate-x-5 bg-black" : "bg-white/45"}`} /></div>
                          </button>
                          <button onClick={() => run("leave", () => deviceApi.leaveGroup(), isOwner ? "Group deleted" : "Device unpaired")} className="flex w-full items-center justify-center gap-2 rounded-full border border-white/12 py-3.5 text-[9px] font-black uppercase tracking-[0.18em] text-white/40 transition hover:border-white/25 hover:bg-white/[0.06] hover:text-white"><LogOut size={13} /> {isOwner ? "Delete group for everyone" : "Leave this group"}</button>
                        </div>
                      </motion.div>
                    ) : (
                      <motion.div key="activity" initial={{ opacity: 0, y: 12, filter: "blur(4px)" }} animate={{ opacity: 1, y: 0, filter: "blur(0px)" }} exit={{ opacity: 0, y: -8, filter: "blur(4px)" }} transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }} className="mt-5 grid gap-5 lg:grid-cols-2">
                        <div className="rounded-[30px] border border-white/10 bg-white/[0.025] p-5">
                          <div className="flex items-center justify-between"><div><p className="text-[9px] font-black uppercase tracking-[0.24em] text-white/40">Incoming</p><h4 className="mt-1 text-lg font-black">Waiting for you</h4></div><Download size={19} className="text-white/45" /></div>
                          <div className="mt-5 space-y-3">
                            {!sync.incoming.length && <div className="rounded-2xl border border-dashed border-white/10 py-12 text-center text-xs text-white/25">Nothing incoming yet.</div>}
                            {sync.incoming.map((transfer) => (
                              <div key={transfer.id} className="device-hub-row rounded-2xl border border-white/8 bg-black/30 p-4">
                                <div className="flex items-start gap-3"><div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white/5">{transfer.kind === "file" ? <Download size={15} /> : <FileText size={15} />}</div><div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{transfer.name}</p><p className="mt-1 text-[10px] text-white/30">{transfer.sender_name} · {formatBytes(transfer.size_bytes)}</p></div><StatusPill status={transfer.recipient_status || "pending"} /></div>
                                {transfer.recipient_status === "pending" && <div className="mt-4 grid grid-cols-2 gap-2"><button onClick={() => decide(transfer, "decline")} className="rounded-full border border-white/10 py-2.5 text-[9px] font-black uppercase tracking-wider text-white/40">Decline</button><button onClick={() => decide(transfer, "approve")} className="rounded-full bg-white py-2.5 text-[9px] font-black uppercase tracking-wider text-black">Approve</button></div>}
                                {["approved", "downloading", "received"].includes(transfer.recipient_status || "") && <button onClick={() => openTransfer(transfer)} className="mt-4 flex w-full items-center justify-center gap-2 rounded-full bg-white py-2.5 text-[9px] font-black uppercase tracking-wider text-black"><ChevronRight size={13} /> Open transfer</button>}
                              </div>
                            ))}
                          </div>
                        </div>
                        <div className="rounded-[30px] border border-white/10 bg-white/[0.025] p-5">
                          <div className="flex items-center justify-between"><div><p className="text-[9px] font-black uppercase tracking-[0.24em] text-white/40">Outgoing</p><h4 className="mt-1 text-lg font-black">Sent from here</h4></div><Send size={19} className="text-white/45" /></div>
                          <div className="mt-5 space-y-3">
                            {!sync.outgoing.length && <div className="rounded-2xl border border-dashed border-white/10 py-12 text-center text-xs text-white/25">Your next drop will appear here.</div>}
                            {sync.outgoing.map((transfer) => (
                              <div key={transfer.id} className="device-hub-row rounded-2xl border border-white/8 bg-black/30 p-4">
                                <div className="flex items-start gap-3"><div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{transfer.name}</p><p className="mt-1 text-[10px] text-white/30">{transfer.provider} · {formatRemaining(transfer.expires_at)}</p></div><StatusPill status={transfer.transfer_status} /></div>
                                <div className="mt-3 flex flex-wrap gap-1.5">{transfer.recipients?.map((recipient) => <span key={recipient.device_id} title={recipient.device_name} className="flex items-center gap-1 rounded-full border border-white/8 px-2 py-1 text-[9px] text-white/35"><span className={`h-1.5 w-1.5 rounded-full ${recipient.status === "received" ? "bg-white" : recipient.status === "declined" || recipient.status === "failed" ? "bg-zinc-700" : "bg-white/55"}`} />{recipient.device_name}</span>)}</div>
                                {transfer.transfer_status === "active" && <button onClick={() => run(`cancel:${transfer.id}`, () => deviceApi.cancelTransfer(transfer.id), "Transfer cancelled")} className="mt-3 flex items-center gap-1.5 text-[9px] font-black uppercase tracking-wider text-white/25 hover:text-white"><X size={11} /> Cancel offer</button>}
                              </div>
                            ))}
                          </div>
                        </div>
                      </motion.div>
                    )}
                    </AnimatePresence>
                  </div>
                )}
              </div>
            </motion.section>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
