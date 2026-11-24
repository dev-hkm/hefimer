import { forwardProviderUpload } from "../../lib/proxy-helpers";

export const onRequest: any = async ({ request }: { request: Request }) =>
  forwardProviderUpload(request, {
    targetUrl: "https://tmpfiles.org/api/v1/upload",
    defaultResponseContentType: "application/json; charset=utf-8",
    errorLabel: "tmpfiles proxy failed",
  });
