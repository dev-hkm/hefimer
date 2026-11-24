import { forwardProviderUpload } from "../../lib/proxy-helpers";

export const onRequest: any = async ({ request }: { request: Request }) =>
  forwardProviderUpload(request, {
    targetUrl: "https://tmpfile.link/api/upload",
    defaultResponseContentType: "application/json; charset=utf-8",
    errorLabel: "tfLink proxy failed",
  });
