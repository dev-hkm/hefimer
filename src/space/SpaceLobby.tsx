import React, { useEffect, useState } from "react";
import { motion } from "motion/react";
import {
  ArrowLeft,
  ArrowRight,
  Box,
  Check,
  Clock,
  Copy,
  Hash,
  Link,
  Loader2,
  Lock,
  LogIn,
  Plus,
  QrCode,
  RefreshCw,
  User,
} from "lucide-react";
import { getUserColor, type RecentSpaceEntry, type SpaceState } from "./types";

interface SpaceCreatedViewProps {
  spaceId: string;
  spaceName: string;
  onCopyCode: () => void;
  onCopyLink: () => void;
  copiedCode: boolean;
  copiedLink: boolean;
  onOpenQR: () => void;
  onEnterSpace: () => void;
  onBack: () => void;
}

export function SpaceCreatedView({
  spaceId,
  spaceName,
  onCopyCode,
  onCopyLink,
  copiedCode,
  copiedLink,
  onOpenQR,
  onEnterSpace,
  onBack,
}: SpaceCreatedViewProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95 }}
      className="flex flex-col items-center py-6 space-y-6 relative w-full pt-14 text-center max-w-[440px] mx-auto"
    >
      <button
        type="button"
        onClick={onBack}
        className="absolute top-0 left-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
      >
        <ArrowLeft size={10} /> Back
      </button>
      <button
        type="button"
        onClick={onBack}
        className="absolute top-0 right-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
      >
        New Space <Plus size={10} />
      </button>

      <div className="flex flex-col items-center space-y-1.5">
        <p className="text-[10px] font-bold uppercase tracking-[0.22em] text-emerald-400">
          {spaceName || "Space Ready"}
        </p>
        <h3 className="text-white/40 text-[10px] font-bold uppercase tracking-[0.2em]">
          Your Space Code
        </h3>
        <div className="h-0.5 w-6 bg-white/10 rounded-full" />
      </div>

      <div className="flex items-center justify-center gap-2.5">
        {spaceId.split("").map((digit, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, scale: 0.5, rotate: -12, y: 18 }}
            animate={{ opacity: 1, scale: 1, rotate: 0, y: 0 }}
            transition={{ type: "spring", stiffness: 400, damping: 16, delay: i * 0.06 + 0.12 }}
            whileHover={{ scale: 1.05, y: -4, transition: { duration: 0.18 } }}
            className="w-12 h-16 sm:w-14 sm:h-20 bg-white/[0.04] border-white/10 text-white border rounded-[28px] flex items-center justify-center text-3xl sm:text-4xl font-black font-sans relative group cursor-default shadow-lg"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-white/[0.05] to-transparent rounded-[28px] pointer-events-none" />
            <span className="relative z-10 tracking-tight drop-shadow-sm">{digit}</span>
          </motion.div>
        ))}
      </div>

      <p className="text-white/35 text-[11px] font-medium tracking-wide leading-relaxed px-4">
        Share this 5-digit code or direct link with teammates to drop files, sticky notes, links, and code together.
      </p>

      <div className="flex gap-3 w-full pt-1">
        <button
          type="button"
          onClick={onCopyLink}
          className={`flex-1 border rounded-full px-5 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
            copiedLink
              ? "bg-white text-black border-white"
              : "bg-white/10 hover:bg-white/20 text-white/75 border-white/10"
          }`}
        >
          {copiedLink ? <Check size={16} /> : <Link size={16} />}
          {copiedLink ? "Copied!" : "Copy Link"}
        </button>
        <button
          type="button"
          onClick={onCopyCode}
          className={`flex-1 border rounded-full px-5 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
            copiedCode
              ? "bg-white text-black border-white"
              : "bg-white/10 hover:bg-white/20 text-white/75 border-white/10"
          }`}
        >
          {copiedCode ? <Check size={16} /> : <Copy size={16} />}
          {copiedCode ? "Copied!" : "Copy Code"}
        </button>
      </div>

      <div className="flex flex-col space-y-3 w-full pt-1">
        <button
          type="button"
          onClick={onOpenQR}
          className="w-full py-3.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/80 rounded-full font-bold text-xs transition-all flex items-center justify-center gap-2"
        >
          <QrCode size={16} />
          Share via QR Code
        </button>

        <button
          type="button"
          onClick={onEnterSpace}
          className="w-full bg-white text-black py-3.5 rounded-full font-bold text-xs hover:scale-[1.01] active:scale-[0.98] transition-all flex items-center justify-center gap-1.5 shadow-[0_0_20px_rgba(255,255,255,0.1)]"
        >
          Enter Space
          <ArrowRight size={16} />
        </button>
      </div>
    </motion.div>
  );
}

