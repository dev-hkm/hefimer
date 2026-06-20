import { isTrustedBrowserRequest, json } from "../../lib/request-guard";

export const onRequest: any = async (context: any) => {
  const { request } = context;

  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  try {
    const targetUrl = "https://litterbox.catbox.moe/resources/internals/api.php";
    const contentType = request.headers.get("content-type");

    const headers = new Headers();
    if (contentType) {
      headers.set("content-type", contentType);
    }

    // Forward the request to Litterbox, streaming the request body directly
    const response = await fetch(targetUrl, {
      method: "POST",
      headers,
      body: request.body,
    });

    const bodyText = await response.text();
    return new Response(bodyText, {
      status: response.status,
      headers: {
        "Content-Type": "text/plain; charset=utf-8",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (err: any) {
    return json({ error: err.message || "Litterbox proxy failed" }, 500);
  }
};
