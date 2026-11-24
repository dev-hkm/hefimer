import { forwardProviderUpload } from "../../lib/proxy-helpers";

export const onRequest: any = async ({ request }: { request: Request }) =>
  forwardProviderUpload(request, {
    targetUrl: "https://uguu.se/upload",
    defaultResponseContentType: "application/json; charset=utf-8",
    errorLabel: "Uguu.se proxy failed",
  });
