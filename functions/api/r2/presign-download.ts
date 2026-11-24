import { S3Client, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import {
  filenameFromObjectKey,
  isRateLimited,
  isSafeObjectKey,
  isTrustedBrowserRequest,
  json,
} from "../../lib/r2-security";

interface Env {
  R2_ACCOUNT_ID: string;
  R2_ACCESS_KEY_ID: string;
  R2_SECRET_ACCESS_KEY: string;
  R2_BUCKET_NAME: string;
}

export const onRequest: any = async (context: any) => {
  const { request, env } = context;

  if (request.method !== "GET") {
    return json({ error: "Method not allowed" }, 405);
  }

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  if (isRateLimited(request)) {
    return json({ error: "Too many requests. Please wait a moment and try again." }, 429);
  }

  const url = new URL(request.url);
  const objectKey = url.searchParams.get("objectKey");
  const filename = objectKey ? filenameFromObjectKey(objectKey) : null;

  if (!objectKey || !filename || !isSafeObjectKey(objectKey)) {
    return json({ error: "Invalid object key" }, 400);
  }

  if (!env.R2_ACCOUNT_ID || !env.R2_ACCESS_KEY_ID || !env.R2_SECRET_ACCESS_KEY || !env.R2_BUCKET_NAME) {
    return json({ error: "R2 storage is not configured on the server." }, 503);
  }

  try {
    const r2Client = new S3Client({
      region: "auto",
      endpoint: `https://${env.R2_ACCOUNT_ID.trim()}.r2.cloudflarestorage.com`,
      credentials: {
        accessKeyId: env.R2_ACCESS_KEY_ID.trim(),
        secretAccessKey: env.R2_SECRET_ACCESS_KEY.trim(),
      },
    });

    const command = new GetObjectCommand({
      Bucket: env.R2_BUCKET_NAME.trim(),
      Key: objectKey,
      ResponseContentDisposition: `attachment; filename*=UTF-8''${encodeURIComponent(filename)}`,
    });

    const signedUrl = await getSignedUrl(r2Client, command, { expiresIn: 300 });

    return json({ url: signedUrl });
  } catch {
    return json({ error: "Failed to generate download URL" }, 500);
  }
};
