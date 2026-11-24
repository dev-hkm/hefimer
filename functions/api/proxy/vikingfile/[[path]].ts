import { isTrustedBrowserRequest, json } from "../../../lib/request-guard";

const ALLOWED_VIKING_PATHS = new Set([
  "get-server",
  "get-upload-url",
  "complete-upload",
]);

export const onRequest: any = async (context: any) => {
  const { request, params } = context;

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  const path = Array.isArray(params.path) ? params.path.join("/") : "";
  if (!ALLOWED_VIKING_PATHS.has(path)) {
    return json({ error: "Unsupported VikingFile endpoint" }, 400);
  }

  if (path === "get-server" && request.method !== "GET") {
    return json({ error: "Method not allowed" }, 405);
  }
  if (path !== "get-server" && request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  try {
    const headers = new Headers();
    const contentType = request.headers.get("content-type");
    if (contentType) {
      headers.set("content-type", contentType);
    }

    const upstream = await fetch(`https://vikingfile.com/api/${path}`, {
      method: request.method,
      headers,
      body: request.method === "POST" ? await request.text() : undefined,
    });

    const bodyText = await upstream.text();
    return new Response(bodyText, {
      status: upstream.status,
      headers: {
        "Content-Type": upstream.headers.get("content-type") || "application/json; charset=utf-8",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (error: any) {
    return json({ error: error?.message || "VikingFile proxy failed" }, 502);
  }
};
