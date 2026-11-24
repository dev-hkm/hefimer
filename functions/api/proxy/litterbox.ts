import { forwardProviderUpload } from "../../lib/proxy-helpers";

export const onRequest: any = async ({ request }: { request: Request }) =>
  forwardProviderUpload(request, {
    targetUrl: "https://litterbox.catbox.moe/resources/internals/api.php",
    defaultResponseContentType: "text/plain; charset=utf-8",
    errorLabel: "Litterbox proxy failed",
  });
