import { isTrustedBrowserRequest, json } from "../../../lib/request-guard";

const SAFE_BIN = /^[a-z0-9][a-z0-9-]{4,63}$/;
const SAFE_FILE_NAME = /^[^\\/\r\n\0]{1,180}$/;

export const onRequestPost: any = async (context: any) => {
  const { request, params } = context;

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  const path = Array.isArray(params.path) ? params.path : [];
  const [bin, ...nameParts] = path;
  const fileName = nameParts.join("/");
  if (!SAFE_BIN.test(bin || "") || !SAFE_FILE_NAME.test(fileName)) {
    return json({ error: "Invalid Filebin upload path" }, 400);
  }

  try {
    const bytes = await request.arrayBuffer();
    const headers = new Headers({
      "Content-Type": request.headers.get("Content-Type") || "application/octet-stream",
      "Content-Length": String(bytes.byteLength),
    });
    const upstream = await fetch(`https://filebin.net/${encodeURIComponent(bin)}/${encodeURIComponent(fileName)}`, {
      method: "POST",
      headers,
      body: bytes,
    });
    const body = await upstream.text();
    return new Response(body, {
      status: upstream.status,
      headers: {
        "Content-Type": upstream.headers.get("Content-Type") || "application/json; charset=utf-8",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (error: any) {
    return json({ error: error?.message || "Filebin upload failed" }, 502);
  }
};
