import { S3Client, PutObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import {
  getSafeFilename,
  isRateLimited,
  isSafeContentType,
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
  const filename = getSafeFilename(url.searchParams.get("filename") || "");
  const contentType = (url.searchParams.get("contentType") || "").trim();

  if (!filename || !isSafeContentType(contentType)) {
    return json({ error: "Invalid filename or content type" }, 400);
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

    const uniqueId = crypto.randomUUID();
    const objectKey = `uploads/${uniqueId}-${filename}`;

    const command = new PutObjectCommand({
      Bucket: env.R2_BUCKET_NAME.trim(),
      Key: objectKey,
      ContentType: contentType,
    });

    const expiresIn = 300;
    const signedUrl = await getSignedUrl(r2Client, command, { expiresIn });

    return json({
      url: signedUrl,
      objectKey,
      expiresAt: Date.now() + expiresIn * 1000,
    });
  } catch {
    return json({ error: "Failed to generate upload URL" }, 500);
  }
};
