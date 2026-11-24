import { isTrustedBrowserRequest, json } from "./request-guard";

export interface ProxyForwardOptions {
  targetUrl: string;
  method?: "POST" | "PUT";
  defaultResponseContentType?: string;
  forwardHeaders?: string[];
  extraHeaders?: Record<string, string>;
  errorLabel?: string;
}

/**
 * Forward a trusted browser upload request to an upstream provider without buffering into memory.
 */
export async function forwardProviderUpload(
  request: Request,
  options: ProxyForwardOptions,
): Promise<Response> {
  const expectedMethod = options.method || "POST";
  if (request.method !== expectedMethod) {
    return json({ error: "Method not allowed" }, 405);
  }

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  try {
    const headers = new Headers(options.extraHeaders);
    const headerNames = [
      "content-type",
      "content-length",
      ...(options.forwardHeaders || []),
    ];
    for (const name of headerNames) {
      const value = request.headers.get(name);
      if (value) {
        headers.set(name, value);
      }
    }

    const response = await fetch(options.targetUrl, {
      method: expectedMethod,
      headers,
      body: request.body,
    });

    const bodyText = await response.text();
    return new Response(bodyText, {
      status: response.status,
      headers: {
        "Content-Type":
          response.headers.get("content-type") ||
          options.defaultResponseContentType ||
          "application/json; charset=utf-8",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (err: any) {
    return json(
      { error: err?.message || options.errorLabel || "Provider proxy failed" },
      502,
    );
  }
}
