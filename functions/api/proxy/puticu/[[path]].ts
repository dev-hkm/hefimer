import { forwardProviderUpload } from "../../../lib/proxy-helpers";
import { isTrustedBrowserRequest, json } from "../../../lib/request-guard";

const SAFE_FILE_NAME = /^[^\\/\r\n\0]{1,180}$/;

export const onRequestPut: any = async (context: any) => {
  const { request, params } = context;

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  const path = Array.isArray(params.path) ? params.path : [];
  const fileName = path.join("/");
  if (!SAFE_FILE_NAME.test(fileName)) {
    return json({ error: "Invalid Put.icu upload filename" }, 400);
  }

  return forwardProviderUpload(request, {
    targetUrl: `https://put.icu/upload/${encodeURIComponent(fileName)}`,
    method: "PUT",
    forwardHeaders: ["linx-expiry", "accept"],
    extraHeaders: {
      Accept: "application/json",
    },
    errorLabel: "Put.icu upload failed",
  });
};
