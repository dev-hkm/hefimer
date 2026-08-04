import { isTrustedBrowserRequest, json } from "../../../lib/request-guard";

const STORAGE_TO_BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

export const onRequest: any = async (context: any) => {
  const { request, params } = context;

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  try {
    const subpath = params.path ? params.path.join("/") : "";
    const url = new URL(request.url);
    const targetUrl = new URL(`https://storage.to/api/${subpath}`);
    targetUrl.search = url.search;

    const headers = new Headers();
    const headerKeys = [
      "content-type",
      "x-visitor-token",
      "x-owner-token",
      "authorization",
      "accept",
      "accept-language",
      "origin",
      "referer",
    ];
    for (const key of headerKeys) {
      const val = request.headers.get(key);
      if (val) {
        headers.set(key, val);
      }
    }
    // Storage.to's WAF rejects the bare server-side client emitted by fetch.
    // Preserve the browser identity without forwarding untrusted arbitrary headers.
    headers.set("user-agent", request.headers.get("user-agent") || STORAGE_TO_BROWSER_USER_AGENT);

    const response = await fetch(targetUrl.toString(), {
      method: request.method,
      headers,
      body: request.method !== "GET" && request.method !== "HEAD" ? request.body : null,
    });

    const bodyText = await response.text();
    return new Response(bodyText, {
      status: response.status,
      headers: {
        "Content-Type": response.headers.get("content-type") || "application/json; charset=utf-8",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (err: any) {
    return json({ error: err.message || "storage.to proxy failed" }, 500);
  }
};
