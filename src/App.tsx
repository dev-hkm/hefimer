import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence, type Variants } from "motion/react";
import {
  Send,
  FileUp,
  Download,
  Copy,
  ExternalLink,
  Check,
  AlertCircle,
  X,
  Loader2,
  Clock,
  Trash2,
  Code,
  Eye,
  EyeOff,
  MessageSquare,
  User,
  Plus,
  LogIn,
  ArrowLeft,
  SendHorizontal,
  ArrowUp,
  Heart,
  ThumbsUp,
  ThumbsDown,
  Smile,
  Frown,
  Flame,
  PartyPopper,
  Ghost,
  Rocket,
  Star,
  Zap,
  LogOut,
  Palette,
  Eraser,
  MousePointer2,
  Minus,
  Circle,
  Square,
  Radio,
  Bell,
  BellOff,
  Edit3,
  ShieldAlert,
  Snowflake,
  Users,
  ShieldCheck,
  Shield,
  FileText,
  Lock,
  ChevronDown,
  Mail,
  Phone,
  MapPin,
  Folder,
  FolderPlus,
  File,
  Sun,
  Moon,
  Monitor,
  QrCode,
  Share2,
  Link,
  Hash,
  ArrowRight,
  Dices,
  Home,
  Files,
  Image,
  Video,
  Music,
  Archive,
  Box,
} from "lucide-react";
import Editor from "react-simple-code-editor";
import Prism from "prismjs";
import JSZip from "jszip";
import "prismjs/components/prism-clike";
import "prismjs/components/prism-markup";
import "prismjs/components/prism-markup-templating";
import "prismjs/components/prism-javascript";
import "prismjs/components/prism-typescript";
import "prismjs/components/prism-css";
import "prismjs/components/prism-json";
import "prismjs/components/prism-bash";
import "prismjs/components/prism-sql";
import "prismjs/components/prism-markdown";
import "prismjs/components/prism-yaml";
import "prismjs/components/prism-python";
import "prismjs/components/prism-c";
import "prismjs/components/prism-cpp";
import "prismjs/components/prism-csharp";
import "prismjs/components/prism-java";
import "prismjs/components/prism-kotlin";
import "prismjs/components/prism-swift";
import "prismjs/components/prism-go";
import "prismjs/components/prism-rust";
import "prismjs/components/prism-ruby";
import "prismjs/components/prism-php";
import "prismjs/components/prism-docker";
import "prismjs/components/prism-lua";
import "prismjs/components/prism-makefile";
import "prismjs/components/prism-graphql";
import "prismjs/components/prism-scss";
import "prismjs/components/prism-kotlin";
import "prismjs/components/prism-swift";
import QRCode from "qrcode";
import { Space } from "./Space";
import { FeatureStationVisual, PairedDeviceConstellation, TransferTunnel } from "./landing/TransferTunnel";
import { DeviceHub } from "./devices/DeviceHub";
import type { DeviceTransfer } from "./devices/device-api";
import {
  downloadFileInBrowser,
  getDirectDownloadMode,
} from "./downloads";

import { rtdb, auth } from "./firebase";
import {
  ref,
  set,
  get,
  child,
  serverTimestamp,
  onValue,
  off,
  push,
  remove,
  update,
  onDisconnect,
  runTransaction,
  query,
  orderByChild,
  endAt,
} from "firebase/database";

enum OperationType {
  CREATE = "create",
  UPDATE = "update",
  DELETE = "delete",
  LIST = "list",
  GET = "get",
  WRITE = "write",
}

interface DatabaseErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId: string | undefined;
    email: string | null | undefined;
    emailVerified: boolean | undefined;
    isAnonymous: boolean | undefined;
    tenantId: string | null | undefined;
    providerInfo: {
      providerId: string;
      displayName: string | null;
      email: string | null;
      photoUrl: string | null;
    }[];
  };
}

function handleDatabaseError(
  error: unknown,
  operationType: OperationType,
  path: string | null,
) {
  const errInfo: DatabaseErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo:
        auth.currentUser?.providerData.map((provider) => ({
          providerId: provider.providerId,
          displayName: provider.displayName,
          email: provider.email,
          photoUrl: provider.photoURL,
        })) || [],
    },
    operationType,
    path,
  };
  console.error("Database Error: ", JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

const vibrate = () => {};

const sanitizeError = (errStr: string | undefined) => {
  if (!errStr) return "";
  if (errStr.includes("BunkerWeb") || errStr.includes("<!DOCTYPE html>")) {
    return "Litterbox server is currently blocking requests (BunkerWeb protection). Please try again later.";
  }
  if (errStr.length > 150) {
    return errStr.substring(0, 150) + "...";
  }
  return errStr;
};

const parseExpire = (expireStr: string) => {
  const match =
    expireStr.match(/^(\d+)([smhpdw])$/i) ||
    expireStr.match(/^(\d+)(sec|min|h|d|w)$/i);
  if (!match) return 60 * 60 * 1000; // default 1h
  const val = parseInt(match[1]);
  const unit = match[2].toLowerCase();
  if (unit === "s" || unit === "sec") return val * 1000;
  if (unit === "m" || unit === "min" || unit === "p") return val * 60 * 1000;
  if (unit === "h") return val * 60 * 60 * 1000;
  if (unit === "d") return val * 24 * 60 * 60 * 1000;
  if (unit === "w") return val * 7 * 24 * 60 * 60 * 1000;
  return 60 * 60 * 1000;
};

const formatFileSize = (text: string) => {
  if (!text) return "0 B";
  let bytes = 0;
  try {
    bytes = new TextEncoder().encode(text).length;
  } catch (e) {
    bytes = text.length;
  }
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
};

const smartIsLink = (text: string) => {
  if (!text) return false;
  const t = text.trim();
  if (/\s/.test(t)) return false;
  const pattern =
    /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/i;
  const commonStarts = ["http://", "https://", "www.", "ftp://"];
  return (
    commonStarts.some((s) => t.toLowerCase().startsWith(s)) || pattern.test(t)
  );
};

const getSmartAction = (text: string) => {
  if (!text) return null;
  const t = text.trim();

  // Email
  if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(t)) {
    return {
      type: "email",
      label: "Send Email",
      Icon: Mail,
      url: `mailto:${t}`,
    };
  }

  // Phone
  if (/^\+?[\d\s-]{8,15}$/.test(t) && !/\n/.test(t)) {
    const cleanPhone = t.replace(/[^\d+]/g, "");
    return {
      type: "phone",
      label: "Call Now",
      Icon: Phone,
      url: `tel:${cleanPhone}`,
    };
  }

  // Coordinates
  if (/^-?\d+(\.\d+)?,\s?-?\d+(\.\d+)?$/.test(t)) {
    return {
      type: "map",
      label: "Maps",
      Icon: MapPin,
      url: `https://www.google.com/maps?q=${encodeURIComponent(t)}`,
    };
  }

  // Hex Color
  if (t.length <= 9 && /^#(?:[0-9a-fA-F]{3,4}){1,2}$/.test(t)) {
    return {
      type: "color",
      label: "Color Info",
      Icon: Palette,
      url: `https://www.color-hex.com/color/${t.replace("#", "")}`,
    };
  }

  // IP Address
  if (/^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$/.test(t)) {
    return {
      type: "ip",
      label: "Whois IP",
      Icon: ShieldAlert,
      url: `https://who.is/whois-ip/ip-address/${t}`,
    };
  }

  // Link
  if (smartIsLink(t)) {
    const url = t.startsWith("http")
      ? t
      : t.toLowerCase().startsWith("ftp")
        ? t
        : `https://${t}`;
    return { type: "link", label: "Open", Icon: ExternalLink, url };
  }

  return null;
};

const detectLanguageAndExtension = (text: string) => {
  if (!text || typeof text !== "string")
    return { id: "text", label: "Plain Text", ext: "txt" };

  const t = text.trim();
  const lines = t.split("\n");
  const firstNonCommentLine =
    lines
      .find((l) => {
        const trimmed = l.trim();
        return (
          trimmed &&
          !trimmed.startsWith("--") &&
          !trimmed.startsWith("//") &&
          !trimmed.startsWith("/*") &&
          !trimmed.startsWith("#")
        );
      })
      ?.trim() || "";

  // HTML / XML / SVG
  if (/^<!DOCTYPE html>|<html/i.test(t) || /<body|<div|<span|<p|<a /i.test(t)) {
    return { id: "markup", label: "HTML", ext: "html" };
  }
  if (/^<\?xml/i.test(t)) {
    if (/<svg/i.test(t)) return { id: "markup", label: "SVG", ext: "svg" };
    return { id: "markup", label: "XML", ext: "xml" };
  }
  if (/<svg/i.test(t)) return { id: "markup", label: "SVG", ext: "svg" };

  // JSON
  if (/^[\{[]/.test(t) && /[\}\]]$/.test(t)) {
    // Check if it looks like JSON without fully parsing (which is expensive)
    // or if it's small enough, just parse it.
    if (t.length < 50000) {
      try {
        JSON.parse(t);
        return { id: "json", label: "JSON", ext: "json" };
      } catch (e) {}
    } else if (/^[\{[]\s*("|\d|true|false|null)/.test(t)) {
      return { id: "json", label: "JSON", ext: "json" };
    }
  }

  // Dockerfile
  if (
    /^FROM\s+[a-z0-9]/i.test(t) &&
    (/RUN\s+/m.test(t) || /EXPOSE\s+/m.test(t) || /ENV\s+/m.test(t))
  ) {
    return { id: "docker", label: "Dockerfile", ext: "dockerfile" };
  }

  // Makefile
  if (/^[a-zA-Z0-9_-]+:\s*($|\n)/m.test(t) && t.includes("\t")) {
    return { id: "makefile", label: "Makefile", ext: "makefile" };
  }

  // Lua
  if (
    /^local\s+[a-zA-Z_]+|^function\s+[a-zA-Z_]+/.test(t) ||
    /end\s*$/.test(t) ||
    /then\n/.test(t)
  ) {
    if (!/interface |type |class /.test(t))
      return { id: "lua", label: "Lua", ext: "lua" };
  }

  // GraphQL
  if (
    /^(query|mutation|subscription|fragment|type|input|schema|enum)\s+[a-zA-Z_]+/.test(
      t,
    ) &&
    t.includes("{")
  ) {
    return { id: "graphql", label: "GraphQL", ext: "graphql" };
  }

  // CSS / SCSS
  if (
    /^[.#a-zA-Z0-9-]+\s*\{/.test(t) ||
    /@import|@media|@font-face|@mixin|@include/.test(t)
  ) {
    if (/\$[a-zA-Z0-9_-]+:/.test(t) || /@mixin|@include/.test(t))
      return { id: "scss", label: "SCSS", ext: "scss" };
    return { id: "css", label: "CSS", ext: "css" };
  }

  // SQL - Improved detection
  const sqlKeywords =
    /^(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|WITH|GRANT|REVOKE|USE|BEGIN|COMMIT|ROLLBACK|DESCRIBE|EXPLAIN|SHOW|SET)\s/i;
  const sqlPatterns =
    /\b(CREATE TABLE|INSERT INTO|UPDATE .* SET|DELETE FROM|SELECT .* FROM|ALTER TABLE|DROP TABLE|GRANT .* TO|CREATE DATABASE|USE .*|PRIMARY KEY|AUTO_INCREMENT|VARCHAR|TIMESTAMP|DEFAULT CURRENT_TIMESTAMP|FOREIGN KEY|REFERENCES|JOIN|WHERE|GROUP BY|ORDER BY|HAVING|LIMIT)\b/i;
  if (
    sqlKeywords.test(firstNonCommentLine) ||
    sqlPatterns.test(t) ||
    (t.includes("--") && sqlKeywords.test(firstNonCommentLine))
  ) {
    return { id: "sql", label: "SQL", ext: "sql" };
  }

  // Python
  if (
    /^(def |class |import |from .* import |if __name__ == ['"]__main__['"]:)/m.test(
      t,
    ) ||
    t.includes(":\n    ")
  ) {
    return { id: "python", label: "Python", ext: "py" };
  }

  // C# / Java
  if (/^(public class |using System;|namespace |\[SerializeField\])/m.test(t)) {
    if (/using System;|\[SerializeField\]/.test(t))
      return { id: "csharp", label: "C#", ext: "cs" };
    return { id: "java", label: "Java", ext: "java" };
  }

  // C / C++
  if (/^#include\s*</m.test(t) || /using namespace std;/.test(t)) {
    if (/iostream|vector|string|std::|cout|cin/.test(t))
      return { id: "cpp", label: "C++", ext: "cpp" };
    return { id: "c", label: "C", ext: "c" };
  }

  // Go
  if (/^package main|^import \(/m.test(t) && /func main\(\)/.test(t)) {
    return { id: "go", label: "Go", ext: "go" };
  }

  // Rust
  if (/^fn main\(\)|^use std::|^let mut/m.test(t)) {
    return { id: "rust", label: "Rust", ext: "rs" };
  }

  // PHP
  if (/^<\?php/i.test(t) || /namespace\s+[a-zA-Z0-9\\]+;/.test(t)) {
    return { id: "php", label: "PHP", ext: "php" };
  }

  // Ruby
  if (
    /^def [a-zA-Z_]+|^require ['"][a-zA-Z_]+['"]/m.test(t) ||
    t.includes("do\n")
  ) {
    return { id: "ruby", label: "Ruby", ext: "rb" };
  }

  // Bash / Shell
  if (
    /^#!\/bin\/(bash|sh|zsh)/.test(t) ||
    /^(echo|ls|cd|mkdir|rm|cp|mv|sudo|apt-get|npm|yarn|git)\s/m.test(t)
  ) {
    return { id: "bash", label: "Bash", ext: "sh" };
  }

  // JavaScript / TypeScript
  if (
    /^(const|let|var|function|import|export|class|interface|type)\s/m.test(t) ||
    /=>/.test(t) ||
    /console\.log/.test(t)
  ) {
    if (/interface |type [A-Z]|: [a-z]+/.test(t))
      return { id: "typescript", label: "TypeScript", ext: "ts" };
    return { id: "javascript", label: "JavaScript", ext: "js" };
  }

  // Kotlin
  if (
    /^(package |import |fun |val |var |class |object |interface )/m.test(t) ||
    t.includes("?:") ||
    t.includes("!!")
  ) {
    if (!/interface |type |class /.test(t))
      return { id: "kotlin", label: "Kotlin", ext: "kt" };
  }

  // Swift
  if (
    /^(import |func |let |var |class |struct |enum |extension |protocol )\s/.test(
      t,
    ) ||
    t.includes("print(")
  ) {
    if (!/package |fun |val |var /.test(t))
      return { id: "swift", label: "Swift", ext: "swift" };
  }

  // Markdown
  if (/^#\s|^##\s|^\*\s|^-\s|^\[.*\]\(.*\)/m.test(t)) {
    return { id: "markdown", label: "Markdown", ext: "md" };
  }

  // YAML
  if (
    /^[a-zA-Z0-9_-]+:\s+/.test(t) &&
    (t.includes("\n  ") || t.includes("\n- "))
  ) {
    return { id: "yaml", label: "YAML", ext: "yaml" };
  }

  return { id: "text", label: "Plain Text", ext: "txt" };
};

const SUPPORTED_LANGUAGES = [
  { id: "text", label: "Plain Text" },
  { id: "sql", label: "SQL" },
  { id: "javascript", label: "JavaScript" },
  { id: "typescript", label: "TypeScript" },
  { id: "python", label: "Python" },
  { id: "java", label: "Java" },
  { id: "cpp", label: "C++" },
  { id: "csharp", label: "C#" },
  { id: "go", label: "Go" },
  { id: "rust", label: "Rust" },
  { id: "php", label: "PHP" },
  { id: "ruby", label: "Ruby" },
  { id: "bash", label: "Bash" },
  { id: "markup", label: "HTML" },
  { id: "css", label: "CSS" },
  { id: "json", label: "JSON" },
  { id: "markdown", label: "Markdown" },
  { id: "yaml", label: "YAML" },
  { id: "docker", label: "Dockerfile" },
  { id: "makefile", label: "Makefile" },
  { id: "lua", label: "Lua" },
  { id: "graphql", label: "GraphQL" },
  { id: "scss", label: "SCSS" },
  { id: "kotlin", label: "Kotlin" },
  { id: "swift", label: "Swift" },
];

const glassCard =
  "bg-black border border-white/20 rounded-[32px] p-6 shadow-[0_0_50px_-12px_rgba(255,255,255,0.2)]";
const glassInput =
  "w-full bg-white/5 border border-white/20 rounded-full px-6 py-4 text-white placeholder-white/30 focus:outline-none focus:ring-1 focus:ring-white/50 transition-all";
const glassButton =
  "w-full bg-white text-black hover:bg-white/90 active:scale-[0.98] rounded-full px-6 py-4 font-bold transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-white/10";
const glassTab =
  "flex-1 py-3 text-center rounded-full transition-all font-bold text-sm sm:text-base";

const copyToClipboard = async (text: string) => {
  if (!text) throw new Error("Nothing to copy");
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.setAttribute("readonly", "");
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.select();
    const copied = document.execCommand("copy");
    document.body.removeChild(textarea);
    if (!copied) throw new Error("Clipboard copy was blocked");
  }
};

function ResultView({
  code,
  fileName,
  expiresAt,
  onReset,
  showToast,
  isR2,
  onBack,
}: any) {
  const [copied, setCopied] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [timeLeft, setTimeLeft] = useState("");
  const [showQR, setShowQR] = useState(false);
  const [downloadQRFn, setDownloadQRFn] = useState<(() => void) | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const handleCopyLink = async () => {
    vibrate();
    try {
      await copyToClipboard(`${window.location.origin}/?code=${code}`);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
      showToast("Link copied", "success");
    } catch (err: any) {
      showToast(err.message || "Failed to copy link", "error");
    }
  };

  useEffect(() => {
    if (!expiresAt) return;
    const updateTimer = () => {
      const now = Date.now();
      const diff = expiresAt - now;
      if (diff <= 0) {
        setTimeLeft("Expired");
        return;
      }
      const m = Math.floor(diff / 60000);
      const s = Math.floor((diff % 60000) / 1000);
      const h = Math.floor(m / 60);
      const remM = m % 60;
      if (h > 0) {
        setTimeLeft(`${h}h ${remM}m ${s}s`);
      } else {
        setTimeLeft(`${m}m ${s}s`);
      }
    };
    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [expiresAt]);

  const handleCopy = () => {
    vibrate();
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95 }}
      className="flex flex-col items-center py-6 space-y-6 relative w-full pt-14"
    >
      {onBack && (
        <button
          onClick={onBack}
          className={`absolute top-0 left-0 flex items-center gap-1.5 px-3 py-1.5 border rounded-full font-bold text-[10px] uppercase tracking-wider transition-all cursor-pointer ${
            isR2
              ? "border-emerald-500/10 bg-emerald-500/5 hover:bg-emerald-500/10 text-emerald-400/60 hover:text-emerald-400 active:scale-95"
              : "border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white active:scale-95"
          }`}
        >
          <ArrowLeft size={10} /> Back
        </button>
      )}
      <button
        onClick={() => {
          vibrate();
          onReset();
        }}
        className={`absolute top-0 right-0 flex items-center gap-1.5 px-3 py-1.5 border rounded-full font-bold text-[10px] uppercase tracking-wider transition-all cursor-pointer ${
          isR2
            ? "border-emerald-500/10 bg-emerald-500/5 hover:bg-emerald-500/10 text-emerald-400/60 hover:text-emerald-400 active:scale-95"
            : "border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white active:scale-95"
        }`}
      >
        New Drop <Plus size={10} />
      </button>

      <div className="flex flex-col items-center space-y-1.5">
        <h3
          className={`${isR2 ? "text-emerald-400/60" : "text-white/40"} text-[10px] font-bold uppercase tracking-[0.2em]`}
        >
          {isR2 ? "Secret R2 Code" : "Your Drop Code"}
        </h3>
        <div
          className={`h-0.5 w-6 ${isR2 ? "bg-emerald-500/30" : "bg-white/10"} rounded-full`}
        />
      </div>

      <div className="flex items-center justify-center gap-2.5">
        {code.split("").map((digit: string, i: number) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, scale: 0.5, rotate: -15, y: 20 }}
            animate={{ opacity: 1, scale: 1, rotate: 0, y: 0 }}
            transition={{
              type: "spring",
              stiffness: 400,
              damping: 15,
              delay: i * 0.08 + 0.2,
            }}
            whileHover={{ scale: 1.05, y: -5, transition: { duration: 0.2 } }}
            className={`w-12 h-16 sm:w-14 sm:h-20 ${isR2 ? "bg-emerald-500/[0.06] border-emerald-500/20 text-emerald-400" : "bg-white/[0.04] border-white/10 text-white"} border rounded-[28px] flex items-center justify-center text-3xl sm:text-4xl font-black font-sans relative group cursor-default shadow-lg ${isR2 ? "shadow-emerald-500/5" : ""}`}
          >
            <div
              className={`absolute inset-0 bg-gradient-to-br ${isR2 ? "from-emerald-500/[0.05]" : "from-white/[0.05]"} to-transparent rounded-[28px] pointer-events-none`}
            />
            <span className="relative z-10 tracking-tight drop-shadow-sm">
              {digit}
            </span>
            <div
              className={`absolute inset-0 rounded-[28px] border ${isR2 ? "border-emerald-500/0 group-hover:border-emerald-500/40" : "border-white/0 group-hover:border-white/20"} transition-all duration-300`}
            />
          </motion.div>
        ))}
      </div>

      <div className="flex flex-col items-center space-y-3 w-full">
        {timeLeft && (
          <div
            className={`flex items-center gap-2 ${isR2 ? "bg-emerald-500/10 border-emerald-500/10" : "bg-white/5 border-white/5"} px-3 py-1.5 rounded-full border`}
          >
            <Clock
              size={12}
              className={isR2 ? "text-emerald-400/40" : "text-white/30"}
            />
            <p
              className={`${isR2 ? "text-emerald-400/60" : "text-white/50"} text-[10px] font-bold tracking-wider`}
            >
              Expires in {timeLeft}
            </p>
          </div>
        )}
        {fileName && (
          <p
            className={`${isR2 ? "text-emerald-400/40" : "text-white/30"} text-[9px] font-medium tracking-widest px-4 italic break-all max-w-[280px] text-center`}
          >
            {fileName}
          </p>
        )}
      </div>

      <div className="flex gap-3 w-full pt-2">
        <button
          onClick={handleCopyLink}
          className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${copiedLink ? (isR2 ? "bg-emerald-500 text-black border-emerald-500" : "bg-white text-black border-white") : isR2 ? "bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border-emerald-500/20" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"}`}
        >
          {copiedLink ? <Check size={16} /> : <Link size={16} />}
          {copiedLink ? "Copied!" : "Copy Link"}
        </button>
        <button
          onClick={handleCopy}
          className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${copied ? (isR2 ? "bg-emerald-500 text-black border-emerald-500" : "bg-white text-black border-white") : isR2 ? "bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border-emerald-500/20" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"}`}
        >
          {copied ? <Check size={16} /> : <Copy size={16} />}
          {copied ? "Copied!" : "Copy Code"}
        </button>
      </div>

      {/* QR Code Section */}
      <div className="flex flex-col space-y-4 w-full pt-1">
        <button
          onClick={() => {
            vibrate();
            setShowQR(true);
          }}
          className={`w-full py-3.5 border rounded-full font-bold text-xs transition-all flex items-center justify-center gap-2 ${isR2 ? "bg-emerald-500/5 hover:bg-emerald-500/10 text-emerald-400/60 border-emerald-500/10" : "bg-white/5 hover:bg-white/10 text-white/75 border-white/10 active:scale-98"}`}
        >
          <QrCode size={16} />
          Share via QR Code
        </button>

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
                {/* Close Button */}
                <button
                  onClick={() => setShowQR(false)}
                  className="absolute top-4 right-4 text-white/40 hover:text-white/80 p-1.5 hover:bg-white/5 rounded-full transition-all cursor-pointer"
                >
                  <X size={16} />
                </button>

                <div className={`absolute inset-0 rounded-[32px] bg-gradient-to-b ${isR2 ? "from-emerald-500/[0.02]" : "from-indigo-500/[0.02]"} to-transparent pointer-events-none`} />
                
                <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-white/40">
                  Share Drop
                </h4>

                <BeautifulQRCode
                  value={`${window.location.origin}/?code=${code}`}
                  size={200}
                  theme={isR2 ? "emerald" : "default"}
                  onDownloadReady={setDownloadQRFn}
                />

                <div className="text-center space-y-1">
                  <p className="text-xs font-medium text-white/80">
                    Scan to open & retrieve this drop
                  </p>
                  <p className="text-[10px] text-white/30">
                    Compatible with any smartphone camera
                  </p>
                </div>

                <div className="flex gap-2 w-full pt-1">
                  <button
                    onClick={() => {
                      vibrate();
                      if (downloadQRFn) downloadQRFn();
                    }}
                    disabled={!downloadQRFn}
                    className="flex-1 bg-white hover:bg-white/90 text-black border border-white/20 rounded-full py-2.5 font-bold text-[10px] uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 active:scale-95 disabled:opacity-50 cursor-pointer"
                  >
                    <Download size={12} /> Save QR Image
                  </button>
                  <button
                    onClick={() => {
                      vibrate();
                      navigator.clipboard.writeText(`${window.location.origin}/?code=${code}`);
                      setCopiedLink(true);
                      setTimeout(() => setCopiedLink(false), 2000);
                    }}
                    className={`flex-1 border rounded-full py-2.5 font-bold text-[10px] uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 active:scale-95 cursor-pointer ${copiedLink ? (isR2 ? "bg-emerald-500 text-black border-emerald-500 animate-pulse" : "bg-white text-black border-white animate-pulse") : "bg-white/5 hover:bg-white/10 text-white border-white/10"}`}
                  >
                    {copiedLink ? <Check size={12} /> : <Copy size={12} />}
                    {copiedLink ? "Copied!" : "Copy Link"}
                  </button>
                </div>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

interface BeautifulQRCodeProps {
  value: string;
  size?: number;
  theme?: "emerald" | "default";
  onDownloadReady?: (downloadFn: () => void) => void;
}

function BeautifulQRCode({
  value,
  size = 220,
  theme = "default",
  onDownloadReady,
}: BeautifulQRCodeProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (!canvasRef.current) return;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    try {
      // Use low error correction for smaller matrix size (fewer dots, cleaner look)
      const qr = QRCode.create(value, { errorCorrectionLevel: "L" });
      const qrSize = qr.modules.size;
      const data = qr.modules.data;

      const dpr = window.devicePixelRatio || 1;
      
      // Calculate layout in physical coordinates to align exactly to physical screen pixels
      const physicalSize = size * dpr;
      
      // Base quiet margin in physical pixels
      const baseMargin = Math.round(14 * dpr);
      
      // Calculate integer physical cell size (force perfect pixel grid alignment)
      const physicalCellSize = Math.max(1, Math.floor((physicalSize - baseMargin * 2) / qrSize));
      
      // Recalculate physical margin to perfectly center the QR code inside the canvas width
      const physicalMargin = Math.round((physicalSize - physicalCellSize * qrSize) / 2);

      // Set canvas physical dimensions
      canvas.width = physicalSize;
      canvas.height = physicalSize;
      
      // Set canvas display dimensions (CSS pixels)
      canvas.style.width = `${size}px`;
      canvas.style.height = `${size}px`;

      // Set theme colors
      const isEmerald = theme === "emerald";
      const bgColor = isEmerald ? "#f0fdf4" : "#ffffff"; // Emerald-50 vs pure white
      const fgColor = isEmerald ? "#047857" : "#000000"; // Emerald-700 vs pure black
      
      ctx.fillStyle = bgColor;
      ctx.fillRect(0, 0, physicalSize, physicalSize);

      const isFinder = (r: number, c: number) => {
        if (r < 7 && c < 7) return true;
        if (r < 7 && c >= qrSize - 7) return true;
        if (r >= qrSize - 7 && c < 7) return true;
        return false;
      };

      ctx.fillStyle = fgColor;

      // Draw modules as solid squares, aligned perfectly to physical pixel boundaries
      for (let r = 0; r < qrSize; r++) {
        for (let c = 0; c < qrSize; c++) {
          if (isFinder(r, c)) continue;
          const isDark = data[r * qrSize + c] === 1;
          if (isDark) {
            const x = physicalMargin + c * physicalCellSize;
            const y = physicalMargin + r * physicalCellSize;
            // Draw exact physical rectangle block with zero fractional coordinates
            ctx.fillRect(x, y, physicalCellSize, physicalCellSize);
          }
        }
      }

      // Draw finder patterns with smooth rounded corners, perfectly scaled in physical pixels
      const drawFinder = (x: number, y: number) => {
        ctx.fillStyle = fgColor;
        ctx.beginPath();
        ctx.roundRect(
          x, 
          y, 
          physicalCellSize * 7, 
          physicalCellSize * 7, 
          physicalCellSize * 1.6
        );
        ctx.fill();

        ctx.fillStyle = bgColor;
        ctx.beginPath();
        ctx.roundRect(
          x + physicalCellSize,
          y + physicalCellSize,
          physicalCellSize * 5,
          physicalCellSize * 5,
          physicalCellSize * 1.1,
        );
        ctx.fill();

        ctx.fillStyle = fgColor;
        ctx.beginPath();
        ctx.roundRect(
          x + physicalCellSize * 2,
          y + physicalCellSize * 2,
          physicalCellSize * 3,
          physicalCellSize * 3,
          physicalCellSize * 0.6,
        );
        ctx.fill();
      };

      // Coordinates for finders in physical space
      drawFinder(physicalMargin, physicalMargin);
      drawFinder(physicalMargin + (qrSize - 7) * physicalCellSize, physicalMargin);
      drawFinder(physicalMargin, physicalMargin + (qrSize - 7) * physicalCellSize);
    } catch (e) {
      console.error("BeautifulQRCode drawing error:", e);
    }
  }, [value, size, theme]);

  useEffect(() => {
    if (onDownloadReady && canvasRef.current) {
      const downloadFn = () => {
        if (!canvasRef.current) return;
        const url = canvasRef.current.toDataURL("image/png");
        const a = document.createElement("a");
        a.href = url;
        a.download = `hefimer_qr.png`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      };
      onDownloadReady(() => downloadFn);
    }
  }, [onDownloadReady]);

  return (
    <div className="relative group p-2 rounded-[28px] bg-black/40 border border-white/5 shadow-2xl">
      <canvas
        ref={canvasRef}
        className="rounded-[20px] block transition-transform duration-300 group-hover:scale-[1.02]"
      />
      <div
        className="absolute -inset-1 rounded-[30px] opacity-0 group-hover:opacity-10 transition-opacity duration-500 blur-xl pointer-events-none -z-10 bg-gradient-to-br from-white to-white/40"
      />
    </div>
  );
}

function ReceiveResult({
  item: initialItem,
  code,
  onReset,
  showToast,
}: any) {
  let item = { ...initialItem };
  const [copiedText, setCopiedText] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const formatTotalSize = (filesList: any[]) => {
    const total = filesList.reduce((acc, f) => acc + (f.fileSize || f.size || 0), 0);
    return formatBytes(total);
  };

  const getFileDownloadUrl = (fileEntry: any) => {
    const value =
      fileEntry?.fileUrl ||
      fileEntry?.url ||
      fileEntry?.raw_url ||
      fileEntry?.downloadUrl ||
      "";
    return typeof value === "string" ? value.trim() : "";
  };

  const isValidDownloadUrl = (url: string) => /^https?:\/\//i.test(url);

  const getFileIcon = (fileName: string, isR2Drop: boolean) => {
    const ext = fileName.split('.').pop()?.toLowerCase();
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'heic'].includes(ext || '')) {
      return <Image size={18} className="text-blue-400" />;
    }
    if (['mp4', 'mkv', 'avi', 'mov', 'webm'].includes(ext || '')) {
      return <Video size={18} className="text-purple-400" />;
    }
    if (['mp3', 'wav', 'ogg', 'm4a', 'flac'].includes(ext || '')) {
      return <Music size={18} className="text-pink-400" />;
    }
    if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext || '')) {
      return <Archive size={18} className="text-yellow-400" />;
    }
    if (['pdf'].includes(ext || '')) {
      return <FileText size={18} className="text-red-400" />;
    }
    return <File size={18} className={isR2Drop ? "text-emerald-400" : "text-white/60"} />;
  };

  const handleDownloadR2File = async (fileEntry: any) => {
    vibrate();
    if (!fileEntry.objectKey) {
      showToast("Error: Object key is missing", "error");
      return;
    }
    try {
      showToast(`Generating download link for ${fileEntry.fileName}...`, "info");
      const apiUrl = `/api/r2/download-url?objectKey=${encodeURIComponent(fileEntry.objectKey)}`;
      const res = await fetch(apiUrl);
      const contentType = res.headers.get("Content-Type") || "";
      let downloadUrl = "";
      if (contentType.includes("application/json")) {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || `Server returned ${res.status}`);
        downloadUrl = data.url;
      } else {
        if (!res.ok) throw new Error(`Server returned ${res.status}`);
        downloadUrl = apiUrl;
      }
      if (!downloadUrl) throw new Error("No download URL returned");
      const a = document.createElement("a");
      a.href = downloadUrl;
      a.download = fileEntry.fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      showToast("Download started", "success");
    } catch (err: any) {
      showToast(err.message || "Failed to download file", "error");
    }
  };

  const handleDownloadStandardFile = async (fileEntry: any) => {
    vibrate();
    if (fileEntry.objectKey) {
      await handleDownloadR2File(fileEntry);
      return;
    }
    const url = getFileDownloadUrl(fileEntry);
    const name = fileEntry.fileName;
    if (!isValidDownloadUrl(url)) {
      showToast("This file link is missing or invalid. Please re-upload the file.", "error");
      return;
    }
    const mode = getDirectDownloadMode(url);
    if (mode === "tab") {
      window.open(url, "_blank", "noopener,noreferrer");
      showToast("Opening download link", "success");
      return;
    }
    await triggerDirectDownload(url, name);
  };

  const handleDownloadAll = async (files: any[]) => {
    vibrate();
    showToast("Starting download of all files...", "info");
    for (let i = 0; i < files.length; i++) {
      const fileEntry = files[i];
      if (fileEntry.objectKey) {
        await handleDownloadR2File(fileEntry);
      } else {
        await handleDownloadStandardFile(fileEntry);
      }
      await new Promise((resolve) => setTimeout(resolve, 600));
    }
    showToast("All files downloaded", "success");
  };

  const handleCopyReceiveLink = async () => {
    vibrate();
    try {
      await copyToClipboard(`${window.location.origin}/?code=${code}`);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
      showToast("Link copied", "success");
    } catch (err: any) {
      showToast(err.message || "Failed to copy link", "error");
    }
  };

  const getProviderNameFromUrl = (url: string): string => {
    if (!url) return "File";
    if (item.objectKey) return "Cloudflare R2";
    if (url.includes("gofile.io")) return "Gofile";
    if (url.includes("catbox.moe")) return "Litterbox";
    if (url.includes("pixeldrain.com")) return "Pixeldrain";
    if (url.includes("storage.to")) return "storage.to";
    if (url.includes("tmpfiles.org")) return "tmpfiles.org";
    try {
      const domain = new URL(url).hostname;
      return domain.replace("www.", "").split(".")[0].replace(/^\w/, (c) => c.toUpperCase());
    } catch {
      return "Host";
    }
  };

  const triggerDirectDownload = async (url: string, fileName: string) => {
    if (!isValidDownloadUrl(url)) {
      showToast("This file link is missing or invalid. Please re-upload the file.", "error");
      return;
    }
    const mode = getDirectDownloadMode(url);
    showToast(mode === "proxy" ? "Starting secure download..." : "Downloading file...", "info");
    await downloadFileInBrowser(url, fileName);
    showToast("Download started", "success");
  };
  const [showLineNumbers, setShowLineNumbers] = useState(false);
  const [renameData, setRenameData] = useState<{
    isOpen: boolean;
    defaultName: string;
  } | null>(null);
  const [renameInput, setRenameInput] = useState("");

  const deleteR2Object = async (objectKey: string) => {
    if (!objectKey) return;
    const apiUrl = "/api/r2/delete";

    console.log("Deleting R2 object for self-destruct:", objectKey);
    const res = await fetch(apiUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ objectKey }),
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.error || `Server returned ${res.status}`);
    }
  };

  useEffect(() => {
    if (item.isOneTime) {
      const performSelfDestruct = async () => {
        try {
          if (item.objectKey) {
            await deleteR2Object(item.objectKey);
          }
          if (Array.isArray(item.files)) {
            await Promise.all(
              item.files
                .filter((fileEntry: any) => fileEntry.objectKey)
                .map((fileEntry: any) => deleteR2Object(fileEntry.objectKey)),
            );
          }
          await remove(ref(rtdb, `drops/${code}`));
          showToast("This drop has self-destructed after viewing", "success");
        } catch (err) {
          console.error("Self-destruct failed:", err);
        }
      };
      performSelfDestruct();
    }
  }, [code, item.isOneTime, showToast, item.objectKey]);

  if (item.text && item.text.startsWith("__HEFIMER_FILE__")) {
    try {
      const parsed = JSON.parse(item.text.replace("__HEFIMER_FILE__", ""));
      if (parsed.isDirectFile) {
        item = {
          ...item,
          type: "file",
          fileName: parsed.fileName,
          fileUrl: parsed.fileUrl,
          objectKey: parsed.objectKey,
          contentType: parsed.contentType,
          fileSize: parsed.fileSize,
          storageProvider: parsed.storageProvider,
          isSecretR2: parsed.isSecretR2,
          text: undefined,
        };
      }
    } catch (e) {
      // Ignore parse errors
    }
  }

  if (item.text && item.text.startsWith("__HEFIMER_FILES__")) {
    try {
      const parsed = JSON.parse(item.text.replace("__HEFIMER_FILES__", ""));
      if (Array.isArray(parsed)) {
        item = {
          ...item,
          files: parsed,
          text: undefined,
        };
      }
    } catch (e) {
      // Ignore parse errors
    }
  }

  const smartAction = getSmartAction(item.text || "");
  const fileSize = formatFileSize(item.text || "");
  const isR2Drop = item.type === "r2_file" || item.isSecretR2 === true;

  const handleCopyText = () => {
    vibrate();
    navigator.clipboard.writeText(item.text);
    setCopiedText(true);
    setTimeout(() => setCopiedText(false), 2000);
  };

  const longPressTimer = useRef<any>(null);
  const isLongPress = useRef(false);

  const handleDownloadText = (customName?: string) => {
    vibrate();
    const langInfo = detectLanguageAndExtension(item.text);
    const defaultName = `hefimer_${code || "received"}.${langInfo.ext}`;
    const finalName = customName || defaultName;
    const blob = new Blob([item.text], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = finalName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast("Download started", "success");
  };

  const handleDownloadWithRename = () => {
    vibrate();
    const langInfo = detectLanguageAndExtension(item.text);
    const defaultName = `hefimer_${code || "received"}.${langInfo.ext}`;
    setRenameInput(defaultName);
    setRenameData({ isOpen: true, defaultName });
  };

  const startLongPress = (e: React.MouseEvent | React.TouchEvent) => {
    // Prevent double firing on mobile
    if (e.type === "mousedown" && "ontouchstart" in window) return;

    isLongPress.current = false;
    if (longPressTimer.current) clearTimeout(longPressTimer.current);

    longPressTimer.current = setTimeout(() => {
      isLongPress.current = true;
      vibrate();
      handleDownloadWithRename();
      longPressTimer.current = null;
    }, 400);
  };

  const endLongPress = (e: React.MouseEvent | React.TouchEvent) => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
      if (!isLongPress.current) {
        handleDownloadText();
      }
    }
  };

  const handleCopyLink = async () => {
    vibrate();
    try {
      await copyToClipboard(`${window.location.origin}/?code=${code}`);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
      showToast("Link copied", "success");
    } catch (err: any) {
      showToast(err.message || "Failed to copy link", "error");
    }
  };

  const highlightCode = (code: string) => {
    const escapeHtml = (unsafe: string) => {
      return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
    };
    if (!code) return "";
    const langId = detectLanguageAndExtension(code).id;
    if (langId === "text") return escapeHtml(code);
    const grammar = Prism.languages[langId];
    if (!grammar) return escapeHtml(code);
    try {
      return Prism.highlight(code, grammar, langId);
    } catch (e) {
      console.error("Highlighting error:", e);
      return escapeHtml(code);
    }
  };

  const lineCount = item.text ? item.text.split("\n").length : 0;
  const charCount = item.text ? item.text.length : 0;

  const handleDownloadR2 = async () => {
    vibrate();
    if (!item.objectKey) {
      showToast("Error: Object key is missing", "error");
      return;
    }
    try {
      showToast("Generating secret download link...", "info");
      const apiUrl = `/api/r2/download-url?objectKey=${encodeURIComponent(item.objectKey)}`;

      console.log("Fetching download URL from:", apiUrl);
      const res = await fetch(apiUrl);
      const contentType = res.headers.get("Content-Type") || "";

      let downloadUrl = "";

      if (contentType.includes("application/json")) {
        const data = await res.json();
        if (!res.ok)
          throw new Error(data.error || `Server returned ${res.status}`);
        downloadUrl = data.url;
      } else {
        // If not JSON, assume the apiUrl itself is the download link (direct file response)
        if (!res.ok) throw new Error(`Server returned ${res.status}`);
        downloadUrl = apiUrl;
      }

      if (!downloadUrl) throw new Error("No download URL returned from server");

      const a = document.createElement("a");
      a.href = downloadUrl;
      a.download = item.fileName || "download";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      showToast("Download started", "success");
    } catch (err: any) {
      console.error("R2 Download Error:", err);
      showToast(err.message || "Failed to download file", "error");
    }
  };

  const handleCopyR2Link = async () => {
    vibrate();
    if (!item.objectKey) {
      showToast("Error: Object key is missing", "error");
      return;
    }
    try {
      const apiUrl = `/api/r2/download-url?objectKey=${encodeURIComponent(item.objectKey)}`;

      console.log("Fetching copy link URL from:", apiUrl);
      const res = await fetch(apiUrl);
      const contentType = res.headers.get("Content-Type") || "";

      let linkToCopy = "";

      if (contentType.includes("application/json")) {
        const data = await res.json();
        if (!res.ok)
          throw new Error(data.error || `Server returned ${res.status}`);
        linkToCopy = data.url;
      } else {
        // If not JSON, assume the apiUrl itself is the link (direct file response)
        if (!res.ok) throw new Error(`Server returned ${res.status}`);
        linkToCopy = apiUrl;
      }

      if (!linkToCopy) throw new Error("No link returned from server");

      navigator.clipboard.writeText(linkToCopy);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
    } catch (err: any) {
      console.error("R2 Copy Link Error:", err);
      showToast(err.message || "Failed to copy link", "error");
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      className="space-y-6 flex flex-col"
    >
      <div className="flex items-center justify-between">
        <h3 className="text-xl font-semibold">Received Data</h3>
        <button
          onClick={() => {
            vibrate();
            onReset();
          }}
          className="text-white/60 hover:text-white text-sm px-4 py-2 bg-white/10 border border-white/10 hover:bg-white/20 rounded-full transition-all"
        >
          &larr; Back
        </button>
      </div>

      {item.files && Array.isArray(item.files) ? (
        <div className="space-y-6 flex flex-col">
          <div className="bg-white/5 border border-white/20 rounded-[40px] p-6 sm:p-8 flex flex-col gap-4 shadow-lg w-full">
            <div className="flex items-center gap-4 border-b border-white/10 pb-4">
              <div className={`w-12 h-12 rounded-xl flex items-center justify-center border shrink-0 ${isR2Drop ? "bg-emerald-400/20 border-emerald-400/30 text-emerald-400" : "bg-white/10 border-white/10 text-white"}`}>
                <Files size={24} className={isR2Drop ? "text-emerald-400" : "text-white"} />
              </div>
              <div className="text-left">
                <h3 className="text-lg font-bold text-white leading-tight">Shared Files</h3>
                <p className="text-white/40 text-xs">Contains {item.files.length} items • {formatTotalSize(item.files)}</p>
              </div>
            </div>
            
            <div className="max-h-[350px] overflow-y-auto space-y-2 pr-1 custom-scrollbar">
              {item.files.map((fileEntry: any, index: number) => {
                const isR2 = !!fileEntry.objectKey;
                return (
                  <div key={index} className="flex items-center justify-between gap-3 p-3 bg-white/[0.02] border border-white/5 rounded-2xl hover:bg-white/[0.04] transition-all">
                    <div className="flex items-center gap-3 overflow-hidden text-left">
                      <div className="w-10 h-10 bg-white/5 rounded-lg flex items-center justify-center border border-white/5 shrink-0">
                        {getFileIcon(fileEntry.fileName || fileEntry.name, isR2Drop)}
                      </div>
                      <div className="overflow-hidden">
                        <p className="text-xs font-semibold text-white/90 truncate max-w-[200px] sm:max-w-[250px]">
                          {fileEntry.fileName || fileEntry.name}
                        </p>
                        <p className="text-[10px] text-white/30 font-medium">
                          {formatBytes(fileEntry.fileSize || fileEntry.size || 0)}
                        </p>
                      </div>
                    </div>
                    
                    <button
                      onClick={() => isR2 ? handleDownloadR2File(fileEntry) : handleDownloadStandardFile(fileEntry)}
                      className={`px-3.5 py-1.5 bg-white text-black rounded-xl text-[10px] font-bold transition-all shrink-0 cursor-pointer ${isR2Drop ? "hover:bg-emerald-400 hover:text-black" : "hover:bg-white/80"}`}
                    >
                      Download
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
          
          <div className="flex gap-3 pt-2">
            <button
              onClick={() => handleDownloadAll(item.files)}
              className={`flex-1 rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 text-center shadow-lg cursor-pointer ${
                isR2Drop
                  ? "bg-emerald-400 text-black hover:bg-emerald-300 border border-emerald-400/20 shadow-emerald-400/20"
                  : "bg-white text-black hover:bg-zinc-200 border border-white/20 shadow-white/5"
              }`}
            >
              <Download size={18} /> Download All
            </button>
            <button
              onClick={handleCopyReceiveLink}
              className={`flex-1 border rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 cursor-pointer ${
                isR2Drop
                  ? copiedLink
                    ? "bg-emerald-400 text-black border-emerald-400"
                    : "bg-emerald-500/10 hover:bg-emerald-500/20 active:bg-emerald-500/30 border-emerald-500/20 text-emerald-400"
                  : copiedLink
                    ? "bg-white text-black border-white"
                    : "bg-white/10 hover:bg-white/20 active:bg-white/30 border-white/20 text-white"
              }`}
            >
              {copiedLink ? <Check size={18} /> : <Copy size={18} />}{" "}
              {copiedLink ? "Copied!" : "Copy Link"}
            </button>
          </div>
        </div>
      ) : (
        <>
          {item.type === "r2_file" && (
        <div className="space-y-6 flex flex-col">
          <div className="bg-emerald-500/[0.03] border border-emerald-500/20 rounded-[40px] p-8 flex flex-col items-center justify-center text-center gap-4 shadow-lg shadow-emerald-500/5">
            <div className="w-16 h-16 bg-emerald-400/20 rounded-full flex items-center justify-center border border-emerald-400/30 shrink-0">
              <ShieldCheck size={32} className="text-emerald-400" />
            </div>
            <div className="space-y-1 w-full overflow-hidden">
              <p className="text-lg font-bold break-words whitespace-normal text-white px-2">
                {item.fileName || "Unknown File"}
              </p>
              <p className="text-emerald-400/40 text-[10px] uppercase tracking-[0.2em] font-black">
                Secret Cloudflare R2 Storage
              </p>
            </div>
            {item.description && (
              <p className="text-white/60 text-sm italic">
                "{item.description}"
              </p>
            )}
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={handleDownloadR2}
              className="flex-1 bg-emerald-400 text-black hover:bg-emerald-300 border border-emerald-400/20 rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 text-center shadow-lg shadow-emerald-400/20"
            >
              <Download size={18} /> Download
            </button>
            <button
              onClick={handleCopyR2Link}
              className={`flex-1 border rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 ${copiedLink ? "bg-emerald-400 text-black border-emerald-400" : "bg-emerald-500/10 hover:bg-emerald-500/20 active:bg-emerald-500/30 border-emerald-500/20 text-emerald-400"}`}
            >
              {copiedLink ? <Check size={18} /> : <Copy size={18} />}{" "}
              {copiedLink ? "Copied!" : "Copy Link"}
            </button>
          </div>
        </div>
      )}

      {(item.type === "text" || (!item.type && item.text)) && (
        <div className="space-y-4 flex flex-col">
          <div className="bg-[#000000] border border-white/10 rounded-[32px] overflow-hidden text-white/90 relative group shadow-2xl">
            <div className="flex flex-col">
              <div className="bg-[#0a0a0a] px-4 py-3 flex items-center justify-between border-b border-white/10 relative">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-white/60 bg-white/10 px-3 py-1 rounded-full border border-white/10 shadow-[0_0_10px_rgba(255,255,255,0.05)]">
                    {detectLanguageAndExtension(item.text).label}
                  </span>
                </div>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => {
                      vibrate();
                      setShowLineNumbers(!showLineNumbers);
                    }}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 hover:bg-white/20 border border-white/10 text-white/40 hover:text-white transition-all shadow-lg active:scale-90"
                    title={
                      showLineNumbers
                        ? "Hide Line Numbers"
                        : "Show Line Numbers"
                    }
                  >
                    {showLineNumbers ? <EyeOff size={14} /> : <Eye size={14} />}
                  </button>
                  <motion.button
                    onMouseDown={startLongPress}
                    onMouseUp={endLongPress}
                    onMouseLeave={endLongPress}
                    onTouchStart={startLongPress}
                    onTouchEnd={endLongPress}
                    onTouchCancel={endLongPress}
                    onContextMenu={(e) => e.preventDefault()}
                    whileHover={{
                      scale: 1.1,
                      backgroundColor: "rgba(16, 185, 129, 0.2)",
                    }}
                    whileTap={{ scale: 0.9 }}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 border border-white/10 text-white/40 hover:text-emerald-400 transition-all shadow-lg"
                    title="Hold to rename"
                  >
                    <Download size={14} />
                  </motion.button>
                  <button
                    onClick={handleCopyText}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 hover:bg-white/20 border border-white/10 text-white/40 hover:text-white transition-all shadow-lg active:scale-90"
                    title={copiedText ? "Copied!" : "Copy"}
                  >
                    {copiedText ? (
                      <Check size={14} className="text-emerald-400" />
                    ) : (
                      <Copy size={14} />
                    )}
                  </button>
                </div>
              </div>
              <div className="max-h-[70vh] overflow-auto custom-scrollbar bg-[#000000] flex relative">
                {showLineNumbers && (
                  <pre
                    className="sticky left-0 bg-[#000000] text-white/20 text-right px-3 py-[24px] font-mono text-[10px] select-none min-h-full m-0 z-10 box-border"
                    style={{
                      lineHeight: "21px",
                      fontFamily: '"JetBrains Mono", "Fira Code", monospace',
                      minWidth: "45px",
                    }}
                  >
                    {Array.from(
                      { length: Math.max(1, lineCount) },
                      (_, i) => i + 1,
                    ).join("\n")}
                  </pre>
                )}
                <div className="flex-1 min-w-0 overflow-visible">
                  <pre
                    className="font-mono text-[14px] text-white p-[24px] m-0 min-w-max"
                    style={{
                      lineHeight: "21px",
                      fontFamily: '"JetBrains Mono", "Fira Code", monospace',
                    }}
                  >
                    <code
                      dangerouslySetInnerHTML={{
                        __html: highlightCode(item.text),
                      }}
                    />
                  </pre>
                </div>
              </div>
              <div className="bg-[#0a0a0a] px-4 py-2 flex items-center justify-between border-t border-white/10 text-[10px] font-bold text-white/20">
                <div className="flex items-center gap-4">
                  <span>
                    {lineCount} {lineCount === 1 ? "Line" : "Lines"}
                  </span>
                  <span>
                    {charCount} {charCount === 1 ? "Char" : "Chars"}
                  </span>
                  <span>{fileSize}</span>
                </div>
              </div>
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            {smartAction ? (
              <>
                <a
                  href={smartAction.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={vibrate}
                  className="flex-[2] bg-white text-black hover:bg-white/90 border border-white/20 rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 shadow-xl shadow-white/5 active:scale-95"
                >
                  <smartAction.Icon size={18} /> {smartAction.label}
                </a>
                <button
                  onClick={handleCopyText}
                  className={`flex-1 border rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 ${copiedText ? "bg-emerald-500 text-black border-emerald-500" : "bg-white/10 hover:bg-white/20 active:bg-white/30 border-white/20 text-white/80"}`}
                >
                  {copiedText ? <Check size={18} /> : <Copy size={18} />}{" "}
                  {copiedText ? "Copied!" : "Copy"}
                </button>
              </>
            ) : (
              <button
                onClick={handleCopyText}
                className={`flex-1 border rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 ${copiedText ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 active:bg-white/30 border-white/20"}`}
              >
                {copiedText ? <Check size={18} /> : <Copy size={18} />}{" "}
                {copiedText ? "Copied!" : "Copy Text"}
              </button>
            )}
          </div>
        </div>
      )}

      {(item.type === "file" || (!item.type && getFileDownloadUrl(item))) && (
        <div className="space-y-6 flex flex-col">
          <div className="bg-white/5 border border-white/20 rounded-[40px] p-8 flex flex-col items-center justify-center text-center gap-4 overflow-hidden w-full">
            <div className="w-16 h-16 bg-white/10 rounded-full flex items-center justify-center border border-white/10 shrink-0">
              <FileUp size={32} className="text-white" />
            </div>
            <p className="text-lg font-bold break-words whitespace-normal text-white w-full px-2">
              {item.fileName || "Unknown File"}
            </p>
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={async () => {
                vibrate();
                if (item.objectKey) {
                  await handleDownloadR2File(item);
                } else {
                  const fileDownloadUrl = getFileDownloadUrl(item);
                  if (!isValidDownloadUrl(fileDownloadUrl)) {
                    showToast("This file link is missing or invalid. Please re-upload the file.", "error");
                    return;
                  }
                  if (!fileDownloadUrl.includes("gofile.io")) {
                    await triggerDirectDownload(fileDownloadUrl, item.fileName);
                  } else {
                    window.open(fileDownloadUrl, "_blank", "noopener,noreferrer");
                    showToast("Opening download link", "success");
                  }
                }
              }}
              className="flex-1 bg-white text-black hover:bg-white/90 border border-white/20 rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 text-center"
            >
              <Download size={18} />{" "}
              {getFileDownloadUrl(item).includes("gofile.io")
                ? "Open in Gofile"
                : "Download"}
            </button>
            <button
              onClick={handleCopyLink}
              className={`flex-1 border rounded-full px-4 py-3 font-bold transition-all flex items-center justify-center gap-2 ${copiedLink ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 active:bg-white/30 border-white/20"}`}
            >
              {copiedLink ? <Check size={18} /> : <Copy size={18} />}{" "}
              {copiedLink ? "Copied!" : "Copy Link"}
            </button>
          </div>
        </div>
      )}
        </>
      )}

      <AnimatePresence>
        {renameData && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
            onClick={() => setRenameData(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="bg-[#0a0a0a] border border-white/10 rounded-[24px] p-5 w-full max-w-sm shadow-[0_0_50px_rgba(0,0,0,0.5)] flex flex-col gap-4"
            >
              <h3 className="text-white text-xs font-bold uppercase tracking-[0.2em] flex items-center gap-2">
                <Edit3 size={14} className="text-emerald-400" /> Rename File
              </h3>
              <input
                type="text"
                autoFocus
                value={renameInput}
                onChange={(e) => setRenameInput(e.target.value)}
                className="w-full bg-[#000000] border border-white/10 rounded-xl px-4 py-3 text-white text-sm font-mono focus:outline-none focus:border-emerald-500/50 transition-all focus:ring-1 focus:ring-emerald-500/50"
              />
              <div className="flex justify-end gap-2 pt-2">
                <button
                  onClick={() => setRenameData(null)}
                  className="px-5 py-2.5 text-xs font-bold text-white/50 hover:text-white transition-colors bg-white/5 hover:bg-white/10 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  onClick={() => {
                    if (renameInput.trim()) {
                      handleDownloadText(renameInput.trim());
                      setRenameData(null);
                    }
                  }}
                  className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-400 active:scale-95 text-black text-xs font-bold rounded-xl transition-all flex items-center gap-2"
                >
                  <Download size={14} /> Save
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

function SendText({ showToast, addToHistory, state, setState }: any) {
  const { text, expire, resultCode, expiresAt } = state;
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [showLineNumbers, setShowLineNumbers] = useState(false);
  const [manualLanguage, setManualLanguage] = useState<string | null>(null);
  const [showLangMenu, setShowLangMenu] = useState(false);
  const [copied, setCopied] = useState(false);
  const [password, setPassword] = useState("");
  const [isOneTime, setIsOneTime] = useState(false);
  const [showOptions, setShowOptions] = useState(false);
  const [renameData, setRenameData] = useState<{
    isOpen: boolean;
    defaultName: string;
  } | null>(null);
  const [renameInput, setRenameInput] = useState("");

  const detected = detectLanguageAndExtension(text);
  const currentLang = manualLanguage
    ? SUPPORTED_LANGUAGES.find((l) => l.id === manualLanguage) || detected
    : detected;

  const handleGenerate = async () => {
    vibrate();
    if (!text.trim()) {
      showToast("Please enter some text or a link", "error");
      return;
    }

    setLoading(true);

    try {
      // Generate a unique 5-digit code
      let generatedCode = "";
      let isUnique = false;
      let attempts = 0;

      while (!isUnique && attempts < 5) {
        generatedCode = Math.floor(10000 + Math.random() * 90000).toString();
        try {
          const snapshot = await get(
            child(ref(rtdb), `drops/${generatedCode}`),
          );
          if (!snapshot.exists()) {
            isUnique = true;
          }
        } catch (err) {
          handleDatabaseError(err, OperationType.GET, `drops/${generatedCode}`);
        }
        attempts++;
      }

      if (!isUnique) {
        throw new Error("Failed to generate a unique code. Please try again.");
      }

      const durationMs = parseExpire(expire);
      const expiresAtDate = new Date(Date.now() + durationMs);

      const dropData: any = {
        text: text,
        type: "text",
        expire: expire,
        expiresAt: expiresAtDate.getTime(),
        timestamp: { ".sv": "timestamp" },
      };

      if (password.trim()) {
        dropData.password = password.trim();
      }
      if (isOneTime) {
        dropData.isOneTime = true;
      }

      try {
        // Save as object to comply with new rules
        await set(ref(rtdb, `drops/${generatedCode}`), dropData);
      } catch (err) {
        handleDatabaseError(err, OperationType.WRITE, `drops/${generatedCode}`);
      }

      setState((prev: any) => ({
        ...prev,
        resultCode: generatedCode,
        expiresAt: expiresAtDate.getTime(),
      }));
      addToHistory({ code: generatedCode, type: "text", action: "sent" });
      showToast("Code generated successfully!", "success");
      window.dispatchEvent(new CustomEvent("hefimer:drop-created", {
        detail: {
          kind: "text",
          name: currentLang.label === "Plain Text" ? "Shared text" : `${currentLang.label} snippet`,
          provider: "Hefimer",
          sizeBytes: new TextEncoder().encode(text).length,
          dropCode: generatedCode,
          expiresAt: expiresAtDate.getTime(),
          payload: dropData,
        },
      }));
    } catch (err: any) {
      console.error(err);
      showToast(
        sanitizeError(err.message) || "Failed to generate code",
        "error",
      );
    } finally {
      setLoading(false);
    }
  };

  const longPressTimer = useRef<any>(null);
  const isLongPress = useRef(false);

  const EXPIRE_OPTIONS = [
    { label: "30 sec", value: "30s" },
    { label: "5 min", value: "5p" },
    { label: "30 min", value: "30p" },
    { label: "1 hour", value: "1h" },
    { label: "12 hours", value: "12h" },
    { label: "24 hours", value: "24h" },
    { label: "48 hours", value: "48h" },
  ];

  const handleCopy = () => {
    vibrate();
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleClear = () => {
    vibrate();
    setState((prev: any) => ({ ...prev, text: "" }));
  };

  const handleDownload = (customName?: string) => {
    vibrate();
    const langInfo = detectLanguageAndExtension(text);
    const randomCode = Math.floor(10000 + Math.random() * 90000).toString();
    const defaultName = `hefimer_${randomCode}.${langInfo.ext}`;
    const finalName = customName || defaultName;
    const blob = new Blob([text], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = finalName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast("Download started", "success");
  };

  const handleDownloadWithRename = () => {
    vibrate();
    const langInfo = detectLanguageAndExtension(text);
    const randomCode = Math.floor(10000 + Math.random() * 90000).toString();
    const defaultName = `hefimer_${randomCode}.${langInfo.ext}`;
    setRenameInput(defaultName);
    setRenameData({ isOpen: true, defaultName });
  };

  const startLongPress = (e: React.MouseEvent | React.TouchEvent) => {
    // Prevent double firing on mobile
    if (e.type === "mousedown" && "ontouchstart" in window) return;

    isLongPress.current = false;
    if (longPressTimer.current) clearTimeout(longPressTimer.current);

    longPressTimer.current = setTimeout(() => {
      isLongPress.current = true;
      vibrate();
      handleDownloadWithRename();
      longPressTimer.current = null;
    }, 400);
  };

  const endLongPress = (e: React.MouseEvent | React.TouchEvent) => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
      if (!isLongPress.current) {
        handleDownload();
      }
    }
  };

  const lineCount = text.split("\n").length;

  return (
    <AnimatePresence mode="wait">
      {resultCode ? (
        <ResultView
          key="result"
          code={resultCode}
          expiresAt={expiresAt}
          onReset={() => {
            vibrate();
            setState({
              text: "",
              expire: "1h",
              resultCode: "",
              expiresAt: null,
            });
          }}
          onBack={() => {
            vibrate();
            setState((prev: any) => ({
              ...prev,
              resultCode: "",
              expiresAt: null,
            }));
          }}
          showToast={showToast}
        />
      ) : (
        <motion.div
          key="form"
          initial={{ opacity: 0, scale: 0.98, y: 5 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: -5 }}
          transition={{ duration: 0.2 }}
          className="space-y-2.5 flex flex-col"
        >
          <div className="relative group">
            <div className="w-full min-h-[320px] max-h-[70vh] bg-[#000000] border border-white/20 rounded-[40px] overflow-hidden relative shadow-2xl flex flex-col transition-all focus-within:ring-1 focus-within:ring-white/40 focus-within:shadow-[0_0_30px_rgba(255,255,255,0.05)]">
              <div className="bg-[#0a0a0a] px-5 py-4 flex items-center justify-between border-b border-white/10 relative">
                <div className="flex items-center gap-2">
                  <div className="relative">
                    <button
                      onClick={() => setShowLangMenu(!showLangMenu)}
                      className="text-[10px] font-black uppercase tracking-[0.2em] text-white/60 bg-white/10 px-3 py-1 rounded-full border border-white/10 shadow-[0_0_10px_rgba(255,255,255,0.05)] hover:bg-white/20 transition-all flex items-center gap-1.5"
                    >
                      {currentLang.label}
                      <motion.div
                        animate={{ rotate: showLangMenu ? 180 : 0 }}
                        transition={{ duration: 0.2 }}
                      >
                        <Code size={8} />
                      </motion.div>
                    </button>
                    <AnimatePresence>
                      {showLangMenu && (
                        <motion.div
                          initial={{ opacity: 0, y: 5, scale: 0.95 }}
                          animate={{ opacity: 1, y: 0, scale: 1 }}
                          exit={{ opacity: 0, y: 5, scale: 0.95 }}
                          className="absolute top-full left-0 mt-2 w-40 bg-[#0a0a0a] border border-white/10 rounded-xl shadow-2xl z-50 overflow-hidden py-1 max-h-[200px] overflow-y-auto custom-scrollbar"
                        >
                          <button
                            onClick={() => {
                              setManualLanguage(null);
                              setShowLangMenu(false);
                            }}
                            className={`w-full text-left px-3 py-2 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors ${manualLanguage === null ? "text-white bg-white/5" : "text-white/40"}`}
                          >
                            Auto Detect
                          </button>
                          {SUPPORTED_LANGUAGES.map((lang) => (
                            <button
                              key={lang.id}
                              onClick={() => {
                                setManualLanguage(lang.id);
                                setShowLangMenu(false);
                              }}
                              className={`w-full text-left px-3 py-2 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors ${manualLanguage === lang.id ? "text-white bg-white/5" : "text-white/40"}`}
                            >
                              {lang.label}
                            </button>
                          ))}
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => {
                      vibrate();
                      setShowLineNumbers(!showLineNumbers);
                    }}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 hover:bg-white/20 border border-white/10 text-white/40 hover:text-white transition-all shadow-lg active:scale-90"
                    title={
                      showLineNumbers
                        ? "Hide Line Numbers"
                        : "Show Line Numbers"
                    }
                  >
                    {showLineNumbers ? <EyeOff size={14} /> : <Eye size={14} />}
                  </button>
                  <motion.button
                    onMouseDown={startLongPress}
                    onMouseUp={endLongPress}
                    onMouseLeave={endLongPress}
                    onTouchStart={startLongPress}
                    onTouchEnd={endLongPress}
                    onTouchCancel={endLongPress}
                    onContextMenu={(e) => e.preventDefault()}
                    whileHover={{
                      scale: 1.1,
                      backgroundColor: "rgba(16, 185, 129, 0.2)",
                    }}
                    whileTap={{ scale: 0.9 }}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 border border-white/10 text-white/40 hover:text-emerald-400 transition-all shadow-lg"
                    title="Hold to rename"
                  >
                    <Download size={14} />
                  </motion.button>
                  <button
                    onClick={handleClear}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 hover:bg-red-500/20 border border-white/10 text-white/40 hover:text-red-400 transition-all shadow-lg active:scale-90"
                    title="Clear Text"
                  >
                    <Trash2 size={14} />
                  </button>
                  <button
                    onClick={handleCopy}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 hover:bg-white/20 border border-white/10 text-white/40 hover:text-white transition-all shadow-lg active:scale-90"
                    title={copied ? "Copied!" : "Copy"}
                  >
                    {copied ? (
                      <Check size={14} className="text-emerald-400" />
                    ) : (
                      <Copy size={14} />
                    )}
                  </button>
                </div>
              </div>
              <div className="flex-1 overflow-auto custom-scrollbar bg-[#000000] flex relative">
                {showLineNumbers && (
                  <pre
                    className="sticky left-0 bg-[#000000] text-white/20 text-right px-3 py-[24px] font-mono text-[10px] select-none min-h-full m-0 z-10 box-border"
                    style={{
                      lineHeight: "21px",
                      fontFamily: '"JetBrains Mono", "Fira Code", monospace',
                      minWidth: "45px",
                    }}
                  >
                    {Array.from(
                      { length: Math.max(1, lineCount) },
                      (_, i) => i + 1,
                    ).join("\n")}
                  </pre>
                )}
                <div className="flex-1 min-w-0 overflow-visible">
                  <Editor
                    value={text}
                    onValueChange={(code) =>
                      setState((prev: any) => ({ ...prev, text: code }))
                    }
                    highlight={(code) => {
                      const escapeHtml = (unsafe: string) => {
                        return unsafe
                          .replace(/&/g, "&amp;")
                          .replace(/</g, "&lt;")
                          .replace(/>/g, "&gt;")
                          .replace(/"/g, "&quot;")
                          .replace(/'/g, "&#039;");
                      };
                      const langId = currentLang.id;
                      if (langId === "text") return escapeHtml(code);
                      const grammar = Prism.languages[langId];
                      if (!grammar) return escapeHtml(code);
                      try {
                        return Prism.highlight(code, grammar, langId);
                      } catch (e) {
                        console.error("Highlighting error:", e);
                        return escapeHtml(code);
                      }
                    }}
                    padding={24}
                    placeholder="Paste code or text here..."
                    className="font-mono text-[14px] text-white outline-none min-w-max editor-container"
                    style={{
                      fontFamily: '"JetBrains Mono", "Fira Code", monospace',
                      backgroundColor: "transparent",
                      lineHeight: "21px",
                      whiteSpace: "pre",
                      minHeight: "100%",
                    }}
                    textareaClassName="focus:outline-none"
                  />
                </div>
              </div>
              <div className="bg-[#0a0a0a] px-4 py-2 flex items-center justify-between border-t border-white/10 text-[10px] font-bold text-white/20">
                <div className="flex items-center gap-4">
                  <span>
                    {lineCount} {lineCount === 1 ? "Line" : "Lines"}
                  </span>
                  <span>
                    {text.length} {text.length === 1 ? "Char" : "Chars"}
                  </span>
                  <span>{formatFileSize(text)}</span>
                </div>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap gap-2 justify-center">
              {EXPIRE_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  onClick={() => {
                    vibrate();
                    setState((prev: any) => ({ ...prev, expire: opt.value }));
                  }}
                  className={`px-4 py-2 text-xs font-bold rounded-full transition-all ${expire === opt.value ? "bg-white text-black shadow-md" : "bg-white/10 text-white/60 hover:bg-white/20 hover:text-white"}`}
                >
                  {opt.label}
                </button>
              ))}
            </div>

            <div className="w-full bg-white/[0.03] border border-white/10 rounded-[24px] flex flex-col transition-all overflow-hidden">
              <div 
                onClick={() => {
                  vibrate();
                  setShowOptions(!showOptions);
                }}
                className="flex items-center justify-between w-full px-4 py-2 cursor-pointer select-none group/header"
              >
                <div className="flex items-center gap-2 text-xs font-bold text-white/50 group-hover/header:text-white transition-colors">
                  <span>Advanced Options</span>
                  <motion.div animate={{ rotate: showOptions ? 180 : 0 }}>
                    <ChevronDown size={14} className="text-white/50 group-hover/header:text-white transition-colors" />
                  </motion.div>
                </div>
              </div>

              <AnimatePresence>
                {showOptions && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.25, ease: "easeInOut" }}
                    className="overflow-hidden"
                  >
                    <div className="px-5 pb-5 pt-3 border-t border-white/5 space-y-4">
                      <div className="space-y-2">
                        <label className="text-[10px] font-black uppercase tracking-widest text-white/30 ml-1">
                          Password (Optional)
                        </label>
                        <div className="relative">
                          <Lock
                            size={14}
                            className="absolute left-4 top-1/2 -translate-y-1/2 text-white/20"
                          />
                          <input
                            type="text"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Set a password..."
                            className="w-full bg-black/40 border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-white/30 transition-all"
                          />
                        </div>
                      </div>

                      <button
                        onClick={() => {
                          vibrate();
                          setIsOneTime(!isOneTime);
                        }}
                        className={`w-full flex items-center justify-between p-3.5 rounded-2xl border transition-all ${isOneTime ? "bg-red-500/10 border-red-500/30 text-red-400" : "bg-white/5 border-white/10 text-white/50 hover:bg-white/[0.08]"}`}
                      >
                        <div className="flex items-center gap-3">
                          <Trash2
                            size={16}
                            className={
                              isOneTime ? "text-red-400" : "text-white/20"
                            }
                          />
                          <div className="text-left">
                            <p className="text-xs font-bold">Self-Destruct</p>
                            <p className="text-[10px] opacity-60">
                              Delete after one successful read
                            </p>
                          </div>
                        </div>
                        <div
                          className={`w-8 h-4 rounded-full relative transition-colors ${isOneTime ? "bg-red-500" : "bg-white/10"}`}
                        >
                          <motion.div
                            animate={{ x: isOneTime ? 18 : 2 }}
                            className="absolute top-1 w-2 h-2 bg-white rounded-full"
                          />
                        </div>
                      </button>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>

          <button
            onClick={handleGenerate}
            disabled={loading}
            className={glassButton}
          >
            {loading && <Loader2 className="animate-spin" size={20} />}
            {loading ? "Generating..." : "Generate Code"}
          </button>
        </motion.div>
      )}

      <AnimatePresence>
        {renameData && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
            onClick={() => setRenameData(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="bg-[#0a0a0a] border border-white/10 rounded-[24px] p-5 w-full max-w-sm shadow-[0_0_50px_rgba(0,0,0,0.5)] flex flex-col gap-4"
            >
              <h3 className="text-white text-xs font-bold uppercase tracking-[0.2em] flex items-center gap-2">
                <Edit3 size={14} className="text-emerald-400" /> Rename File
              </h3>
              <input
                type="text"
                autoFocus
                value={renameInput}
                onChange={(e) => setRenameInput(e.target.value)}
                className="w-full bg-[#000000] border border-white/10 rounded-xl px-4 py-3 text-white text-sm font-mono focus:outline-none focus:border-emerald-500/50 transition-all focus:ring-1 focus:ring-emerald-500/50"
              />
              <div className="flex justify-end gap-2 pt-2">
                <button
                  onClick={() => setRenameData(null)}
                  className="px-5 py-2.5 text-xs font-bold text-white/50 hover:text-white transition-colors bg-white/5 hover:bg-white/10 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  onClick={() => {
                    if (renameInput.trim()) {
                      handleDownload(renameInput.trim());
                      setRenameData(null);
                    }
                  }}
                  className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-400 active:scale-95 text-black text-xs font-bold rounded-xl transition-all flex items-center gap-2"
                >
                  <Download size={14} /> Save
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </AnimatePresence>
  );
}

function R2SendFile({
  showToast,
  addToHistory,
  state,
  setState,
  onExit,
  stats,
  updateStats,
}: any) {
  const { file, resultCode, expiresAt } = state;
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [description, setDescription] = useState("");
  const [customFileName, setCustomFileName] = useState("");
  const [password, setPassword] = useState("");
  const [isOneTime, setIsOneTime] = useState(false);
  const [expire, setExpire] = useState("1h");
  const [showOptions, setShowOptions] = useState(false);
  const [isFolderMode, setIsFolderMode] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const activeUploadRequests = useRef(new Set<XMLHttpRequest>());
  const multipartUpload = useRef<{ objectKey: string; uploadId: string } | null>(null);
  const uploadWasCancelled = useRef(false);

  const cancelR2Upload = async () => {
    uploadWasCancelled.current = true;
    activeUploadRequests.current.forEach((request) => request.abort());
    activeUploadRequests.current.clear();
    const session = multipartUpload.current;
    multipartUpload.current = null;
    if (session) {
      await fetch("/api/r2/multipart/abort", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(session),
      }).catch(() => undefined);
    }
    showToast("Upload cancelled", "info");
  };

  const handleR2Upload = async () => {
    vibrate();
    if (!file) {
      showToast("Please select a file", "error");
      return;
    }

    setUploading(true);
    setProgress(0);
    uploadWasCancelled.current = false;

    let progressTimer: any;

    try {
      let fileToUpload: File | Blob = file;
      let finalName = customFileName.trim() || (file as any).name;
      let finalType = (file as any).type || "application/octet-stream";

      if (isFolderMode && (file as any).originalFiles) {
        showToast("Zipping folder contents...", "info");
        const zip = new JSZip();
        const files = (file as any).originalFiles;

        for (let i = 0; i < files.length; i++) {
          const f = files[i];
          const relativePath = f.webkitRelativePath || f.name;
          zip.file(relativePath, f);
        }

        fileToUpload = await zip.generateAsync({ type: "blob" }, (metadata) => {
          setProgress(Math.round(metadata.percent * 0.3)); // First 30% for zipping
        });

        finalName = finalName.endsWith(".zip") ? finalName : `${finalName}.zip`;
        finalType = "application/zip";
      }

      const maxUploadSize = 5 * 1024 * 1024 * 1024;
      if (fileToUpload.size > maxUploadSize) {
        throw new Error("Secret R2 uploads support files up to 5 GB");
      }

      const getPresignedUpload = async () => {
        const apiUrl = `/api/r2/upload-url?filename=${encodeURIComponent(finalName)}&contentType=${encodeURIComponent(finalType)}`;
        const presignRes = await fetch(apiUrl, { cache: "no-store" });

        if (!presignRes.ok) {
          const errorData = await presignRes.json().catch(() => ({}));
          throw new Error(errorData.error || "Failed to prepare the R2 upload");
        }

        return presignRes.json() as Promise<{ url: string; objectKey: string }>;
      };

      const putFile = (url: string, blob: Blob, onProgress?: (loaded: number) => void) =>
        new Promise<string>((resolve, reject) => {
          const xhr = new XMLHttpRequest();
          activeUploadRequests.current.add(xhr);
          xhr.upload.onprogress = (e) => {
            if (!e.lengthComputable) return;
            onProgress?.(e.loaded);
            if (!onProgress) {
              const pct = Math.round((e.loaded / e.total) * 100);
              setProgress(isFolderMode ? 30 + Math.round(pct * 0.7) : pct);
            }
          };
          xhr.onload = () => {
            activeUploadRequests.current.delete(xhr);
            if (xhr.status >= 200 && xhr.status < 300) {
              const etag = xhr.getResponseHeader("ETag");
              resolve(etag || "");
            } else reject(new Error(`R2 returned status ${xhr.status}`));
          };
          xhr.onerror = () => {
            activeUploadRequests.current.delete(xhr);
            reject(new Error("Secure storage did not accept the upload. Please try again."));
          };
          xhr.onabort = () => {
            activeUploadRequests.current.delete(xhr);
            reject(new Error("Upload cancelled"));
          };
          xhr.open("PUT", url);
          xhr.setRequestHeader("Content-Type", finalType);
          xhr.send(blob);
        });

      let objectKey = "";
      let uploadedFilesList: any[] = [];
      const isMulti = (file as any).isMultiFile;

      if (isMulti) {
        const filesToUpload = (file as any).originalFiles;
        const totalFiles = filesToUpload.length;
        const fileProgresses = new Array(totalFiles).fill(0);

        for (let i = 0; i < totalFiles; i++) {
          const currentFile = filesToUpload[i];
          const currentName = currentFile.name;
          const currentType = currentFile.type || "application/octet-stream";

          showToast(`Uploading file ${i + 1} of ${totalFiles}: ${currentName}`, "info");

          const apiUrl = `/api/r2/upload-url?filename=${encodeURIComponent(currentName)}&contentType=${encodeURIComponent(currentType)}`;
          const presignRes = await fetch(apiUrl, { cache: "no-store" });
          if (!presignRes.ok) {
            const errorData = await presignRes.json().catch(() => ({}));
            throw new Error(errorData.error || `Failed to prepare R2 upload for ${currentName}`);
          }
          const { url, objectKey: currentObjectKey } = await presignRes.json();

          await putFile(url, currentFile, (loaded) => {
            fileProgresses[i] = (loaded / currentFile.size) * 100;
            const avgProgress = Math.round(
              fileProgresses.reduce((acc, p) => acc + p, 0) / totalFiles
            );
            setProgress(avgProgress);
          });

          uploadedFilesList.push({
            objectKey: currentObjectKey,
            fileName: currentName,
            fileSize: currentFile.size,
            contentType: currentType,
          });
        }
      } else {
        const { url, objectKey: singleObjectKey } = await getPresignedUpload();
        objectKey = singleObjectKey;
        await putFile(url, fileToUpload);
      }

      // 3. Save metadata to Firebase
      let generatedCode = "";
      let isUnique = false;
      let attempts = 0;

      while (!isUnique && attempts < 5) {
        generatedCode = Math.floor(10000 + Math.random() * 90000).toString();
        const snapshot = await get(child(ref(rtdb), `drops/${generatedCode}`));
        if (!snapshot.exists()) isUnique = true;
        attempts++;
      }

      const expireMap: any = {
        "30s": 30 * 1000,
        "5p": 5 * 60 * 1000,
        "30p": 30 * 60 * 1000,
        "1h": 60 * 60 * 1000,
        "12h": 12 * 60 * 60 * 1000,
        "24h": 24 * 60 * 60 * 1000,
        "48h": 48 * 60 * 60 * 1000,
      };
      const expiresAtDate = new Date(
        Date.now() + (expireMap[expire] || 3600000),
      );

      const dropData: any = {
        type: "r2_file",
        fileName: isMulti ? `${uploadedFilesList.length} files` : finalName,
        fileSize: file.size,
        contentType: isMulti ? "application/octet-stream" : finalType,
        description: description.trim(),
        storageProvider: "hefimer-r2",
        isSecretR2: true,
        expiresAt: expiresAtDate.getTime(),
        timestamp: { ".sv": "timestamp" },
      };

      if (isMulti) {
        dropData.files = uploadedFilesList;
      } else {
        dropData.objectKey = objectKey;
      }

      if (password.trim()) {
        dropData.password = password.trim();
      }
      if (isOneTime) {
        dropData.isOneTime = true;
      }

      await set(ref(rtdb, `drops/${generatedCode}`), dropData);

      updateStats(isMulti ? `${uploadedFilesList.length} files` : finalName, file.size, true);
      setState((prev: any) => ({
        ...prev,
        resultCode: generatedCode,
        expiresAt: dropData.expiresAt,
      }));
      addToHistory({
        code: generatedCode,
        fileName: finalName,
        fileSize: file.size,
        type: "r2_file",
        action: "sent",
        isR2: true,
        objectKey, // Lưu mã file vào lịch sử
      });
      showToast("Secretly uploaded to Cloudflare R2!", "success");
    } catch (err: any) {
      console.error(err);
      const session = multipartUpload.current;
      multipartUpload.current = null;
      if (session) {
        fetch("/api/r2/multipart/abort", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(session),
        }).catch(() => undefined);
      }
      showToast(err.message || "R2 Upload failed", uploadWasCancelled.current ? "info" : "error");
    } finally {
      if (progressTimer) clearInterval(progressTimer);
      setUploading(false);
      activeUploadRequests.current.clear();
    }
  };

  return (
    <AnimatePresence mode="wait">
      {resultCode ? (
        <ResultView
          key="result"
          code={resultCode}
          fileName={customFileName || file?.name}
          expiresAt={expiresAt}
          onReset={() => {
            vibrate();
            setState({ file: null, resultCode: "", expiresAt: null });
          }}
          onBack={() => {
            vibrate();
            setState((prev: any) => ({
              ...prev,
              resultCode: "",
              expiresAt: null,
            }));
          }}
          showToast={showToast}
          isR2={true}
        />
      ) : (
        <motion.div
          key="form"
          initial={{ opacity: 0, scale: 0.98, y: 5 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: -5 }}
          className="space-y-2.5 flex flex-col"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse shadow-[0_0_8px_rgba(52,211,153,0.5)]" />
              <span className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-400/60">
                Secret R2 Mode
              </span>
            </div>
            <button
              onClick={onExit}
              className="text-[10px] font-bold uppercase tracking-widest text-white/30 hover:text-white transition-colors bg-white/5 px-3 py-1.5 rounded-full border border-white/5"
            >
              Exit Secret Mode
            </button>
          </div>

          <div
            onClick={() => !uploading && fileInputRef.current?.click()}
            onDragOver={(e) => {
              e.preventDefault();
              !uploading && setIsDragging(true);
            }}
            onDragLeave={(e) => {
              e.preventDefault();
              setIsDragging(false);
            }}
            onDrop={(e) => {
              e.preventDefault();
              setIsDragging(false);
              if (!uploading && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                const filesList = e.dataTransfer.files;
                const fileToSet = filesList.length > 1
                  ? {
                      name: `${filesList.length} files`,
                      size: Array.from(filesList).reduce((acc, f) => acc + f.size, 0),
                      originalFiles: Array.from(filesList),
                      isMultiFile: true,
                    }
                  : filesList[0];
                setState((prev: any) => ({
                  ...prev,
                  file: fileToSet,
                }));
                vibrate();
              }
            }}
            className={`w-full h-36 bg-emerald-500/[0.03] hover:bg-emerald-500/[0.06] border border-dashed ${isDragging ? "border-emerald-400 bg-emerald-500/10" : "border-emerald-500/20 hover:border-emerald-500/40"} rounded-[40px] flex items-center justify-center cursor-pointer transition-all group relative overflow-hidden`}
          >
            {uploading && (
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${progress}%` }}
                className="absolute bottom-0 left-0 h-1.5 bg-emerald-400 shadow-[0_0_15px_rgba(52,211,153,0.5)]"
              />
            )}

            <input
              type="file"
              className="hidden"
              ref={fileInputRef}
              {...(isFolderMode
                ? ({ webkitdirectory: "", directory: "" } as any)
                : { multiple: true })}
              onChange={(e) => {
                if (e.target.files && e.target.files.length > 0) {
                  const firstFile = e.target.files[0];
                  const displayName =
                    isFolderMode && (firstFile as any).webkitRelativePath
                      ? (firstFile as any).webkitRelativePath.split("/")[0]
                      : firstFile.name;

                  const fileToSet = isFolderMode
                    ? {
                        name: displayName,
                        size: Array.from(e.target.files).reduce(
                          (acc, f) => acc + f.size,
                          0,
                        ),
                        originalFiles: e.target.files,
                      }
                    : e.target.files.length > 1
                    ? {
                        name: `${e.target.files.length} files`,
                        size: Array.from(e.target.files).reduce(
                          (acc, f) => acc + f.size,
                          0,
                        ),
                        originalFiles: Array.from(e.target.files),
                        isMultiFile: true,
                      }
                    : firstFile;

                  setState((prev: any) => ({
                    ...prev,
                    file: fileToSet as any,
                  }));
                  vibrate();
                }
              }}
            />

            {uploading ? (
              <div className="flex items-center gap-6 w-fit mx-auto transition-all">
                <div className="w-16 h-16 bg-emerald-400/10 rounded-full flex items-center justify-center transition-all border border-emerald-400/10 shadow-inner shrink-0">
                  <Loader2 size={28} className="text-emerald-400 animate-spin" />
                </div>
                <div className="flex flex-col items-start justify-center">
                  <p className="text-white font-bold text-base text-left leading-tight mb-1">
                    Uploading... {progress}%
                  </p>
                </div>
              </div>
            ) : file ? (
              <div className="flex items-center gap-6 animate-in fade-in slide-in-from-bottom-4 duration-500 w-fit mx-auto">
                <div className="relative group/file shrink-0">
                  <div className="w-16 h-16 bg-emerald-400/10 rounded-full flex items-center justify-center border border-emerald-400/20 shadow-xl transition-transform group-hover/file:scale-105">
                    {isFolderMode ? (
                      <Folder size={28} className="text-emerald-400" />
                    ) : (file as any).isMultiFile ? (
                      <Files size={28} className="text-emerald-400" />
                    ) : (
                      <File size={28} className="text-emerald-400" />
                    )}
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setState((prev: any) => ({ ...prev, file: null }));
                      vibrate();
                    }}
                    className="absolute -top-2 -right-2 w-7 h-7 bg-white text-black rounded-full flex items-center justify-center border border-black shadow-lg hover:bg-emerald-400 hover:scale-110 active:scale-95 transition-all z-20"
                    title="Remove File"
                  >
                    <X size={14} strokeWidth={3} />
                  </button>
                </div>
                <div className="text-left w-[200px]">
                  <h4 className="text-white font-bold text-base break-words leading-tight mb-1 line-clamp-2">
                    {file.name}
                  </h4>
                  <p className="text-emerald-400/60 text-xs font-medium tracking-wide">
                    {(file.size / 1024 / 1024).toFixed(2)} MB
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-6 w-fit mx-auto transition-all">
                <div className="w-16 h-16 bg-emerald-400/10 group-hover:bg-emerald-400/20 rounded-full flex items-center justify-center transition-all border border-emerald-400/10 shrink-0 shadow-inner">
                  {isFolderMode ? (
                    <FolderPlus
                      size={28}
                      className="text-emerald-400/70 group-hover:text-emerald-400 group-hover:scale-110 transition-all"
                    />
                  ) : (
                    <ShieldCheck
                      size={28}
                      className="text-emerald-400/70 group-hover:text-emerald-400 group-hover:scale-110 transition-all"
                    />
                  )}
                </div>

                <div className="flex flex-col items-start justify-center">
                  <p className="text-white font-bold text-base text-left leading-tight mb-1">
                    {isFolderMode
                      ? "Tap to select a folder"
                      : "Tap to select file(s)"}
                  </p>
                  <div className="flex items-center gap-2 text-[10px] uppercase font-bold tracking-[0.05em] text-emerald-400/40 mt-1">
                    <span>
                      Max file:{" "}
                      <span className="text-emerald-400/70 tracking-normal text-[11px] font-mono">
                        5 GB direct
                      </span>
                    </span>
                    <div className="w-1 h-1 rounded-full bg-emerald-400/20" />
                    <span className="flex items-center gap-1 group-hover:text-emerald-400/60 transition-colors">
                      <Lock size={10} /> Encrypted
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="flex flex-wrap gap-2 justify-center">
            {[
              { label: "30 sec", value: "30s" },
              { label: "5 min", value: "5p" },
              { label: "30 min", value: "30p" },
              { label: "1 hour", value: "1h" },
              { label: "12 hours", value: "12h" },
              { label: "24 hours", value: "24h" },
              { label: "48 hours", value: "48h" },
            ].map((opt) => (
              <button
                key={opt.value}
                onClick={() => {
                  vibrate();
                  setExpire(opt.value);
                }}
                className={`px-4 py-2 text-xs font-bold rounded-full transition-all ${expire === opt.value ? "bg-emerald-400 text-black shadow-lg shadow-emerald-400/20" : "bg-white/5 text-white/40 hover:bg-white/10 hover:text-white"}`}
              >
                {opt.label}
              </button>
            ))}
          </div>

          <div className="w-full bg-white/[0.03] border border-white/10 rounded-[24px] flex flex-col transition-all overflow-hidden">
            <div 
              onClick={() => {
                vibrate();
                setShowOptions(!showOptions);
              }}
              className="flex items-center justify-between w-full px-4 py-2 cursor-pointer select-none group/header"
            >
              <div className="flex items-center gap-2 text-xs font-bold text-white/50 group-hover/header:text-white transition-colors">
                <span>Advanced Options</span>
                <motion.div animate={{ rotate: showOptions ? 180 : 0 }}>
                  <ChevronDown size={14} className="text-white/50 group-hover/header:text-white transition-colors" />
                </motion.div>
              </div>

              <div 
                className="flex bg-black/40 p-0.5 rounded-xl border border-white/5 shadow-inner"
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  onClick={() => {
                    vibrate();
                    setIsFolderMode(false);
                    setState((prev: any) => ({ ...prev, file: null }));
                  }}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-[10px] text-[9px] font-bold uppercase tracking-wider transition-all ${!isFolderMode ? "bg-emerald-500/20 text-emerald-400 shadow-sm" : "text-white/30 hover:text-white/60"}`}
                >
                  <File size={9} strokeWidth={2.5} /> File
                </button>
                <button
                  onClick={() => {
                    vibrate();
                    setIsFolderMode(true);
                    setState((prev: any) => ({ ...prev, file: null }));
                  }}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-[10px] text-[9px] font-bold uppercase tracking-wider transition-all ${isFolderMode ? "bg-emerald-500/20 text-emerald-400 shadow-sm" : "text-white/30 hover:text-white/60"}`}
                >
                  <Folder size={9} strokeWidth={2.5} /> Folder
                </button>
              </div>
            </div>

            <AnimatePresence>
              {showOptions && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.25, ease: "easeInOut" }}
                  className="overflow-hidden"
                >
                  <div className="px-5 pb-5 pt-3 border-t border-white/5 space-y-4">
                    <div className="space-y-2">
                      <label className="text-[10px] font-black uppercase tracking-widest text-white/30 ml-1">
                        Display Name
                      </label>
                      <input
                        type="text"
                        value={customFileName}
                        onChange={(e) => setCustomFileName(e.target.value)}
                        placeholder="Custom filename..."
                        className="w-full bg-black/40 border border-white/10 rounded-2xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-emerald-400/40 transition-all"
                      />
                    </div>

                    <div className="space-y-2">
                      <label className="text-[10px] font-black uppercase tracking-widest text-white/30 ml-1">
                        Description
                      </label>
                      <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="Add a note..."
                        className="w-full bg-black/40 border border-white/10 rounded-2xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-emerald-400/40 transition-all resize-none h-20"
                      />
                    </div>

                    <div className="space-y-2">
                      <label className="text-[10px] font-black uppercase tracking-widest text-white/30 ml-1">
                        Password (Optional)
                      </label>
                      <div className="relative">
                        <Lock
                          size={14}
                          className="absolute left-4 top-1/2 -translate-y-1/2 text-white/20"
                        />
                        <input
                          type="text"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          placeholder="Set a password..."
                          className="w-full bg-black/40 border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-emerald-400/40 transition-all"
                        />
                      </div>
                    </div>

                    <button
                      onClick={() => {
                        vibrate();
                        setIsOneTime(!isOneTime);
                      }}
                      className={`w-full flex items-center justify-between p-3.5 rounded-2xl border transition-all ${isOneTime ? "bg-red-500/10 border-red-500/30 text-red-400" : "bg-white/5 border-white/10 text-white/50 hover:bg-white/[0.08]"}`}
                    >
                      <div className="flex items-center gap-3">
                        <Trash2
                          size={16}
                          className={isOneTime ? "text-red-400" : "text-white/20"}
                        />
                        <div className="text-left">
                          <p className="text-xs font-bold">Self-Destruct</p>
                          <p className="text-[10px] opacity-60">
                            Delete after one successful read
                          </p>
                        </div>
                      </div>
                      <div
                        className={`w-8 h-4 rounded-full relative transition-colors ${isOneTime ? "bg-red-500" : "bg-white/10"}`}
                      >
                        <motion.div
                          animate={{ x: isOneTime ? 18 : 2 }}
                          className="absolute top-1 w-2 h-2 bg-white rounded-full"
                        />
                      </div>
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <button
            onClick={uploading ? cancelR2Upload : handleR2Upload}
            disabled={!file && !uploading}
            className={`w-full bg-emerald-400 text-black hover:bg-emerald-300 active:scale-[0.98] rounded-full px-6 py-4 font-black text-xs uppercase tracking-widest transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-emerald-400/20`}
          >
            {uploading ? (
              <Loader2 size={20} className="animate-spin" />
            ) : (
              <ShieldCheck size={20} />
            )}
            {uploading ? `Cancel upload ${progress}%` : "Secret Upload to R2"}
          </button>

          {stats && stats.files.length > 0 && (
            <div className="mt-4 p-4 bg-emerald-500/5 border border-emerald-500/10 rounded-3xl space-y-3">
              <div className="flex justify-between items-center">
                <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-400/60">
                  24h History
                </h4>
                <span className="text-[10px] font-bold text-emerald-400">
                  Total: {(stats.totalSize / 1024 / 1024).toFixed(2)} MB / 100 GB
                </span>
              </div>
              <div className="max-h-[120px] overflow-y-auto custom-scrollbar space-y-2 pr-1">
                {stats.files.map((f: any, i: number) => (
                  <div
                    key={i}
                    className="flex justify-between items-center text-[10px]"
                  >
                    <span className="text-white/60 truncate max-w-[150px]">
                      {f.name}
                    </span>
                    <span className="text-emerald-400/40">
                      {(f.size / 1024 / 1024).toFixed(2)} MB
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function SendFile({
  showToast,
  addToHistory,
  state,
  setState,
  stats,
  updateStats,
}: any) {
  const { file, resultCode, expiresAt } = state;
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [password, setPassword] = useState("");
  const [isOneTime, setIsOneTime] = useState(false);
  const [showOptions, setShowOptions] = useState(false);
  const [expire, setExpire] = useState("72h");
  const [isFolderMode, setIsFolderMode] = useState(false);
  const [selectedProvider, setSelectedProvider] = useState("storageto");
  const fileInputRef = useRef<HTMLInputElement>(null);
  const activeXhrRef = useRef<XMLHttpRequest | null>(null);
  const uploadCancelledRef = useRef(false);

  const isLocal = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";

  const UPLOAD_PROVIDERS = [
    {
      id: "storageto",
      name: "storage.to",
      maxSizeLabel: "25 GB",
      expiry: "Deletes after 3 days (1-7 days configurable)",
      speed: "Fast, powered by Cloudflare R2",
      features: "High-speed downloads via Cloudflare CDN. Unthrottled bandwidth.",
    },
    {
      id: "gofile",
      name: "Gofile",
      maxSizeLabel: "3 GB",
      expiry: "Deletes after 10 days of inactivity",
      speed: "Fast, but subject to rate limits",
      features: "Secure & anonymous, supports flexible expiration settings.",
    },
    {
      id: "tmpfiles",
      name: "tmpfiles",
      maxSizeLabel: "100 MB",
      expiry: "Deletes after 60 minutes",
      speed: "Standard speed",
      features: "Short-lived temporary hosting. Automatically deleted after 1 hour.",
    },
    {
      id: "litterbox",
      name: "Litterbox",
      maxSizeLabel: isLocal ? "1 GB" : "100 MB",
      expiry: "Temporary (1, 12, 24, or 72 hours)",
      speed: "Standard speed",
      features: "Strictly temporary file hosting, files deleted automatically.",
    }
  ];

  const selectedProviderInfo =
    UPLOAD_PROVIDERS.find((prov) => prov.id === selectedProvider) ||
    UPLOAD_PROVIDERS[0];

  const getMaxSizeText = () => selectedProviderInfo.maxSizeLabel;

  const bindXhr = (xhr: XMLHttpRequest) => {
    activeXhrRef.current = xhr;
    return xhr;
  };

  const clearActiveXhr = (xhr?: XMLHttpRequest) => {
    if (!xhr || activeXhrRef.current === xhr) {
      activeXhrRef.current = null;
    }
  };

  const cancelUpload = () => {
    if (!loading) return;
    uploadCancelledRef.current = true;
    const xhr = activeXhrRef.current;
    if (xhr) {
      xhr.abort();
    }
    clearActiveXhr(xhr || undefined);
    setLoading(false);
    setProgress(0);
    showToast("Upload canceled", "success");
  };

  const handleSelectProvider = (provId: string) => {
    setSelectedProvider(provId);
    if (provId === "litterbox") {
      const validLitterboxOptions = ["1h", "12h", "24h", "72h"];
      if (!validLitterboxOptions.includes(expire)) {
        setExpire("1h");
      }
    } else if (provId === "tmpfiles") {
      setExpire("1h");
    } else if (provId === "storageto") {
      const validStorageToOptions = ["24h", "48h", "72h", "120h", "168h"];
      if (!validStorageToOptions.includes(expire)) {
        setExpire("72h");
      }
    }
  };

  const EXPIRE_OPTIONS = selectedProvider === "litterbox"
    ? [
        { label: "1 hour", value: "1h" },
        { label: "12 hours", value: "12h" },
        { label: "24 hours", value: "24h" },
        { label: "72 hours", value: "72h" },
      ]
    : selectedProvider === "tmpfiles"
    ? [
        { label: "1 hour (Fixed)", value: "1h" }
      ]
    : selectedProvider === "storageto"
    ? [
        { label: "1 day", value: "24h" },
        { label: "2 days", value: "48h" },
        { label: "3 days (Default)", value: "72h" },
        { label: "5 days", value: "120h" },
        { label: "7 days", value: "168h" },
      ]
    : [
        { label: "30 sec", value: "30s" },
        { label: "5 min", value: "5p" },
        { label: "30 min", value: "30p" },
        { label: "1 hour", value: "1h" },
        { label: "12 hours", value: "12h" },
        { label: "24 hours", value: "24h" },
        { label: "48 hours", value: "48h" },
      ];

  const handleUpload = async () => {
    vibrate();
    if (!file) {
      showToast("Please select a file", "error");
      return;
    }

    uploadCancelledRef.current = false;

    const getSelectedProviderName = () => {
      const p = UPLOAD_PROVIDERS.find(x => x.id === selectedProvider);
      return p ? p.name : "selected host";
    };

    let sizeLimit = 3 * 1024 * 1024 * 1024; // Default Gofile (3 GB)
    let limitLabel = "3 GB";

    if (selectedProvider === "litterbox") {
      sizeLimit = isLocal ? 1 * 1024 * 1024 * 1024 : 100 * 1024 * 1024;
      limitLabel = isLocal ? "1 GB" : "100 MB (due to Cloudflare Workers proxy limits)";
    } else if (selectedProvider === "storageto") {
      sizeLimit = 25 * 1024 * 1024 * 1024;
      limitLabel = "25 GB";
    } else if (selectedProvider === "tmpfiles") {
      sizeLimit = 100 * 1024 * 1024;
      limitLabel = "100 MB";
    }

    if (file.size > sizeLimit) {
      showToast(`File size exceeds limit of ${limitLabel} for ${getSelectedProviderName()}`, "error");
      return;
    }

    setLoading(true);
    setProgress(0);
    try {
      let fileUrl = "";
      let uploadedFilesList: any[] = [];
      const isMulti = (file as any).isMultiFile;

      if (isMulti) {
        const filesToUpload = (file as any).originalFiles;
        const totalFiles = filesToUpload.length;
        const fileProgresses = new Array(totalFiles).fill(0);

        for (let i = 0; i < totalFiles; i++) {
          const currentFile = filesToUpload[i];
          const currentName = currentFile.name;
          const currentType = currentFile.type || "application/octet-stream";

          showToast(`Uploading file ${i + 1} of ${totalFiles}: ${currentName}`, "info");

          const updateOverallProgress = (pct: number) => {
            fileProgresses[i] = pct;
            const avgProgress = Math.round(
              fileProgresses.reduce((acc, p) => acc + p, 0) / totalFiles
            );
            setProgress(avgProgress);
          };

          let currentFileUrl = "";

          if (selectedProvider === "gofile") {
            let serverName = "";
            const cachedServer = sessionStorage.getItem("gofile_server");
            try {
              const serverRes = await fetch("https://api.gofile.io/servers");
              if (serverRes.ok) {
                const serverData = await serverRes.json();
                if (serverData.status === "ok" && serverData.data?.servers?.[0]?.name) {
                  serverName = serverData.data.servers[0].name;
                  sessionStorage.setItem("gofile_server", serverName);
                }
              }
            } catch (err) {
              console.warn("Failed to fetch Gofile server list:", err);
            }
            if (!serverName) {
              serverName = cachedServer || "store1";
            }

            const gofileData = new FormData();
            gofileData.append("file", currentFile, currentName);

            const uploadJson = await new Promise<any>((resolve, reject) => {
              const xhr = bindXhr(new XMLHttpRequest());
              xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                  const pct = Math.round((e.loaded / e.total) * 100);
                  updateOverallProgress(pct);
                }
              };
              xhr.onload = () => {
                clearActiveXhr(xhr);
                if (xhr.status >= 200 && xhr.status < 300) {
                  resolve(JSON.parse(xhr.responseText));
                } else reject(new Error(`Gofile returned status ${xhr.status}`));
              };
              xhr.onabort = () => { clearActiveXhr(xhr); reject(new Error("UPLOAD_CANCELED")); };
              xhr.onerror = () => { clearActiveXhr(xhr); reject(new Error("Network error")); };
              xhr.open("POST", `https://${serverName}.gofile.io/contents/uploadfile`);
              xhr.send(gofileData);
            });

            if (uploadJson.status !== "ok") {
              throw new Error(uploadJson.status || "Gofile upload failed");
            }
            const gofileCode = uploadJson.data.parentFolderCode || uploadJson.data.id;
            currentFileUrl = `https://gofile.io/d/${gofileCode}`;
          } 
          else if (selectedProvider === "litterbox") {
            const lbData = new FormData();
            lbData.append("reqtype", "fileupload");
            const lbTime = expire === "12h" || expire === "24h" || expire === "72h" ? expire : (expire === "48h" ? "72h" : "1h");
            lbData.append("time", lbTime);
            lbData.append("fileToUpload", currentFile, currentName);

            const uploadResult = await new Promise<any>((resolve, reject) => {
              const xhr = bindXhr(new XMLHttpRequest());
              xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                  const pct = Math.round((e.loaded / e.total) * 100);
                  updateOverallProgress(pct);
                }
              };
              xhr.onload = () => {
                clearActiveXhr(xhr);
                resolve(xhr.responseText);
              };
              xhr.onabort = () => { clearActiveXhr(xhr); reject(new Error("UPLOAD_CANCELED")); };
              xhr.onerror = () => { clearActiveXhr(xhr); reject(new Error("Network error")); };
              xhr.open("POST", "/api/proxy/litterbox");
              xhr.send(lbData);
            });

            if (typeof uploadResult === "string" && uploadResult.startsWith("https://")) {
              currentFileUrl = uploadResult.trim();
            } else if (typeof uploadResult === "string" && uploadResult.includes("https://")) {
              const match = uploadResult.match(/(https:\/\/litterbox\.catbox\.moe\/files\/[a-zA-Z0-9.\-_]+)/);
              if (match) currentFileUrl = match[1];
            }
            if (!currentFileUrl) throw new Error("Litterbox upload failed");
          } 
          else if (selectedProvider === "storageto") {
            let visitorToken = localStorage.getItem("hefimer_storageto_visitor_token");
            if (!visitorToken) {
              visitorToken = Math.random().toString(36).substring(2, 18) + Math.random().toString(36).substring(2, 18);
              localStorage.setItem("hefimer_storageto_visitor_token", visitorToken);
            }

            const initRes = await fetch("/api/proxy/storageto/upload/init", {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                "X-Visitor-Token": visitorToken,
              },
              body: JSON.stringify({
                filename: currentName,
                content_type: currentType,
                size: currentFile.size,
              }),
            });
            if (!initRes.ok) throw new Error(`Init storage.to upload failed for ${currentName}`);
            const initData = await initRes.json();
            if (!initData.success) throw new Error(initData.error || "Init storage.to failed");

            const r2Key = initData.r2_key;
            const uploadUrl = initData.upload_url;
            if (!uploadUrl) throw new Error("Upload URL missing from storage.to");

            await new Promise<void>((resolve, reject) => {
              const xhr = bindXhr(new XMLHttpRequest());
              xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                  const pct = Math.round((e.loaded / e.total) * 100);
                  updateOverallProgress(pct);
                }
              };
              xhr.onload = () => {
                clearActiveXhr(xhr);
                if (xhr.status >= 200 && xhr.status < 300) resolve();
                else reject(new Error(`storage.to PUT failed (status ${xhr.status})`));
              };
              xhr.onabort = () => { clearActiveXhr(xhr); reject(new Error("UPLOAD_CANCELED")); };
              xhr.onerror = () => { clearActiveXhr(xhr); reject(new Error("Network error")); };
              xhr.open("PUT", uploadUrl);
              xhr.setRequestHeader("Content-Type", currentType);
              xhr.send(currentFile);
            });

            const confirmRes = await fetch("/api/proxy/storageto/upload/confirm", {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                "X-Visitor-Token": visitorToken,
              },
              body: JSON.stringify({
                filename: currentName,
                size: currentFile.size,
                content_type: currentType,
                r2_key: r2Key,
              }),
            });
            if (!confirmRes.ok) throw new Error("Confirm failed");
            const confirmData = await confirmRes.json();
            currentFileUrl = confirmData.file?.raw_url || confirmData.file?.url;
            if (!currentFileUrl) throw new Error("Confirm URL missing");
          } 
          else if (selectedProvider === "tmpfiles") {
            const tfData = new FormData();
            tfData.append("file", currentFile, currentName);

            const uploadResult = await new Promise<any>((resolve, reject) => {
              const xhr = bindXhr(new XMLHttpRequest());
              xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                  const pct = Math.round((e.loaded / e.total) * 100);
                  updateOverallProgress(pct);
                }
              };
              xhr.onload = () => {
                clearActiveXhr(xhr);
                if (xhr.status >= 200 && xhr.status < 300) {
                  try {
                    resolve(JSON.parse(xhr.responseText));
                  } catch {
                    resolve(xhr.responseText);
                  }
                } else reject(new Error(`tmpfiles returned status ${xhr.status}`));
              };
              xhr.onabort = () => { clearActiveXhr(xhr); reject(new Error("UPLOAD_CANCELED")); };
              xhr.onerror = () => { clearActiveXhr(xhr); reject(new Error("Network error")); };
              xhr.open("POST", "/api/proxy/tmpfiles");
              xhr.send(tfData);
            });

            if (uploadResult && uploadResult.status === "success" && uploadResult.data?.url) {
              const viewUrl = uploadResult.data.url;
              currentFileUrl = viewUrl.replace("tmpfiles.org/", "tmpfiles.org/dl/");
            } else {
              throw new Error(uploadResult.error || "tmpfiles upload failed");
            }
          }

          uploadedFilesList.push({
            fileUrl: currentFileUrl,
            fileName: currentName,
            fileSize: currentFile.size,
            mimeType: currentType,
          });
        }
      } else {
        let fileToUpload: File | Blob = file;
        let finalName = file.name;

      if (isFolderMode && (file as any).originalFiles) {
        showToast("Zipping folder contents...", "info");
        const zip = new JSZip();
        const files = (file as any).originalFiles;

        for (let i = 0; i < files.length; i++) {
          const f = files[i];
          const relativePath = f.webkitRelativePath || f.name;
          zip.file(relativePath, f);
        }

        fileToUpload = await zip.generateAsync({ type: "blob" }, (metadata) => {
          setProgress(Math.round(metadata.percent * 0.3)); // First 30% for zipping
        });

        finalName = finalName.endsWith(".zip") ? finalName : `${finalName}.zip`;
      }

      if (fileToUpload.size > sizeLimit) {
        showToast(`Zipped folder size (${(fileToUpload.size / 1024 / 1024).toFixed(2)} MB) exceeds limit of ${limitLabel} for ${getSelectedProviderName()}`, "error");
        setLoading(false);
        return;
      }

      const uploadWithProgress = (
        url: string,
        formData: FormData,
      ): Promise<any> => {
        return new Promise((resolve, reject) => {
          const xhr = bindXhr(new XMLHttpRequest());
          xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
              const pct = Math.round((e.loaded / e.total) * 100);
              const normalizedPct = isFolderMode
                ? 30 + Math.round(pct * 0.7)
                : pct;
              setProgress(normalizedPct);
            }
          };
          xhr.onload = () => {
            clearActiveXhr(xhr);
            if (xhr.status >= 200 && xhr.status < 300) {
              try {
                resolve(JSON.parse(xhr.responseText));
              } catch (e) {
                resolve(xhr.responseText);
              }
            } else {
              try {
                const errJson = JSON.parse(xhr.responseText);
                reject(new Error(errJson.error || errJson.message || `Upload failed with status ${xhr.status}`));
              } catch {
                reject(new Error(xhr.responseText || `Upload failed with status ${xhr.status}`));
              }
            }
          };
          xhr.onabort = () => {
            clearActiveXhr(xhr);
            reject(new Error("UPLOAD_CANCELED"));
          };
          xhr.onerror = () => {
            clearActiveXhr(xhr);
            reject(new Error("Network error during upload"));
          };
          xhr.open("POST", url);
          xhr.send(formData);
        });
      };

      try {
        if (selectedProvider === "gofile") {
          let serverName = "";
          const cachedServer = sessionStorage.getItem("gofile_server");

          try {
            showToast("Getting Gofile server...", "info");
            const serverRes = await fetch("https://api.gofile.io/servers");
            if (serverRes.ok) {
              const serverData = await serverRes.json();
              if (serverData.status === "ok" && serverData.data?.servers?.[0]?.name) {
                serverName = serverData.data.servers[0].name;
                sessionStorage.setItem("gofile_server", serverName);
              }
            }
          } catch (err) {
            console.warn("Failed to fetch Gofile server list:", err);
          }

          if (!serverName) {
            if (cachedServer) {
              serverName = cachedServer;
            } else {
              const fallbackServers = ["store-eu-par-3", "store1", "store3"];
              serverName = fallbackServers[Math.floor(Math.random() * fallbackServers.length)];
            }
          }

          showToast("Uploading via Gofile.io...", "info");
          const gofileData = new FormData();
          gofileData.append("file", fileToUpload, finalName);
          const uploadJson = await uploadWithProgress(
            `https://${serverName}.gofile.io/contents/uploadfile`,
            gofileData,
          );

          if (uploadJson.status !== "ok") {
            throw new Error(uploadJson.status || "Gofile upload failed");
          }

          const gofileCode =
            uploadJson.data.parentFolderCode || uploadJson.data.id;

          // Gofile has blocked direct web downloads for free users. We MUST redirect to the landing page.
          fileUrl = `https://gofile.io/d/${gofileCode}`;

          // Try to set expiration in Gofile if possible
          const guestToken = uploadJson.data.guestToken;
          if (guestToken && gofileCode) {
            try {
              const durationMs = parseExpire(expire);
              const expireTimestamp = Math.floor(
                (Date.now() + durationMs) / 1000,
              );
              await fetch("https://api.gofile.io/contents", {
                method: "PUT",
                headers: {
                  "Content-Type": "application/json",
                  Authorization: `Bearer ${guestToken}`,
                },
                body: JSON.stringify({
                  contentId: gofileCode,
                  attribute: "expire",
                  attributeValue: expireTimestamp,
                }),
              });
            } catch (err) {
              console.warn("Could not set Gofile expiration", err);
            }
          }
        } else if (selectedProvider === "litterbox") {
          showToast("Uploading via Litterbox...", "info");
          const lbData = new FormData();
          lbData.append("reqtype", "fileupload");
          
          const lbTime = expire === "12h" || expire === "24h" || expire === "72h" ? expire : (expire === "48h" ? "72h" : "1h");
          lbData.append("time", lbTime);
          lbData.append("fileToUpload", fileToUpload, finalName);

          const uploadResult = await uploadWithProgress(
            "/api/proxy/litterbox",
            lbData,
          );

          if (typeof uploadResult === "string" && uploadResult.startsWith("https://")) {
            fileUrl = uploadResult.trim();
          } else if (typeof uploadResult === "string" && uploadResult.includes("https://")) {
            const match = uploadResult.match(/(https:\/\/litterbox\.catbox\.moe\/files\/[a-zA-Z0-9.\-_]+)/);
            if (match) {
              fileUrl = match[1];
            }
          }
        } else if (selectedProvider === "storageto") {
          showToast("Initializing upload with storage.to...", "info");
          
          let visitorToken = localStorage.getItem("hefimer_storageto_visitor_token");
          if (!visitorToken) {
            visitorToken = Math.random().toString(36).substring(2, 18) + Math.random().toString(36).substring(2, 18);
            localStorage.setItem("hefimer_storageto_visitor_token", visitorToken);
          }

          const initRes = await fetch("/api/proxy/storageto/upload/init", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "X-Visitor-Token": visitorToken,
            },
            body: JSON.stringify({
              filename: finalName,
              content_type: fileToUpload.type || "application/octet-stream",
              size: fileToUpload.size,
            }),
          });

          if (!initRes.ok) {
            const errData = await initRes.json().catch(() => ({}));
            throw new Error(errData.error || `Failed to initialize storage.to upload (status ${initRes.status})`);
          }

          const initData = await initRes.json();
          if (!initData.success) {
            throw new Error(initData.error || "Failed to initialize storage.to upload");
          }

          const r2Key = initData.r2_key;
          let fileId = "";
          let ownerToken = initData.owner_token || "";

          if (initData.type === "single") {
            const uploadUrl = initData.upload_url;
            if (!uploadUrl) throw new Error("Failed to retrieve upload URL from storage.to");

            // Perform direct PUT upload
            showToast("Uploading bytes to storage.to...", "info");
            await new Promise<void>((resolve, reject) => {
              const xhr = bindXhr(new XMLHttpRequest());
              xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                  const pct = Math.round((e.loaded / e.total) * 100);
                  const normalizedPct = isFolderMode ? 30 + Math.round(pct * 0.7) : pct;
                  setProgress(normalizedPct);
                }
              };
              xhr.onload = () => {
                clearActiveXhr(xhr);
                if (xhr.status >= 200 && xhr.status < 300) {
                  resolve();
                } else {
                  reject(new Error(`PUT upload failed with status ${xhr.status}`));
                }
              };
              xhr.onabort = () => {
                clearActiveXhr(xhr);
                reject(new Error("UPLOAD_CANCELED"));
              };
              xhr.onerror = () => {
                clearActiveXhr(xhr);
                reject(new Error("Network error during storage.to PUT upload"));
              };
              xhr.open("PUT", uploadUrl);
              xhr.setRequestHeader("Content-Type", fileToUpload.type || "application/octet-stream");
              xhr.send(fileToUpload);
            });
          } else if (initData.type === "multipart") {
            const uploadId = initData.upload_id;
            const partSize = initData.part_size;
            const totalParts = initData.total_parts;
            const initialUrls = initData.initial_urls || {};

            showToast(`Uploading ${totalParts} parts to storage.to...`, "info");

            const completedParts: { partNumber: number; etag: string }[] = [];

            for (let partNumber = 1; partNumber <= totalParts; partNumber++) {
              let partUploadUrl = initialUrls[partNumber.toString()];

              if (!partUploadUrl) {
                const partsRes = await fetch("/api/proxy/storageto/upload/parts", {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Owner ${ownerToken}`,
                  },
                  body: JSON.stringify({
                    upload_id: uploadId,
                    part_numbers: [partNumber],
                  }),
                });
                if (!partsRes.ok) throw new Error(`Failed to fetch part ${partNumber} URL`);
                const partsData = await partsRes.json();
                const partObj = partsData.part_urls?.find((p: any) => p.partNumber === partNumber);
                if (!partObj) throw new Error(`Could not get URL for part ${partNumber}`);
                partUploadUrl = partObj.url;
              }

              const start = (partNumber - 1) * partSize;
              const end = Math.min(start + partSize, fileToUpload.size);
              const chunk = fileToUpload.slice(start, end);

              // Upload the chunk
              const etag = await new Promise<string>((resolve, reject) => {
                const xhr = bindXhr(new XMLHttpRequest());
                xhr.upload.onprogress = (e) => {
                  if (e.lengthComputable) {
                    const chunkPct = Math.round((e.loaded / e.total) * 100);
                    const overallPct = Math.round(((partNumber - 1) / totalParts) * 100 + (chunkPct / totalParts));
                    const normalizedPct = isFolderMode ? 30 + Math.round(overallPct * 0.7) : overallPct;
                    setProgress(normalizedPct);
                  }
                };
                xhr.onload = () => {
                  clearActiveXhr(xhr);
                  if (xhr.status >= 200 && xhr.status < 300) {
                    const etagHeader = xhr.getResponseHeader("ETag");
                    if (etagHeader) {
                      resolve(etagHeader);
                    } else {
                      const fallbackEtag = xhr.getResponseHeader("etag") || xhr.getResponseHeader("ETAG");
                      if (fallbackEtag) resolve(fallbackEtag);
                      else reject(new Error(`ETag header missing in part ${partNumber} upload response`));
                    }
                  } else {
                    reject(new Error(`Part ${partNumber} upload failed with status ${xhr.status}`));
                  }
                };
                xhr.onabort = () => {
                  clearActiveXhr(xhr);
                  reject(new Error("UPLOAD_CANCELED"));
                };
                xhr.onerror = () => {
                  clearActiveXhr(xhr);
                  reject(new Error(`Network error during part ${partNumber} upload`));
                };
                xhr.open("PUT", partUploadUrl);
                xhr.send(chunk);
              });

              completedParts.push({ partNumber, etag });
            }

            // Complete multipart
            showToast("Assembling parts...", "info");
            const completeRes = await fetch("/api/proxy/storageto/upload/complete-multipart", {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                "Authorization": `Owner ${ownerToken}`,
              },
              body: JSON.stringify({
                upload_id: uploadId,
                parts: completedParts,
              }),
            });

            if (!completeRes.ok) {
              const errData = await completeRes.json().catch(() => ({}));
              throw new Error(errData.error || "Failed to assemble multipart upload");
            }
          } else {
            throw new Error(`Unsupported upload type: ${initData.type}`);
          }

          // 3. Confirm upload to finalize
          showToast("Finalizing upload with storage.to...", "info");
          const confirmRes = await fetch("/api/proxy/storageto/upload/confirm", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "X-Visitor-Token": visitorToken,
            },
            body: JSON.stringify({
              filename: finalName,
              size: fileToUpload.size,
              content_type: fileToUpload.type || "application/octet-stream",
              r2_key: r2Key,
            }),
          });

          if (!confirmRes.ok) {
            const errData = await confirmRes.json().catch(() => ({}));
            throw new Error(errData.error || `Failed to confirm storage.to upload (status ${confirmRes.status})`);
          }

          const confirmData = await confirmRes.json();
          if (!confirmData.success || !confirmData.file?.url) {
            throw new Error(confirmData.error || "Failed to confirm file with storage.to");
          }

          fileUrl = confirmData.file.raw_url || confirmData.file.url;
          fileId = confirmData.file.id;
          ownerToken = confirmData.owner_token || ownerToken;

          // 4. Set custom expiry if it is configured
          if (fileId && ownerToken && expire) {
            let days = 3;
            if (expire === "24h") days = 1;
            else if (expire === "48h") days = 2;
            else if (expire === "72h") days = 3;
            else if (expire === "120h") days = 5;
            else if (expire === "168h") days = 7;

            if (days !== 3) {
              try {
                await fetch(`/api/proxy/storageto/file/${fileId}/expiry`, {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Owner ${ownerToken}`,
                  },
                  body: JSON.stringify({ days }),
                });
              } catch (err) {
                console.warn("Could not set custom expiry for storage.to:", err);
              }
            }
          }
        } else if (selectedProvider === "tmpfiles") {
          showToast("Uploading via tmpfiles.org...", "info");
          const tfData = new FormData();
          tfData.append("file", fileToUpload, finalName);

          const uploadResult = await uploadWithProgress(
            "/api/proxy/tmpfiles",
            tfData,
          );

          if (uploadResult && uploadResult.status === "success" && uploadResult.data?.url) {
            const viewUrl = uploadResult.data.url;
            fileUrl = viewUrl.replace("tmpfiles.org/", "tmpfiles.org/dl/");
          } else {
            throw new Error(
              uploadResult.error ||
                uploadResult.message ||
                `Upload failed: ${JSON.stringify(uploadResult)}`
            );
          }
        }
      } catch (e: any) {
        if (uploadCancelledRef.current) {
          throw new Error("UPLOAD_CANCELED");
        }
        console.error(`${selectedProvider} upload error:`, e);
        throw new Error(e.message || "File upload failed");
      }
    }

      showToast("Saving code...", "info");

      // Generate a unique 5-digit code
      let generatedCode = "";
      let isUnique = false;
      let attempts = 0;

      while (!isUnique && attempts < 5) {
        generatedCode = Math.floor(10000 + Math.random() * 90000).toString();
        try {
          const snapshot = await get(
            child(ref(rtdb), `drops/${generatedCode}`),
          );
          if (!snapshot.exists()) {
            isUnique = true;
          }
        } catch (err) {
          handleDatabaseError(err, OperationType.GET, `drops/${generatedCode}`);
        }
        attempts++;
      }

      if (!isUnique) {
        throw new Error("Failed to generate a unique code. Please try again.");
      }

      const durationMs = parseExpire(expire);
      const expiresAtDate = new Date(Date.now() + durationMs);

      const dropData: any = {
        isDirectFile: true,
        fileName: isMulti ? `${uploadedFilesList.length} files` : file.name,
        fileUrl: isMulti ? uploadedFilesList[0].fileUrl : fileUrl,
        mimeType: isMulti ? "application/octet-stream" : (file.type || "application/octet-stream"),
        type: "file",
        expire: expire,
        text: isMulti
          ? `__HEFIMER_FILES__${JSON.stringify(uploadedFilesList)}`
          : `__HEFIMER_FILE__${JSON.stringify({ isDirectFile: true, fileName: file.name, fileUrl: fileUrl })}`,
        expiresAt: expiresAtDate.getTime(),
        timestamp: { ".sv": "timestamp" },
      };

      if (isMulti) {
        dropData.files = uploadedFilesList;
      }

      if (password.trim()) {
        dropData.password = password.trim();
      }
      if (isOneTime) {
        dropData.isOneTime = true;
      }

      try {
        // Save as object to comply with new rules
        await set(ref(rtdb, `drops/${generatedCode}`), dropData);
      } catch (err) {
        handleDatabaseError(err, OperationType.WRITE, `drops/${generatedCode}`);
      }

      setState((prev: any) => ({
        ...prev,
        resultCode: generatedCode,
        expiresAt: expiresAtDate.getTime(),
      }));
      addToHistory({
        code: generatedCode,
        fileName: isMulti ? `${uploadedFilesList.length} files` : file.name,
        fileSize: file.size,
        type: "file",
        action: "sent",
      });
      updateStats(isMulti ? `${uploadedFilesList.length} files` : file.name, file.size, false);
      showToast("File uploaded and code generated!", "success");
      window.dispatchEvent(new CustomEvent("hefimer:drop-created", {
        detail: {
          kind: "file",
          name: isMulti ? `${uploadedFilesList.length} files` : file.name,
          provider: selectedProviderInfo.name,
          sizeBytes: file.size,
          dropCode: generatedCode,
          expiresAt: expiresAtDate.getTime(),
          payload: dropData,
        },
      }));
    } catch (err: any) {
      if (err?.message !== "UPLOAD_CANCELED" && !uploadCancelledRef.current) {
        console.error(err);
        showToast(sanitizeError(err.message) || "An error occurred", "error");
      }
    } finally {
      clearActiveXhr();
      setLoading(false);
    }
  };

  return (
    <AnimatePresence mode="wait">
      {resultCode ? (
        <ResultView
          key="result"
          code={resultCode}
          fileName={file?.name}
          expiresAt={expiresAt}
          onReset={() => {
            vibrate();
            setState({ file: null, resultCode: "", expiresAt: null });
          }}
          onBack={() => {
            vibrate();
            setState((prev: any) => ({
              ...prev,
              resultCode: "",
              expiresAt: null,
            }));
          }}
          showToast={showToast}
        />
      ) : (
        <motion.div
          key="form"
          initial={{ opacity: 0, scale: 0.98, y: 5 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: -5 }}
          transition={{ duration: 0.2 }}
          className="space-y-2.5 flex flex-col"
        >
          <div
            onClick={() => !loading && fileInputRef.current?.click()}
            onDragOver={(e) => {
              e.preventDefault();
              !loading && setIsDragging(true);
            }}
            onDragLeave={(e) => {
              e.preventDefault();
              setIsDragging(false);
            }}
            onDrop={(e) => {
              e.preventDefault();
              setIsDragging(false);
              if (!loading && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                const filesList = e.dataTransfer.files;
                const fileToSet = filesList.length > 1
                  ? {
                      name: `${filesList.length} files`,
                      size: Array.from(filesList).reduce((acc, f) => acc + f.size, 0),
                      originalFiles: Array.from(filesList),
                      isMultiFile: true,
                    }
                  : filesList[0];
                setState((prev: any) => ({
                  ...prev,
                  file: fileToSet,
                }));
                vibrate();
              }
            }}
            className={`w-full h-36 bg-white/[0.02] hover:bg-white/[0.04] border border-dashed ${isDragging ? "border-white/50 bg-white/10" : "border-white/20 hover:border-white/40"} rounded-[40px] flex items-center justify-center cursor-pointer transition-all group relative overflow-hidden`}
          >
            {loading && (
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${progress}%` }}
                className="absolute bottom-0 left-0 h-1.5 bg-white shadow-[0_0_15px_rgba(255,255,255,0.5)]"
              />
            )}

            <input
              type="file"
              className="hidden"
              ref={fileInputRef}
              {...(isFolderMode
                ? ({ webkitdirectory: "", directory: "" } as any)
                : { multiple: true })}
              onChange={(e) => {
                if (e.target.files && e.target.files.length > 0) {
                  const firstFile = e.target.files[0];
                  // If folder mode, try to infer folder name from path
                  const displayName =
                    isFolderMode && (firstFile as any).webkitRelativePath
                      ? (firstFile as any).webkitRelativePath.split("/")[0]
                      : firstFile.name;

                  // We simulate a folder object for UI display
                  const fileToSet = isFolderMode
                    ? {
                        name: displayName,
                        size: Array.from(e.target.files).reduce(
                          (acc, f) => acc + f.size,
                          0,
                        ),
                        originalFiles: e.target.files, // just in case
                      }
                    : e.target.files.length > 1
                    ? {
                        name: `${e.target.files.length} files`,
                        size: Array.from(e.target.files).reduce(
                          (acc, f) => acc + f.size,
                          0,
                        ),
                        originalFiles: Array.from(e.target.files),
                        isMultiFile: true,
                      }
                    : firstFile;

                  setState((prev: any) => ({
                    ...prev,
                    file: fileToSet as any,
                  }));
                  vibrate();
                }
              }}
            />

            {loading ? (
              <div className="flex flex-col sm:flex-row items-center gap-4 sm:gap-6 w-full px-6 py-4 justify-center transition-all text-center sm:text-left">
                <div className="w-14 h-14 bg-white/5 rounded-full flex items-center justify-center transition-all border border-white/10 shadow-inner shrink-0">
                  <Loader2 size={24} className="text-white/70 animate-spin" />
                </div>

                <div className="flex flex-col items-center sm:items-start justify-center">
                  <p className="text-white font-bold text-base leading-tight mb-1 truncate max-w-[240px]">
                    Uploading... {progress}%
                  </p>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      cancelUpload();
                    }}
                    className="mt-2 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-3 py-1.5 text-[10px] font-bold uppercase tracking-[0.18em] text-white/65 transition-all hover:bg-red-500/15 hover:text-red-300 hover:border-red-400/30"
                  >
                    <X size={12} />
                    Cancel Upload
                  </button>
                </div>
              </div>
            ) : file ? (
              <div className="flex flex-col sm:flex-row items-center gap-4 sm:gap-6 animate-in fade-in slide-in-from-bottom-4 duration-500 w-full px-6 py-4 justify-center text-center sm:text-left">
                <div className="relative group/file shrink-0">
                  <div className="w-14 h-14 bg-white/5 rounded-full flex items-center justify-center border border-white/10 shadow-xl transition-transform group-hover/file:scale-105">
                    {isFolderMode ? (
                      <Folder size={24} className="text-white/80" />
                    ) : (file as any).isMultiFile ? (
                      <Files size={24} className="text-white/80" />
                    ) : (
                      <File size={24} className="text-white/80" />
                    )}
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setState((prev: any) => ({ ...prev, file: null }));
                      vibrate();
                    }}
                    className="absolute -top-1 -right-1 w-6 h-6 bg-white text-black rounded-full flex items-center justify-center border border-black shadow-lg hover:bg-emerald-400 hover:scale-110 active:scale-95 transition-all z-20"
                    title="Remove File"
                  >
                    <X size={12} strokeWidth={3} />
                  </button>
                </div>
                <div className="flex flex-col items-center sm:items-start justify-center max-w-[220px]">
                  <h4 className="text-white font-bold text-sm break-all leading-tight mb-1 line-clamp-2">
                    {file.name}
                  </h4>
                  <p className="text-white/40 text-xs font-medium tracking-wide">
                    {(file.size / 1024 / 1024).toFixed(2)} MB
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex flex-col sm:flex-row items-center gap-4 sm:gap-6 w-full px-4 py-4 justify-center text-center sm:text-left transition-all">
                <div className="w-14 h-14 bg-white/5 group-hover:bg-white/10 rounded-full flex items-center justify-center transition-all border border-white/10 shrink-0 shadow-inner">
                  {isFolderMode ? (
                    <FolderPlus
                      size={24}
                      className="text-white/70 group-hover:text-white group-hover:scale-110 transition-all"
                    />
                  ) : (
                    <FileUp
                      size={24}
                      className="text-white/70 group-hover:text-white group-hover:scale-110 transition-all"
                    />
                  )}
                </div>

                <div className="flex flex-col items-center sm:items-start justify-center">
                  <p className="text-white font-bold text-base leading-tight">
                    {isFolderMode
                      ? "Tap to select a folder"
                      : "Tap to select file(s)"}
                  </p>
                  {!loading && (
                    <div className="flex flex-wrap items-center justify-center sm:justify-start gap-x-2 gap-y-1.5 text-[9px] uppercase font-bold tracking-[0.05em] text-white/40 mt-1.5">
                      <span className="inline-flex items-center gap-1 rounded-full border border-emerald-400/15 bg-emerald-400/10 px-1.5 py-0.5 text-[8px] tracking-[0.1em] text-emerald-300">
                        <span className="h-1 w-1 rounded-full bg-emerald-300" />
                        {selectedProviderInfo.name}
                      </span>
                      <div className="hidden sm:block w-1 h-1 rounded-full bg-white/20" />
                      <span>
                        Max:{" "}
                        <span className="text-white/70 tracking-normal text-[10px] font-mono">
                          {getMaxSizeText()}
                        </span>
                      </span>
                      <div className="hidden sm:block w-1 h-1 rounded-full bg-white/20" />
                      <span>
                        Today:{" "}
                        <span className="text-white/70 tracking-normal text-[10px] font-mono">
                          {((stats?.totalSize || 0) / 1024 / 1024).toFixed(2)} MB
                        </span>
                      </span>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="w-full bg-white/[0.03] border border-white/10 rounded-[24px] flex flex-col transition-all overflow-hidden">
            <div 
              onClick={() => {
                vibrate();
                setShowOptions(!showOptions);
              }}
              className="flex items-center justify-between w-full px-4 py-2 cursor-pointer select-none group/header"
            >
              <div className="flex items-center gap-2 text-xs font-bold text-white/50 group-hover/header:text-white transition-colors">
                <span>Advanced Options</span>
                <motion.div animate={{ rotate: showOptions ? 180 : 0 }}>
                  <ChevronDown size={14} className="text-white/50 group-hover/header:text-white transition-colors" />
                </motion.div>
              </div>

              <div 
                className="flex bg-black/40 p-0.5 rounded-xl border border-white/5 shadow-inner"
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  onClick={() => {
                    vibrate();
                    setIsFolderMode(false);
                    setState((prev: any) => ({ ...prev, file: null }));
                  }}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-[10px] text-[9px] font-bold uppercase tracking-wider transition-all ${!isFolderMode ? "bg-white/15 text-white shadow-sm" : "text-white/30 hover:text-white/60"}`}
                >
                  <File size={9} strokeWidth={2.5} /> File
                </button>
                <button
                  onClick={() => {
                    vibrate();
                    setIsFolderMode(true);
                    setState((prev: any) => ({ ...prev, file: null }));
                  }}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-[10px] text-[9px] font-bold uppercase tracking-wider transition-all ${isFolderMode ? "bg-white/15 text-white shadow-sm" : "text-white/30 hover:text-white/60"}`}
                >
                  <Folder size={9} strokeWidth={2.5} /> Folder
                </button>
              </div>
            </div>

            <AnimatePresence>
              {showOptions && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.25, ease: "easeInOut" }}
                  className="overflow-hidden"
                >
                  <div className="px-5 pb-5 pt-3 border-t border-white/5 space-y-4">
                    <div className="space-y-2">
                      <label className="text-[10px] font-black uppercase tracking-widest text-white/30 ml-1">
                        Upload Host Provider
                      </label>
                      <div className="grid grid-cols-2 gap-2 px-1">
                        {UPLOAD_PROVIDERS.map((prov) => {
                          const isSelected = selectedProvider === prov.id;
                          return (
                            <button
                              key={prov.id}
                              type="button"
                              onClick={() => handleSelectProvider(prov.id)}
                              className={`w-full py-2.5 px-2 rounded-2xl border text-xs font-bold transition-all flex flex-col items-center justify-center gap-0.5 ${
                                isSelected
                                  ? "bg-white text-black border-white shadow-[0_0_15px_rgba(255,255,255,0.2)]"
                                  : "bg-white/[0.02] border-white/10 text-white/60 hover:bg-white/[0.05] hover:text-white"
                              }`}
                            >
                              <span>{prov.name}</span>
                              <span className={`text-[8px] font-mono tracking-tight ${isSelected ? "text-black/50" : "text-white/30"}`}>
                                {prov.maxSizeLabel}
                              </span>
                            </button>
                          );
                        })}
                      </div>

                      {/* Selected Host Limits Info Card */}
                      {(() => {
                        const prov = UPLOAD_PROVIDERS.find(p => p.id === selectedProvider);
                        if (!prov) return null;
                        return (
                          <div className="mx-1 mt-2.5 p-3 bg-white/[0.02] border border-white/10 rounded-2xl flex flex-col gap-1.5 backdrop-blur-md">
                            <p className="text-white font-bold text-[10px] uppercase tracking-wider border-b border-white/5 pb-1 flex items-center gap-1.5">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                              {prov.name} Limits & Info
                            </p>
                            <div className="text-[10px] text-white/70 space-y-1">
                              <div className="flex justify-between">
                                <span className="text-white/40">Max File Size:</span>
                                <span className="text-emerald-400 font-mono font-bold">{prov.maxSizeLabel}</span>
                              </div>
                              <div className="flex justify-between gap-4">
                                <span className="text-white/40 shrink-0">Expiry Policy:</span>
                                <span className="text-amber-400 text-right leading-tight">{prov.expiry}</span>
                              </div>
                              <div className="flex justify-between gap-4">
                                <span className="text-white/40 shrink-0">Speed & Features:</span>
                                <span className="text-blue-400 text-right leading-tight">{prov.speed}</span>
                              </div>
                              <div className="text-[9px] text-white/40 leading-relaxed border-t border-white/5 pt-1.5 mt-1 italic text-left">
                                {prov.features}
                              </div>
                            </div>
                          </div>
                        );
                      })()}
                    </div>

                    <div className="space-y-2">
                      <label className="text-[10px] font-black uppercase tracking-widest text-white/30 ml-1">
                        Self-Destruct Timer
                      </label>
                      <div className="flex flex-wrap gap-2 justify-center">
                        {EXPIRE_OPTIONS.map((opt) => (
                          <button
                            key={opt.value}
                            onClick={() => {
                              vibrate();
                              setExpire(opt.value);
                            }}
                            className={`px-3.5 py-2 rounded-full text-xs font-bold transition-all ${expire === opt.value ? "bg-white text-black shadow-[0_0_10px_rgba(255,255,255,0.3)]" : "bg-white/5 text-white/40 hover:bg-white/10"}`}
                          >
                            {opt.label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="space-y-2">
                      <label className="text-[10px] font-black uppercase tracking-widest text-white/30 ml-1">
                        Password (Optional)
                      </label>
                      <div className="relative">
                        <Lock
                          size={14}
                          className="absolute left-4 top-1/2 -translate-y-1/2 text-white/20"
                        />
                        <input
                          type="text"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          placeholder="Set a password..."
                          className="w-full bg-black/40 border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-white/30 transition-all"
                        />
                      </div>
                    </div>

                    <button
                      onClick={() => {
                        vibrate();
                        setIsOneTime(!isOneTime);
                      }}
                      className={`w-full flex items-center justify-between p-3.5 rounded-2xl border transition-all ${isOneTime ? "bg-red-500/10 border-red-500/30 text-red-400" : "bg-white/5 border-white/10 text-white/50 hover:bg-white/[0.08]"}`}
                    >
                      <div className="flex items-center gap-3">
                        <Trash2
                          size={16}
                          className={isOneTime ? "text-red-400" : "text-white/20"}
                        />
                        <div className="text-left">
                          <p className="text-xs font-bold">Self-Destruct</p>
                          <p className="text-[10px] opacity-60">
                            Delete after one successful read
                          </p>
                        </div>
                      </div>
                      <div
                        className={`w-8 h-4 rounded-full relative transition-colors ${isOneTime ? "bg-red-500" : "bg-white/10"}`}
                      >
                        <motion.div
                          animate={{ x: isOneTime ? 18 : 2 }}
                          className="absolute top-1 w-2 h-2 bg-white rounded-full"
                        />
                      </div>
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <button
            onClick={handleUpload}
            disabled={loading || !file}
            className={glassButton}
          >
            {loading && <Loader2 className="animate-spin" size={20} />}
            {loading ? "Uploading..." : "Upload & Generate Code"}
          </button>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function Receive({
  showToast,
  addToHistory,
  state,
  setState,
}: any) {
  const { code, result } = state;
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [showPasswordPrompt, setShowPasswordPrompt] = useState(false);
  const [inputPassword, setInputPassword] = useState("");
  const [pendingData, setPendingData] = useState<any>(null);
  const [isInputFocused, setIsInputFocused] = useState(false);
  const hasAutoFetched = useRef(false);

  const handleRetrieve = async (bypassPassword = false) => {
    vibrate();
    if (code.length !== 5) {
      showToast("Please enter exactly 5 digits", "error");
      return;
    }

    setLoading(true);

    try {
      const snapshot = await get(child(ref(rtdb), `drops/${code}`));

      if (snapshot.exists()) {
        const data = snapshot.val();
        const expiresAt = new Date(data.expiresAt);

        if (expiresAt < new Date()) {
          showToast("Code has expired", "error");
          return;
        }

        if (data.password && !bypassPassword) {
          setPendingData(data);
          setShowPasswordPrompt(true);
          setLoading(false);
          return;
        }

        if (data.password && bypassPassword) {
          if (inputPassword !== data.password) {
            showToast("Incorrect password", "error");
            setLoading(false);
            return;
          }
        }

        setState((prev: any) => ({ ...prev, result: { ...data, code: code } }));
        window.dispatchEvent(new CustomEvent("hefimer:drop-opened", { detail: { code } }));
        addToHistory({
          code: code,
          type: data.type || "text",
          fileName: data.fileName,
          action: "received",
        });
        showToast("Data retrieved successfully!", "success");
      } else {
        showToast("Code not found", "error");
      }
    } catch (err: any) {
      console.error(err);
      if (err.message && err.message.toLowerCase().includes("permission")) {
        handleDatabaseError(err, OperationType.GET, `drops/${code}`);
      }
      showToast(
        sanitizeError(err.message) || "Failed to retrieve data",
        "error",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (
      code &&
      code.length === 5 &&
      !result &&
      !loading &&
      !hasAutoFetched.current
    ) {
      hasAutoFetched.current = true;
      handleRetrieve();
    }
  }, [code]);

  const handlePasswordSubmit = () => {
    vibrate();
    if (!inputPassword.trim()) {
      showToast("Please enter the password", "error");
      return;
    }
    handleRetrieve(true);
  };

  return (
    <AnimatePresence mode="wait">
      {result ? (
        <ReceiveResult
          key="result"
          item={result}
          code={code}
          onReset={() => {
            vibrate();
            setState({ code: "", result: null });
          }}
          showToast={showToast}
        />
      ) : (
        <motion.div
          key="form"
          initial={{ opacity: 0, scale: 0.98, y: 5 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: -5 }}
          transition={{ duration: 0.2 }}
          className="space-y-6 flex flex-col items-center relative"
        >
          <div className="w-full max-w-[340px] relative h-20 flex items-center justify-center">
            {/* Hidden Input overlaid on top */}
            <input
              type="text"
              value={code}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, "").slice(0, 5);
                setState((prev: any) => ({ ...prev, code: val }));
                if (val.length === 5) vibrate();
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter" && code.length === 5 && !loading) {
                  handleRetrieve();
                }
              }}
              onFocus={() => setIsInputFocused(true)}
              onBlur={() => setIsInputFocused(false)}
              pattern="\d*"
              inputMode="numeric"
              className="absolute inset-0 w-full h-full opacity-0 cursor-text z-20 text-[1px]"
              autoComplete="one-time-code"
            />
            
            {/* Visual Slots */}
            <div className="flex gap-3 sm:gap-4 justify-between w-full z-10 pointer-events-none">
              {[0, 1, 2, 3, 4].map((index) => {
                const char = code[index] || "";
                const showCursor = isInputFocused && code.length === index;
                
                return (
                  <div
                    key={index}
                    className={`w-11 h-14 sm:w-14 sm:h-18 flex items-center justify-center border-b-[3px] transition-all duration-200 relative ${
                      char
                        ? "border-white text-white"
                        : showCursor
                          ? "border-white"
                          : "border-white/15"
                    }`}
                  >
                    <span className="text-3xl sm:text-5xl font-black font-sans select-none leading-none">
                      {char}
                    </span>
                    {/* Blinking cursor */}
                    {showCursor && (
                      <motion.div
                        animate={{ opacity: [1, 0, 1] }}
                        transition={{ repeat: Infinity, duration: 1.0, ease: "easeInOut" }}
                        className="absolute w-0.5 h-6 sm:h-8 bg-white"
                      />
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          <button
            onClick={() => handleRetrieve()}
            disabled={loading || code.length !== 5}
            className={glassButton + " max-w-[340px]"}
          >
            {loading ? (
              <Loader2 className="animate-spin" size={20} />
            ) : (
              <Download size={20} />
            )}
            {loading ? "Retrieving..." : "Retrieve"}
          </button>

          <AnimatePresence>
            {showPasswordPrompt && (
              <div className="fixed inset-0 z-[110] flex items-center justify-center p-6">
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  onClick={() => setShowPasswordPrompt(false)}
                  className="absolute inset-0 bg-black/80 backdrop-blur-xl"
                />
                <motion.div
                  initial={{ opacity: 0, scale: 0.9, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.9, y: 20 }}
                  className="relative w-full max-w-sm bg-zinc-900/90 border border-white/10 rounded-[40px] p-8 shadow-2xl"
                >
                  <div className="flex flex-col items-center text-center space-y-6">
                    <div className="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center border border-white/10">
                      <Lock size={24} className="text-white" />
                    </div>
                    <div>
                      <h3 className="text-white font-black uppercase tracking-widest text-sm">
                        Protected Drop
                      </h3>
                      <p className="text-white/40 text-[10px] uppercase tracking-widest mt-1">
                        Enter password to unlock
                      </p>
                    </div>
                    <input
                      type="password"
                      value={inputPassword}
                      onChange={(e) => setInputPassword(e.target.value)}
                      placeholder="Password..."
                      autoFocus
                      className="w-full bg-black/40 border border-white/10 rounded-2xl px-6 py-4 text-center text-sm text-white focus:outline-none focus:border-white/30 transition-all"
                      onKeyDown={(e) =>
                        e.key === "Enter" && handlePasswordSubmit()
                      }
                    />
                    <div className="flex gap-3 w-full">
                      <button
                        onClick={() => setShowPasswordPrompt(false)}
                        className="flex-1 bg-white/5 hover:bg-white/10 border border-white/10 rounded-full py-4 font-bold text-[10px] uppercase tracking-widest text-white/40"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={handlePasswordSubmit}
                        className="flex-1 bg-white text-black rounded-full py-4 font-black text-[10px] uppercase tracking-widest hover:scale-[1.02] active:scale-[0.98] transition-all"
                      >
                        Unlock
                      </button>
                    </div>
                  </div>
                </motion.div>
              </div>
            )}
          </AnimatePresence>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function HistoryView({ history, setHistory, showToast }: any) {
  const [copiedItemCode, setCopiedItemCode] = useState<string | null>(null);
  const clearHistory = () => {
    vibrate();
    setHistory([]);
    localStorage.removeItem("hefimer_history");
    showToast("History cleared", "success");
  };

  if (history.length === 0) {
    return (
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="flex flex-col items-center justify-center py-12 text-white/40"
      >
        <Clock size={48} className="mb-4 opacity-20" />
        <p>No recent history</p>
      </motion.div>
    );
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case "file":
        return <FileUp size={14} />;
      case "r2_file":
        return <ShieldCheck size={14} className="text-emerald-400" />;
      case "text":
        return <Code size={14} />;
      case "room":
        return <MessageSquare size={14} />;
      case "board":
        return <Palette size={14} />;
      default:
        return <Clock size={14} />;
    }
  };

  return (
    <div className="space-y-4 max-h-[350px] overflow-y-auto pr-1.5 custom-scrollbar">
      <AnimatePresence>
        {history.map((item: any, i: number) => (
          <motion.div
            key={`${item.code}-${item.action}-${item.createdAt}-${i}`}
            layout
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="bg-white/5 border border-white/10 rounded-[24px] p-4 flex items-center justify-between group hover:bg-white/10 transition-all"
          >
            <div className="flex flex-col overflow-hidden pr-4">
              <div className="flex items-center gap-2">
                <span className="text-2xl font-sans font-bold tracking-widest text-white">
                  {item.code}
                </span>
                <span
                  className={`text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full ${["sent", "created"].includes(item.action) ? "bg-green-500/20 text-green-400" : "bg-blue-500/20 text-blue-400"}`}
                >
                  {["sent", "created"].includes(item.action)
                    ? "Created"
                    : item.action === "joined"
                      ? "Joined"
                      : "Received"}
                </span>
              </div>
              <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
                {item.fileName ? (
                  <>
                    <span className="text-[10px] text-white/50 shrink-0">
                      {getTypeIcon(item.isR2 ? "r2_file" : "file")}
                    </span>
                    <span
                      className="text-[10px] text-white/40 truncate max-w-[140px]"
                      title={item.fileName}
                    >
                      {item.fileName}
                    </span>
                    {item.fileSize && (
                      <>
                        <span className="text-white/20">•</span>
                        <span className="text-[9px] text-white/30 shrink-0">
                          {(item.fileSize / (1024 * 1024)).toFixed(2)} MB
                        </span>
                      </>
                    )}
                  </>
                ) : (
                  <>
                    <span className="text-[10px] text-white/50 shrink-0">
                      {getTypeIcon(item.type)}
                    </span>
                    <span className="text-[10px] text-white/40 uppercase tracking-widest">
                      {item.type}
                    </span>
                  </>
                )}
                <span className="text-white/20">•</span>
                <span className="text-[10px] text-white/20">
                  {new Date(item.createdAt).toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
              </div>
            </div>
            <button
              onClick={() => {
                vibrate();
                navigator.clipboard.writeText(item.code);
                setCopiedItemCode(item.code);
                setTimeout(() => setCopiedItemCode(null), 2000);
              }}
              className={`w-10 h-10 rounded-full flex items-center justify-center transition-all shrink-0 cursor-pointer ${copiedItemCode === item.code ? "bg-emerald-500 text-black hover:bg-emerald-400" : "bg-white/5 text-white/60 hover:text-black hover:bg-white"}`}
              title={copiedItemCode === item.code ? "Copied!" : "Copy Code"}
            >
              {copiedItemCode === item.code ? <Check size={16} /> : <Copy size={16} />}
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}

export interface HistoryItem {
  code: string;
  fileName?: string;
  fileSize?: number;
  isR2?: boolean;
  roomName?: string;
  boardName?: string;
  type: "file" | "text" | "room" | "board" | "r2_file";
  action: "sent" | "received" | "created" | "joined";
  createdAt: number;
}

function Chat({
  showToast,
  addToHistory,
  adminRoomId,
  state,
  setState,
  isAdmin,
  isFrozen,
}: any) {
  const {
    roomId,
    roomName,
    roomNameInput,
    roomExpire,
    userName,
    inRoom,
    showCreatedModal,
  } = state;

  const REACTIONS = [
    { id: "heart", icon: Heart, color: "text-red-500", label: "Love" },
    { id: "thumb", icon: ThumbsUp, color: "text-blue-500", label: "Like" },
    { id: "smile", icon: Smile, color: "text-yellow-500", label: "Haha" },
    { id: "frown", icon: Frown, color: "text-purple-500", label: "Sad" },
    { id: "fire", icon: Flame, color: "text-orange-500", label: "Hot" },
    {
      id: "party",
      icon: PartyPopper,
      color: "text-pink-500",
      label: "Celebrate",
    },
    { id: "ghost", icon: Ghost, color: "text-white", label: "Ghost" },
    { id: "rocket", icon: Rocket, color: "text-cyan-500", label: "Rocket" },
    { id: "star", icon: Star, color: "text-yellow-400", label: "Star" },
    { id: "zap", icon: Zap, color: "text-yellow-300", label: "Cool" },
  ];

  const [messages, setMessages] = useState<any[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [memberCount, setMemberCount] = useState(0);
  const [timeLeft, setTimeLeft] = useState<string>("");
  const [isCriticalTime, setIsCriticalTime] = useState(false);
  const [roomExpiresAt, setRoomExpiresAt] = useState<number | null>(null);
  const [hoveredMsg, setHoveredMsg] = useState<string | null>(null);
  const [activeReactionMsgId, setActiveReactionMsgId] = useState<string | null>(null);
  const [activeLobbyTab, setActiveLobbyTab] = useState<"join" | "create">("join");
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [showQR, setShowQR] = useState(false);
  const [downloadQRFn, setDownloadQRFn] = useState<(() => void) | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const [customCode, setCustomCode] = useState("");
  const [checkingCode, setCheckingCode] = useState(false);
  const [codeStatus, setCodeStatus] = useState<"available" | "taken" | "idle">("idle");

  useEffect(() => {
    // Generate a random 5-digit code on mount
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
        const snapshot = await get(child(ref(rtdb), `chats/${customCode}`));
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
        console.error("Availability check failed:", err);
        setCodeStatus("available");
      } finally {
        setCheckingCode(false);
      }
    };

    const delayDebounce = setTimeout(() => {
      checkCodeAvailability();
    }, 300);

    return () => clearTimeout(delayDebounce);
  }, [customCode]);

  const handleCustomCodeChange = (val: string) => {
    const cleaned = val.replace(/\D/g, "").slice(0, 5);
    setCustomCode(cleaned);
  };

  const generateRandomCustomCode = () => {
    vibrate();
    const newCode = Math.floor(10000 + Math.random() * 90000).toString();
    setCustomCode(newCode);
  };

  useEffect(() => {
    if (inRoom && roomId) {
      const roomRef = ref(rtdb, `chats/${roomId}`);
      const messagesRef = ref(rtdb, `chats/${roomId}/messages`);
      const membersRef = ref(rtdb, `chats/${roomId}/members`);
      const userMemberRef = ref(
        rtdb,
        `chats/${roomId}/members/${auth.currentUser?.uid || "anon" + Math.random().toString(36).substr(2, 9)}`,
      );

      // Listen to room data for name and expiration
      const unsubscribeRoom = onValue(roomRef, (snapshot) => {
        if (snapshot.exists()) {
          const data = snapshot.val();
          setState((prev: any) => ({ ...prev, roomName: data.name || "" }));
          setRoomExpiresAt(data.expiresAt || null);

          if (data.expiresAt && data.expiresAt < Date.now()) {
            showToast("Room has expired", "error");
            setState((prev: any) => ({ ...prev, inRoom: false }));
          }
        } else {
          showToast("Room not found", "error");
          setState((prev: any) => ({ ...prev, inRoom: false }));
        }
      });

      // Set presence (only if not admin)
      if (!adminRoomId) {
        set(userMemberRef, { name: userName, joinedAt: serverTimestamp() });
        onDisconnect(userMemberRef).remove();
      }

      const statsRef = ref(rtdb, "stats");
      const unsubscribeStats = onValue(statsRef, (snapshot) => {
        const data = snapshot.val();
        if (data) {
          // Sync app context if needed
        }
      });

      const unsubscribeMessages = onValue(messagesRef, (snapshot) => {
        const data = snapshot.val();
        if (data) {
          const msgList = Object.entries(data).map(([id, val]: any) => ({
            id,
            ...val,
          }));
          setMessages(msgList.sort((a, b) => a.timestamp - b.timestamp));
        } else {
          setMessages([]);
        }
      });

      const unsubscribeMembers = onValue(membersRef, (snapshot) => {
        setMemberCount(snapshot.size);
      });

      return () => {
        unsubscribeRoom();
        unsubscribeMessages();
        unsubscribeMembers();
        unsubscribeStats();
        if (!adminRoomId) remove(userMemberRef);
      };
    }
  }, [inRoom, roomId, userName, adminRoomId, setState]);

  // Separate timer for countdown to avoid re-fetching room data every second
  useEffect(() => {
    if (!roomExpiresAt || !inRoom) return;

    const updateTimer = () => {
      const diff = roomExpiresAt - Date.now();
      if (diff <= 0) {
        showToast("Room has expired", "error");
        setState((prev: any) => ({ ...prev, inRoom: false }));
        return;
      }

      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      setTimeLeft(
        `${hours.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`,
      );
      setIsCriticalTime(diff < 60000);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [roomExpiresAt, inRoom, setState, showToast]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleDeleteMessage = async (msgId: string) => {
    if (!adminRoomId) return;
    vibrate();
    try {
      await remove(ref(rtdb, `chats/${roomId}/messages/${msgId}`));
      showToast("Message deleted", "success");
    } catch (err) {
      showToast("Failed to delete", "error");
    }
  };

  const handleCreateRoom = async () => {
    vibrate();
    if (isFrozen && !isAdmin) {
      showToast("System is currently frozen", "error");
      return;
    }
    if (!userName.trim()) {
      showToast("Please enter your name", "error");
      return;
    }
    if (!roomNameInput.trim()) {
      showToast("Please enter a room name", "error");
      return;
    }
    if (customCode.length !== 5) {
      showToast("Room code must be exactly 5 digits", "error");
      return;
    }
    if (codeStatus === "taken") {
      showToast("Room code is already in use", "error");
      return;
    }
    setLoading(true);
    try {
      const roomRef = ref(rtdb, `chats/${customCode}`);

      // Final sanity check before writing to database
      const snapshot = await get(roomRef);
      if (snapshot.exists()) {
        const data = snapshot.val();
        if (data.expiresAt && data.expiresAt >= Date.now()) {
          showToast("Room code is already taken", "error");
          setCodeStatus("taken");
          setLoading(false);
          return;
        }
      }

      let duration = 60 * 60 * 1000; // default 1h
      const match = roomExpire.match(/^(\d+)([mh])$/);
      if (match) {
        const val = parseInt(match[1]);
        const unit = match[2];
        duration = unit === "m" ? val * 60 * 1000 : val * 60 * 60 * 1000;
      }

      const expiresAt = Date.now() + duration;

      await set(roomRef, {
        createdAt: serverTimestamp(),
        creator: userName,
        name: roomNameInput.trim(),
        expiresAt: expiresAt,
      });

      setState((prev: any) => ({
        ...prev,
        roomId: customCode,
        roomName: roomNameInput.trim(),
        showCreatedModal: true,
      }));

      // Add to history
      const historyObj: any = {
        code: customCode,
        roomName: roomNameInput.trim(),
        type: "room",
        action: "sent",
      };
      if (addToHistory) addToHistory(historyObj);

      showToast("Room created!", "success");
    } catch (err) {
      showToast("Failed to create room", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleJoinRoom = async () => {
    vibrate();
    if (!userName.trim() || !roomId.trim()) {
      showToast("Please enter name and room ID", "error");
      return;
    }
    if (roomId.length !== 5) {
      showToast("Room ID must be 5 digits", "error");
      return;
    }
    setLoading(true);
    try {
      const roomSnapshot = await get(child(ref(rtdb), `chats/${roomId}`));
      if (!roomSnapshot.exists()) {
        showToast("Room not found", "error");
        return;
      }
      const data = roomSnapshot.val();
      if (data.expiresAt && data.expiresAt < Date.now()) {
        showToast("Room has expired", "error");
        return;
      }
      const membersSnapshot = await get(
        child(ref(rtdb), `chats/${roomId}/members`),
      );
      if (membersSnapshot.size >= 50) {
        showToast("Room is full (max 50)", "error");
        return;
      }

      setState((prev: any) => ({
        ...prev,
        inRoom: true,
        roomName: data.name || "",
      }));

      // Add to history
      const historyObj: any = {
        code: roomId,
        roomName: data.name || "Unnamed Room",
        type: "room",
        action: "received",
      };
      if (addToHistory) addToHistory(historyObj);

      showToast("Joined room!", "success");
    } catch (err) {
      showToast("Failed to join room", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleSendMessage = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!newMessage.trim()) return;
    if (isFrozen && !isAdmin) {
      showToast("System is currently frozen", "error");
      return;
    }
    vibrate();
    const msg = newMessage;
    setNewMessage("");
    try {
      const messagesRef = ref(rtdb, `chats/${roomId}/messages`);
      await push(messagesRef, {
        sender: userName,
        senderId: auth.currentUser?.uid || "anon",
        text: msg,
        timestamp: Date.now(),
        isAdmin: isAdmin,
      });
    } catch (err) {
      showToast("Failed to send message", "error");
    }
  };

  const handleReaction = async (msgId: string, reactionId: string) => {
    vibrate();
    const reactionRef = ref(
      rtdb,
      `chats/${roomId}/messages/${msgId}/reactions/${reactionId}`,
    );
    try {
      await runTransaction(reactionRef, (currentValue) => {
        return (currentValue || 0) + 1;
      });
    } catch (err) {
      console.error("Reaction failed:", err);
    }
  };

  if (showCreatedModal) {
    const handleCopyCode = () => {
      vibrate();
      navigator.clipboard.writeText(roomId);
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2000);
      showToast("Room ID copied", "success");
    };

    const handleCopyLink = () => {
      vibrate();
      const directLink = `${window.location.origin}/chat-room?code=${roomId}`;
      navigator.clipboard.writeText(directLink);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
      showToast("Direct link copied", "success");
    };

    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="flex flex-col items-center py-6 space-y-6 relative w-full pt-14 text-center max-w-[440px] mx-auto"
      >
        {/* Back and Action Buttons */}
        <button
          onClick={() => {
            vibrate();
            setState((prev: any) => ({ ...prev, showCreatedModal: false }));
          }}
          className="absolute top-0 left-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
        >
          <ArrowLeft size={10} /> Back
        </button>
        <button
          onClick={() => {
            vibrate();
            setState((prev: any) => ({ ...prev, showCreatedModal: false }));
          }}
          className="absolute top-0 right-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
        >
          New Room <Plus size={10} />
        </button>

        <div className="flex flex-col items-center space-y-1.5">
          <h3 className="text-white/40 text-[10px] font-bold uppercase tracking-[0.2em]">
            Your Room Code
          </h3>
          <div className="h-0.5 w-6 bg-white/10 rounded-full" />
        </div>

        {/* 5 digit boxes */}
        <div className="flex items-center justify-center gap-2.5">
          {roomId.split("").map((digit: string, i: number) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, scale: 0.5, rotate: -15, y: 20 }}
              animate={{ opacity: 1, scale: 1, rotate: 0, y: 0 }}
              transition={{
                type: "spring",
                stiffness: 400,
                damping: 15,
                delay: i * 0.08 + 0.2,
              }}
              whileHover={{ scale: 1.05, y: -5, transition: { duration: 0.2 } }}
              className="w-12 h-16 sm:w-14 sm:h-20 bg-white/[0.04] border-white/10 text-white border rounded-[28px] flex items-center justify-center text-3xl sm:text-4xl font-black font-sans relative group cursor-default shadow-lg"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-white/[0.05] to-transparent rounded-[28px] pointer-events-none" />
              <span className="relative z-10 tracking-tight drop-shadow-sm">
                {digit}
              </span>
              <div className="absolute inset-0 rounded-[28px] border border-white/0 group-hover:border-white/20 transition-all duration-300" />
            </motion.div>
          ))}
        </div>

        <div className="flex flex-col items-center space-y-3 w-full pt-2">
          <p className="text-white/30 text-[10px] font-medium tracking-wide leading-relaxed px-4">
            Share this 5-digit code or direct link with others to start chatting.
          </p>
        </div>

        {/* Copy Buttons */}
        <div className="flex gap-3 w-full pt-2">
          <button
            onClick={handleCopyLink}
            className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
              copiedLink ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"
            }`}
          >
            {copiedLink ? <Check size={16} /> : <Link size={16} />}
            {copiedLink ? "Copied!" : "Copy Link"}
          </button>
          <button
            onClick={handleCopyCode}
            className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
              copiedCode ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"
            }`}
          >
            {copiedCode ? <Check size={16} /> : <Copy size={16} />}
            {copiedCode ? "Copied!" : "Copy Code"}
          </button>
        </div>

        {/* QR Section */}
        <div className="flex flex-col space-y-4 w-full pt-1">
          <button
            onClick={() => {
              vibrate();
              setShowQR(true);
            }}
            className="w-full py-3.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/75 rounded-full font-bold text-xs transition-all flex items-center justify-center gap-2 active:scale-98 cursor-pointer"
          >
            <QrCode size={16} />
            Share via QR Code
          </button>

          <button
            onClick={() => {
              vibrate();
              setState((prev: any) => ({
                ...prev,
                showCreatedModal: false,
                inRoom: true,
              }));
            }}
            className="w-full bg-white text-black py-3.5 rounded-full font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-1.5 shadow-[0_0_20px_rgba(255,255,255,0.1)]"
          >
            Enter Room
            <ArrowRight size={16} />
          </button>
        </div>

        {/* Beautiful QR Modal */}
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
                
                <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-white/40">
                  Share Room Link
                </h4>

                <BeautifulQRCode
                  value={`${window.location.origin}/chat-room?code=${roomId}`}
                  size={200}
                  theme="default"
                  onDownloadReady={setDownloadQRFn}
                />

                <div className="flex gap-2 w-full mt-2">
                  <button
                    onClick={() => {
                      vibrate();
                      navigator.clipboard.writeText(`${window.location.origin}/chat-room?code=${roomId}`);
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

  if (inRoom) {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.98 }}
        animate={{ opacity: 1, scale: 1 }}
        className="flex flex-col h-[calc(100dvh-190px)] min-h-[480px] max-h-[820px] sm:h-[calc(100vh-230px)] sm:min-h-[560px] bg-[#000000] border border-white/10 rounded-[28px] overflow-hidden shadow-2xl max-w-[800px] mx-auto w-full"
      >
        {/* Chat Room Header */}
        <div className="bg-[#0a0a0a]/90 backdrop-blur-md px-4 py-3 sm:px-5 sm:py-3.5 flex items-center justify-between border-b border-white/10">
          <div className="flex items-center gap-3">
            <button
              onClick={() => {
                vibrate();
                setState((prev: any) => ({ ...prev, inRoom: false }));
              }}
              className="text-white/40 hover:text-white hover:bg-white/5 p-2 rounded-xl transition-all"
              title="Back"
            >
              <ArrowLeft size={16} />
            </button>
            <div>
              <h3 className="font-bold text-sm flex items-center gap-2 text-white">
                {roomName || `Room: ${roomId}`}
                <span className="text-[9px] bg-white/10 px-2 py-0.5 rounded-full text-white/60 font-semibold border border-white/5">
                  {memberCount}/50
                </span>
              </h3>
              <p className="text-[9px] text-white/40 tracking-wider font-bold">
                Identity: {userName}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* Copyable Room ID badge */}
            <div
              className="flex items-center gap-1.5 bg-white/5 hover:bg-white/10 border border-white/10 px-2.5 py-1.5 rounded-xl group cursor-pointer transition-all"
              onClick={() => {
                vibrate();
                navigator.clipboard.writeText(roomId);
                showToast("Room ID copied", "success");
              }}
              title="Copy Room ID"
            >
              <span className="text-[10px] font-sans font-bold text-white/50 group-hover:text-white transition-colors">
                {roomId}
              </span>
              <Copy
                size={11}
                className="text-white/30 group-hover:text-white transition-colors"
              />
            </div>
            
            {/* Countdown timer */}
            <div className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border text-[10px] font-sans font-bold tracking-tight ${
              isCriticalTime ? "text-red-400 bg-red-500/10 border-red-500/20" : "text-white/60 bg-white/5 border-white/10"
            }`}>
              <Clock size={11} />
              <span>{timeLeft}</span>
            </div>

            {/* Exit Room button */}
            <button
              onClick={() => {
                vibrate();
                setState((prev: any) => ({ ...prev, inRoom: false }));
              }}
              className="text-red-400/60 hover:text-red-400 hover:bg-red-500/10 p-2 rounded-xl transition-all"
              title="Leave Room"
            >
              <LogOut size={15} />
            </button>
          </div>
        </div>

        {/* Message Stream */}
        <div
          ref={scrollRef}
          className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar bg-[radial-gradient(circle_at_center,rgba(255,255,255,0.02)_0%,transparent_100%)]"
        >
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-white/20 gap-2">
              <MessageSquare size={36} strokeWidth={1.5} className="opacity-40" />
              <p className="text-[10px] font-bold uppercase tracking-widest text-white/30">
                No messages yet
              </p>
            </div>
          ) : (
            messages.map((msg) => (
              <motion.div
                initial={{ opacity: 0, y: 10, scale: 0.98 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                key={msg.id}
                className={`flex flex-col ${msg.sender === userName ? "items-end" : "items-start"} group/msg w-full`}
              >
                {/* Message Meta Info */}
                <div className="flex items-center gap-2 mb-1 px-1">
                  <span
                    className={`text-[9px] font-bold tracking-tight ${msg.isAdmin ? "text-yellow-500" : "text-white/40"}`}
                  >
                    {msg.isAdmin && <Zap size={10} className="inline mr-1" />}
                    {msg.sender}
                  </span>
                  <span className="text-[8px] text-white/30">
                    {new Date(msg.timestamp).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                  {adminRoomId && (
                    <button
                      onClick={() => handleDeleteMessage(msg.id)}
                      className="opacity-0 group-hover/msg:opacity-100 text-red-500 hover:text-red-400 transition-all p-0.5"
                    >
                      <Trash2 size={10} />
                    </button>
                  )}
                </div>

                {/* Message Bubble + Reaction Trigger */}
                <div
                  className="relative group/react max-w-[85%] cursor-pointer"
                  onClick={(e) => {
                    e.stopPropagation();
                    vibrate();
                    setActiveReactionMsgId(activeReactionMsgId === msg.id ? null : msg.id);
                  }}
                >
                  <div
                    className={`px-3.5 py-2 rounded-2xl text-[13px] sm:text-sm shadow-md whitespace-pre-wrap break-words transition-all ${
                      msg.isAdmin
                        ? "bg-yellow-500 text-black font-bold shadow-[0_0_15px_rgba(234,179,8,0.2)]"
                        : msg.sender === userName
                          ? "bg-white text-black rounded-tr-none font-medium hover:bg-zinc-100"
                          : "bg-white/[0.04] text-white rounded-tl-none border border-white/5 hover:bg-white/[0.06] hover:border-white/10"
                    }`}
                  >
                    {msg.text}
                  </div>

                  {/* Reaction Picker Overlay */}
                  <AnimatePresence>
                    {activeReactionMsgId === msg.id && (
                      <>
                        {/* Click Shield to close the picker when tapping outside */}
                        <div
                          className="fixed inset-0 z-40 bg-transparent"
                          onClick={(e) => {
                            e.stopPropagation();
                            setActiveReactionMsgId(null);
                          }}
                        />
                        <motion.div
                          initial={{ opacity: 0, scale: 0.9, y: msg.sender === userName ? 5 : -5 }}
                          animate={{ opacity: 1, scale: 1, y: 0 }}
                          exit={{ opacity: 0, scale: 0.9, y: msg.sender === userName ? 5 : -5 }}
                          className={`absolute bottom-full mb-2 bg-zinc-950/95 border border-white/10 backdrop-blur-xl rounded-2xl p-1.5 flex gap-1 shadow-2xl z-50 ${msg.sender === userName ? "right-0" : "left-0"}`}
                        >
                          {REACTIONS.map((r) => {
                            const Icon = r.icon;
                            return (
                              <motion.button
                                key={r.id}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleReaction(msg.id, r.id);
                                  setActiveReactionMsgId(null);
                                }}
                                whileHover={{ scale: 1.3, y: -4 }}
                                whileTap={{ scale: 0.9 }}
                                className="w-7.5 h-7.5 flex items-center justify-center hover:bg-white/10 rounded-xl transition-colors"
                                title={r.label}
                              >
                                <Icon size={14} className={r.color} />
                              </motion.button>
                            );
                          })}
                        </motion.div>
                      </>
                    )}
                  </AnimatePresence>
                </div>

                {/* Reaction Badges List */}
                {msg.reactions && (
                  <div
                    className={`flex flex-wrap gap-1 mt-1.5 ${msg.sender === userName ? "justify-end" : "justify-start"}`}
                  >
                    {Object.entries(msg.reactions).map(([id, count]: any) => {
                      const reaction = REACTIONS.find((r) => r.id === id);
                      if (!reaction) return null;
                      const Icon = reaction.icon;
                      return (
                        <div
                          key={id}
                          className="flex items-center gap-1 bg-white/5 border border-white/10 rounded-full px-2 py-0.5 text-[9px] font-bold animate-in fade-in zoom-in duration-300"
                        >
                          <Icon size={9} className={reaction.color} />
                          <span className="text-white/60">{count}</span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </motion.div>
            ))
          )}
        </div>

        {/* Input Bar Form */}
        <form
          onSubmit={handleSendMessage}
          className="p-3 bg-[#0a0a0a] border-t border-white/10 flex items-center justify-center"
        >
          <div className="relative flex items-center w-full max-w-[700px]">
            <input
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              placeholder="Type a message..."
              className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-4 pr-12 py-3 text-sm focus:outline-none focus:border-white/25 focus:bg-white/[0.06] transition-all placeholder:text-white/25 text-white"
            />
            <button
              type="submit"
              disabled={!newMessage.trim()}
              className="absolute right-1.5 w-9 h-9 bg-white text-black disabled:bg-white/5 disabled:text-white/20 rounded-xl flex items-center justify-center hover:scale-105 active:scale-95 transition-all group"
            >
              <ArrowUp
                size={18}
                strokeWidth={2.5}
                className="group-hover:-translate-y-0.5 transition-transform"
              />
            </button>
          </div>
        </form>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-[900px] mx-auto px-4"
    >
      <div className="space-y-10">
        {/* User Identity Header block */}
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

        {/* Two Columns Split Grid */}
        <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-10 md:gap-0 relative">
          {/* Vertical Separator Line */}
          <div className="hidden md:block absolute left-1/2 top-0 bottom-0 w-px bg-white/10 -translate-x-1/2 origin-top pointer-events-none" />

          {/* Left Column: Join Room */}
          <div className="space-y-6 md:pr-12 text-left flex flex-col justify-between">
            <div className="space-y-2">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-white/60">
                  <LogIn size={18} />
                </div>
                <div>
                  <h3 className="text-xl font-bold tracking-tight text-white">Join Room</h3>
                  <p className="text-xs text-white/40">Access an existing room</p>
                </div>
              </div>
              <p className="text-[11px] text-white/30 leading-relaxed pt-1">
                Enter the 5-digit room ID shared by your friend to join their active session.
              </p>
            </div>

            <div className="space-y-4 pt-2">
              <div className="space-y-2">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Room Access Code
                </label>
                <div className="relative">
                  <Hash className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={16} />
                  <input
                    value={roomId}
                    onChange={(e) =>
                      setState((prev: any) => ({
                        ...prev,
                        roomId: e.target.value.replace(/\D/g, "").slice(0, 5),
                      }))
                    }
                    placeholder="5-digit Room ID"
                    className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-10 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold tracking-widest font-sans"
                  />
                </div>
              </div>

              <button
                onClick={handleJoinRoom}
                disabled={loading}
                className="w-full bg-white text-black py-3 rounded-2xl font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 shadow-md shadow-white/5 flex items-center justify-center gap-1.5"
              >
                {loading ? "Joining..." : "Join Chat Room"}
                <ArrowRight size={14} />
              </button>
            </div>
          </div>

          {/* Right Column: Create Room */}
          <div className="space-y-6 md:pl-12 text-left border-t border-white/10 pt-8 md:border-t-0 md:pt-0">
            <div className="space-y-2">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-white/60">
                  <Plus size={18} />
                </div>
                <div>
                  <h3 className="text-xl font-bold tracking-tight text-white">Create Room</h3>
                  <p className="text-xs text-white/40">Start a new chat session</p>
                </div>
              </div>
              <p className="text-[11px] text-white/30 leading-relaxed pt-1">
                Launch a secure, private room with customized expiration settings.
              </p>
            </div>

            <div className="space-y-4">
              <div className="space-y-2">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Room Name
                </label>
                <input
                  value={roomNameInput}
                  onChange={(e) =>
                    setState((prev: any) => ({
                      ...prev,
                      roomNameInput: e.target.value,
                    }))
                  }
                  placeholder="Enter room name..."
                  className="w-full bg-white/[0.04] border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold"
                />
              </div>

              <div className="space-y-2">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Room Code (5 digits)
                </label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Hash className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={16} />
                    <input
                      value={customCode}
                      onChange={(e) => handleCustomCodeChange(e.target.value)}
                      placeholder="Enter 5-digit code..."
                      className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-10 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold tracking-widest font-sans"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={generateRandomCustomCode}
                    className="px-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-white/60 hover:text-white transition-all flex items-center justify-center cursor-pointer active:scale-95"
                    title="Randomize Code"
                  >
                    <Dices size={16} />
                  </button>
                </div>
                {/* Real-time Status */}
                {customCode.length === 5 && (
                  <p
                    className={`text-[10px] font-bold ml-3 flex items-center gap-1.5 transition-all ${
                      checkingCode
                        ? "text-white/40 animate-pulse"
                        : codeStatus === "available"
                        ? "text-emerald-400"
                        : "text-rose-400"
                    }`}
                  >
                    {checkingCode ? (
                      <>
                        <Loader2 className="animate-spin" size={12} /> Checking availability...
                      </>
                    ) : codeStatus === "available" ? (
                      <>
                        <Check size={12} className="text-emerald-400" /> Room code is available
                      </>
                    ) : (
                      <>
                        <X size={12} className="text-rose-400" /> Room code is already in use
                      </>
                    )}
                  </p>
                )}
                {customCode.length > 0 && customCode.length < 5 && (
                  <p className="text-[10px] text-white/30 ml-3 font-semibold">
                    Code must be exactly 5 digits
                  </p>
                )}
              </div>

              <div className="space-y-2.5">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Expiration Duration
                </label>
                <div className="grid grid-cols-3 gap-1.5">
                  {["10m", "30m", "1h", "6h", "24h"].map((opt) => (
                    <button
                      key={opt}
                      onClick={() => {
                        vibrate();
                        setState((prev: any) => ({ ...prev, roomExpire: opt }));
                      }}
                      className={`py-2 text-[10px] font-bold rounded-xl border transition-all ${
                        roomExpire === opt
                          ? "bg-white text-black border-white shadow-md shadow-white/5"
                          : "bg-white/[0.04] text-white/40 border-white/5 hover:border-white/10 hover:text-white/60"
                      }`}
                    >
                      {opt}
                    </button>
                  ))}
                  <input
                    type="text"
                    placeholder="Custom"
                    className="bg-white/[0.04] border border-white/5 rounded-xl px-1 py-2 text-[10px] text-center focus:outline-none focus:border-white/10 focus:bg-white/[0.06] transition-all placeholder:text-white/20 font-bold"
                    onChange={(e) =>
                      setState((prev: any) => ({
                        ...prev,
                        roomExpire: e.target.value,
                      }))
                    }
                  />
                </div>
              </div>

              <button
                onClick={handleCreateRoom}
                disabled={loading}
                className="w-full bg-white text-black py-3 rounded-2xl font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 shadow-md shadow-white/5 flex items-center justify-center gap-1.5"
              >
                {loading ? "Creating..." : "Launch Room"}
                <ArrowRight size={14} />
              </button>
            </div>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

function AdminPanel({
  showToast,
  addToHistory,
  onExit,
  chatState,
  setChatState,
  boardState,
  setBoardState,
  globalStats,
}: any) {
  const [rooms, setRooms] = useState<any[]>([]);
  const [boards, setBoards] = useState<any[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<string | null>(null);
  const [selectedBoard, setSelectedBoard] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [deleteBoardConfirmId, setDeleteBoardConfirmId] = useState<
    string | null
  >(null);
  const [isClearingAll, setIsClearingAll] = useState(false);
  const [isNuking, setIsNuking] = useState(false);
  const [isNukingBoards, setIsNukingBoards] = useState(false);
  const [onlineCount, setOnlineCount] = useState(0);

  // New Power Features State
  const [broadcastMsg, setBroadcastMsg] = useState("");
  const [broadcastTitle, setBroadcastTitle] = useState("");
  const [broadcastIcon, setBroadcastIcon] = useState("Zap");
  const [broadcastTheme, setBroadcastTheme] = useState("dark");
  const [broadcastDuration, setBroadcastDuration] = useState("0"); // 0 = permanent
  const [systemNotice, setSystemNotice] = useState(globalStats.notice || "");
  const [noticeTheme, setNoticeTheme] = useState(
    globalStats.noticeTheme || "blue",
  );
  const [noticeDuration, setNoticeDuration] = useState("0"); // 0 = permanent
  const [isEditingLikes, setIsEditingLikes] = useState(false);
  const [newLikes, setNewLikes] = useState(globalStats.likes || 0);
  const [newAdminPass, setNewAdminPass] = useState(
    globalStats.passwords?.admin || "",
  );
  const [newSecretPass, setNewSecretPass] = useState(
    globalStats.passwords?.secret || "",
  );

  // Sync local inputs with global stats when they change remotely
  useEffect(() => {
    if (!isEditingLikes) setNewLikes(globalStats.likes);
  }, [globalStats.likes]);

  useEffect(() => {
    if (globalStats.passwords) {
      setNewAdminPass(globalStats.passwords.admin);
      setNewSecretPass(globalStats.passwords.secret);
    }
  }, [globalStats.passwords]);

  useEffect(() => {
    setSystemNotice(globalStats.notice);
    setNoticeTheme(globalStats.noticeTheme);
  }, [globalStats.notice, globalStats.noticeTheme]);

  const NOTICE_THEMES = [
    { id: "blue", color: "bg-blue-600", label: "Blue" },
    { id: "red", color: "bg-red-600", label: "Red" },
    { id: "green", color: "bg-green-600", label: "Green" },
    { id: "yellow", color: "bg-yellow-600", label: "Yellow" },
    { id: "purple", color: "bg-purple-600", label: "Purple" },
    { id: "pink", color: "bg-pink-600", label: "Pink" },
    { id: "orange", color: "bg-orange-600", label: "Orange" },
    { id: "dark", color: "bg-zinc-800", label: "Dark" },
  ];

  const BROADCAST_ICONS = [
    { id: "Zap", icon: Zap },
    { id: "Bell", icon: Bell },
    { id: "ShieldAlert", icon: ShieldAlert },
    { id: "Flame", icon: Flame },
    { id: "Star", icon: Star },
    { id: "Radio", icon: Radio },
  ];

  useEffect(() => {
    const roomsRef = ref(rtdb, "chats");
    const boardsRef = ref(rtdb, "boards");
    const dropsRef = ref(rtdb, "drops");
    const presenceRef = ref(rtdb, "presence");

    const unsubRooms = onValue(
      roomsRef,
      (snapshot) => {
        const data = snapshot.val();
        if (data) {
          const list = Object.entries(data)
            .filter(([id, val]) => val !== null && typeof val === "object")
            .map(([id, val]: any) => ({ id, ...val }));
          setRooms(list);
        } else {
          setRooms([]);
        }
        setLoading(false);
      },
      (error) => {
        console.error("AdminPanel fetch error:", error);
        setLoading(false);
        showToast("Failed to fetch rooms: " + error.message, "error");
      },
    );

    const unsubBoards = onValue(
      boardsRef,
      (snapshot) => {
        const data = snapshot.val();
        if (data) {
          const list = Object.entries(data)
            .filter(([id, val]) => val !== null && typeof val === "object")
            .map(([id, val]: any) => ({ id, ...val }));
          setBoards(list);
        } else {
          setBoards([]);
        }
      },
      (error) => {
        console.error("AdminPanel fetch boards error:", error);
        showToast("Failed to fetch boards: " + error.message, "error");
      },
    );

    const unsubDrops = onValue(dropsRef, (snapshot) => {
      const data = snapshot.val();
      if (data) {
        // Stats handled via props
      }
    });

    const unsubPresence = onValue(presenceRef, (snapshot) => {
      const data = snapshot.val();
      if (data) {
        setOnlineCount(Object.keys(data).length);
      } else {
        setOnlineCount(0);
      }
    });

    return () => {
      off(roomsRef);
      off(dropsRef);
      off(presenceRef);
    };
  }, []);

  const handleDeleteRoom = async (roomId: string) => {
    if (deleteConfirmId !== roomId) {
      setDeleteConfirmId(roomId);
      setTimeout(() => setDeleteConfirmId(null), 3000);
      return;
    }
    try {
      await remove(ref(rtdb, `chats/${roomId}`));
      showToast("Room deleted", "success");
      setDeleteConfirmId(null);
    } catch (err) {
      showToast("Failed to delete room", "error");
    }
  };

  const handleDeleteBoard = async (boardId: string) => {
    if (deleteBoardConfirmId !== boardId) {
      setDeleteBoardConfirmId(boardId);
      setTimeout(() => setDeleteBoardConfirmId(null), 3000);
      return;
    }
    try {
      await remove(ref(rtdb, `boards/${boardId}`));
      await remove(ref(rtdb, `board_paths/${boardId}`));
      showToast("Board deleted", "success");
      setDeleteBoardConfirmId(null);
    } catch (err) {
      showToast("Failed to delete board", "error");
    }
  };

  const handleExtendRoom = async (roomId: string) => {
    const room = rooms.find((r) => r.id === roomId);
    if (!room) return;
    const newExpiry = (room.expiresAt || Date.now()) + 24 * 60 * 60 * 1000;
    try {
      await update(ref(rtdb, `chats/${roomId}`), { expiresAt: newExpiry });
      showToast("Room extended by 24h", "success");
    } catch (err) {
      showToast("Failed to extend room", "error");
    }
  };

  const handleExtendBoard = async (boardId: string) => {
    const board = boards.find((b) => b.id === boardId);
    if (!board) return;
    const newExpiry = (board.expiresAt || Date.now()) + 24 * 60 * 60 * 1000;
    try {
      await update(ref(rtdb, `boards/${boardId}`), { expiresAt: newExpiry });
      showToast("Board extended by 24h", "success");
    } catch (err) {
      showToast("Failed to extend board", "error");
    }
  };

  const handleGlobalBroadcast = async () => {
    if (!broadcastMsg.trim() && !broadcastTitle.trim()) return;
    showToast("Broadcasting message...", "info");
    try {
      const expiresAt =
        broadcastDuration === "0"
          ? 0
          : Date.now() + parseInt(broadcastDuration) * 60 * 1000;
      await set(ref(rtdb, "stats/broadcast"), {
        title: broadcastTitle || "SYSTEM ANNOUNCEMENT",
        message: broadcastMsg,
        icon: broadcastIcon,
        theme: broadcastTheme,
        timestamp: Date.now(),
        expiresAt: expiresAt,
        id: Math.random().toString(36).substr(2, 9),
      });
      showToast("Global broadcast sent", "success");
      setBroadcastMsg("");
      setBroadcastTitle("");
    } catch (err) {
      showToast("Broadcast failed", "error");
    }
  };

  const handleClearBroadcast = async () => {
    try {
      await remove(ref(rtdb, "stats/broadcast"));
      showToast("Broadcast disabled", "success");
    } catch (err) {
      showToast("Failed", "error");
    }
  };

  const handleClearAllGlobalComms = async () => {
    try {
      await update(ref(rtdb, "stats"), {
        notice: "",
        noticeExpiresAt: 0,
        broadcast: null,
      });
      showToast("All global comms cleared", "success");
    } catch (err) {
      showToast("Failed", "error");
    }
  };

  const handleUpdateNotice = async () => {
    try {
      const expiresAt =
        noticeDuration === "0"
          ? 0
          : Date.now() + parseInt(noticeDuration) * 60 * 1000;
      await update(ref(rtdb, "stats"), {
        notice: systemNotice,
        noticeTheme: noticeTheme,
        noticeExpiresAt: expiresAt,
      });
      showToast("System notice updated", "success");
    } catch (err) {
      showToast("Update failed", "error");
    }
  };

  const handleClearNotice = async () => {
    try {
      await update(ref(rtdb, "stats"), { notice: "", noticeExpiresAt: 0 });
      showToast("Notice disabled", "success");
    } catch (err) {
      showToast("Failed", "error");
    }
  };

  const handleToggleFreeze = async () => {
    try {
      await update(ref(rtdb, "stats"), { isFrozen: !globalStats.isFrozen });
      showToast(
        globalStats.isFrozen ? "System Unfrozen" : "System Frozen",
        "success",
      );
    } catch (err) {
      showToast("Failed to toggle freeze", "error");
    }
  };

  const handleUpdateLikes = async () => {
    try {
      await update(ref(rtdb, "stats"), { likes: newLikes });
      showToast("Global likes updated", "success");
      setIsEditingLikes(false);
    } catch (err) {
      showToast("Failed to update likes", "error");
    }
  };

  const handleUpdatePasswords = async () => {
    if (!newAdminPass.trim() || !newSecretPass.trim()) {
      showToast("Passwords cannot be empty", "error");
      return;
    }
    try {
      await update(ref(rtdb, "stats/passwords"), {
        admin: newAdminPass.trim(),
        secret: newSecretPass.trim(),
      });
      showToast("Security settings updated", "success");
    } catch (err) {
      showToast("Failed to update passwords", "error");
    }
  };

  const handleClearAllExpired = async (expiredRooms: any[]) => {
    if (!isClearingAll) {
      setIsClearingAll(true);
      setTimeout(() => setIsClearingAll(false), 3000);
      return;
    }
    showToast("Cleaning up...", "info");
    try {
      const promises = expiredRooms.map((room) =>
        remove(ref(rtdb, `chats/${room.id}`)),
      );
      await Promise.all(promises);
      showToast("All expired rooms deleted", "success");
      setIsClearingAll(false);
    } catch (err) {
      showToast("Failed to clear rooms", "error");
    }
  };

  const handleNukeAll = async () => {
    if (!isNuking) {
      setIsNuking(true);
      setTimeout(() => setIsNuking(false), 3000);
      return;
    }
    showToast("NUKING ALL ROOMS...", "error");
    try {
      await remove(ref(rtdb, "chats"));
      showToast("ALL ROOMS WIPED", "success");
      setIsNuking(false);
    } catch (err) {
      showToast("Nuke failed", "error");
    }
  };

  const handleNukeAllBoards = async () => {
    if (!isNukingBoards) {
      setIsNukingBoards(true);
      setTimeout(() => setIsNukingBoards(false), 3000);
      return;
    }
    showToast("NUKING ALL BOARDS...", "error");
    try {
      await remove(ref(rtdb, "boards"));
      await remove(ref(rtdb, "board_paths"));
      showToast("ALL BOARDS WIPED", "success");
      setIsNukingBoards(false);
    } catch (err) {
      showToast("Nuke failed", "error");
    }
  };

  const activeRooms = rooms.filter((r) => r.expiresAt > Date.now());
  const expiredRooms = rooms.filter((r) => r.expiresAt <= Date.now());
  const activeBoards = boards.filter((b) => b.expiresAt > Date.now());
  const expiredBoards = boards.filter((b) => b.expiresAt <= Date.now());

  if (selectedRoom) {
    return (
      <div className="space-y-4">
        <button
          onClick={() => setSelectedRoom(null)}
          className="flex items-center gap-2 text-white/40 hover:text-white transition-colors text-xs font-bold"
        >
          <ArrowLeft size={14} /> Back to Dashboard
        </button>
        <Chat
          showToast={showToast}
          addToHistory={addToHistory}
          adminRoomId={selectedRoom}
          state={{
            ...chatState,
            roomId: selectedRoom,
            inRoom: true,
            userName: "Admin",
          }}
          setState={setChatState}
          isAdmin={true}
          isFrozen={globalStats.isFrozen}
        />
      </div>
    );
  }

  if (selectedBoard) {
    return (
      <div className="space-y-4">
        <button
          onClick={() => setSelectedBoard(null)}
          className="flex items-center gap-2 text-white/40 hover:text-white transition-colors text-xs font-bold"
        >
          <ArrowLeft size={14} /> Back to Dashboard
        </button>
        <Board
          showToast={showToast}
          addToHistory={addToHistory}
          state={{
            ...boardState,
            boardId: selectedBoard,
            inBoard: true,
            userName: "Admin",
          }}
          setState={setBoardState}
        />
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6 pb-10"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-2 h-2 rounded-full bg-white animate-pulse shadow-[0_0_10px_rgba(255,255,255,0.5)]" />
          <h3 className="font-bold text-sm text-white">Admin Dashboard</h3>
        </div>
        <button
          onClick={onExit}
          className="text-white/40 hover:text-white text-xs font-bold flex items-center gap-2 transition-colors"
        >
          <LogOut size={14} /> Exit Admin
        </button>
      </div>

      {/* Global Controls */}
      <div className="space-y-4">
        <div className="grid grid-cols-3 gap-3">
          <button
            onClick={handleToggleFreeze}
            className={`p-4 rounded-3xl border flex flex-col items-center gap-2 transition-all ${
              globalStats.isFrozen
                ? "bg-white text-black border-white"
                : "bg-white/5 border-white/10 text-white/60 hover:bg-white/10 hover:border-white/30"
            }`}
          >
            {globalStats.isFrozen ? (
              <Snowflake size={20} className="animate-pulse" />
            ) : (
              <ShieldCheck size={20} />
            )}
            <span className="text-[10px] font-bold">
              {globalStats.isFrozen ? "Frozen" : "Freeze"}
            </span>
          </button>

          <button
            onClick={handleClearAllGlobalComms}
            className="p-4 rounded-3xl border bg-white/5 border-white/10 text-white/60 hover:bg-white/10 hover:border-white/30 flex flex-col items-center gap-2 transition-all"
          >
            <BellOff size={20} />
            <span className="text-[10px] font-bold">Clear All</span>
          </button>

          <button
            onClick={handleNukeAll}
            className={`p-4 rounded-3xl border flex flex-col items-center gap-2 transition-all ${
              isNuking
                ? "bg-white text-black border-white animate-pulse scale-105"
                : "bg-white/5 border-white/10 text-white/60 hover:bg-white/10 hover:border-white/30"
            }`}
          >
            <Flame size={20} />
            <span className="text-[10px] font-bold">
              {isNuking ? "Confirm?" : "Nuke Rooms"}
            </span>
          </button>

          <button
            onClick={handleNukeAllBoards}
            className={`p-4 rounded-3xl border flex flex-col items-center gap-2 transition-all ${
              isNukingBoards
                ? "bg-white text-black border-white animate-pulse scale-105"
                : "bg-white/5 border-white/10 text-white/60 hover:bg-white/10 hover:border-white/30"
            }`}
          >
            <Palette size={20} />
            <span className="text-[10px] font-bold">
              {isNukingBoards ? "Confirm?" : "Nuke Boards"}
            </span>
          </button>
        </div>

        <div className="p-5 bg-white/5 border border-white/10 rounded-3xl space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-white/60">
              <Radio size={14} className="animate-pulse" />
              <span className="text-[10px] font-bold">
                Global Broadcast (Modal)
              </span>
            </div>
            <button
              onClick={handleClearBroadcast}
              className="text-[10px] font-bold text-white/40 hover:text-white transition-colors"
            >
              Turn Off
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-3">
              <input
                value={broadcastTitle}
                onChange={(e) => setBroadcastTitle(e.target.value)}
                placeholder="Announcement title..."
                className="w-full bg-black/40 border border-white/10 rounded-2xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-white/30 transition-all"
              />
              <textarea
                value={broadcastMsg}
                onChange={(e) => setBroadcastMsg(e.target.value)}
                placeholder="Announcement message (optional)..."
                className="w-full bg-black/40 border border-white/10 rounded-2xl px-4 py-3 text-xs text-white focus:outline-none focus:border-white/30 transition-all h-24 resize-none"
              />
            </div>

            <div className="flex flex-col justify-between space-y-3">
              <div className="space-y-3">
                <div className="flex flex-wrap gap-1.5">
                  {BROADCAST_ICONS.map((i) => {
                    const Icon = i.icon;
                    return (
                      <button
                        key={i.id}
                        onClick={() => setBroadcastIcon(i.id)}
                        className={`w-7 h-7 rounded-full flex items-center justify-center transition-all ${broadcastIcon === i.id ? "bg-white text-black" : "bg-white/5 text-white/40 hover:bg-white/10"}`}
                      >
                        <Icon size={12} />
                      </button>
                    );
                  })}
                </div>

                <div className="flex flex-wrap gap-1.5">
                  {NOTICE_THEMES.map((t) => (
                    <button
                      key={t.id}
                      onClick={() => setBroadcastTheme(t.id)}
                      className={`w-5 h-5 rounded-full border-2 transition-all ${t.color} ${broadcastTheme === t.id ? "border-white scale-110" : "border-transparent opacity-40 hover:opacity-100"}`}
                      title={t.label}
                    />
                  ))}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <select
                  value={broadcastDuration}
                  onChange={(e) => setBroadcastDuration(e.target.value)}
                  className="flex-1 bg-black border border-white/10 rounded-full px-3 py-2 text-[9px] font-bold text-white/60 focus:outline-none focus:border-white/30 transition-all"
                >
                  <option value="0">Permanent</option>
                  <option value="1">1 Min</option>
                  <option value="5">5 Mins</option>
                  <option value="15">15 Mins</option>
                  <option value="30">30 Mins</option>
                  <option value="60">1 Hour</option>
                </select>
                <button
                  onClick={handleGlobalBroadcast}
                  className="flex-1 py-2 bg-white text-black rounded-full text-[10px] font-bold hover:scale-105 active:scale-95 transition-all"
                >
                  Broadcast
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="p-5 bg-white/5 border border-white/10 rounded-3xl space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-white/60">
              <Bell size={14} />
              <span className="text-[10px] font-bold">Scrolling Notice</span>
            </div>
            <button
              onClick={handleClearNotice}
              className="text-[10px] font-bold text-white/40 hover:text-white transition-colors"
            >
              Turn Off
            </button>
          </div>

          <div className="space-y-4">
            <input
              value={systemNotice}
              onChange={(e) => setSystemNotice(e.target.value)}
              placeholder="Notice content..."
              className="w-full bg-black/40 border border-white/10 rounded-2xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-white/30 transition-all"
            />

            <div className="flex flex-wrap gap-2">
              {NOTICE_THEMES.map((t) => (
                <button
                  key={t.id}
                  onClick={() => setNoticeTheme(t.id)}
                  className={`w-6 h-6 rounded-full border-2 transition-all ${t.color} ${noticeTheme === t.id ? "border-white scale-125" : "border-transparent opacity-50 hover:opacity-100"}`}
                  title={t.label}
                />
              ))}
            </div>

            <div className="flex items-center gap-3">
              <select
                value={noticeDuration}
                onChange={(e) => setNoticeDuration(e.target.value)}
                className="bg-black border border-white/10 rounded-full px-4 py-2 text-[10px] font-bold text-white/60 focus:outline-none focus:border-white/30 transition-all"
              >
                <option value="0">Permanent</option>
                <option value="1">1 Minute</option>
                <option value="5">5 Minutes</option>
                <option value="15">15 Minutes</option>
                <option value="30">30 Minutes</option>
                <option value="60">1 Hour</option>
                <option value="1440">1 Day</option>
              </select>
              <button
                onClick={handleUpdateNotice}
                className="flex-1 py-2 bg-white text-black rounded-full text-[10px] font-bold hover:bg-white/90 transition-colors"
              >
                Update
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 gap-3">
        <div className="p-4 bg-white/5 border border-white/10 rounded-3xl space-y-1 relative group overflow-hidden">
          <p className="text-[10px] text-white/40 font-bold">Total Likes</p>
          {isEditingLikes ? (
            <div className="flex items-center gap-2">
              <input
                type="number"
                value={newLikes}
                onChange={(e) => setNewLikes(parseInt(e.target.value))}
                className="w-full bg-black/40 border border-white/20 rounded-xl px-2 py-1 text-sm font-sans text-white focus:outline-none focus:border-white/40"
              />
              <button
                onClick={handleUpdateLikes}
                className="text-white hover:text-white/80 transition-colors"
              >
                <Check size={16} />
              </button>
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <p className="text-2xl font-sans font-bold text-white">
                {globalStats.likes}
              </p>
              <button
                onClick={() => setIsEditingLikes(true)}
                className="opacity-0 group-hover:opacity-100 text-white/20 hover:text-white transition-all"
              >
                <Edit3 size={14} />
              </button>
            </div>
          )}
        </div>
        <div className="p-4 bg-white/5 border border-white/10 rounded-3xl space-y-1">
          <p className="text-[10px] text-white/40 font-bold">Online Users</p>
          <div className="flex items-center gap-2">
            <p className="text-2xl font-sans font-bold text-white">
              {onlineCount}
            </p>
            <Users size={14} className="text-white/20" />
          </div>
        </div>
      </div>

      {/* App Branding Settings */}
      <div className="p-5 bg-white/5 border border-white/10 rounded-3xl space-y-4">
        <div className="flex items-center gap-2 text-white/60">
          <Monitor size={14} />
          <span className="text-[10px] font-bold">App Branding</span>
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3 bg-white/5 rounded-xl border border-white/10">
          <div>
            <h4 className="text-white/80 font-semibold text-xs mb-1">Developer Branding</h4>
            <p className="text-[10px] text-white/40 font-bold">Current: <span className="text-white">{globalStats.developer || "Khanh Minh"}</span></p>
          </div>
          <div className="flex items-center gap-1 bg-black/40 rounded-full p-1 border border-white/10">
            <button 
              onClick={() => update(ref(rtdb, "stats"), { developer: "Khanh Minh" })}
              className={`px-4 py-1.5 rounded-full text-[10px] font-bold transition-all ${(!globalStats.developer || globalStats.developer === "Khanh Minh") ? "bg-white text-black" : "text-white/40 hover:text-white"}`}
            >
              Khanh Minh
            </button>
            <button 
              onClick={() => update(ref(rtdb, "stats"), { developer: "Minh Ngoc" })}
              className={`px-4 py-1.5 rounded-full text-[10px] font-bold transition-all ${globalStats.developer === "Minh Ngoc" ? "bg-white text-black" : "text-white/40 hover:text-white"}`}
            >
              Minh Ngoc
            </button>
          </div>
        </div>
      </div>

      {/* Security Settings */}
      <div className="p-5 bg-white/5 border border-white/10 rounded-3xl space-y-4">
        <div className="flex items-center gap-2 text-white/60">
          <Lock size={14} />
          <span className="text-[10px] font-bold">Security Settings</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-2">
            <p className="text-[9px] font-bold text-white/40 uppercase tracking-widest pl-1">
              Admin Password
            </p>
            <input
              type="text"
              value={newAdminPass}
              onChange={(e) => setNewAdminPass(e.target.value)}
              placeholder="Admin password..."
              className="w-full bg-black/40 border border-white/10 rounded-2xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-white/30 transition-all font-mono"
            />
          </div>
          <div className="space-y-2">
            <p className="text-[9px] font-bold text-white/40 uppercase tracking-widest pl-1">
              Secret Mode Password
            </p>
            <input
              type="text"
              value={newSecretPass}
              onChange={(e) => setNewSecretPass(e.target.value)}
              placeholder="Secret password..."
              className="w-full bg-black/40 border border-white/10 rounded-2xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-white/30 transition-all font-mono"
            />
          </div>
        </div>

        <button
          onClick={handleUpdatePasswords}
          className="w-full py-3 bg-white text-black rounded-full text-[10px] font-bold hover:bg-white/90 transition-colors flex items-center justify-center gap-2"
        >
          <ShieldCheck size={14} /> Update Security Credentials
        </button>
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between px-2">
          <h4 className="text-[11px] font-bold text-white/60">
            Active Boards Control
          </h4>
          <span className="text-[10px] font-sans text-white/20">
            {activeBoards.length} active
          </span>
        </div>

        <div className="space-y-3">
          {loading ? (
            <div className="p-10 text-center">
              <Loader2
                className="animate-spin mx-auto text-white/20"
                size={24}
              />
            </div>
          ) : activeBoards.length === 0 ? (
            <div className="p-10 text-center text-white/20 font-bold text-xs border border-dashed border-white/10 rounded-3xl">
              No active boards
            </div>
          ) : (
            activeBoards.map((board) => (
              <div
                key={board.id}
                className="p-4 bg-white/5 border border-white/10 rounded-3xl flex items-center justify-between group hover:bg-white/10 transition-all"
              >
                <div className="flex-1 min-w-0 pr-4">
                  <h4 className="font-bold text-sm truncate text-white">
                    {board.name || "Unnamed Board"}
                  </h4>
                  <p className="text-[10px] text-white/40 font-sans truncate">
                    {board.creator} •{" "}
                    {new Date(board.expiresAt).toLocaleTimeString()}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleExtendBoard(board.id)}
                    className="w-9 h-9 bg-white/5 hover:bg-white/10 text-white/40 hover:text-white rounded-full flex items-center justify-center transition-all border border-white/10"
                    title="Extend 24h"
                  >
                    <Clock size={16} />
                  </button>
                  <button
                    onClick={() => setSelectedBoard(board.id)}
                    className="w-9 h-9 bg-white/5 hover:bg-white text-white hover:text-black rounded-full flex items-center justify-center transition-all border border-white/10"
                  >
                    <Eye size={16} />
                  </button>
                  <button
                    onClick={() => handleDeleteBoard(board.id)}
                    className={`w-9 h-9 rounded-full flex items-center justify-center transition-all border ${
                      deleteBoardConfirmId === board.id
                        ? "bg-red-500 text-white border-red-500 animate-pulse"
                        : "bg-white/5 hover:bg-red-500/20 text-white/40 hover:text-red-500 border-white/10"
                    }`}
                  >
                    {deleteBoardConfirmId === board.id ? (
                      <Check size={16} />
                    ) : (
                      <Trash2 size={16} />
                    )}
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between px-2">
          <h4 className="text-[11px] font-bold text-white/60">
            Active Rooms Control
          </h4>
          <span className="text-[10px] font-sans text-white/20">
            {activeRooms.length} active
          </span>
        </div>

        <div className="space-y-3">
          {loading ? (
            <div className="p-10 text-center">
              <Loader2
                className="animate-spin mx-auto text-white/20"
                size={24}
              />
            </div>
          ) : activeRooms.length === 0 ? (
            <div className="p-10 text-center text-white/20 font-bold text-xs border border-dashed border-white/10 rounded-3xl">
              No active rooms
            </div>
          ) : (
            activeRooms.map((room) => (
              <div
                key={room.id}
                className="p-4 bg-white/5 border border-white/10 rounded-3xl flex items-center justify-between group hover:bg-white/10 transition-all"
              >
                <div className="flex-1 min-w-0 pr-4">
                  <h4 className="font-bold text-sm truncate text-white">
                    {room.name || "Unnamed Room"}
                  </h4>
                  <p className="text-[10px] text-white/40 font-sans truncate">
                    {room.creator} •{" "}
                    {new Date(room.expiresAt).toLocaleTimeString()}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleExtendRoom(room.id)}
                    className="w-9 h-9 bg-white/5 hover:bg-white/10 text-white/40 hover:text-white rounded-full flex items-center justify-center transition-all border border-white/10"
                    title="Extend 24h"
                  >
                    <Clock size={16} />
                  </button>
                  <button
                    onClick={() => setSelectedRoom(room.id)}
                    className="w-9 h-9 bg-white/5 hover:bg-white text-white hover:text-black rounded-full flex items-center justify-center transition-all border border-white/10"
                  >
                    <Eye size={16} />
                  </button>
                  <button
                    onClick={() => handleDeleteRoom(room.id)}
                    className={`w-9 h-9 rounded-full flex items-center justify-center transition-all border ${
                      deleteConfirmId === room.id
                        ? "bg-red-500 text-white border-red-500 animate-pulse"
                        : "bg-white/5 hover:bg-red-500/20 text-white/40 hover:text-red-500 border-white/10"
                    }`}
                  >
                    {deleteConfirmId === room.id ? (
                      <Check size={16} />
                    ) : (
                      <Trash2 size={16} />
                    )}
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {expiredRooms.length > 0 && (
        <div className="space-y-4 pt-4 border-t border-white/5">
          <div className="flex items-center justify-between px-2">
            <h4 className="text-[11px] font-bold uppercase tracking-widest text-white/30">
              Expired Archives
            </h4>
            <button
              onClick={() => handleClearAllExpired(expiredRooms)}
              className={`text-[10px] font-bold uppercase tracking-widest transition-all px-3 py-1 rounded-full border ${
                isClearingAll
                  ? "bg-red-500 text-white border-red-500 animate-pulse"
                  : "text-red-400/40 hover:text-red-400 border-red-400/20 hover:border-red-400/40"
              }`}
            >
              {isClearingAll ? "Confirm Clear All?" : "Clear All"}
            </button>
          </div>
        </div>
      )}
    </motion.div>
  );
}

function Board({ showToast, addToHistory, state, setState }: any) {
  const {
    boardId,
    boardName,
    boardNameInput,
    boardExpire,
    userName,
    inBoard,
    showCreatedModal,
  } = state;
  const [loading, setLoading] = useState(false);
  const [paths, setPaths] = useState<any[]>([]);
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [showQR, setShowQR] = useState(false);
  const [downloadQRFn, setDownloadQRFn] = useState<(() => void) | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [currentColor, setCurrentColor] = useState("#ffffff");
  const [currentSize, setCurrentSize] = useState(4);
  const [tool, setTool] = useState<"pen" | "eraser">("pen");
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [isClearing, setIsClearing] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const contextRef = useRef<CanvasRenderingContext2D | null>(null);
  const currentPathRef = useRef<{ x: number; y: number }[]>([]);

  const COLORS = [
    "#ffffff",
    "#ff4444",
    "#ff8800",
    "#ffff44",
    "#44ff44",
    "#00ffff",
    "#4444ff",
    "#8844ff",
    "#ff44ff",
    "#ff8888",
    "#88ff88",
    "#8888ff",
    "#c0c0c0",
    "#800000",
    "#808000",
    "#008000",
    "#008080",
    "#000080",
    "#800080",
    "#ffd700",
    "#adff2f",
    "#7fffd4",
    "#da70d6",
    "#f0e68c",
  ];

  // Board setup logic
  const handleCreateBoard = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!boardNameInput.trim() || !userName.trim()) {
      showToast("Please enter board name and your name", "error");
      return;
    }

    setLoading(true);
    const generatedId = Math.floor(10000 + Math.random() * 90000).toString();
    try {
      let duration = 3600000; // 1h
      if (boardExpire === "6h") duration = 6 * 3600000;
      else if (boardExpire === "12h") duration = 12 * 3600000;
      else if (boardExpire === "24h") duration = 24 * 3600000;
      else if (boardExpire.startsWith("custom:")) {
        const hours = parseInt(boardExpire.split(":")[1]);
        duration = hours * 3600000;
      }

      const expiresAt = Date.now() + duration;

      await set(ref(rtdb, `boards/${generatedId}`), {
        name: boardNameInput,
        creator: userName,
        expiresAt,
        createdAt: serverTimestamp(),
      });

      setState((prev: any) => ({
        ...prev,
        boardId: generatedId,
        boardName: boardNameInput,
        showCreatedModal: true,
      }));
      addToHistory({
        code: generatedId,
        boardName: boardNameInput,
        type: "board",
        action: "created",
      });
      showToast("Board created!", "success");
    } catch (err: any) {
      console.error(err);
      if (err.message && err.message.toLowerCase().includes("permission")) {
        handleDatabaseError(err, OperationType.WRITE, `boards/${generatedId}`);
      }
      showToast(sanitizeError(err.message) || "An error occurred", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleJoinBoard = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!boardId.trim() || !userName.trim()) {
      showToast("Please enter board ID and your name", "error");
      return;
    }
    if (boardId.trim().length !== 5 || isNaN(Number(boardId.trim()))) {
      showToast("Board ID must be 5 digits", "error");
      return;
    }

    setLoading(true);
    const cleanId = boardId.trim();
    try {
      const snapshot = await get(child(ref(rtdb), `boards/${cleanId}`));
      if (snapshot.exists()) {
        const data = snapshot.val();
        if (data.expiresAt < Date.now()) {
          showToast("Board has expired", "error");
          return;
        }
        setState((prev: any) => ({
          ...prev,
          boardId: cleanId,
          boardName: data.name,
          inBoard: true,
        }));
        addToHistory({
          code: cleanId,
          boardName: data.name,
          type: "board",
          action: "joined",
        });
        showToast("Joined board!", "success");
      } else {
        showToast("Board not found", "error");
      }
    } catch (err: any) {
      console.error(err);
      if (err.message && err.message.toLowerCase().includes("permission")) {
        handleDatabaseError(err, OperationType.GET, `boards/${cleanId}`);
      }
      showToast(sanitizeError(err.message) || "An error occurred", "error");
    } finally {
      setLoading(false);
    }
  };

  // Canvas logic
  useEffect(() => {
    if (!inBoard || !canvasRef.current) return;

    const canvas = canvasRef.current;
    const parent = canvas.parentElement;
    if (!parent) return;

    const handleResize = () => {
      const parent = canvas.parentElement;
      if (!parent) return;

      // Set canvas size
      canvas.width = parent.clientWidth * 2;
      canvas.height = parent.clientHeight * 2;
      canvas.style.width = `${parent.clientWidth}px`;
      canvas.style.height = `${parent.clientHeight}px`;

      const context = canvas.getContext("2d");
      if (context) {
        context.scale(2, 2);
        context.lineCap = "round";
        context.lineJoin = "round";
        contextRef.current = context;
        redraw(paths);
      }
    };

    handleResize();
    const resizeObserver = new ResizeObserver(handleResize);
    resizeObserver.observe(parent);

    // Listen for paths
    const pathsRef = ref(rtdb, `board_paths/${boardId}`);
    const unsubscribe = onValue(pathsRef, (snapshot) => {
      if (snapshot.exists()) {
        const data = snapshot.val();
        const loadedPaths = Object.values(data);
        setPaths(loadedPaths);
      } else {
        setPaths([]);
      }
    });

    return () => {
      off(pathsRef);
      resizeObserver.disconnect();
    };
  }, [inBoard, boardId]);

  // Redraw when paths change
  useEffect(() => {
    redraw(paths);
  }, [paths]);

  const redraw = (allPaths: any[]) => {
    const canvas = canvasRef.current;
    const context = contextRef.current;
    if (!canvas || !context) return;

    context.clearRect(0, 0, canvas.width, canvas.height);
    allPaths.forEach((path) => {
      if (!path.points || path.points.length === 0) return;
      context.beginPath();
      context.strokeStyle = path.color;
      context.lineWidth = path.size;
      context.moveTo(path.points[0].x, path.points[0].y);
      path.points.forEach((p: any) => {
        context.lineTo(p.x, p.y);
      });
      context.stroke();
    });
  };

  const startDrawing = (e: React.MouseEvent | React.TouchEvent) => {
    const { offsetX, offsetY } = getCoordinates(e);
    setIsDrawing(true);
    currentPathRef.current = [{ x: offsetX, y: offsetY }];

    if (contextRef.current) {
      contextRef.current.beginPath();
      contextRef.current.moveTo(offsetX, offsetY);
      contextRef.current.strokeStyle =
        tool === "eraser" ? "#000000" : currentColor;
      contextRef.current.lineWidth = currentSize;
    }
  };

  const draw = (e: React.MouseEvent | React.TouchEvent) => {
    if (!isDrawing) return;
    const { offsetX, offsetY } = getCoordinates(e);
    currentPathRef.current.push({ x: offsetX, y: offsetY });

    if (contextRef.current) {
      contextRef.current.lineTo(offsetX, offsetY);
      contextRef.current.stroke();
    }
  };

  const stopDrawing = async () => {
    if (!isDrawing) return;
    setIsDrawing(false);

    // Sync to Firebase
    const newPath = {
      color: tool === "eraser" ? "#000000" : currentColor,
      size: currentSize,
      points: currentPathRef.current,
      timestamp: serverTimestamp(),
    };

    try {
      await push(ref(rtdb, `board_paths/${boardId}`), newPath);
    } catch (err) {
      console.error("Failed to sync path", err);
      if (
        err instanceof Error &&
        err.message.toLowerCase().includes("permission")
      ) {
        handleDatabaseError(err, OperationType.WRITE, `board_paths/${boardId}`);
      }
    }
  };

  const getCoordinates = (e: React.MouseEvent | React.TouchEvent) => {
    const canvas = canvasRef.current;
    if (!canvas) return { offsetX: 0, offsetY: 0 };

    const rect = canvas.getBoundingClientRect();
    if ("touches" in e) {
      return {
        offsetX: e.touches[0].clientX - rect.left,
        offsetY: e.touches[0].clientY - rect.top,
      };
    } else {
      return {
        offsetX: e.nativeEvent.offsetX,
        offsetY: e.nativeEvent.offsetY,
      };
    }
  };

  const clearBoard = async () => {
    if (!isClearing) {
      setIsClearing(true);
      vibrate();
      setTimeout(() => setIsClearing(false), 3000);
      return;
    }

    setIsClearing(false);
    try {
      await remove(ref(rtdb, `board_paths/${boardId}`));
      showToast("Board cleared", "success");
    } catch (err) {
      console.error("Failed to clear board", err);
      if (
        err instanceof Error &&
        err.message.toLowerCase().includes("permission")
      ) {
        handleDatabaseError(
          err,
          OperationType.DELETE,
          `board_paths/${boardId}`,
        );
      }
      showToast("Failed to clear board", "error");
    }
  };

  const downloadBoard = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    // Create a temporary canvas to add background
    const tempCanvas = document.createElement("canvas");
    tempCanvas.width = canvas.width;
    tempCanvas.height = canvas.height;
    const tempCtx = tempCanvas.getContext("2d");
    if (!tempCtx) return;

    // Fill background
    tempCtx.fillStyle = "#000000";
    tempCtx.fillRect(0, 0, tempCanvas.width, tempCanvas.height);

    // Draw original canvas content
    tempCtx.drawImage(canvas, 0, 0);

    const link = document.createElement("a");
    link.download = `board-${boardId}-${Date.now()}.png`;
    link.href = tempCanvas.toDataURL("image/png");
    link.click();
    showToast("Board downloaded!", "success");
  };

  if (showCreatedModal) {
    const handleCopyCode = () => {
      vibrate();
      navigator.clipboard.writeText(boardId);
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2000);
      showToast("Board ID copied", "success");
    };

    const handleCopyLink = () => {
      vibrate();
      const directLink = `${window.location.origin}/board?code=${boardId}`;
      navigator.clipboard.writeText(directLink);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
      showToast("Direct link copied", "success");
    };

    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="flex flex-col items-center py-6 space-y-6 relative w-full pt-14 text-center max-w-[440px] mx-auto"
      >
        {/* Back and Action Buttons */}
        <button
          onClick={() => {
            vibrate();
            setState((prev: any) => ({ ...prev, showCreatedModal: false }));
          }}
          className="absolute top-0 left-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
        >
          <ArrowLeft size={10} /> Back
        </button>
        <button
          onClick={() => {
            vibrate();
            setState((prev: any) => ({ ...prev, showCreatedModal: false }));
          }}
          className="absolute top-0 right-0 flex items-center gap-1.5 px-3 py-1.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/50 hover:text-white rounded-full font-bold text-[10px] uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
        >
          New Board <Plus size={10} />
        </button>

        <div className="flex flex-col items-center space-y-1.5">
          <h3 className="text-white/40 text-[10px] font-bold uppercase tracking-[0.2em]">
            Your Board Code
          </h3>
          <div className="h-0.5 w-6 bg-white/10 rounded-full" />
        </div>

        {/* 5 digit boxes */}
        <div className="flex items-center justify-center gap-2.5">
          {boardId.split("").map((digit: string, i: number) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, scale: 0.5, rotate: -15, y: 20 }}
              animate={{ opacity: 1, scale: 1, rotate: 0, y: 0 }}
              transition={{
                type: "spring",
                stiffness: 400,
                damping: 15,
                delay: i * 0.08 + 0.2,
              }}
              whileHover={{ scale: 1.05, y: -5, transition: { duration: 0.2 } }}
              className="w-12 h-16 sm:w-14 sm:h-20 bg-white/[0.04] border-white/10 text-white border rounded-[28px] flex items-center justify-center text-3xl sm:text-4xl font-black font-sans relative group cursor-default shadow-lg"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-white/[0.05] to-transparent rounded-[28px] pointer-events-none" />
              <span className="relative z-10 tracking-tight drop-shadow-sm">
                {digit}
              </span>
              <div className="absolute inset-0 rounded-[28px] border border-white/0 group-hover:border-white/20 transition-all duration-300" />
            </motion.div>
          ))}
        </div>

        <div className="flex flex-col items-center space-y-3 w-full pt-2">
          <p className="text-white/30 text-[10px] font-medium tracking-wide leading-relaxed px-4">
            Share this 5-digit code or direct link with others to start drawing.
          </p>
        </div>

        {/* Copy Buttons */}
        <div className="flex gap-3 w-full pt-2">
          <button
            onClick={handleCopyLink}
            className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
              copiedLink ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"
            }`}
          >
            {copiedLink ? <Check size={16} /> : <Link size={16} />}
            {copiedLink ? "Copied!" : "Copy Link"}
          </button>
          <button
            onClick={handleCopyCode}
            className={`flex-1 border rounded-full px-6 py-3.5 font-bold text-xs transition-all flex items-center justify-center gap-2 ${
              copiedCode ? "bg-white text-black border-white" : "bg-white/10 hover:bg-white/20 text-white/60 border-white/10"
            }`}
          >
            {copiedCode ? <Check size={16} /> : <Copy size={16} />}
            {copiedCode ? "Copied!" : "Copy Code"}
          </button>
        </div>

        {/* QR Section */}
        <div className="flex flex-col space-y-4 w-full pt-1">
          <button
            onClick={() => {
              vibrate();
              setShowQR(true);
            }}
            className="w-full py-3.5 border border-white/10 bg-white/5 hover:bg-white/10 text-white/75 rounded-full font-bold text-xs transition-all flex items-center justify-center gap-2 active:scale-98 cursor-pointer"
          >
            <QrCode size={16} />
            Share via QR Code
          </button>

          <button
            onClick={() => {
              vibrate();
              setState((prev: any) => ({
                ...prev,
                showCreatedModal: false,
                inBoard: true,
              }));
            }}
            className="w-full bg-white text-black py-3.5 rounded-full font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-1.5 shadow-[0_0_20px_rgba(255,255,255,0.1)]"
          >
            Enter Board
            <ArrowRight size={16} />
          </button>
        </div>

        {/* Beautiful QR Modal */}
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
                
                <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-white/40">
                  Share Board Link
                </h4>

                <BeautifulQRCode
                  value={`${window.location.origin}/board?code=${boardId}`}
                  size={200}
                  theme="default"
                  onDownloadReady={setDownloadQRFn}
                />

                <div className="flex gap-2 w-full mt-2">
                  <button
                    onClick={() => {
                      vibrate();
                      navigator.clipboard.writeText(`${window.location.origin}/board?code=${boardId}`);
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

  if (inBoard) {
    return (
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="flex flex-col h-[calc(100dvh-180px)] min-h-[500px] max-h-[860px] sm:h-[calc(100vh-210px)] sm:min-h-[620px] relative"
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-4 px-2">
          <div className="flex items-center gap-2 sm:gap-3">
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-white/40">
              <Palette size={16} className="sm:w-5 sm:h-5" />
            </div>
            <div className="flex flex-col min-w-0">
              <h3 className="font-bold text-xs sm:text-sm truncate max-w-[100px] sm:max-w-[200px]">
                {boardName}
              </h3>
              <div className="flex items-center gap-1 sm:gap-2">
                <span className="text-[8px] sm:text-[10px] text-white/40 font-sans bg-white/5 px-1.5 py-0.5 rounded-full uppercase tracking-wider border border-white/5">
                  {boardId}
                </span>
                <span className="w-0.5 h-0.5 rounded-full bg-white/20"></span>
                <span className="text-[8px] sm:text-[10px] text-white/40 italic truncate max-w-[60px] sm:max-w-none">
                  By {state.userName}
                </span>
              </div>
            </div>
          </div>
          <div className="flex gap-1.5 sm:gap-2">
            <button
              onClick={() => {
                vibrate();
                setState((prev: any) => ({
                  ...prev,
                  inBoard: false,
                  boardId: "",
                  boardName: "",
                }));
              }}
              className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-white/40 hover:text-white hover:bg-white/10 transition-all"
              title="New Board"
            >
              <Plus size={16} className="sm:w-[18px] sm:h-[18px]" />
            </button>
            <button
              onClick={downloadBoard}
              className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-white/40 hover:text-white hover:bg-white/10 transition-all"
              title="Download"
            >
              <Download size={14} className="sm:w-4 sm:h-4" />
            </button>
            <button
              onClick={() => {
                vibrate();
                setState((prev: any) => ({ ...prev, inBoard: false }));
              }}
              className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-500/60 hover:text-red-500 hover:bg-red-500/20 transition-all"
              title="Exit"
            >
              <LogOut size={14} className="sm:w-4 sm:h-4" />
            </button>
          </div>
        </div>

        {/* Toolbar - Mobile Optimized */}
        <div className="flex flex-col gap-3 mb-4 p-3 sm:p-4 bg-[#0a0a0a] rounded-[32px] border border-white/10 shadow-2xl">
          <div className="flex flex-wrap items-center gap-2 sm:gap-4">
            {/* Tools */}
            <div className="flex gap-1 p-1 bg-white/5 rounded-full border border-white/5">
              <button
                onClick={() => setTool("pen")}
                className={`w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center transition-all ${tool === "pen" ? "bg-white text-black shadow-lg scale-105" : "text-white/40 hover:bg-white/5"}`}
              >
                <Palette size={16} className="sm:w-[18px] sm:h-[18px]" />
              </button>
              <button
                onClick={() => setTool("eraser")}
                className={`w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center transition-all ${tool === "eraser" ? "bg-white text-black shadow-lg scale-105" : "text-white/40 hover:bg-white/5"}`}
              >
                <Eraser size={16} className="sm:w-[18px] sm:h-[18px]" />
              </button>
            </div>

            {/* Size Slider */}
            <div className="flex-1 min-w-[100px] flex items-center gap-2 sm:gap-3 px-3 py-1.5 sm:py-2 bg-white/5 rounded-full border border-white/5">
              <div className="flex-1 h-1 bg-white/10 rounded-full overflow-hidden relative">
                <motion.div
                  className="absolute inset-y-0 left-0 bg-white"
                  animate={{ width: `${(currentSize / 20) * 100}%` }}
                />
                <input
                  type="range"
                  min="1"
                  max="20"
                  value={currentSize}
                  onChange={(e) => setCurrentSize(parseInt(e.target.value))}
                  className="absolute inset-0 opacity-0 cursor-pointer"
                />
              </div>
              <span className="text-[8px] sm:text-[10px] font-sans text-white/60 w-4 text-right">
                {currentSize}
              </span>
            </div>

            {/* Clear Button */}
            <button
              onClick={clearBoard}
              className={`flex items-center justify-center gap-1.5 sm:gap-2 px-3 sm:px-4 h-8 sm:h-10 rounded-full transition-all text-[8px] sm:text-[10px] font-bold uppercase tracking-widest border ${
                isClearing
                  ? "bg-red-500 text-white border-red-500 animate-pulse"
                  : "bg-red-500/10 border-red-500/20 text-red-500/60 hover:text-red-500 hover:bg-red-500/20"
              }`}
            >
              <Trash2 size={12} className="sm:w-3.5 sm:h-3.5" />
              {isClearing ? "Confirm?" : "Clear"}
            </button>
          </div>

          {/* Colors Grid - Expanded and Wrapped */}
          <div className="flex flex-wrap gap-1.5 sm:gap-2 justify-center max-h-[80px] sm:max-h-none overflow-y-auto custom-scrollbar p-1">
            {COLORS.map((color) => (
              <button
                key={color}
                onClick={() => {
                  setCurrentColor(color);
                  setTool("pen");
                  vibrate();
                }}
                className={`w-6 h-6 sm:w-8 sm:h-8 rounded-full border-2 transition-all relative group ${currentColor === color && tool === "pen" ? "border-white scale-110 z-10 shadow-lg" : "border-transparent hover:scale-105"}`}
                style={{ backgroundColor: color }}
              >
                {currentColor === color && tool === "pen" && (
                  <motion.div
                    layoutId="color-active"
                    className="absolute -inset-1 border border-white/40 rounded-full pointer-events-none"
                  />
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Canvas Area */}
        <div className="flex-1 bg-black rounded-[32px] border border-white/10 overflow-hidden relative touch-none shadow-[inset_0_0_100px_rgba(255,255,255,0.02)]">
          <canvas
            ref={canvasRef}
            onMouseDown={startDrawing}
            onMouseMove={draw}
            onMouseUp={stopDrawing}
            onMouseLeave={stopDrawing}
            onTouchStart={startDrawing}
            onTouchMove={draw}
            onTouchEnd={stopDrawing}
            className="cursor-crosshair w-full h-full"
          />

          {/* Subtle Grid Background */}
          <div
            className="absolute inset-0 pointer-events-none opacity-[0.03]"
            style={{
              backgroundImage:
                "radial-gradient(circle, white 1px, transparent 1px)",
              backgroundSize: "30px 30px",
            }}
          />
        </div>

        <div className="flex items-center justify-between mt-4 px-2">
          <p className="text-[8px] sm:text-[9px] text-white/20 italic flex items-center gap-1.5">
            <Zap size={10} />
            Optimized sync active
          </p>
          <p className="text-[8px] sm:text-[9px] text-white/20 font-sans">
            {paths.length} strokes
          </p>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-[900px] mx-auto px-4"
    >
      <div className="space-y-10">
        {/* User Identity Header block */}
        <div className="max-w-[400px] mx-auto text-center space-y-2.5 bg-[#0a0a0a]/90 border border-white/10 rounded-3xl p-5 shadow-xl">
          <label className="text-[10px] uppercase tracking-[0.2em] font-bold text-white/40 block">
            Your Display Name
          </label>
          <div className="relative">
            <User className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={18} />
            <input
              value={userName}
              onChange={(e) =>
                setState((prev: any) => ({
                  ...prev,
                  userName: e.target.value,
                }))
              }
              placeholder="Enter your name..."
              className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-11 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 text-center font-bold"
            />
          </div>
        </div>

        {/* Two Columns Split Grid */}
        <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-10 md:gap-0 relative">
          {/* Vertical Separator Line */}
          <div className="hidden md:block absolute left-1/2 top-0 bottom-0 w-px bg-white/10 -translate-x-1/2 origin-top pointer-events-none" />

          {/* Left Column: Join Board */}
          <div className="space-y-6 md:pr-12 text-left flex flex-col justify-between">
            <div className="space-y-2">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-white/60">
                  <LogIn size={18} />
                </div>
                <div>
                  <h3 className="text-xl font-bold tracking-tight text-white">Join Board</h3>
                  <p className="text-xs text-white/40">Access an existing canvas</p>
                </div>
              </div>
              <p className="text-[11px] text-white/30 leading-relaxed pt-1">
                Enter the 5-digit board ID to join a live shared drawing board.
              </p>
            </div>

            <form onSubmit={handleJoinBoard} className="space-y-4 pt-2">
              <div className="space-y-2">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Board Access Code
                </label>
                <div className="relative">
                  <Hash className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30" size={16} />
                  <input
                    value={boardId}
                    onChange={(e) =>
                      setState((prev: any) => ({
                        ...prev,
                        boardId: e.target.value.replace(/\D/g, "").slice(0, 5),
                      }))
                    }
                    placeholder="5-digit Board ID"
                    className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-10 pr-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold tracking-widest font-sans"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-white text-black py-3 rounded-2xl font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 shadow-md shadow-white/5 flex items-center justify-center gap-1.5"
              >
                {loading ? (
                  <Loader2 className="animate-spin" size={14} />
                ) : (
                  <>
                    <span>Join Board</span>
                    <ArrowRight size={14} />
                  </>
                )}
              </button>
            </form>
          </div>

          {/* Right Column: Create Board */}
          <div className="space-y-6 md:pl-12 text-left border-t border-white/10 pt-8 md:border-t-0 md:pt-0">
            <div className="space-y-2">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-white/60">
                  <Plus size={18} />
                </div>
                <div>
                  <h3 className="text-xl font-bold tracking-tight text-white">Create Board</h3>
                  <p className="text-xs text-white/40">Start a new shared canvas</p>
                </div>
              </div>
              <p className="text-[11px] text-white/30 leading-relaxed pt-1">
                Launch a collaborative workspace for brainstorming, annotations, and real-time sketching.
              </p>
            </div>

            <form onSubmit={handleCreateBoard} className="space-y-4">
              <div className="space-y-2">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Board Name
                </label>
                <input
                  value={boardNameInput}
                  onChange={(e) =>
                    setState((prev: any) => ({
                      ...prev,
                      boardNameInput: e.target.value,
                    }))
                  }
                  placeholder="Enter board name..."
                  className="w-full bg-white/[0.04] border border-white/10 rounded-2xl px-4 py-3 text-sm text-white focus:outline-none focus:border-white/20 focus:bg-white/[0.06] transition-all placeholder:text-white/25 font-bold"
                />
              </div>

              <div className="space-y-2.5">
                <label className="text-[10px] uppercase tracking-widest font-bold text-white/40 block ml-3">
                  Expiration Duration
                </label>
                <div className="grid grid-cols-5 gap-1">
                  {["1h", "6h", "12h", "24h"].map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => {
                        vibrate();
                        setState((prev: any) => ({ ...prev, boardExpire: t }));
                      }}
                      className={`py-2 text-[10px] font-bold rounded-xl border transition-all ${
                        boardExpire === t
                          ? "bg-white text-black border-white shadow-md shadow-white/5"
                          : "bg-white/[0.04] text-white/40 border-white/5 hover:border-white/10 hover:text-white/60"
                      }`}
                    >
                      {t}
                    </button>
                  ))}
                  <input
                    type="text"
                    placeholder="Custom"
                    className="bg-white/[0.04] border border-white/5 rounded-xl px-1 py-2 text-[10px] text-center focus:outline-none focus:border-white/10 focus:bg-white/[0.06] transition-all placeholder:text-white/20 font-bold"
                    onChange={(e) =>
                      setState((prev: any) => ({
                        ...prev,
                        boardExpire: `custom:${e.target.value}`,
                      }))
                    }
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-white text-black py-3 rounded-2xl font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 shadow-md shadow-white/5 flex items-center justify-center gap-1.5"
              >
                {loading ? (
                  <Loader2 className="animate-spin" size={14} />
                ) : (
                  <>
                    <span>Launch Board</span>
                    <ArrowRight size={14} />
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

function LikeWidget({
  showToast,
  likes,
  variant = "default",
}: {
  showToast: (msg: string, type?: any) => void;
  likes: number;
  variant?: "default" | "landing";
}) {
  const [dislikePos, setDislikePos] = useState({ x: 0, y: 0 });
  const [landingDislikeRoaming, setLandingDislikeRoaming] = useState(false);
  const [showTrollText, setShowTrollText] = useState(false);
  const trollTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const landingRatingRef = useRef<HTMLElement | null>(null);
  const landingDislikeRef = useRef<HTMLButtonElement | null>(null);

  const handleLike = async () => {
    vibrate();
    const likesRef = ref(rtdb, "stats/likes");
    try {
      await runTransaction(likesRef, (current) => {
        return (current || 0) + 1;
      });
      showToast("Thanks for your feedback!", "success");
    } catch (err) {
      console.error("Like failed", err);
    }
  };

  const moveDislike = () => {
    const randomX = Math.floor(Math.random() * 350) - 300; // Move more to the left
    const randomY = Math.floor(Math.random() * 100) - 50;
    setDislikePos({ x: randomX, y: randomY });
  };

  const triggerTroll = () => {
    setShowTrollText(true);
    if (trollTimeoutRef.current) clearTimeout(trollTimeoutRef.current);
    trollTimeoutRef.current = setTimeout(() => setShowTrollText(false), 800);
  };

  const handleDislikeInteraction = () => {
    moveDislike();
    triggerTroll();
  };

  const handleLandingDislikeInteraction = () => {
    const card = landingRatingRef.current;
    const button = landingDislikeRef.current;
    if (!card || !button) {
      handleDislikeInteraction();
      return;
    }

    const cardRect = card.getBoundingClientRect();
    const buttonRect = button.getBoundingClientRect();
    const inset = cardRect.width < 520 ? 14 : 24;
    const maxX = Math.max(inset, cardRect.width - buttonRect.width - inset);
    const maxY = Math.max(inset, cardRect.height - buttonRect.height - inset);

    setLandingDislikeRoaming(true);
    setDislikePos({
      x: inset + Math.random() * (maxX - inset),
      y: inset + Math.random() * (maxY - inset),
    });
    triggerTroll();
  };

  if (variant === "landing") {
    return (
      <motion.section
        ref={landingRatingRef}
        className="hefimer-rating-console"
        initial={{ opacity: 0, y: 36 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
        aria-labelledby="hefimer-rating-title"
      >
        <div className="hefimer-rating-scan" aria-hidden="true"><i /><i /><i /><i /><i /></div>
        <div className="hefimer-rating-copy">
          <span className="hefimer-mono">SIGNAL QUALITY / FEEDBACK</span>
          <h2 id="hefimer-rating-title">Did this page<br />get you there?</h2>
          <p>One tap helps us know if Hefimer is clear and useful.</p>
        </div>

        <div className="hefimer-rating-controls">
          <div className="hefimer-rating-count">
            <span className="hefimer-mono">POSITIVE SIGNALS</span>
            <strong>{likes.toLocaleString()}</strong>
          </div>
          <div className="hefimer-rating-actions">
            <button className="hefimer-rating-yes" onClick={handleLike} aria-label="Rate this page positively">
              <ThumbsUp size={20} />
              <span>It got me there</span>
              <ArrowRight size={16} />
            </button>
            <motion.div
              animate={landingDislikeRoaming ? { left: dislikePos.x, top: dislikePos.y } : undefined}
              transition={{ type: "spring", stiffness: 600, damping: 15 }}
              className={`hefimer-rating-no-runner${landingDislikeRoaming ? " is-roaming" : ""}`}
            >
              <button
                ref={landingDislikeRef}
                className="hefimer-rating-no"
                onMouseEnter={handleLandingDislikeInteraction}
                onClick={handleLandingDislikeInteraction}
                aria-label="Negative rating is currently unavailable"
              >
                <ThumbsDown size={17} />
                <span>Not quite</span>
              </button>
            </motion.div>
          </div>
          <AnimatePresence>
            {showTrollText && (
              <motion.p
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 8 }}
                className="hefimer-rating-troll hefimer-mono"
              >
                Dislike is currently unavailable=))
              </motion.p>
            )}
          </AnimatePresence>
        </div>
      </motion.section>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-50px" }}
      transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
      className="w-full max-w-[500px] mt-4 relative"
    >
      <div className="flex items-center justify-between px-8">
        <h3 className="text-sm font-bold text-white/80 tracking-tight">
          How would you rate this page?
        </h3>

        <div className="flex items-center gap-8 relative">
          {/* Like Button */}
          <div className="flex flex-col items-center gap-1">
            <button
              onClick={handleLike}
              className="w-12 h-12 rounded-full border-2 border-white/20 flex items-center justify-center bg-white/5 hover:bg-white hover:text-black transition-all active:scale-90 group"
            >
              <ThumbsUp
                size={20}
                className="group-hover:scale-110 transition-transform"
              />
            </button>
            <span className="text-xs font-sans font-bold text-white/40">
              {likes}
            </span>
          </div>

          {/* Dislike Button (Troll) */}
          <div className="flex flex-col items-center gap-1">
            <motion.div
              animate={{ x: dislikePos.x, y: dislikePos.y }}
              transition={{ type: "spring", stiffness: 600, damping: 15 }}
              className="z-20"
            >
              <button
                onMouseEnter={handleDislikeInteraction}
                onClick={handleDislikeInteraction}
                className="w-12 h-12 rounded-full border-2 border-white/20 flex items-center justify-center bg-white/5 cursor-not-allowed transition-all active:scale-90 group"
              >
                <ThumbsDown
                  size={20}
                  className="group-hover:scale-110 transition-transform"
                />
              </button>
            </motion.div>
            <span className="text-xs font-sans font-bold text-white/40">0</span>
          </div>
        </div>
      </div>

      {/* Troll Text */}
      <AnimatePresence>
        {showTrollText && (
          <motion.div
            initial={{ opacity: 0, y: 10, x: "-50%" }}
            animate={{ opacity: 1, y: 0, x: "-50%" }}
            exit={{ opacity: 0, y: 10, x: "-50%" }}
            className="absolute -bottom-8 left-1/2 text-[11px] font-bold text-white/40 italic whitespace-nowrap pointer-events-none"
          >
            Dislike is currently unavailable=))
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
function PolicyModal({
  type,
  onClose,
}: {
  type: "privacy" | "terms";
  onClose: () => void;
}) {
  const isPrivacy = type === "privacy";
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.9, opacity: 0, y: 20 }}
        transition={{ type: "spring", stiffness: 300, damping: 25 }}
        className="relative w-full max-w-[500px] max-h-[80vh] overflow-y-auto rounded-[32px] p-6 custom-scrollbar"
        style={{
          background: "#0a0a0a",
          backdropFilter: "blur(20px)",
          border: "1px solid rgba(255,255,255,0.06)",
          boxShadow: "0 25px 60px rgba(0,0,0,0.8)",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-full bg-white/5 hover:bg-white/10 text-white/50 hover:text-white transition-colors z-10"
        >
          <X size={16} />
        </button>

        <h2 className="text-white font-bold text-lg mb-5 flex items-center gap-2.5">
          {isPrivacy ? (
            <Shield size={20} className="text-white/60" />
          ) : (
            <FileText size={20} className="text-white/60" />
          )}
          {isPrivacy ? "Privacy Policy" : "Terms of Service"}
        </h2>

        {isPrivacy ? (
          <div className="text-white/60 text-[13px] leading-relaxed space-y-4">
            <p>
              <strong className="text-white/80">Last Updated:</strong> May 2026
            </p>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">1. Overview</h3>
              <p>
                Hefimer is a fast file sharing, text sharing, and online
                collaboration tool that uses a simple 5-digit access code. We
                are committed to protecting your privacy.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                2. Data We Collect
              </h3>
              <p>
                • <strong className="text-white/70">No Login Required:</strong>{" "}
                You do not need to create an account, provide an email, or any
                personal information to use the service.
              </p>
              <p>
                • <strong className="text-white/70">Shared Content:</strong>{" "}
                Text and files you upload are stored temporarily on our systems
                (Firebase/Google Cloud for text, Gofile.io for files).
              </p>
              <p>
                • <strong className="text-white/70">Technical Data:</strong> We
                may collect basic information such as browser type and access
                time to improve the service.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                3. How We Use Data
              </h3>
              <p>
                • Temporarily store content to transfer files/text between
                devices.
              </p>
              <p>• Manage chat rooms and collaborative drawing boards.</p>
              <p>• Display usage statistics (total likes, online users).</p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                4. Storage & Auto-Deletion
              </h3>
              <p>
                • All data has an automatic expiration time (from 5 minutes to
                24 hours, as you choose).
              </p>
              <p>
                • "One-time" mode automatically deletes the data immediately
                after the first access.
              </p>
              <p>
                • Chat rooms and boards are also automatically deleted after
                they expire.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                5. Third-Party Services
              </h3>
              <p>
                • <strong className="text-white/70">Firebase (Google):</strong>{" "}
                Stores text data, chat rooms, and boards. See Google's privacy
                policy.
              </p>
              <p>
                • <strong className="text-white/70">Gofile.io:</strong> Stores
                uploaded files. See Gofile's privacy policy.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">6. Security</h3>
              <p>• You can set a password to protect your shared content.</p>
              <p>
                • Keep your 5-digit access code secret — anyone with the code
                can access the content.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">7. Contact</h3>
              <p>
                If you have questions about this privacy policy, please reach
                out to us at:
              </p>
              <p className="mt-1">
                📧{" "}
                <strong className="text-white/70">
                  contact@hefimer.com
                </strong>
              </p>
              <p className="mt-1 text-white/40 text-[11px] italic">
                (Please replace with your actual email address)
              </p>
            </div>
          </div>
        ) : (
          <div className="text-white/60 text-[13px] leading-relaxed space-y-4">
            <p>
              <strong className="text-white/80">Last Updated:</strong> May 2026
            </p>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                1. Acceptance of Terms
              </h3>
              <p>
                By accessing and using Hefimer, you agree to comply with these
                terms. If you do not agree, please discontinue use of the
                service.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                2. Purpose of Use
              </h3>
              <p>
                Hefimer is designed for sharing files, text, and online
                collaboration for personal, educational, and lawful work
                purposes. You may not use it to distribute illegal content.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                3. Prohibited Activities
              </h3>
              <p>You may not use the service to:</p>
              <p>
                • Share content that violates the law, is obscene, violent, or
                harmful.
              </p>
              <p>• Distribute malware, viruses, or malicious code.</p>
              <p>
                • Spam, abuse the system, or launch denial-of-service attacks.
              </p>
              <p>
                • Infringe on the intellectual property rights of third parties.
              </p>
              <p>
                • Use the service for commercial purposes without permission.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                4. User Responsibility
              </h3>
              <p>• You are solely responsible for the content you share.</p>
              <p>
                • You are responsible for keeping your access codes and
                passwords secure.
              </p>
              <p>
                • We are not liable for any loss or damage arising from the use
                of the service.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                5. Third-Party Services
              </h3>
              <p>
                • File sharing uses Gofile.io. You must also comply with their
                terms of service.
              </p>
              <p>
                • We do not control and are not responsible for the content or
                conduct of third-party services.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                6. Service Availability
              </h3>
              <p>
                • We do not guarantee uninterrupted 24/7 service availability.
              </p>
              <p>
                • We reserve the right to suspend, modify, or discontinue the
                service at any time without prior notice.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                7. Limitation of Liability
              </h3>
              <p>• The service is provided "as-is" without any warranties.</p>
              <p>
                • We are not liable for any direct, indirect, or incidental
                damages.
              </p>
            </div>

            <div>
              <h3 className="text-white/80 font-semibold mb-1">
                8. Changes to Terms
              </h3>
              <p>
                We may update these terms at any time. Continued use after
                changes constitutes acceptance of the new terms.
              </p>
            </div>
          </div>
        )}
      </motion.div>
    </motion.div>
  );
}

const FAQ_ITEMS = [
  {
    q: "What is Hefimer?",
    a: "Hefimer is a fast, lightweight web tool for sharing files, text/code snippets, and real-time collaboration — all accessed through a simple 5-digit code. No account required.",
  },
  {
    q: "How do I share a file or text?",
    a: "Go to the Send tab, upload a file or paste your text, choose an expiration time, and click Generate. You'll receive a 5-digit code to share with anyone.",
  },
  {
    q: "How do I receive shared content?",
    a: "Go to the Receive tab, enter the 5-digit code you received, and click Retrieve. The shared file or text will be displayed instantly.",
  },
  {
    q: "How long does shared content last?",
    a: "You can choose an expiration time from 5 minutes up to 24 hours when sending. After the time expires, the content is automatically and permanently deleted.",
  },
  {
    q: "What is One-Time mode?",
    a: "When enabled, the shared content is automatically deleted immediately after the first person accesses it. Perfect for sensitive information.",
  },
  {
    q: "Can I protect my shared content with a password?",
    a: "Yes. When sending, you can set an optional password. Recipients will need to enter the correct password before they can view the content.",
  },
  {
    q: "What is a Chat Room?",
    a: "A Chat Room is a real-time messaging space with emoji reactions. Create or join a room using a 5-digit code. Rooms auto-delete after their expiration time.",
  },
  {
    q: "What is a Board?",
    a: "A Board is a collaborative drawing canvas. Create or join a board with a 5-digit code and draw together with others in real-time.",
  },
  {
    q: "Is Hefimer free?",
    a: "Yes, Hefimer is completely free to use. No subscriptions, no hidden fees.",
  },
  {
    q: "Do I need to create an account?",
    a: "No. Hefimer requires no login, no email, and no personal information. Just enter a code and start sharing.",
  },
  {
    q: "What about data and privacy?",
    a: "All shared content is automatically deleted after expiration. We don't collect personal data. Password-protected content adds an extra layer of security. See our Privacy Policy for full details.",
  },
  {
    q: "Is Hefimer open source?",
    a: "Hefimer is currently a closed-source project. If you're interested in contributing or have questions, feel free to reach out.",
  },
];

const MockupAppWindow = () => {
  const containerVariants: Variants = {
    hidden: {},
    visible: { transition: { staggerChildren: 0.07, delayChildren: 0.5 } },
  };
  const rowVariants: Variants = {
    hidden: { opacity: 0, y: 10 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.16, 1, 0.3, 1] as const } },
  };
  const fadeIn: Variants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { duration: 0.6, ease: "easeOut" } },
  };

  return (
    <div
      className="w-full max-w-[480px] mx-auto select-none"
      style={{ perspective: "1200px", perspectiveOrigin: "50% 50%" }}
    >
      {/* 3D window — animate: slide from right + tilt settle */}
      <motion.div
        initial={{ opacity: 0, x: 60, rotateY: -28, rotateX: 8, scale: 0.92 }}
        animate={{ opacity: 1, x: 0, rotateY: -12, rotateX: 4, scale: 1 }}
        transition={{ type: "spring", stiffness: 55, damping: 16, delay: 0.3 }}
        style={{
          transformStyle: "preserve-3d",
          boxShadow: "-24px 28px 80px rgba(0,0,0,0.85), -4px 0 0 0 rgba(255,255,255,0.04), 0 0 0 1px rgba(255,255,255,0.07)",
          borderRadius: "18px",
          overflow: "hidden",
          background: "#0a0a0a",
          willChange: "transform",
        }}
      >
        {/* ── Browser title bar ── */}
        <motion.div
          variants={fadeIn}
          initial="hidden"
          animate="visible"
          style={{ background: "#111", borderBottom: "1px solid rgba(255,255,255,0.06)", padding: "10px 14px", display: "flex", alignItems: "center", gap: 10 }}
        >
          <div style={{ display: "flex", gap: 5 }}>
            <div style={{ width: 9, height: 9, borderRadius: "50%", background: "rgba(255,255,255,0.22)" }} />
            <div style={{ width: 9, height: 9, borderRadius: "50%", background: "rgba(255,255,255,0.10)" }} />
            <div style={{ width: 9, height: 9, borderRadius: "50%", background: "rgba(255,255,255,0.05)" }} />
          </div>
          <div style={{ flex: 1, display: "flex", justifyContent: "center" }}>
            <div style={{ background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 6, padding: "3px 12px", display: "flex", alignItems: "center", gap: 6 }}>
              <div style={{ width: 6, height: 6, borderRadius: "50%", background: "rgba(255,255,255,0.18)" }} />
              <span style={{ fontSize: 9, fontFamily: "monospace", color: "rgba(255,255,255,0.28)", letterSpacing: "0.04em" }}>hefimer.pages.dev</span>
            </div>
          </div>
          <div style={{ width: 50 }} />
        </motion.div>

        {/* ── App top-bar: logo ── */}
        <motion.div
          variants={fadeIn}
          initial="hidden"
          animate="visible"
          transition={{ delay: 0.55 }}
          style={{ background: "#0d0d0d", borderBottom: "1px solid rgba(255,255,255,0.05)", padding: "8px 16px", display: "flex", alignItems: "center", justifyContent: "space-between" }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ repeat: Infinity, duration: 20, ease: "linear" }}
              style={{ width: 18, height: 18, flexShrink: 0 }}
            >
              <img src="/hefimer-orbit.svg" alt="Hefimer" draggable={false} style={{ width: "100%", height: "100%", objectFit: "contain", opacity: 0.7 }} />
            </motion.div>
            <span style={{ fontSize: 9, fontWeight: 900, color: "rgba(255,255,255,0.7)", letterSpacing: "0.2em", textTransform: "uppercase" }}>Hefimer</span>
          </div>
          <div style={{ display: "flex", gap: 4 }}>
            {["Chat", "Board"].map((label) => (
              <div key={label} style={{ padding: "3px 8px", borderRadius: 6, background: "rgba(255,255,255,0.04)", display: "flex", alignItems: "center" }}>
                <span style={{ fontSize: 8, fontWeight: 600, color: "rgba(255,255,255,0.25)", letterSpacing: "0.05em" }}>{label}</span>
              </div>
            ))}
          </div>
        </motion.div>

        {/* ── Main content ── */}
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          style={{ padding: "20px 20px 22px", background: "#0a0a0a", display: "flex", flexDirection: "column", gap: 12 }}
        >

          {/* Page title */}
          <motion.div variants={rowVariants}>
            <h2 style={{ fontSize: 26, fontWeight: 900, color: "white", margin: 0, letterSpacing: "-0.04em", lineHeight: 1 }}>Send</h2>
            <p style={{ fontSize: 9, color: "rgba(255,255,255,0.35)", margin: "4px 0 0", letterSpacing: "0.01em" }}>Share a file or text that disappears when its moment is over.</p>
          </motion.div>

          {/* Nav tabs — Send File | Send Text (no Home) */}
          <motion.div variants={rowVariants} style={{ display: "flex", gap: 20, borderBottom: "1px solid rgba(255,255,255,0.08)", paddingBottom: 0 }}>
            {[
              { label: "Send File", active: true },
              { label: "Send Text", active: false },
            ].map(({ label, active }) => (
              <div key={label} style={{ position: "relative", paddingBottom: 8, cursor: "pointer" }}>
                <span style={{ fontSize: 9, fontWeight: 700, color: active ? "white" : "rgba(255,255,255,0.4)", letterSpacing: "0.01em", display: "flex", alignItems: "center", gap: 5 }}>
                  {label}
                </span>
                {active && (
                  <motion.div
                    layoutId="mock-tab-indicator"
                    style={{ position: "absolute", bottom: 0, left: 0, right: 0, height: 1, background: "white", borderRadius: 1 }}
                  />
                )}
              </div>
            ))}
          </motion.div>

          {/* Drop zone */}
          <motion.div
            variants={rowVariants}
            whileHover={{ scale: 1.01, borderColor: "rgba(255,255,255,0.3)" }}
            style={{
              border: "1.5px dashed rgba(255,255,255,0.18)",
              borderRadius: 24,
              height: 110,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 14,
              padding: "0 16px",
              background: "rgba(255,255,255,0.015)",
              position: "relative",
              overflow: "hidden",
              cursor: "pointer",
            }}
          >
            {/* Subtle shimmer sweep */}
            <motion.div
              initial={{ x: "-100%" }}
              animate={{ x: "200%" }}
              transition={{ repeat: Infinity, duration: 2.4, ease: "linear", delay: 1.2 }}
              style={{ position: "absolute", inset: 0, background: "linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.03) 50%, transparent 100%)", pointerEvents: "none" }}
            />
            <div style={{ width: 42, height: 42, borderRadius: "50%", background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)", display: "flex", alignItems: "center", flexShrink: 0, justifyContent: "center" }}>
              <motion.div
                animate={{ y: [-2, 2, -2] }}
                transition={{ repeat: Infinity, duration: 2.2, ease: "easeInOut" }}
                style={{ display: "flex", alignItems: "center", justifyContent: "center" }}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
              </motion.div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 4, alignItems: "flex-start", minWidth: 0 }}>
              <span style={{ fontSize: 11, fontWeight: 700, color: "white", letterSpacing: "-0.01em" }}>Tap to select file(s)</span>
              <div style={{ display: "flex", alignItems: "center", gap: 5, flexWrap: "wrap" }}>
                <motion.span
                  animate={{ opacity: [0.7, 1, 0.7] }}
                  transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
                  style={{ fontSize: 7, fontWeight: 800, letterSpacing: "0.08em", color: "rgba(52,211,153,0.85)", background: "rgba(52,211,153,0.08)", border: "1px solid rgba(52,211,153,0.15)", borderRadius: 99, padding: "1.5px 5.5px", display: "flex", alignItems: "center", gap: 3.5, textTransform: "uppercase" }}
                >
                  <span style={{ width: 4.5, height: 4.5, borderRadius: "50%", background: "rgba(52,211,153,0.9)", display: "inline-block" }} />
                  storage.to
                </motion.span>
                <div style={{ width: 3, height: 3, borderRadius: "50%", background: "rgba(255,255,255,0.15)" }} />
                <span style={{ fontSize: 8, color: "rgba(255,255,255,0.3)", fontWeight: 600 }}>Max 25 GB</span>
                <div style={{ width: 3, height: 3, borderRadius: "50%", background: "rgba(255,255,255,0.15)" }} />
                <span style={{ fontSize: 8, color: "rgba(255,255,255,0.3)", fontWeight: 600 }}>Today: 0.00 MB</span>
              </div>
            </div>
          </motion.div>

          {/* Advanced Options row */}
          <motion.div
            variants={rowVariants}
            style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 18, display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 14px" }}
          >
            <span style={{ fontSize: 9, fontWeight: 700, color: "rgba(255,255,255,0.4)", letterSpacing: "0.02em" }}>Advanced Options</span>
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <div style={{ display: "flex", background: "rgba(0,0,0,0.4)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 10, padding: 2 }}>
                <span style={{ fontSize: 8, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em", background: "rgba(255,255,255,0.15)", color: "white", borderRadius: 8, padding: "3px 8px" }}>File</span>
                <span style={{ fontSize: 8, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em", color: "rgba(255,255,255,0.3)", padding: "3px 8px" }}>Folder</span>
              </div>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.35)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </div>
          </motion.div>

          {/* Upload / Send button */}
          <motion.div
            variants={rowVariants}
            whileHover={{ scale: 1.02, boxShadow: "0 0 20px rgba(255,255,255,0.15)" }}
            style={{ background: "white", borderRadius: 99, padding: "10px 0", display: "flex", alignItems: "center", justifyContent: "center", gap: 7, cursor: "pointer" }}
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="black" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            <span style={{ fontSize: 11, fontWeight: 800, color: "black", letterSpacing: "0.04em" }}>Upload &amp; Get Code</span>
          </motion.div>
        </motion.div>

        {/* Bottom reflection */}
        <div style={{ height: 2, background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.04), transparent)" }} />
      </motion.div>
    </div>
  );
};

function LegacyLandingPage({ setActiveTab, onOpenHistory, likesCount, showToast, setShowPolicy, developer }: any) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full flex flex-col space-y-16 pt-2 pb-10"
    >
      {/* Hero Section */}
      <div className="max-w-[1200px] mx-auto px-4 py-4 grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center text-left">
        {/* Left Column: Brand Info, Tagline, CTAs */}
        <div className="lg:col-span-6 flex flex-col items-start space-y-6 text-left">
          {/* Brand Badge */}
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ type: "spring", stiffness: 100, damping: 15 }}
            className="inline-flex items-center gap-3 px-4 py-2 rounded-full border border-white/10 bg-white/5 backdrop-blur-md hover:border-white/20 transition-all select-none"
          >
            <motion.img
              src="/hefimer-orbit.svg"
              alt="Hefimer Logo"
              draggable="false"
              animate={{ rotate: 360 }}
              transition={{ repeat: Infinity, duration: 25, ease: "linear" }}
              className="w-6 h-6 object-contain"
            />
            <span className="text-[10px] tracking-[0.2em] font-black uppercase text-white/70">
              Hefimer
            </span>
          </motion.div>

          {/* Hero Title */}
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ type: "spring", stiffness: 85, damping: 14, delay: 0.1 }}
            className="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight text-white leading-[1.08] font-sans"
          >
            Instant Transfer,<br />
            <span className="text-white/30 text-2xl sm:text-3xl lg:text-4xl font-medium block mt-2">
              No signup. No storage. Just a 5-digit code.
            </span>
          </motion.h1>

          {/* Hero Description */}
          <motion.p
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut", delay: 0.2 }}
            className="text-sm sm:text-base text-white/40 leading-relaxed max-w-xl"
          >
            Hefimer is a <span className="text-white/70 font-semibold">transfer tool</span>, not a storage service. Send files, text, and messages instantly — they disappear after the timer ends. Nothing is saved permanently.
          </motion.p>

          {/* Not-storage disclaimer */}
          <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, ease: "easeOut", delay: 0.35 }}
            className="flex items-center gap-2 text-[10px] font-bold tracking-[0.15em] uppercase text-white/30"
          >
            <span className="flex items-center gap-1.5 border border-white/10 bg-white/[0.03] rounded-full px-3 py-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-white/30" />
              Not a cloud storage
            </span>
            <span className="flex items-center gap-1.5 border border-white/10 bg-white/[0.03] rounded-full px-3 py-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-white/30" />
              Auto-deletes on expiry
            </span>
          </motion.div>

          {/* Hero CTA Buttons */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ type: "spring", stiffness: 120, damping: 15, delay: 0.3 }}
            className="flex flex-wrap items-center gap-4 pt-2"
          >
            <button
              onClick={() => { vibrate(); setActiveTab("file"); }}
              className="px-8 py-4 bg-white text-black rounded-full font-bold text-xs hover:scale-105 active:scale-95 transition-all shadow-[0_0_30px_rgba(255,255,255,0.15)] flex items-center gap-2 cursor-pointer"
            >
              Start Sharing <ArrowRight size={14} />
            </button>
            <button
              onClick={() => { vibrate(); onOpenHistory(); }}
              className="px-8 py-4 bg-white/5 hover:bg-white/10 border border-white/10 text-white/80 rounded-full font-bold text-xs active:scale-95 transition-all flex items-center gap-2 cursor-pointer"
            >
              <Clock size={14} /> View History
            </button>
          </motion.div>
        </div>

        {/* Right Column: App UI Mockup */}
        <div className="lg:col-span-6 w-full flex justify-center items-center py-6" style={{ overflow: "visible" }}>
          <MockupAppWindow />
        </div>
      </div>

      {/* Feature CTA Grid */}
      <div className="space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          className="text-center space-y-1 px-4"
        >
          <h2 className="text-xs uppercase tracking-[0.25em] font-bold text-white/30">
            Transfer Tools
          </h2>
          <p className="text-xl font-bold tracking-tight text-white">
            Pick a method — everything auto-deletes after transfer
          </p>
        </motion.div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 max-w-[1200px] mx-auto px-4">
          {/* Card: Send File */}
          <motion.div
            initial={{ opacity: 0, y: 30, scale: 0.96 }}
            whileInView={{ opacity: 1, y: 0, scale: 1 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ type: "spring", stiffness: 80, damping: 15 }}
            whileHover={{ y: -6, scale: 1.01 }}
            onClick={() => { vibrate(); setActiveTab("file"); }}
            className="group relative bg-zinc-950/70 border border-white/10 rounded-[32px] p-6 space-y-4 hover:border-white/20 transition-all cursor-pointer shadow-xl overflow-hidden text-left"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-white/[0.01] to-transparent pointer-events-none" />
            <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-white/70 group-hover:scale-110 group-hover:bg-white/10 group-hover:text-white transition-all">
              <FileUp size={22} />
            </div>
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                Send Files
                <ArrowRight size={14} className="opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </h3>
              <p className="text-xs text-white/40 leading-relaxed">
                Drop any file — images, PDFs, videos, or archives. Transferred via a one-time presigned URL. <span className="text-white/60">Not stored permanently.</span> Auto-deleted when the timer runs out.
              </p>
            </div>
          </motion.div>

          {/* Card: Send Text */}
          <motion.div
            initial={{ opacity: 0, y: 30, scale: 0.96 }}
            whileInView={{ opacity: 1, y: 0, scale: 1 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ type: "spring", stiffness: 80, damping: 15, delay: 0.1 }}
            whileHover={{ y: -6, scale: 1.01 }}
            onClick={() => { vibrate(); setActiveTab("text"); }}
            className="group relative bg-zinc-950/70 border border-white/10 rounded-[32px] p-6 space-y-4 hover:border-white/20 transition-all cursor-pointer shadow-xl overflow-hidden text-left"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-white/[0.01] to-transparent pointer-events-none" />
            <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-white/70 group-hover:scale-110 group-hover:bg-white/10 group-hover:text-white transition-all">
              <Code size={22} />
            </div>
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                Share Text & Code
                <ArrowRight size={14} className="opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </h3>
              <p className="text-xs text-white/40 leading-relaxed">
                Paste logs, snippets, or notes. The receiver reads it through the 5-digit code. <span className="text-white/60">Expires and vanishes</span> — never stored beyond the timer.
              </p>
            </div>
          </motion.div>

          {/* Card: Chat Room */}
          <motion.div
            initial={{ opacity: 0, y: 30, scale: 0.96 }}
            whileInView={{ opacity: 1, y: 0, scale: 1 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ type: "spring", stiffness: 80, damping: 15, delay: 0.2 }}
            whileHover={{ y: -6, scale: 1.01 }}
            onClick={() => { vibrate(); setActiveTab("chat"); }}
            className="group relative bg-zinc-950/70 border border-white/10 rounded-[32px] p-6 space-y-4 hover:border-white/20 transition-all cursor-pointer shadow-xl overflow-hidden text-left"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-white/[0.01] to-transparent pointer-events-none" />
            <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-white/70 group-hover:scale-110 group-hover:bg-white/10 group-hover:text-white transition-all">
              <MessageSquare size={22} />
            </div>
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                Live Chat Rooms
                <ArrowRight size={14} className="opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </h3>
              <p className="text-xs text-white/40 leading-relaxed">
                Spin up a temporary chat room with a 5-digit code. <span className="text-white/60">Real-time, anonymous</span>, and gone when the session ends. No account needed.
              </p>
            </div>
          </motion.div>

          {/* Card: Drawing Board */}
          <motion.div
            initial={{ opacity: 0, y: 30, scale: 0.96 }}
            whileInView={{ opacity: 1, y: 0, scale: 1 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ type: "spring", stiffness: 80, damping: 15, delay: 0.3 }}
            whileHover={{ y: -6, scale: 1.01 }}
            onClick={() => { vibrate(); setActiveTab("board"); }}
            className="group relative bg-zinc-950/70 border border-white/10 rounded-[32px] p-6 space-y-4 hover:border-white/20 transition-all cursor-pointer shadow-xl overflow-hidden text-left"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-white/[0.01] to-transparent pointer-events-none" />
            <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-white/70 group-hover:scale-110 group-hover:bg-white/10 group-hover:text-white transition-all">
              <Palette size={22} />
            </div>
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                Shared Board
                <ArrowRight size={14} className="opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </h3>
              <p className="text-xs text-white/40 leading-relaxed">
                Collaborate on a shared whiteboard in real-time. <span className="text-white/60">Session-based only</span> — strokes sync live and disappear with the session.
              </p>
            </div>
          </motion.div>
        </div>
      </div>

      {/* Guide Section */}
      <div className="space-y-8 max-w-[1200px] mx-auto px-4 w-full">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          className="text-center space-y-1"
        >
          <h2 className="text-xs uppercase tracking-[0.25em] font-bold text-white/30">
            How It Works
          </h2>
          <p className="text-xl font-bold tracking-tight text-white">
            Transfer in seconds, gone after it lands
          </p>
        </motion.div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 w-full">
          {[
            {
              step: "01",
              title: "Pick Transfer Type",
              desc: "File, text snippet, live chat, or whiteboard — choose what you need to transfer right now.",
              icon: Zap,
              color: "from-white/5 to-transparent",
              accent: "text-white/60"
            },
            {
              step: "02",
              title: "Set Expiry Timer",
              desc: "Pick how long before it auto-deletes. 10 minutes is enough for a quick handoff. Max 72 hours.",
              icon: Clock,
              color: "from-white/5 to-transparent",
              accent: "text-white/60"
            },
            {
              step: "03",
              title: "Share Access",
              desc: "Copy the unique 5-digit code or share the direct link with friends. No logins required to open.",
              icon: Link,
              color: "from-white/5 to-transparent",
              accent: "text-white/60"
            },
            {
              step: "04",
              title: "Self-Destruct",
              desc: "When the countdown expires, files, messages, and whiteboard tracks are completely deleted.",
              icon: Shield,
              color: "from-white/5 to-transparent",
              accent: "text-white/60"
            }
          ].map((item, idx) => {
            const Icon = item.icon;
            return (
              <motion.div
                key={idx}
                initial={{ opacity: 0, y: 30, scale: 0.95 }}
                whileInView={{ opacity: 1, y: 0, scale: 1 }}
                viewport={{ once: true, margin: "-80px" }}
                transition={{ type: "spring", stiffness: 80, damping: 15, delay: idx * 0.1 }}
                className="relative bg-zinc-950 border border-white/5 rounded-3xl p-5 space-y-3 overflow-hidden text-left"
              >
                <div className={`absolute inset-0 bg-gradient-to-b ${item.color} pointer-events-none`} />
                <div className="flex justify-between items-center relative z-10">
                  <div className={`w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center ${item.accent}`}>
                    <Icon size={16} />
                  </div>
                  <span className="text-xl font-black font-mono text-white/10 tracking-widest">{item.step}</span>
                </div>
                <div className="space-y-1 relative z-10">
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider">{item.title}</h4>
                  <p className="text-[11px] text-white/35 leading-relaxed">{item.desc}</p>
                </div>
              </motion.div>
            );
          })}
        </div>
      </div>

      {/* Landing FAQ Section */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
      >
        <FAQSection />
      </motion.div>

      {/* Bottom CTA Section */}
      <motion.div
        initial={{ opacity: 0, y: 40, scale: 0.96 }}
        whileInView={{ opacity: 1, y: 0, scale: 1 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ type: "spring", stiffness: 60, damping: 15 }}
        className="w-full max-w-[1200px] mx-auto px-6 sm:px-12 py-12 border border-white/10 bg-zinc-950/40 rounded-[32px] flex flex-col md:flex-row md:items-center md:justify-between gap-6 relative overflow-hidden backdrop-blur-md text-left"
      >
        <div className="absolute inset-0 bg-gradient-to-b from-white/[0.02] to-transparent pointer-events-none" />
        <div className="space-y-2 max-w-2xl relative z-10">
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Ready to drop some files?
          </h2>
          <p className="text-xs sm:text-sm text-white/40 leading-relaxed">
            No signups, no profiles, no permanent logs. Share files, text snippets, live chat rooms, or whiteboard collaborations instantly via a simple 5-digit code.
          </p>
        </div>
        <div className="shrink-0 relative z-10">
          <button
            onClick={() => { vibrate(); setActiveTab("file"); window.scrollTo({ top: 0, behavior: "smooth" }); }}
            className="px-8 py-4 bg-white text-black rounded-full font-bold text-xs hover:scale-105 active:scale-95 transition-all shadow-[0_0_30px_rgba(255,255,255,0.15)] flex items-center gap-2 cursor-pointer whitespace-nowrap"
          >
            Start Sharing Now <ArrowRight size={14} />
          </button>
        </div>
      </motion.div>

      {/* Separator, Like Widget, and Footer */}
      <div className="flex flex-col items-center w-full pt-8">
        <div className="w-full max-w-[500px] h-px bg-gradient-to-r from-transparent via-white/10 to-transparent my-12" />
        <LikeWidget showToast={showToast} likes={likesCount} />
        
        <div className="w-full max-w-[500px] h-px bg-gradient-to-r from-transparent via-white/10 to-transparent my-12" />
        <footer className="w-full max-w-[500px] text-center px-4 mb-12">
          <p className="text-white/30 text-[11px] leading-relaxed font-medium italic mb-4">
            Your instant web hub for quick file sharing, real-time messaging, and
            live collaborative tools. Fast, lightweight, and works on any device.
          </p>

          <p className="text-white/35 text-[10px] mb-4">
            Developed by <span className="text-white/60">{developer}</span>
          </p>

          {/* Policy Links */}
          <div className="flex items-center justify-center gap-4 text-[10px]">
            <button
              onClick={() => {
                vibrate();
                setShowPolicy("privacy");
              }}
              className="text-white/20 hover:text-white/50 transition-colors underline decoration-white/10 underline-offset-2 cursor-pointer"
            >
              Privacy Policy
            </button>
            <span className="text-white/10">|</span>
            <button
              onClick={() => {
                vibrate();
                setShowPolicy("terms");
              }}
              className="text-white/20 hover:text-white/50 transition-colors underline decoration-white/10 underline-offset-2 cursor-pointer"
            >
              Terms of Service
            </button>
          </div>

          <p className="text-white/15 text-[9px] mt-3">
            © 2026 Hefimer. All rights reserved.
          </p>
        </footer>
      </div>
    </motion.div>
  );
}

const LANDING_FEATURES = [
  {
    id: "file",
    eyebrow: "FILES",
    title: "Move the file, not the friction.",
    description: "Choose one or more files, set an expiry, and share the five-digit access code.",
    accent: "cyan",
    index: "01",
  },
  {
    id: "text",
    eyebrow: "TEXT + CODE",
    title: "Pass the exact words along.",
    description: "Paste text or code, choose how long it remains available, and share access.",
    accent: "violet",
    index: "02",
  },
  {
    id: "chat",
    eyebrow: "LIVE CHAT",
    title: "A room for right now.",
    description: "Create or join a temporary real-time chat room with a five-digit code.",
    accent: "coral",
    index: "03",
  },
  {
    id: "board",
    eyebrow: "SHARED BOARD",
    title: "Draw in the same moment.",
    description: "Open a live, session-based canvas and draw together from different devices.",
    accent: "amber",
    index: "04",
  },
] as const;

function LandingPage({ setActiveTab, onOpenHistory, likesCount, showToast, setShowPolicy, developer }: any) {
  const openFeature = (feature: "file" | "text" | "chat" | "board") => {
    vibrate();
    setActiveTab(feature);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.7 }}
      className="hefimer-landing w-full"
    >
      <section className="hefimer-hero" aria-labelledby="hefimer-hero-title">
        <div className="hefimer-hero-aurora" aria-hidden="true">
          <span />
          <span />
          <span />
          <span />
        </div>

        <motion.div
          className="hefimer-hero-brand"
          initial={{ opacity: 0, y: -18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        >
          <motion.img
            src="/hefimer-orbit-clean.svg"
            alt="Hefimer"
            draggable={false}
            animate={{ rotate: 360 }}
            transition={{ duration: 24, repeat: Infinity, ease: "linear" }}
          />
          <span>HEFIMER</span>
          <i />
          <small>TEMPORARY SHARING</small>
        </motion.div>

        <div className="hefimer-hero-layout">
          <div className="hefimer-hero-copy">
            <motion.p
              className="hefimer-hero-kicker hefimer-mono"
              initial={{ opacity: 0, x: -24 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.18, duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
            >
              SEND → CODE → RECEIVE → DISAPPEAR
            </motion.p>

            <h1 id="hefimer-hero-title" className="hefimer-hero-title">
              {["Send it.", "Share five digits.", "Let time clear it."].map((line, index) => (
                <span className={index === 1 ? "hefimer-title-spectrum" : ""} key={line}>
                  <motion.i
                    initial={{ y: "115%", rotate: 3 }}
                    animate={{ y: 0, rotate: 0 }}
                    transition={{
                      delay: 0.16 + index * 0.12,
                      duration: 0.9,
                      ease: [0.16, 1, 0.3, 1],
                    }}
                  >
                    {line}
                  </motion.i>
                </span>
              ))}
            </h1>

            <motion.p
              className="hefimer-hero-description"
              initial={{ opacity: 0, y: 24 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.55, duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
            >
              Share files, text, temporary chat rooms, and collaborative boards through a simple five-digit code. No account required.
            </motion.p>

            <motion.div
              className="hefimer-hero-actions"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.68, duration: 0.7 }}
            >
              <button className="hefimer-primary-action" onClick={() => openFeature("file")}>
                <span>Start sharing</span>
                <ArrowRight size={17} />
              </button>
              <button
                className="hefimer-secondary-action"
                onClick={() => {
                  vibrate();
                  onOpenHistory();
                }}
              >
                <Clock size={16} />
                <span>History</span>
              </button>
            </motion.div>
          </div>

          <motion.div
            className="hefimer-hero-sculpture"
            initial={{ opacity: 0, scale: 0.78, rotateY: -22 }}
            animate={{ opacity: 1, scale: 1, rotateY: -8 }}
            transition={{ delay: 0.28, duration: 1.2, ease: [0.16, 1, 0.3, 1] }}
            aria-hidden="true"
          >
            <div className="hefimer-sculpture-shell">
              <span className="hefimer-sculpture-ring hefimer-sculpture-ring--one" />
              <span className="hefimer-sculpture-ring hefimer-sculpture-ring--two" />
              <span className="hefimer-sculpture-ring hefimer-sculpture-ring--three" />
              <motion.img
                src="/hefimer-orbit-clean.svg"
                alt=""
                draggable={false}
                animate={{ rotate: 360 }}
                transition={{ duration: 18, repeat: Infinity, ease: "linear" }}
              />
              <span className="hefimer-sculpture-code hefimer-mono">
                <i /> <i /> <i /> <i /> <i />
              </span>
              <span className="hefimer-sculpture-node hefimer-sculpture-node--file">FILE</span>
              <span className="hefimer-sculpture-node hefimer-sculpture-node--text">TEXT</span>
              <span className="hefimer-sculpture-node hefimer-sculpture-node--chat">CHAT</span>
              <span className="hefimer-sculpture-node hefimer-sculpture-node--board">BOARD</span>
            </div>
          </motion.div>
        </div>

        <motion.div
          className="hefimer-hero-index hefimer-mono"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.9 }}
        >
          <span>FILE</span><i /><span>TEXT</span><i /><span>CHAT</span><i /><span>BOARD</span>
        </motion.div>
      </section>

      <section className="hefimer-journey" aria-labelledby="hefimer-journey-title">
        <motion.div
          className="hefimer-section-intro"
          initial={{ opacity: 0, y: 28 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.5 }}
          transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
        >
          <span className="hefimer-mono">THE TRANSFER</span>
          <h2 id="hefimer-journey-title">One short journey.<br />Nothing permanent.</h2>
          <p>Scroll through the same four moments every Hefimer share follows.</p>
        </motion.div>
        <TransferTunnel />
      </section>

      <section className="hefimer-stations" aria-labelledby="hefimer-stations-title">
        <motion.div
          className="hefimer-section-intro hefimer-section-intro--wide"
          initial={{ opacity: 0, y: 28 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.5 }}
          transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
        >
          <span className="hefimer-mono">FOUR WAYS IN</span>
          <h2 id="hefimer-stations-title">Different things.<br />The same simple code.</h2>
        </motion.div>

        <div className="hefimer-station-grid">
          {LANDING_FEATURES.map((feature, index) => (
            <motion.button
              key={feature.id}
              className={`hefimer-station-card hefimer-station-card--${feature.accent}`}
              initial={{ opacity: 0, y: 46, clipPath: "inset(12% 0 0 0 round 28px)" }}
              whileInView={{ opacity: 1, y: 0, clipPath: "inset(0% 0 0 0 round 28px)" }}
              viewport={{ once: true, margin: "-80px" }}
              transition={{ delay: index * 0.08, duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
              whileHover={{ y: -10, rotateX: 2, rotateY: index % 2 === 0 ? 2 : -2 }}
              whileTap={{ scale: 0.985 }}
              onClick={() => openFeature(feature.id)}
            >
              <div className="hefimer-station-meta hefimer-mono">
                <span>{feature.eyebrow}</span>
                <span>{feature.index}</span>
              </div>
              <FeatureStationVisual kind={feature.id} />
              <div className="hefimer-station-copy">
                <h3>{feature.title}</h3>
                <p>{feature.description}</p>
                <span className="hefimer-station-link">Open {feature.eyebrow.toLowerCase()} <ArrowRight size={14} /></span>
              </div>
            </motion.button>
          ))}
        </div>
      </section>

      <PairedDeviceConstellation
        onOpenDevices={() => window.dispatchEvent(new Event("hefimer:open-devices"))}
      />

      <motion.div
        className="hefimer-faq-wrap"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{ duration: 0.7 }}
      >
        <FAQSection variant="landing" />
      </motion.div>

      <div className="hefimer-landing-footer">
        <LikeWidget showToast={showToast} likes={likesCount} variant="landing" />

        <motion.section
          className="hefimer-terminal-cta"
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.9, ease: [0.16, 1, 0.3, 1] }}
          aria-labelledby="hefimer-terminal-title"
        >
          <div className="hefimer-terminal-spectrum" aria-hidden="true"><i /><i /><i /><i /></div>
          <div className="hefimer-terminal-radar" aria-hidden="true">
            <i /><i /><i /><i />
            <span className="hefimer-terminal-packet hefimer-terminal-packet--file">
              <FileUp size={15} />
              <small>FILE</small>
            </span>
            <span className="hefimer-terminal-packet hefimer-terminal-packet--code">
              <b /><b /><b /><b /><b />
              <small>CODE</small>
            </span>
            <motion.img
              src="/hefimer-orbit-clean.svg"
              alt=""
              draggable={false}
              animate={{ rotate: 360 }}
              transition={{ duration: 28, repeat: Infinity, ease: "linear" }}
            />
          </div>
          <div className="hefimer-terminal-copy">
            <span className="hefimer-mono">TRANSFER CHAMBER / READY</span>
            <h2 id="hefimer-terminal-title">Make it move.<br /><em>Then let go.</em></h2>
            <p>Send a file now, or enter the five-digit code waiting for you.</p>
          </div>
          <div className="hefimer-terminal-actions">
            <button className="hefimer-terminal-send" onClick={() => openFeature("file")}>
              <span><FileUp size={18} /> Send a file</span>
              <ArrowRight size={18} />
            </button>
            <button
              className="hefimer-terminal-receive"
              onClick={() => {
                vibrate();
                setActiveTab("receive");
                window.scrollTo({ top: 0, behavior: "smooth" });
              }}
            >
              <span><Hash size={18} /> Enter a code</span>
              <ArrowRight size={18} />
            </button>
          </div>
          <div className="hefimer-terminal-status hefimer-mono">
            <span><i /> NO ACCOUNT REQUIRED</span>
            <span>FILE - CODE - RECEIVE - EXPIRE</span>
          </div>
        </motion.section>

        <footer>
          <div className="hefimer-footer-brand">
            <img src="/hefimer-orbit-clean.svg" alt="" draggable={false} />
            <span>HEFIMER</span>
          </div>
          <p>Temporary sharing for files, text, live chat, and collaborative boards.</p>
          <p>Developed by <strong>{developer}</strong></p>
          <div className="hefimer-footer-links">
            <button onClick={() => { vibrate(); setShowPolicy("privacy"); }}>Privacy Policy</button>
            <button onClick={() => { vibrate(); setShowPolicy("terms"); }}>Terms of Service</button>
          </div>
          <small>© 2026 Hefimer. All rights reserved.</small>
        </footer>
      </div>
    </motion.div>
  );
}

function FAQSection({ variant = "default" }: { variant?: "default" | "landing" } = {}) {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  if (variant === "landing") {
    return (
      <section className="hefimer-faq-console" aria-labelledby="hefimer-faq-title">
        <div className="hefimer-faq-heading">
          <span className="hefimer-mono">OPEN CHANNEL / FAQ</span>
          <h2 id="hefimer-faq-title">A few things<br />before you send.</h2>
          <p>Short answers about codes, expiration, privacy, and the ways Hefimer can be used.</p>
        </div>

        <div className="hefimer-faq-list">
          {FAQ_ITEMS.map((item, i) => {
            const isOpen = openIndex === i;
            return (
              <motion.article
                key={item.q}
                className={`hefimer-faq-channel${isOpen ? " is-open" : ""}`}
                initial={{ opacity: 0, x: 30 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true, margin: "-45px" }}
                transition={{ duration: 0.6, delay: Math.min(i * 0.045, 0.3), ease: [0.16, 1, 0.3, 1] }}
              >
                <button
                  onClick={() => {
                    vibrate();
                    setOpenIndex(isOpen ? null : i);
                  }}
                  aria-expanded={isOpen}
                >
                  <span className="hefimer-faq-number hefimer-mono">F{String(i + 1).padStart(2, "0")}</span>
                  <span className="hefimer-faq-question">{item.q}</span>
                  <span className="hefimer-faq-toggle"><ChevronDown size={18} /></span>
                </button>
                <AnimatePresence initial={false}>
                  {isOpen && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.34, ease: [0.16, 1, 0.3, 1] }}
                      className="hefimer-faq-answer"
                    >
                      <p>{item.a}</p>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.article>
            );
          })}
        </div>
      </section>
    );
  }

  return (
    <div className="w-full px-4 mb-4">
      <div className="max-w-[1200px] mx-auto">
        <motion.h2
          initial={{ opacity: 0, y: 15 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-50px" }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          className="text-white/90 text-lg font-bold mb-6 tracking-wide"
          style={{ fontFamily: "monospace" }}
        >
          FAQ
        </motion.h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
          {FAQ_ITEMS.map((item, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-50px" }}
              transition={{ duration: 0.6, delay: i * 0.05, ease: [0.16, 1, 0.3, 1] }}
              className="border-t border-white/[0.06]"
            >
              <button
                onClick={() => {
                  vibrate();
                  setOpenIndex(openIndex === i ? null : i);
                }}
                className="w-full flex items-start gap-3 py-4 text-left group"
              >
                <span className="text-white/30 group-hover:text-white/50 transition-colors mt-0.5 text-sm shrink-0">
                  {openIndex === i ? "−" : "+"}
                </span>
                <span className="text-white/70 text-[13px] leading-relaxed group-hover:text-white/90 transition-colors">
                  {item.q}
                </span>
              </button>
              <AnimatePresence>
                {openIndex === i && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.25, ease: "easeInOut" }}
                    className="overflow-hidden"
                  >
                    <p className="text-white/40 text-[12px] leading-relaxed pl-6 pb-4 pr-4">
                      {item.a}
                    </p>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [activeTab, setActiveTab] = useState<
    "home" | "text" | "file" | "receive" | "history" | "chat" | "board" | "space"
  >(() => {
    const path = window.location.pathname;
    if (path === "/chat-room") return "chat";
    if (path === "/board") return "board";
    if (path === "/space") return "space";
    const params = new URLSearchParams(window.location.search);
    if (params.get("code")) return "file";
    // Show landing page only on the very first visit
    const hasVisited = localStorage.getItem("hefimer_visited");
    if (!hasVisited) {
      localStorage.setItem("hefimer_visited", "1");
      return "home";
    }
    return "file";
  });

  // Sync activeTab state with history entries for browser navigation
  useEffect(() => {
    const currentPath = window.location.pathname;
    const currentState = window.history.state;

    if (activeTab === "chat") {
      if (currentPath !== "/chat-room") {
        window.history.pushState({ tab: "chat" }, "", "/chat-room");
      }
    } else if (activeTab === "board") {
      if (currentPath !== "/board") {
        window.history.pushState({ tab: "board" }, "", "/board");
      }
    } else if (activeTab === "space") {
      if (currentPath !== "/space") {
        window.history.pushState({ tab: "space" }, "", "/space");
      }
    } else {
      // Path is "/"
      if (currentPath !== "/") {
        window.history.pushState({ tab: activeTab }, "", "/");
      } else if (!currentState || currentState.tab !== activeTab) {
        window.history.pushState({ tab: activeTab }, "", "/" + window.location.search);
      }
    }
  }, [activeTab]);

  // Handle browser back/forward buttons
  useEffect(() => {
    const handlePopState = (event: PopStateEvent) => {
      const path = window.location.pathname;
      if (path === "/chat-room") {
        setActiveTab("chat");
      } else if (path === "/board") {
        setActiveTab("board");
      } else if (path === "/space") {
        setActiveTab("space");
      } else {
        if (event.state && event.state.tab) {
          setActiveTab(event.state.tab);
        } else {
          setActiveTab("home");
        }
      }
    };
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);
  const [chatTabClicks, setChatTabClicks] = useState(0);
  const [logoClicks, setLogoClicks] = useState(0);
  const logoClickTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [showAdminPass, setShowAdminPass] = useState(false);
  const [adminPass, setAdminPass] = useState("");
  const [isAdmin, setIsAdmin] = useState(false);
  const [showPolicy, setShowPolicy] = useState<"privacy" | "terms" | null>(
    null,
  );
  const [globalDeveloper, setGlobalDeveloper] = useState("Khanh Minh");

  // Clean up legacy light theme and force dark mode
  useEffect(() => {
    document.documentElement.classList.remove("light-theme");
    localStorage.removeItem("themeMode");
    
    // Disable automatic browser scroll restoration on refresh/load
    if ("scrollRestoration" in window.history) {
      window.history.scrollRestoration = "manual";
    }
  }, []);

  // Reset scroll to top on tab changes to avoid layout displacement
  useEffect(() => {
    window.scrollTo(0, 0);
    const timer = setTimeout(() => {
      window.scrollTo(0, 0);
    }, 50);
    return () => clearTimeout(timer);
  }, [activeTab]);

  // R2 Secret Mode States
  const [isR2Mode, setIsR2Mode] = useState(false);
  const [showR2Auth, setShowR2Auth] = useState(false);
  const [r2AuthPass, setR2AuthPass] = useState("");
  const [r2TabClicks, setR2TabClicks] = useState(0);
  const [chatState, setChatState] = useState<{
    roomId: string;
    roomName: string;
    roomNameInput: string;
    roomExpire: string;
    userName: string;
    inRoom: boolean;
    showCreatedModal: boolean;
  }>({
    roomId: "",
    roomName: "",
    roomNameInput: "",
    roomExpire: "1h",
    userName: "",
    inRoom: false,
    showCreatedModal: false,
  });

  const [boardState, setBoardState] = useState<{
    boardId: string;
    boardName: string;
    boardNameInput: string;
    boardExpire: string;
    userName: string;
    inBoard: boolean;
    showCreatedModal: boolean;
    boardMode: "create" | "join";
  }>({
    boardId: "",
    boardName: "",
    boardNameInput: "",
    boardExpire: "1h",
    userName: "",
    inBoard: false,
    showCreatedModal: false,
    boardMode: "create",
  });

  const [spaceState, setSpaceState] = useState<{
    spaceId: string;
    spaceName: string;
    spaceNameInput: string;
    spaceExpire: string;
    userName: string;
    inSpace: boolean;
    showCreatedModal: boolean;
    spaceMode: "create" | "join";
  }>({
    spaceId: "",
    spaceName: "",
    spaceNameInput: "",
    spaceExpire: "1h",
    userName: "",
    inSpace: false,
    showCreatedModal: false,
    spaceMode: "create",
  });

  const [globalNotice, setGlobalNotice] = useState("");
  const [globalNoticeEnabled, setGlobalNoticeEnabled] = useState(false);
  const [noticeTheme, setNoticeTheme] = useState("blue");
  const [noticeExpiresAt, setNoticeExpiresAt] = useState(0);
  const [activeBroadcast, setActiveBroadcast] = useState<any>(null);
  const lastBroadcastId = useRef<string | null>(null);
  const [isFrozen, setIsFrozen] = useState(false);
  const [likesCount, setLikesCount] = useState(0);
  const [isNoticeDismissed, setIsNoticeDismissed] = useState(false);
  const [adminMasterPass, setAdminMasterPass] = useState("meobinh95");
  const [secretMasterPass, setSecretMasterPass] = useState("meobinh905");

  useEffect(() => {
    setIsNoticeDismissed(false);
  }, [globalNotice]);

  const [onlineCount, setOnlineCount] = useState(0);

  useEffect(() => {
    // heartbeaT Presence System
    const connectedRef = ref(rtdb, ".info/connected");
    const presenceRef = ref(rtdb, "presence");

    // Tạo Session ID cố định cho tab này
    let sessionId = sessionStorage.getItem("presence_session_id");
    if (!sessionId) {
      sessionId = "sess_" + Math.random().toString(36).substring(2, 15);
      sessionStorage.setItem("presence_session_id", sessionId);
    }

    const myPresenceRef = ref(rtdb, `presence/${sessionId}`);

    // Hàm cập nhật trạng thái (Heartbeat)
    const updateHeartbeat = () => {
      set(myPresenceRef, {
        lastSeen: Date.now(),
        id: sessionId,
      });
    };

    const handleConnectedChange = (snap: any) => {
      if (snap.val() === true) {
        onDisconnect(myPresenceRef).remove();
        updateHeartbeat();
      }
    };

    const handlePresenceChange = (snap: any) => {
      if (snap.exists()) {
        const data = snap.val();
        const now = Date.now();
        // CHỈ đếm những người có tương tác trong vòng 2 phút qua
        // Điều này giúp loại bỏ hoàn toàn các bản ghi "ma" do crash hoặc treo máy
        const activeUsers = Object.values(data).filter((user: any) => {
          return user && user.lastSeen && now - user.lastSeen < 120000;
        });
        setOnlineCount(activeUsers.length || 1); // Luôn ít nhất là 1 nếu mình đang xem
      } else {
        setOnlineCount(1);
      }
    };

    const connectedUnsubscribe = onValue(connectedRef, handleConnectedChange);
    const presenceUnsubscribe = onValue(presenceRef, handlePresenceChange);

    // Gửi heartbeat mỗi 30s để duy trì trạng thái "Online"
    const heartbeatInterval = setInterval(updateHeartbeat, 30000);

    const cleanup = () => {
      clearInterval(heartbeatInterval);
      remove(myPresenceRef);
    };

    window.addEventListener("beforeunload", cleanup);

    return () => {
      clearInterval(heartbeatInterval);
      presenceUnsubscribe();
      window.removeEventListener("beforeunload", cleanup);
      cleanup();
    };
  }, []);

  // Timer for auto-expiry of notice
  useEffect(() => {
    if (noticeExpiresAt === 0) return;

    const checkExpiry = () => {
      if (Date.now() >= noticeExpiresAt) {
        setGlobalNotice("");
      }
    };

    const timer = setInterval(checkExpiry, 1000);
    return () => clearInterval(timer);
  }, [noticeExpiresAt]);
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const codeParam = params.get("code");
    if (codeParam) {
      const code = codeParam.replace(/\D/g, "").slice(0, 5);
      if (code.length === 5) {
        const checkType = async () => {
          try {
            // Check drops
            const dropSnapshot = await get(child(ref(rtdb), `drops/${code}`));
            if (dropSnapshot.exists()) {
              setActiveTab("receive");
              setReceiveState((prev) => ({ ...prev, code }));
              return;
            }

            // Check rooms
            const roomSnapshot = await get(child(ref(rtdb), `chats/${code}`));
            if (roomSnapshot.exists()) {
              setActiveTab("chat");
              setChatState((prev) => ({ ...prev, roomId: code }));
              return;
            }

            // Check boards
            const boardSnapshot = await get(child(ref(rtdb), `boards/${code}`));
            if (boardSnapshot.exists()) {
              setActiveTab("board");
              setBoardState((prev) => ({
                ...prev,
                boardId: code,
                boardMode: "join",
              }));
              return;
            }

            // Default to receive tab if not found anywhere
            setActiveTab("receive");
            setReceiveState((prev) => ({ ...prev, code }));
          } catch (e) {
            console.error("Error checking code type:", e);
            setActiveTab("receive");
            setReceiveState((prev) => ({ ...prev, code }));
          }
        };
        checkType();
        // Clean URL
        window.history.replaceState(
          {},
          document.title,
          window.location.pathname,
        );
      }
    }
  }, []);

  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error" | "info";
  } | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [r2Stats, setR2Stats] = useState<{
    firstUploadTime: number;
    totalSize: number;
    files: { name: string; size: number; timestamp: number }[];
  }>({ firstUploadTime: 0, totalSize: 0, files: [] });
  const [standardStats, setStandardStats] = useState<{
    firstUploadTime: number;
    totalSize: number;
  }>({ firstUploadTime: 0, totalSize: 0 });

  const updateUploadStats = (
    fileName: string,
    fileSize: number,
    isR2: boolean,
  ) => {
    const now = Date.now();
    if (isR2) {
      setR2Stats((prev) => {
        let newStats = { ...prev };
        // Check if 24h passed
        if (
          prev.firstUploadTime === 0 ||
          now - prev.firstUploadTime > 24 * 60 * 60 * 1000
        ) {
          newStats = {
            firstUploadTime: now,
            totalSize: fileSize,
            files: [{ name: fileName, size: fileSize, timestamp: now }],
          };
        } else {
          newStats = {
            ...prev,
            totalSize: prev.totalSize + fileSize,
            files: [
              ...prev.files,
              { name: fileName, size: fileSize, timestamp: now },
            ],
          };
        }
        localStorage.setItem("hefimer_r2_stats", JSON.stringify(newStats));
        return newStats;
      });
    } else {
      setStandardStats((prev) => {
        let newStats = { ...prev };
        // Check if 24h passed
        if (
          prev.firstUploadTime === 0 ||
          now - prev.firstUploadTime > 24 * 60 * 60 * 1000
        ) {
          newStats = { firstUploadTime: now, totalSize: fileSize };
        } else {
          newStats = { ...prev, totalSize: prev.totalSize + fileSize };
        }
        localStorage.setItem(
          "hefimer_standard_stats",
          JSON.stringify(newStats),
        );
        return newStats;
      });
    }
  };

  const addToHistory = (item: Omit<HistoryItem, "createdAt">) => {
    const newItem = { ...item, createdAt: Date.now() };
    setHistory((prev) => {
      // Avoid duplicates for same code and action
      const exists = prev.find(
        (h) => h.code === item.code && h.action === item.action,
      );
      if (exists) return prev;

      const updated = [newItem, ...prev].slice(0, 20);
      localStorage.setItem("hefimer_history", JSON.stringify(updated));
      return updated;
    });
  };

  useEffect(() => {
    (window as any).addToHistory = addToHistory;
  }, [history]);

  // Automatic Cleanup of Expired Drops
  useEffect(() => {
    const performCleanup = async () => {
      try {
        const now = Date.now();
        const expiredQuery = query(
          ref(rtdb, "drops"),
          orderByChild("expiresAt"),
          endAt(now)
        );
        const snapshot = await get(expiredQuery);
        if (snapshot.exists()) {
          const expiredData = snapshot.val();
          console.log("Found expired drops for cleanup:", Object.keys(expiredData));
          
          for (const [code, data] of Object.entries(expiredData) as [string, any][]) {
            console.log(`Cleaning up expired drop: ${code}`);
            
            // 1. Delete from R2 if it is a file and has an objectKey
            let r2DeleteSuccess = true;
            if (data.objectKey) {
              try {
                const apiUrl = "/api/r2/delete";

                const res = await fetch(apiUrl, {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                  },
                  body: JSON.stringify({ objectKey: data.objectKey }),
                });

                if (!res.ok) {
                  const errData = await res.json().catch(() => ({}));
                  throw new Error(errData.error || `Server returned ${res.status}`);
                }

                console.log(`Deleted R2 object for expired drop ${code}: ${data.objectKey}`);
              } catch (r2Err) {
                console.error(`Failed to delete R2 object for ${code}:`, r2Err);
                r2DeleteSuccess = false;
              }
            }
            
            // 2. Delete from Firebase only if R2 deletion was successful (or skipped)
            if (r2DeleteSuccess) {
              await remove(ref(rtdb, `drops/${code}`));
              console.log(`Deleted Firebase node for expired drop ${code}`);
            }
          }
        }
      } catch (err) {
        console.error("Expired drops cleanup failed:", err);
      }
    };

    performCleanup();
    const interval = setInterval(performCleanup, 60000);
    return () => clearInterval(interval);
  }, []);

  const handleLogoClick = () => {
    vibrate();
    
    if (logoClickTimeoutRef.current) {
      clearTimeout(logoClickTimeoutRef.current);
    }
    
    logoClickTimeoutRef.current = setTimeout(() => {
      setLogoClicks(0);
    }, 2000);
    
    const newCount = logoClicks + 1;
    setLogoClicks(newCount);
    
    if (newCount >= 5) {
      if (isAdmin) {
        showToast("Admin mode is already active", "info");
      } else {
        setShowAdminPass(true);
      }
      setLogoClicks(0);
      if (logoClickTimeoutRef.current) {
        clearTimeout(logoClickTimeoutRef.current);
        logoClickTimeoutRef.current = null;
      }
    }
  };

  const handleChatTabClick = () => {
    vibrate();
    if (activeTab === "chat") {
      const newCount = chatTabClicks + 1;
      setChatTabClicks(newCount);
      if (newCount >= 5) {
        if (isAdmin) {
          // If already admin, just stay on chat tab (AdminPanel is already showing)
          setChatTabClicks(0);
        } else {
          setShowAdminPass(true);
          setChatTabClicks(0);
        }
      }
    } else {
      setActiveTab("chat");
      setChatTabClicks(1);
    }
  };

  const handleAdminAuth = (e: React.FormEvent) => {
    e.preventDefault();
    if (adminPass === adminMasterPass) {
      setIsAdmin(true);
      setShowAdminPass(false);
      setAdminPass("");
      showToast("Admin mode activated", "success");
    } else {
      showToast("Incorrect password", "error");
      setAdminPass("");
    }
  };

  const handleR2Auth = (e: React.FormEvent) => {
    e.preventDefault();
    if (r2AuthPass === secretMasterPass) {
      setIsR2Mode(true);
      setShowR2Auth(false);
      setR2AuthPass("");
      showToast("Secret mode activated", "success");
    } else {
      showToast("Incorrect password", "error");
      setR2AuthPass("");
    }
  };

  // Lifted states
  const [sendFileState, setSendFileState] = useState<{
    file: File | null;
    resultCode: string;
    expiresAt: number | null;
  }>({ file: null, resultCode: "", expiresAt: null });

  const [sendTextState, setSendTextState] = useState<{
    text: string;
    expire: string;
    resultCode: string;
    expiresAt: number | null;
  }>({ text: "", expire: "1h", resultCode: "", expiresAt: null });

  const [receiveState, setReceiveState] = useState<{
    code: string;
    result: any;
  }>({ code: "", result: null });

  const handleAutoDownload = async (transfer: DeviceTransfer) => {
    const payload = transfer.payload as any;
    const entries = Array.isArray(payload?.files) && payload.files.length ? payload.files : [payload];
    for (const entry of entries) {
      const url = [entry?.fileUrl, entry?.url, entry?.raw_url, entry?.downloadUrl]
        .find((value) => typeof value === "string" && /^https:\/\//i.test(value));
      if (!url) throw new Error(`The download link for ${entry?.fileName || transfer.name} is unavailable`);
      await downloadFileInBrowser(url, entry?.fileName || transfer.name || "download");
      if (entries.length > 1) await new Promise((resolve) => window.setTimeout(resolve, 350));
    }
  };

  useEffect(() => {
    const saved = localStorage.getItem("hefimer_history");
    if (saved) {
      try {
        setHistory(JSON.parse(saved));
      } catch (e) {}
    }

    const savedR2Stats = localStorage.getItem("hefimer_r2_stats");
    if (savedR2Stats) {
      try {
        const parsed = JSON.parse(savedR2Stats);
        // Check if 24h passed
        if (
          parsed.firstUploadTime !== 0 &&
          Date.now() - parsed.firstUploadTime > 24 * 60 * 60 * 1000
        ) {
          setR2Stats({ firstUploadTime: 0, totalSize: 0, files: [] });
          localStorage.removeItem("hefimer_r2_stats");
        } else {
          setR2Stats(parsed);
        }
      } catch (e) {}
    }

    const savedStandardStats = localStorage.getItem("hefimer_standard_stats");
    if (savedStandardStats) {
      try {
        const parsed = JSON.parse(savedStandardStats);
        // Check if 24h passed
        if (
          parsed.firstUploadTime !== 0 &&
          Date.now() - parsed.firstUploadTime > 24 * 60 * 60 * 1000
        ) {
          setStandardStats({ firstUploadTime: 0, totalSize: 0 });
          localStorage.removeItem("hefimer_standard_stats");
        } else {
          setStandardStats(parsed);
        }
      } catch (e) {}
    }

    // Global Stats & Freeze
    const statsRef = ref(rtdb, "stats");
    const unsubStats = onValue(statsRef, (snapshot) => {
      const data = snapshot.val();
      if (data) {
        setGlobalNotice(data.notice || "");
        setGlobalNoticeEnabled(data.noticeEnabled || false);
        setGlobalDeveloper(data.developer || "Khanh Minh");
        setNoticeTheme(data.noticeTheme || "blue");
        setNoticeExpiresAt(data.noticeExpiresAt || 0);
        setIsFrozen(data.isFrozen || false);
        setLikesCount(data.likes || 0);
        if (data.passwords) {
          if (data.passwords.admin) setAdminMasterPass(data.passwords.admin);
          if (data.passwords.secret) setSecretMasterPass(data.passwords.secret);
        }
        if (data.broadcast && data.broadcast.id !== lastBroadcastId.current) {
          const isExpired =
            data.broadcast.expiresAt !== 0 &&
            Date.now() >= data.broadcast.expiresAt;
          if (!isExpired) {
            setActiveBroadcast(data.broadcast);
            lastBroadcastId.current = data.broadcast.id;
          }
        } else if (!data.broadcast) {
          setActiveBroadcast(null);
          lastBroadcastId.current = null;
        }
      }
    });

    // Timer for auto-expiry of notice
    const broadcastTimer = setInterval(() => {
      if (
        activeBroadcast &&
        activeBroadcast.expiresAt !== 0 &&
        Date.now() >= activeBroadcast.expiresAt
      ) {
        setActiveBroadcast(null);
      }
    }, 1000);

    // Presence Tracking
    const connectedRef = ref(rtdb, ".info/connected");
    const unsubConnected = onValue(connectedRef, (snap) => {
      if (snap.val() === true) {
        const myPresenceRef = push(ref(rtdb, "presence"));
        onDisconnect(myPresenceRef).remove();
        set(myPresenceRef, { lastActive: serverTimestamp() });
      }
    });

    return () => {
      off(statsRef);
      off(connectedRef);
      clearInterval(broadcastTimer);
    };
  }, []);

  const showToast = (
    message: string,
    type: "success" | "error" | "info" = "info",
  ) => {
    setToast({ message, type });
    if (type !== "info") {
      setTimeout(() => setToast(null), 3000);
    }
  };

  const isNoticeActive =
    globalNotice &&
    globalNoticeEnabled &&
    (noticeExpiresAt === 0 || Date.now() < noticeExpiresAt) &&
    !isNoticeDismissed;
  const isDedicatedToolScreen =
    activeTab === "chat" || activeTab === "board" || activeTab === "space" || activeTab === "home";

  const NOTICE_THEMES: Record<string, string> = {
    blue: "bg-blue-600/90",
    red: "bg-red-600/90",
    green: "bg-green-600/90",
    yellow: "bg-yellow-600/90",
    purple: "bg-purple-600/90",
    pink: "bg-pink-600/90",
    orange: "bg-orange-600/90",
    dark: "bg-zinc-900/90",
  };

  const BROADCAST_THEMES: Record<
    string,
    { bg: string; accent: string; text: string; iconBg: string; btn: string }
  > = {
    blue: {
      bg: "bg-blue-700",
      accent: "text-white",
      text: "text-white",
      iconBg: "bg-white/20",
      btn: "bg-white text-blue-700",
    },
    red: {
      bg: "bg-red-700",
      accent: "text-white",
      text: "text-white",
      iconBg: "bg-white/20",
      btn: "bg-white text-red-700",
    },
    green: {
      bg: "bg-green-700",
      accent: "text-white",
      text: "text-white",
      iconBg: "bg-white/20",
      btn: "bg-white text-green-700",
    },
    yellow: {
      bg: "bg-yellow-400",
      accent: "text-black",
      text: "text-black",
      iconBg: "bg-black/10",
      btn: "bg-black text-yellow-500",
    },
    purple: {
      bg: "bg-purple-700",
      accent: "text-white",
      text: "text-white",
      iconBg: "bg-white/20",
      btn: "bg-white text-purple-700",
    },
    pink: {
      bg: "bg-pink-700",
      accent: "text-white",
      text: "text-white",
      iconBg: "bg-white/20",
      btn: "bg-white text-pink-700",
    },
    orange: {
      bg: "bg-orange-700",
      accent: "text-white",
      text: "text-white",
      iconBg: "bg-white/20",
      btn: "bg-white text-orange-700",
    },
    dark: {
      bg: "bg-zinc-900",
      accent: "text-white",
      text: "text-white/90",
      iconBg: "bg-white/5",
      btn: "bg-white text-black",
    },
  };

  const BROADCAST_ICONS: Record<string, any> = {
    Zap,
    Bell,
    ShieldAlert,
    Flame,
    Star,
    Radio,
  };

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-start overflow-x-clip bg-[#000000] p-4 text-white selection:bg-white/30 sm:p-6">
      {/* Floating Actions Dock */}
      <div className="fixed top-4 right-4 z-50 flex items-center gap-2">
        <button
          onClick={() => {
            vibrate();
            setIsHistoryModalOpen(true);
          }}
          className="flex w-9 h-9 sm:w-10 sm:h-10 items-center justify-center rounded-full border border-white/10 bg-black/45 text-white/70 backdrop-blur-md transition-all hover:bg-white/10 hover:text-white active:scale-95 shadow-lg cursor-pointer select-none"
          title="History"
          style={{ WebkitTapHighlightColor: "transparent" }}
        >
          <Clock size={16} />
        </button>
      </div>

      <DeviceHub
        showToast={showToast}
        onAutoDownload={handleAutoDownload}
        onOpenDrop={(code) => {
          setReceiveState({ code, result: null });
          setActiveTab("receive");
        }}
      />

      {/* Admin Password Override Modal */}
      <AnimatePresence>
        {showAdminPass && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md"
            onClick={() => {
              setShowAdminPass(false);
              setAdminPass("");
            }}
          >
            <motion.form
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              onSubmit={handleAdminAuth}
              className="w-full max-w-sm bg-zinc-950/80 border border-white/10 rounded-[32px] p-8 shadow-2xl relative flex flex-col gap-5"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex flex-col items-center gap-2 mb-1">
                <div className="w-12 h-12 rounded-full bg-white/5 border border-white/10 flex items-center justify-center">
                  <ShieldAlert size={22} className="text-white/60 animate-pulse" />
                </div>
                <h3 className="font-bold uppercase tracking-[0.2em] text-xs text-white/60 mt-2">
                  System Override
                </h3>
                <p className="text-[10px] text-white/40 text-center">
                  Please enter the administrator override passcode.
                </p>
              </div>
              <div className="relative font-mono">
                <Lock size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-white/20" />
                <input
                  type="password"
                  value={adminPass}
                  onChange={(e) => setAdminPass(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-black/40 border border-white/10 rounded-2xl pl-12 pr-4 py-3 text-center focus:outline-none focus:border-white/30 transition-all font-sans tracking-[0.5em] text-lg text-white"
                  autoFocus
                />
              </div>
              <div className="flex gap-3 mt-1">
                <button
                  type="button"
                  onClick={() => {
                    setShowAdminPass(false);
                    setAdminPass("");
                  }}
                  className="flex-1 bg-white/5 py-3 rounded-2xl text-[10px] font-bold uppercase tracking-widest hover:bg-white/10 transition-colors border border-white/5 text-white/70"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 bg-white text-black py-3 rounded-2xl text-[10px] font-bold uppercase tracking-widest hover:bg-white/90 transition-colors shadow-lg shadow-white/10"
                >
                  Authorize
                </button>
              </div>
            </motion.form>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Isolated Full-Screen Admin Dashboard */}
      <AnimatePresence>
        {isAdmin && (
          <motion.div
            key="admin-screen"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-[#000000] z-[400] overflow-y-auto flex flex-col justify-start p-4 sm:p-6"
          >
            <div className="w-full max-w-4xl mx-auto py-8">
              <div className="bg-[#050505] border border-white/10 rounded-[32px] p-6 sm:p-8 shadow-2xl relative backdrop-blur-xl">
                {/* Decorative background glow */}
                <div className="absolute inset-0 bg-gradient-to-b from-white/[0.015] to-transparent rounded-[32px] pointer-events-none" />
                
                <AdminPanel
                  showToast={showToast}
                  addToHistory={addToHistory}
                  onExit={() => {
                    vibrate();
                    setIsAdmin(false);
                  }}
                  chatState={chatState}
                  setChatState={setChatState}
                  boardState={boardState}
                  setBoardState={setBoardState}
                  globalStats={{
                    developer: globalDeveloper,
                    notice: globalNotice,
                    noticeEnabled: globalNoticeEnabled,
                    noticeTheme,
                    noticeExpiresAt,
                    isFrozen,
                    likes: likesCount,
                    passwords: {
                      admin: adminMasterPass,
                      secret: secretMasterPass,
                    },
                  }}
                />
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Global Notice Banner */}
      <AnimatePresence>
        {isNoticeActive && (
          <motion.div
            initial={{ y: -50, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: -50, opacity: 0 }}
            className={`fixed top-0 left-0 right-0 z-[100] backdrop-blur-md border-b border-white/10 py-2 overflow-hidden flex items-center ${NOTICE_THEMES[noticeTheme] || NOTICE_THEMES.blue}`}
          >
            <div className="flex-1 overflow-hidden">
              <div className="flex whitespace-nowrap animate-marquee">
                <span className="text-[10px] font-bold uppercase tracking-[0.3em] px-4 flex items-center gap-2">
                  <Bell size={12} className="animate-pulse" /> {globalNotice}
                </span>
                <span className="text-[10px] font-bold uppercase tracking-[0.3em] px-4 flex items-center gap-2">
                  <Bell size={12} className="animate-pulse" /> {globalNotice}
                </span>
                <span className="text-[10px] font-bold uppercase tracking-[0.3em] px-4 flex items-center gap-2">
                  <Bell size={12} className="animate-pulse" /> {globalNotice}
                </span>
              </div>
            </div>
            <button
              onClick={() => setIsNoticeDismissed(true)}
              className="px-4 text-white/60 hover:text-white transition-colors border-l border-white/10 h-full flex items-center"
              title="Dismiss"
            >
              <X size={14} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Formal Broadcast Modal */}
      <AnimatePresence>
        {activeBroadcast && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[300] bg-black/90 backdrop-blur-md flex items-center justify-center p-6"
          >
            <motion.div
              initial={{ scale: 0.9, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              className={`w-full max-w-md border border-white/10 rounded-[40px] p-8 relative shadow-2xl ${BROADCAST_THEMES[activeBroadcast.theme]?.bg || BROADCAST_THEMES.dark.bg}`}
            >
              <div className="flex flex-col items-center text-center space-y-6">
                <div
                  className={`w-20 h-20 rounded-full flex items-center justify-center shadow-xl ${BROADCAST_THEMES[activeBroadcast.theme]?.iconBg || BROADCAST_THEMES.dark.iconBg} ${BROADCAST_THEMES[activeBroadcast.theme]?.accent || BROADCAST_THEMES.dark.accent}`}
                >
                  {(() => {
                    const Icon = BROADCAST_ICONS[activeBroadcast.icon] || Zap;
                    return <Icon size={40} />;
                  })()}
                </div>
                <div className="space-y-2">
                  <h2
                    className={`text-2xl font-black uppercase tracking-tighter ${BROADCAST_THEMES[activeBroadcast.theme]?.accent || BROADCAST_THEMES.dark.accent}`}
                  >
                    {activeBroadcast.title}
                  </h2>
                  <div
                    className={`h-1 w-12 mx-auto rounded-full opacity-20 ${BROADCAST_THEMES[activeBroadcast.theme]?.accent || BROADCAST_THEMES.dark.accent.replace("text-", "bg-")}`}
                  />
                </div>
                {activeBroadcast.message && (
                  <p
                    className={`text-sm leading-relaxed font-medium ${BROADCAST_THEMES[activeBroadcast.theme]?.text || BROADCAST_THEMES.dark.text}`}
                  >
                    {activeBroadcast.message}
                  </p>
                )}
                <button
                  onClick={() => setActiveBroadcast(null)}
                  className={`w-full py-4 rounded-full font-bold uppercase tracking-widest text-xs hover:scale-105 active:scale-95 transition-all shadow-lg ${BROADCAST_THEMES[activeBroadcast.theme]?.btn || BROADCAST_THEMES.dark.btn}`}
                >
                  Got it
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Freeze Overlay */}
      <AnimatePresence>
        {isFrozen && !isAdmin && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[200] bg-black/80 backdrop-blur-xl flex flex-col items-center justify-center p-6 text-center"
          >
            <div className="w-20 h-20 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center mb-6">
              <Snowflake size={40} className="text-red-500 animate-pulse" />
            </div>
            <h2 className="text-2xl font-bold uppercase tracking-widest mb-2">
              System Frozen
            </h2>
            <p className="text-white/40 text-sm max-w-xs leading-relaxed">
              The administrator has temporarily suspended all operations for
              maintenance. Please check back later.
            </p>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Online Counter */}
      <motion.div
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        className="hidden"
      >
        <div className="relative">
          <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse shadow-[0_0_8px_rgba(16,185,129,0.8)]" />
          <div className="absolute inset-0 w-2 h-2 bg-emerald-500 rounded-full animate-ping opacity-40" />
        </div>
        <span className="text-white/80 font-bold text-[11px] tracking-wider font-mono">
          {onlineCount} <span className="opacity-40 font-medium">ONLINE</span>
        </span>
      </motion.div>

      {/* Brand Dock (Temporarily Disabled) */}
      {/* 
      <AnimatePresence>
        {isBrandDockVisible && (
          <motion.div
            initial={{ opacity: 0, y: -24, scale: 0.97 }}
            animate={{ opacity: 1, x: 0, y: 0 }}
            exit={{ opacity: 0, y: -26, scale: 0.97 }}
            transition={{ type: "spring", stiffness: 330, damping: 30 }}
            className="fixed top-2 left-2 right-2 sm:top-3 sm:left-8 sm:right-8 z-50 flex h-16 sm:h-[72px] items-center gap-2 sm:gap-4 rounded-[22px] sm:rounded-[26px] border border-white/15 bg-black/55 px-3 sm:px-5 shadow-[0_16px_44px_rgba(0,0,0,0.42)] backdrop-blur-2xl"
          >
        <div className="w-11 h-11 sm:w-16 sm:h-16 shrink-0">
          <img
            src="/hefimer-orbit.svg"
            alt="Hefimer"
            className="w-full h-full object-contain drop-shadow-[0_0_12px_rgba(255,255,255,0.18)]"
          />
        </div>
        <div className="flex flex-col items-start">
          <span className="text-[19px] sm:text-2xl font-bold tracking-[-0.05em] leading-none text-white">
            Hefimer
          </span>
          <span className="hidden sm:flex mt-1.5 items-center gap-2 text-[10px] tracking-[0.08em] font-medium text-white/35">
            <span className="block h-px w-4 bg-white/30" />
            <span>Developed by</span>
            <span className="text-white/60">{globalDeveloper}</span>
          </span>
        </div>
        <div className="ml-auto flex items-center gap-2 sm:gap-3">
          <div className="flex items-center gap-2 rounded-full border border-white/[0.12] bg-white/[0.04] px-2 sm:px-2.5 py-2 text-[10px] font-bold tracking-[0.14em] text-white/65">
            <span className="relative flex w-2 h-2">
              <span className="absolute inset-0 rounded-full bg-emerald-400 animate-ping opacity-50" />
              <span className="relative w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_10px_rgba(52,211,153,0.9)]" />
            </span>
            <span className="hidden sm:inline">
              {onlineCount} ONLINE
            </span>
          </div>
          <button
            onClick={() => {
              vibrate();
              setIsHistoryModalOpen(true);
            }}
            className="flex w-9 h-9 sm:w-10 sm:h-10 items-center justify-center rounded-xl sm:rounded-2xl border border-white/[0.12] bg-white/[0.06] text-white/75 transition-all hover:bg-white/[0.12] hover:text-white active:scale-90"
            title="History"
          >
            <Clock size={18} />
        </div>
          </motion.div>
        )}
      </AnimatePresence>
      */}


      <div className="w-full flex flex-col items-center justify-start pt-0 pb-16 sm:pb-12">
        <div className="w-full max-w-[1280px] z-10 flex flex-col">
          {/* Header */}
          <div className="hidden">
            <motion.h1
              initial={{ opacity: 0, y: 30, filter: "blur(10px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{ duration: 1.2, ease: [0.22, 1, 0.36, 1] }}
              className="text-4xl font-bold tracking-tight mb-1"
            >
              Hefimer
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 10 }}
              animate={{
                opacity: 1,
                y: 0,
                textShadow: [
                  "0 0 0px rgba(255,255,255,0)",
                  "0 0 20px rgba(255,255,255,0.8)",
                  "0 0 0px rgba(255,255,255,0)",
                ],
              }}
              transition={{
                opacity: { duration: 1, delay: 0.6 },
                y: { duration: 1, delay: 0.6 },
                textShadow: {
                  duration: 2,
                  delay: 1.2,
                  times: [0, 0.5, 1],
                  ease: "easeInOut",
                },
              }}
              className="text-white/40 text-xs mt-1"
            >
              Developed by <span className="text-white/60">{globalDeveloper}</span>
            </motion.p>
          </div>

          {/* History Modal */}
          <AnimatePresence>
            {isHistoryModalOpen && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md"
                onClick={() => setIsHistoryModalOpen(false)}
              >
                <motion.div
                  initial={{ opacity: 0, scale: 0.95, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95, y: 20 }}
                  className="w-full max-w-md bg-zinc-900 border border-white/10 rounded-3xl p-6 shadow-2xl"
                  onClick={(e) => e.stopPropagation()}
                >
                  <div className="flex justify-between items-center mb-5 pb-3 border-b border-white/10">
                    <div className="flex items-center gap-2">
                      <Clock size={16} className="text-white/50" />
                      <h3 className="text-sm font-bold text-white tracking-wide">Recent History</h3>
                    </div>
                    <div className="flex items-center gap-2">
                      {history.length > 0 && (
                        <button
                          onClick={() => {
                            vibrate();
                            setHistory([]);
                            localStorage.removeItem("hefimer_history");
                            showToast("History cleared", "success");
                          }}
                          className="text-white/40 hover:text-white hover:bg-white/10 transition-colors text-[10px] uppercase tracking-wider font-bold flex items-center gap-1.5 px-3 py-1.5 bg-white/5 rounded-full cursor-pointer"
                        >
                          <Trash2 size={12} /> Clear
                        </button>
                      )}
                      <button
                        onClick={() => setIsHistoryModalOpen(false)}
                        className="text-white/40 hover:text-white transition-colors p-1.5 bg-white/5 rounded-full hover:bg-white/10 cursor-pointer flex items-center justify-center"
                      >
                        <X size={16} />
                      </button>
                    </div>
                  </div>
                  <HistoryView
                    history={history}
                    setHistory={setHistory}
                    showToast={showToast}
                  />
                </motion.div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Main Card */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{
              duration: 0.35,
              ease: "easeOut",
            }}
            className={
              isDedicatedToolScreen
                ? "w-full"
                : "w-full grid grid-cols-1 xl:grid-cols-2 gap-10 xl:gap-0 relative"
            }
          >
            {/* Central Divider Line */}
            {!isDedicatedToolScreen && (
              <motion.div
                initial={{ scaleY: 0 }}
                animate={{ scaleY: 1 }}
                transition={{ duration: 1.2, ease: [0.16, 1, 0.3, 1] }}
                className="hidden xl:block absolute left-1/2 top-0 bottom-0 w-px bg-white/10 -translate-x-1/2 origin-top pointer-events-none"
              />
            )}

            <motion.div
              initial={{ opacity: 0, scale: 0.72, y: 12 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1], delay: 0.25 }}
              className={
                isDedicatedToolScreen
                  ? "hidden"
                  : "hidden xl:flex absolute left-1/2 top-[290px] -translate-x-1/2 -translate-y-1/2 z-10 flex-col items-center justify-center cursor-pointer select-none"
              }
              onClick={handleLogoClick}
              style={{ WebkitTapHighlightColor: "transparent" }}
            >
              {/* Invisible black mask to cleanly cut the divider line behind the logo */}
              <div className="absolute w-40 h-40 rounded-full bg-[#000000] pointer-events-none select-none" />

              <motion.img
                src="/hefimer-orbit.svg"
                alt="Hefimer mark"
                draggable="false"
                animate={{ rotate: 360 }}
                transition={{
                  repeat: Infinity,
                  duration: 25,
                  ease: "linear",
                }}
                className="relative z-10 w-44 h-44 object-contain drop-shadow-[0_0_35px_rgba(255,255,255,0.15)] opacity-95 pointer-events-none select-none"
              />
            </motion.div>
            <section className={isDedicatedToolScreen ? "min-w-0" : "min-w-0 xl:pr-[120px]"}>
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
              className={isDedicatedToolScreen ? "hidden" : "mb-8"}
            >
              <p className="text-[10px] uppercase tracking-[0.24em] font-bold text-white/30 mb-3">
                Create a drop
              </p>
              <h1
                onClick={handleLogoClick}
                className="text-4xl sm:text-5xl font-bold tracking-[-0.05em] text-white cursor-pointer select-none"
                style={{ WebkitTapHighlightColor: "transparent" }}
              >
                Send
              </h1>
              <p className="mt-2 text-sm text-white/40">
                Share a file or text that disappears when its moment is over.
              </p>
            </motion.div>

            {/* Send modes */}
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.15, ease: [0.16, 1, 0.3, 1] }}
              className={
                isDedicatedToolScreen
                  ? "hidden"
                  : "flex w-full items-center gap-7 border-b border-white/10 mb-8"
              }
            >
              {[
                { id: "file", label: "Send File", icon: FileUp },
                { id: "text", label: "Send Text", icon: Code },
              ].map((tab) => {
                const Icon = tab.icon;
                const isActive = activeTab === tab.id;
                return (
                  <React.Fragment key={tab.id}>
                    <button
                      onClick={() => {
                      if (tab.id === "chat") handleChatTabClick();
                      else if (tab.id === "file") {
                        vibrate();
                        if (activeTab === "file") {
                          const newCount = r2TabClicks + 1;
                          setR2TabClicks(newCount);
                          if (newCount >= 6) {
                            if (isR2Mode) {
                              setIsR2Mode(false);
                              showToast("Secret mode deactivated", "success");
                            } else {
                              setShowR2Auth(true);
                            }
                            setR2TabClicks(0);
                          }
                        } else {
                          setActiveTab("file");
                          setR2TabClicks(1);
                          setChatTabClicks(0);
                        }
                      } else {
                        vibrate();
                        setActiveTab(tab.id as any);
                        setChatTabClicks(0);
                        setR2TabClicks(0);
                      }
                    }}
                      className={`relative flex items-center justify-center h-11 shrink-0 px-1 transition-colors whitespace-nowrap focus:outline-none ${
                      isActive
                        ? "text-white"
                        : "text-white/60 hover:text-white"
                    }`}
                    >
                      {isActive && (
                        <motion.div
                          layoutId="nav-pill"
                          className="absolute bottom-0 left-0 right-0 h-px bg-white z-0"
                          transition={{
                            type: "spring",
                            stiffness: 300,
                            damping: 25,
                          }}
                        />
                      )}
                      <div className="relative z-10 flex items-center gap-2">
                        <Icon size={18} className="shrink-0" />
                        <span className="font-bold text-[12px] leading-none whitespace-nowrap">
                          {tab.label}
                        </span>
                      </div>
                    </button>
                  </React.Fragment>
                );
              })}
            </motion.div>

            {/* Inline Toast */}
            <AnimatePresence mode="wait">
              {toast && (
                <motion.div
                  initial={{ opacity: 0, height: 0, marginBottom: 0 }}
                  animate={{ opacity: 1, height: "auto", marginBottom: 16 }}
                  exit={{ opacity: 0, height: 0, marginBottom: 0 }}
                  className="overflow-hidden"
                >
                  <div
                    className={`flex items-center gap-3 px-4 py-3 rounded-[16px] border shadow-xl ${
                      toast.type === "error"
                        ? "bg-white text-black border-white"
                        : toast.type === "success"
                          ? "bg-white text-black border-white"
                          : "bg-black border-white/40 text-white"
                    }`}
                  >
                    {toast.type === "error" && (
                      <AlertCircle size={18} className="shrink-0" />
                    )}
                    {toast.type === "success" && (
                      <Check size={18} className="shrink-0" />
                    )}
                    {toast.type === "info" && (
                      <Loader2 size={18} className="animate-spin shrink-0" />
                    )}
                    <span className="font-medium text-sm">{toast.message}</span>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {isDedicatedToolScreen && activeTab !== "home" && (
              <div className="mb-10 flex flex-col gap-6 border-b border-white/10 pb-6 sm:flex-row sm:items-end sm:justify-between">
                <div className="flex items-start gap-4">
                  <button
                    onClick={() => {
                      vibrate();
                      setActiveTab("home");
                      setChatTabClicks(0);
                      setR2TabClicks(0);
                    }}
                    className="mt-1 flex w-10 h-10 shrink-0 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.04] text-white/50 transition-all hover:bg-white/[0.1] hover:text-white"
                    title="Back to sharing"
                  >
                    <ArrowLeft size={18} />
                  </button>
                  <div>
                    <p className="text-[10px] uppercase tracking-[0.24em] font-bold text-white/30">
                      Live workspace
                    </p>
                    <h1 className="mt-2 text-4xl sm:text-5xl font-bold tracking-[-0.05em] text-white">
                      {activeTab === "chat" ? "Chat Room" : activeTab === "space" ? "Space" : "Board"}
                    </h1>
                    <p className="mt-2 text-sm text-white/40">
                      {activeTab === "chat"
                        ? "Create a temporary room or join a live conversation."
                        : activeTab === "space"
                          ? "An infinite collaborative canvas for dropping files and media."
                          : "Create a shared canvas or join a board in progress."}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Content Area */}
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.25, ease: [0.16, 1, 0.3, 1] }}
              className="relative"
            >
              <AnimatePresence mode="wait">
                {activeTab === "home" && (
                  <motion.div
                    key="home"
                    initial={{ opacity: 0, scale: 0.97, y: 15 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.97, y: -15 }}
                    transition={{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
                  >
                    <LandingPage
                      setActiveTab={setActiveTab}
                      onOpenHistory={() => setIsHistoryModalOpen(true)}
                      likesCount={likesCount}
                      showToast={showToast}
                      setShowPolicy={setShowPolicy}
                      developer={globalDeveloper}
                    />
                  </motion.div>
                )}
                {activeTab === "file" && (
                  <motion.div
                    key="file"
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 10 }}
                    transition={{ duration: 0.2 }}
                  >
                    {showR2Auth ? (
                      <motion.form
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        onSubmit={handleR2Auth}
                        className="space-y-4 p-6 bg-white/5 rounded-[32px] border border-white/10 shadow-2xl"
                      >
                        <div className="flex flex-col items-center gap-2 mb-2">
                          <Lock size={32} className="text-white/40" />
                          <h3 className="font-bold uppercase tracking-[0.2em] text-xs text-white/60">
                            Secret Access
                          </h3>
                        </div>
                        <input
                          type="password"
                          value={r2AuthPass}
                          onChange={(e) => setR2AuthPass(e.target.value)}
                          placeholder="••••••••"
                          className="w-full bg-black/40 border border-white/20 rounded-full px-5 py-4 text-center focus:outline-none focus:border-white/40 transition-all font-sans tracking-[0.5em] text-lg text-white"
                          autoFocus
                        />
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => {
                              setShowR2Auth(false);
                              setR2AuthPass("");
                            }}
                            className="flex-1 bg-white/5 py-3.5 rounded-full text-[10px] font-bold uppercase tracking-widest hover:bg-white/10 transition-colors"
                          >
                            Cancel
                          </button>
                          <button
                            type="submit"
                            className="flex-1 bg-white text-black py-3.5 rounded-full text-[10px] font-bold uppercase tracking-widest hover:bg-white/90 transition-colors"
                          >
                            Verify
                          </button>
                        </div>
                      </motion.form>
                    ) : isR2Mode ? (
                      <R2SendFile
                        showToast={showToast}
                        addToHistory={addToHistory}
                        state={sendFileState}
                        setState={setSendFileState}
                        onExit={() => {
                          vibrate();
                          setIsR2Mode(false);
                          showToast("Secret mode deactivated", "success");
                        }}
                        stats={r2Stats}
                        updateStats={updateUploadStats}
                      />
                    ) : (
                      <SendFile
                        showToast={showToast}
                        addToHistory={addToHistory}
                        state={sendFileState}
                        setState={setSendFileState}
                        stats={standardStats}
                        updateStats={updateUploadStats}
                      />
                    )}
                  </motion.div>
                )}
                {activeTab === "text" && (
                  <motion.div
                    key="text"
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 10 }}
                    transition={{ duration: 0.2 }}
                  >
                    <SendText
                      showToast={showToast}
                      addToHistory={addToHistory}
                      state={sendTextState}
                      setState={setSendTextState}
                    />
                  </motion.div>
                )}
                {activeTab === "chat" && (
                  <motion.div
                    key="chat"
                    initial={{ opacity: 0, scale: 0.97, y: 15 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.97, y: 15 }}
                    transition={{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
                  >
                    <Chat
                      showToast={showToast}
                      addToHistory={addToHistory}
                      state={chatState}
                      setState={setChatState}
                      isAdmin={isAdmin}
                      isFrozen={isFrozen}
                    />
                  </motion.div>
                )}
                {activeTab === "board" && (
                  <motion.div
                    key="board"
                    initial={{ opacity: 0, scale: 0.97, y: 15 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.97, y: 15 }}
                    transition={{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
                  >
                    <Board
                      showToast={showToast}
                      addToHistory={addToHistory}
                      state={boardState}
                      setState={setBoardState}
                    />
                  </motion.div>
                )}
                {activeTab === "space" && (
                  <motion.div
                    key="space"
                    initial={{ opacity: 0, scale: 0.97, y: 15 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.97, y: 15 }}
                    transition={{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
                  >
                    <Space
                      showToast={showToast}
                      addToHistory={addToHistory}
                      state={spaceState}
                      setState={setSpaceState}
                    />
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
            </section>

            <aside
              className={
                isDedicatedToolScreen
                  ? "hidden"
                  : "border-t border-white/10 pt-10 xl:border-t-0 xl:pt-0 xl:pl-10"
              }
            >
              <motion.div
                initial={{ opacity: 0, y: 24 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.8, delay: 0.1, ease: [0.16, 1, 0.3, 1] }}
              >
                <p className="text-[10px] uppercase tracking-[0.24em] font-bold text-white/30 mb-3">
                  Open a drop
                </p>
                <h2 className="text-4xl sm:text-5xl font-bold tracking-[-0.05em] text-white">
                  Receive
                </h2>
                <p className="mt-2 mb-10 text-sm leading-relaxed text-white/40">
                  Enter the five-digit code you received to open a file or text.
                </p>
              </motion.div>
              <motion.div
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.8, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
                className="xl:pl-[110px]"
              >
                <Receive
                  showToast={showToast}
                  addToHistory={addToHistory}
                  state={receiveState}
                  setState={setReceiveState}
                />
              </motion.div>
            </aside>
          </motion.div>

          <div
            className={
              isDedicatedToolScreen
                ? "hidden"
                : "mt-14 grid grid-cols-1 gap-4 sm:grid-cols-2"
            }
          >
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-50px" }}
              transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
              className="sm:col-span-2 flex items-end justify-between border-b border-white/10 pb-5"
            >
              <div>
                <p className="text-[10px] uppercase tracking-[0.24em] font-bold text-white/30">
                  Live tools
                </p>
                <h2 className="mt-2 text-2xl font-bold tracking-[-0.04em] text-white">
                  Continue together
                </h2>
              </div>
              <span className="hidden sm:block text-xs text-white/35">
                Temporary, collaborative spaces
              </span>
            </motion.div>
            {[
              {
                id: "chat",
                label: "Chat Room",
                description: "Start a private live conversation with a simple room code.",
                icon: MessageSquare,
              },
              {
                id: "board",
                label: "Board",
                description: "Draw, annotate and collaborate on a shared canvas.",
                icon: Palette,
              },
              {
                id: "space",
                label: "Space",
                description: "An infinite collaborative canvas for dropping files and media.",
                icon: Box,
              },
            ].map((tool, idx) => {
              const Icon = tool.icon;
              return (
                <motion.button
                  key={tool.id}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, margin: "-50px" }}
                  transition={{ duration: 0.7, delay: idx * 0.1, ease: [0.16, 1, 0.3, 1] }}
                  onClick={() => {
                    if (tool.id === "chat") {
                      handleChatTabClick();
                      return;
                    }
                    if (tool.id === "space") {
                      vibrate();
                      setActiveTab("space");
                      setChatTabClicks(0);
                      setR2TabClicks(0);
                      return;
                    }
                    vibrate();
                    setActiveTab("board");
                    setChatTabClicks(0);
                    setR2TabClicks(0);
                  }}
                  className="group relative min-h-[168px] overflow-hidden rounded-[30px] border border-white/10 bg-white/[0.025] p-6 text-left transition-all hover:-translate-y-1 hover:border-white/25 hover:bg-white/[0.06]"
                >
                  <div className="absolute top-0 right-0 w-32 h-32 rounded-full bg-white/[0.035] blur-2xl transition-opacity group-hover:opacity-100 opacity-50" />
                  <div className="relative flex h-full flex-col justify-between">
                    <div className="flex items-start justify-between">
                      <div className="flex w-12 h-12 items-center justify-center rounded-2xl border border-white/10 bg-black/30 text-white/75 transition-transform group-hover:scale-110">
                        <Icon size={22} />
                      </div>
                      <ChevronDown size={20} className="-rotate-90 text-white/30 transition-transform group-hover:translate-x-1 group-hover:text-white" />
                    </div>
                    <div>
                      <h3 className="text-xl font-bold tracking-[-0.03em] text-white flex items-center gap-2">
                        {tool.label}
                        <span className="px-1.5 py-0.5 text-[8px] font-black tracking-widest uppercase rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 shadow-[0_0_10px_rgba(245,158,11,0.05)]">BETA</span>
                      </h3>
                      <p className="mt-2 max-w-sm text-sm leading-relaxed text-white/40">
                        {tool.description}
                      </p>
                    </div>
                  </div>
                </motion.button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Policy Modal */}
      <AnimatePresence>
        {showPolicy && (
          <PolicyModal type={showPolicy} onClose={() => setShowPolicy(null)} />
        )}
      </AnimatePresence>

      {!isDedicatedToolScreen && (
        <>
          {/* Separator */}
          <div className="w-full max-w-[500px] h-px bg-gradient-to-r from-transparent via-white/10 to-transparent my-12" />

          {/* FAQ Section */}
          <FAQSection />

          {/* Separator */}
          <div className="w-full max-w-[500px] h-px bg-gradient-to-r from-transparent via-white/10 to-transparent my-12" />

          {/* Like Widget */}
          <LikeWidget showToast={showToast} likes={likesCount} />

          {/* Separator */}
          <div className="w-full max-w-[500px] h-px bg-gradient-to-r from-transparent via-white/10 to-transparent my-12" />

          {/* Footer */}
          <motion.footer
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1, duration: 1 }}
            className="max-w-[500px] text-center px-4 mb-12"
          >
            <p className="text-white/30 text-[11px] leading-relaxed font-medium italic mb-4">
              Your instant web hub for quick file sharing, real-time messaging, and
              live collaborative tools. Fast, lightweight, and works on any device.
            </p>

            <p className="text-white/35 text-[10px] mb-4">
              Developed by <span className="text-white/60">{globalDeveloper}</span>
            </p>

            {/* Policy Links */}
            <div className="flex items-center justify-center gap-4 text-[10px]">
              <button
                onClick={() => {
                  vibrate();
                  setShowPolicy("privacy");
                }}
                className="text-white/20 hover:text-white/50 transition-colors underline decoration-white/10 underline-offset-2"
              >
                Privacy Policy
              </button>
              <span className="text-white/10">|</span>
              <button
                onClick={() => {
                  vibrate();
                  setShowPolicy("terms");
                }}
                className="text-white/20 hover:text-white/50 transition-colors underline decoration-white/10 underline-offset-2"
              >
                Terms of Service
              </button>
            </div>

            <p className="text-white/15 text-[9px] mt-3">
              © 2026 Hefimer. All rights reserved.
            </p>
          </motion.footer>
        </>
      )}
    </div>
  );
}