interface SpaceLobbyProps {
  state: SpaceState;
  setState: React.Dispatch<React.SetStateAction<SpaceState>>;
  customCode: string;
  onCustomCodeChange: (val: string) => void;
  onRandomizeCode: () => void;
  checkingCode: boolean;
  codeStatus: "available" | "taken" | "idle";
  loading: boolean;
  recentSpaces: RecentSpaceEntry[];
  createPassword: string;
  onCreatePasswordChange: (val: string) => void;
  joinPassword: string;
  onJoinPasswordChange: (val: string) => void;
  joinRequiresPassword: boolean;
  onCreateSpace: (e?: React.FormEvent) => void;
  onJoinSpace: (e?: React.FormEvent) => void;
  onSelectRecent: (entry: RecentSpaceEntry) => void;
}

const EXPIRE_OPTIONS = [
  { label: "1 Hour", value: "1h" },
  { label: "6 Hours", value: "6h" },
  { label: "1 Day", value: "24h" },
  { label: "3 Days", value: "72h" },
];

export function SpaceLobby({
  state,
  setState,
  customCode,
  onCustomCodeChange,
  onRandomizeCode,
  checkingCode,
  codeStatus,
  loading,
  recentSpaces,
  createPassword,
  onCreatePasswordChange,
  joinPassword,
  onJoinPasswordChange,
  joinRequiresPassword,
  onCreateSpace,
  onJoinSpace,
  onSelectRecent,
}: SpaceLobbyProps) {
  const { spaceId, spaceNameInput, spaceExpire, userName } = state;
  const [showPassField, setShowPassField] = useState(false);

  useEffect(() => {
    if (createPassword) setShowPassField(true);
  }, [createPassword]);

  const userColor = getUserColor(userName || "Guest");

  return (
    <motion.div
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-[900px] mx-auto px-2 sm:px-4"
    >
      <div className="space-y-8">
        {/* Display Name Card */}
        <div className="max-w-[420px] mx-auto text-center space-y-2.5 bg-[#0a0a0a]/90 border border-white/10 rounded-3xl p-5 shadow-xl">
          <div className="flex items-center justify-center gap-2">
            <span
              className="w-2.5 h-2.5 rounded-full"
              style={{ backgroundColor: userColor }}
            />
            <label className="text-[10px] uppercase tracking-[0.2em] font-bold text-white/40 block">
              Your Display Name
            </label>
          </div>
          <div className="relative">
            <User className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={17} />
            <input
              value={userName}
              onChange={(e) => setState((prev) => ({ ...prev, userName: e.target.value }))}
              placeholder="Enter your name..."
              maxLength={32}
              className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-11 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/25 focus:bg-white/[0.06] transition-all placeholder:text-white/25 text-center font-bold"
            />
          </div>
        </div>

        {/* Recent Spaces Quick Strip */}
        {recentSpaces.length > 0 && (
          <div className="flex flex-wrap items-center justify-center gap-2">
            <span className="text-[10px] uppercase tracking-[0.18em] font-bold text-white/30 flex items-center gap-1 mr-1">
              <Clock size={11} />
              Recent:
            </span>
            {recentSpaces.map((entry) => (
              <button
                key={entry.code}
                type="button"
                onClick={() => onSelectRecent(entry)}
                className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-white/10 bg-white/[0.03] hover:bg-white/[0.08] hover:border-white/25 text-xs text-white/75 transition-all"
              >
                <span className="font-mono font-bold text-emerald-300">{entry.code}</span>
                <span className="text-white/50 max-w-[120px] truncate">{entry.name}</span>
              </button>
            ))}
          </div>
        )}

        {/* Dual Column: Join vs Create */}
        <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-10 md:gap-0 relative">
          <div className="hidden md:block absolute left-1/2 top-0 bottom-0 w-px bg-white/10 -translate-x-1/2 pointer-events-none" />

          {/* Join Form */}
          <form
            onSubmit={onJoinSpace}
            className="space-y-6 md:pr-12 text-left flex flex-col justify-between"
          >
            <div className="space-y-2">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-white/60">
                  <LogIn size={18} />
                </div>
                <div>
                  <h3 className="text-xl font-bold tracking-tight text-white">Join Space</h3>
                  <p className="text-xs text-white/40">Access an active canvas</p>
                </div>
              </div>
              <p className="text-[11px] text-white/30 leading-relaxed pt-1">
                Enter the 5-digit space code shared by your teammate to collaborate in real time.
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
                      setState((prev) => ({
                        ...prev,
                        spaceId: e.target.value.replace(/\D/g, "").slice(0, 5),
                      }))
                    }
                    placeholder="5-digit Space ID"
                    maxLength={5}
                    inputMode="numeric"
                    className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-10 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/25 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold tracking-widest"
                  />
                </div>
              </div>

              {joinRequiresPassword && (
                <div className="space-y-2">
                  <label className="text-[10px] uppercase tracking-widest font-bold text-amber-300/80 block ml-3">
                    Space Passcode Required
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-amber-300/60" size={15} />
                    <input
                      type="password"
                      value={joinPassword}
                      onChange={(e) => onJoinPasswordChange(e.target.value)}
                      placeholder="Enter space passcode..."
                      autoFocus
                      className="w-full bg-white/[0.04] border border-amber-400/30 rounded-2xl pl-10 pr-4 py-3 text-sm text-white focus:outline-none focus:border-amber-400/60 transition-all placeholder:text-white/25"
                    />
                  </div>
                </div>
              )}

              <button
                type="submit"
                disabled={loading || spaceId.length !== 5}
                className="w-full bg-white text-black py-3 rounded-2xl font-bold text-xs hover:scale-[1.01] active:scale-[0.98] transition-all disabled:opacity-45 disabled:cursor-not-allowed shadow-md shadow-white/5 flex items-center justify-center gap-1.5"
              >
                {loading ? <Loader2 size={16} className="animate-spin" /> : "Enter Space"}
                <ArrowRight size={16} />
              </button>
            </div>
          </form>

          {/* Create Form */}
          <form
            onSubmit={onCreateSpace}
            className="space-y-6 md:pl-12 text-left flex flex-col justify-between"
          >
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
                Start a temporary infinite workspace for files, sticky notes, links, and code snippets.
              </p>
            </div>

            <div className="space-y-4 pt-2">
              <div className="space-y-2">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Space Title
                </label>
                <input
                  value={spaceNameInput}
                  onChange={(e) => setState((prev) => ({ ...prev, spaceNameInput: e.target.value }))}
                  placeholder="e.g. Design Assets, Sprint Board"
                  maxLength={50}
                  className="w-full bg-white/[0.04] border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-white/25 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-medium"
                />
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between ml-3">
                  <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block">
                    Custom Code
                  </label>
                  <button
                    type="button"
                    onClick={onRandomizeCode}
                    className="inline-flex items-center gap-1 text-[9px] uppercase tracking-wider font-bold text-white/40 hover:text-white transition-colors"
                  >
                    <RefreshCw size={10} />
                    Randomize
                  </button>
                </div>
                <div className="relative">
                  <Hash className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={16} />
                  <input
                    value={customCode}
                    onChange={(e) => onCustomCodeChange(e.target.value)}
                    placeholder="5-digit ID"
                    maxLength={5}
                    inputMode="numeric"
                    className={`w-full bg-white/[0.04] border ${
                      customCode.length === 5
                        ? codeStatus === "available"
                          ? "border-green-500/35 focus:border-green-500/60"
                          : codeStatus === "taken"
                            ? "border-red-500/35 focus:border-red-500/60"
                            : "border-white/10"
                        : "border-white/10"
                    } rounded-2xl pl-10 pr-16 py-3 text-sm text-white focus:outline-none focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold tracking-widest`}
                  />
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center">
                    {checkingCode ? (
                      <Loader2 size={14} className="text-white/30 animate-spin" />
                    ) : customCode.length === 5 ? (
                      codeStatus === "available" ? (
                        <Check size={16} className="text-green-400" />
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
                <div className="flex items-center justify-between ml-3">
                  <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block">
                    Auto-Delete After
                  </label>
                  <button
                    type="button"
                    onClick={() => setShowPassField((v) => !v)}
                    className="inline-flex items-center gap-1 text-[9px] uppercase tracking-wider font-bold text-white/40 hover:text-white transition-colors"
                  >
                    <Lock size={10} />
                    {showPassField ? "Hide Lock" : "Passcode Lock"}
                  </button>
                </div>
                <div className="flex gap-1.5 p-1 bg-white/[0.02] border border-white/5 rounded-2xl">
                  {EXPIRE_OPTIONS.map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => setState((prev) => ({ ...prev, spaceExpire: opt.value }))}
                      className={`flex-1 py-2 text-[11px] font-bold rounded-xl transition-all ${
                        spaceExpire === opt.value || (!spaceExpire && opt.value === "1h")
                          ? "bg-white/15 text-white shadow-sm"
                          : "text-white/35 hover:text-white/70 hover:bg-white/[0.04]"
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {showPassField && (
                <div className="space-y-1.5">
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={15} />
                    <input
                      type="password"
                      value={createPassword}
                      onChange={(e) => onCreatePasswordChange(e.target.value)}
                      placeholder="Optional passcode..."
                      className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-sm text-white focus:outline-none focus:border-white/25 placeholder:text-white/25"
                    />
                  </div>
                </div>
              )}

              <button
                type="submit"
                disabled={
                  loading ||
                  !spaceNameInput.trim() ||
                  customCode.length !== 5 ||
                  codeStatus === "taken"
                }
                className="w-full bg-white/10 border border-white/15 text-white py-3 rounded-2xl font-bold text-xs hover:bg-white/15 transition-all disabled:opacity-45 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {loading ? <Loader2 size={16} className="animate-spin" /> : <Plus size={16} />}
                Create Space
              </button>
            </div>
          </form>
        </div>
      </div>
    </motion.div>
  );
}
