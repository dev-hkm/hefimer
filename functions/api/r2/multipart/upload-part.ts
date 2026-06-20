import { uploadMultipartPartProxy, isSafeObjectKey, isSafeUploadId, type R2Env } from "../../../lib/r2";
import { isTrustedBrowserRequest, json } from "../../../lib/request-guard";

export const onRequest: any = async ({ request, env }: { request: Request; env: R2Env }) => {
  if (request.method !== "PUT") {
    return json({ error: "Method not allowed" }, 405);
  }

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  const url = new URL(request.url);
  const objectKey = url.searchParams.get("objectKey") || "";
  const uploadId = url.searchParams.get("uploadId") || "";
  const partNumberStr = url.searchParams.get("partNumber") || "";
  const partNumber = parseInt(partNumberStr, 10);

  if (
    !objectKey ||
    !isSafeObjectKey(objectKey) ||
    !uploadId ||
    !isSafeUploadId(uploadId) ||
    isNaN(partNumber) ||
    partNumber < 1 ||
    partNumber > 10_000
  ) {
    return json({ error: "Invalid multipart upload reference" }, 400);
  }

  if (!env.R2_ACCOUNT_ID || !env.R2_ACCESS_KEY_ID || !env.R2_SECRET_ACCESS_KEY || !env.R2_BUCKET_NAME) {
    return json({ error: "R2 storage is not configured." }, 503);
  }

  try {
    const contentLength = request.headers.get("content-length");
    const contentLengthNum = contentLength ? parseInt(contentLength, 10) : undefined;
    const bodyBuffer = await request.arrayBuffer();

    const etag = await uploadMultipartPartProxy(
      env,
      objectKey,
      uploadId,
      partNumber,
      new Uint8Array(bodyBuffer),
      contentLengthNum
    );

    return new Response(JSON.stringify({ success: true }), {
      status: 200,
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "ETag": etag || "",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Expose-Headers": "ETag",
      },
    });
  } catch (error: any) {
    console.error("R2 proxy upload-part error:", error);
    return json({ error: error.message || "Failed to upload part" }, 500);
  }
};
