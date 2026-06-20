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
    const targetUrl = "https://tmpfiles.org/api/v1/upload";
    const contentType = request.headers.get("content-type");

    const headers = new Headers();
    if (contentType) {
      headers.set("content-type", contentType);
    }

    // Forward the request to tmpfiles.org, streaming the request body directly
    const response = await fetch(targetUrl, {
      method: "POST",
      headers,
      body: request.body,
    });

    const bodyText = await response.text();
    return new Response(bodyText, {
      status: response.status,
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (err: any) {
    return json({ error: err.message || "tmpfiles proxy failed" }, 500);
  }
};
