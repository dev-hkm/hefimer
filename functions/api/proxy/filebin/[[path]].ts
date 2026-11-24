import { forwardProviderUpload } from "../../../lib/proxy-helpers";
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

  return forwardProviderUpload(request, {
    targetUrl: `https://filebin.net/${encodeURIComponent(bin)}/${encodeURIComponent(fileName)}`,
    method: "POST",
    extraHeaders: {
      "Content-Type": request.headers.get("Content-Type") || "application/octet-stream",
    },
    errorLabel: "Filebin upload failed",
  });
};
