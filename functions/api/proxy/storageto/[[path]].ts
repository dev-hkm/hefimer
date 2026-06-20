import { isTrustedBrowserRequest, json } from "../../../lib/request-guard";

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
    ];
    for (const key of headerKeys) {
      const val = request.headers.get(key);
      if (val) {
        headers.set(key, val);
      }
    }

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
