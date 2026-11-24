import { forwardProviderUpload } from "../../lib/proxy-helpers";

export const onRequest: any = async ({ request }: { request: Request }) =>
  forwardProviderUpload(request, {
    targetUrl: "https://tempfile.org/api/upload/local",
    defaultResponseContentType: "application/json; charset=utf-8",
    errorLabel: "TempFile proxy failed",
  });
