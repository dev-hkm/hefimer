const JSON_HEADERS = {
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store, max-age=0",
  "X-Content-Type-Options": "nosniff",
};

const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX_REQUESTS = 20;
const requestBuckets = new Map<string, { count: number; resetAt: number }>();

export function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });
}

export function isTrustedBrowserRequest(request: Request) {
  const targetOrigin = new URL(request.url).origin;
  const origin = request.headers.get("Origin");
  const fetchSite = request.headers.get("Sec-Fetch-Site");
  const referer = request.headers.get("Referer");

  if (origin && origin !== targetOrigin) return false;
  if (fetchSite && !["same-origin", "same-site", "none"].includes(fetchSite)) return false;
  if (!origin && !fetchSite) {
    if (!referer) return false;
    try {
      if (new URL(referer).origin !== targetOrigin) return false;
    } catch {
      return false;
    }
  }
  return true;
}

export function isRateLimited(request: Request) {
  const clientId = request.headers.get("CF-Connecting-IP") || "unknown";
  const now = Date.now();
  const bucket = requestBuckets.get(clientId);
  if (!bucket || bucket.resetAt <= now) {
    requestBuckets.set(clientId, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return false;
  }
  bucket.count += 1;
  return bucket.count > RATE_LIMIT_MAX_REQUESTS;
}
