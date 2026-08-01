export function normalizeApiBase(value: string | undefined) {
  const trimmed = (value || "").trim().replace(/\/+$/, "");
  if (!trimmed) return "";

  const url = new URL(trimmed);
  const localHttp = url.protocol === "http:"
    && ["localhost", "127.0.0.1"].includes(url.hostname);
  if (url.protocol !== "https:" && !localHttp) {
    throw new Error("The configured API origin must use HTTPS");
  }
  if (url.pathname !== "/" || url.search || url.hash || url.username || url.password) {
    throw new Error("The configured API origin must not include a path, query, or credentials");
  }
  return url.origin;
}

const configuredApiBase = normalizeApiBase(import.meta.env?.VITE_API_BASE_URL);

export function buildApiUrl(path: string, base = configuredApiBase) {
  if (!path.startsWith("/api/")) {
    throw new Error(`API paths must start with /api/: ${path}`);
  }
  return base ? `${base}${path}` : path;
}

export function apiUrl(path: string) {
  return buildApiUrl(path);
}
