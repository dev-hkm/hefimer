import { forwardProviderUpload } from "../../lib/proxy-helpers";

export const onRequest: any = async ({ request }: { request: Request }) =>
  forwardProviderUpload(request, {
    targetUrl: "https://temp.sh/upload",
    defaultResponseContentType: "text/plain; charset=utf-8",
    errorLabel: "temp.sh proxy failed",
  });
