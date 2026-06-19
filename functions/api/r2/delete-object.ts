import { S3Client, DeleteObjectCommand } from "@aws-sdk/client-s3";
import {
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

  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  if (!isTrustedBrowserRequest(request)) {
    return json({ error: "Cross-origin requests are not allowed" }, 403);
  }

  if (isRateLimited(request)) {
    return json({ error: "Too many requests. Please wait a moment and try again." }, 429);
  }

  try {
    const { objectKey } = await request.json();

    if (!objectKey || !isSafeObjectKey(objectKey)) {
      return json({ error: "Invalid object key" }, 400);
    }

    if (!env.R2_ACCOUNT_ID || !env.R2_ACCESS_KEY_ID || !env.R2_SECRET_ACCESS_KEY || !env.R2_BUCKET_NAME) {
      return json({ error: "R2 storage is not configured on the server." }, 503);
    }

    const r2Client = new S3Client({
      region: "auto",
      endpoint: `https://${env.R2_ACCOUNT_ID.trim()}.r2.cloudflarestorage.com`,
      credentials: {
        accessKeyId: env.R2_ACCESS_KEY_ID.trim(),
        secretAccessKey: env.R2_SECRET_ACCESS_KEY.trim(),
      },
    });

    const command = new DeleteObjectCommand({
      Bucket: env.R2_BUCKET_NAME.trim(),
      Key: objectKey,
    });

    await r2Client.send(command);

    return json({ success: true });
  } catch (error) {
    console.error("Delete object error:", error);
    return json({ error: "Failed to delete object from R2" }, 500);
  }
};
