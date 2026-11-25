import "./polyfill";
import {
  AbortMultipartUploadCommand,
  CompleteMultipartUploadCommand,
  CreateMultipartUploadCommand,
  DeleteObjectCommand,
  GetObjectCommand,
  PutObjectCommand,
  S3Client,
  UploadPartCommand,
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";

export interface R2Env {
  PAIR_TOKENS?: KVNamespace;
  R2_ACCOUNT_ID: string;
  R2_ACCESS_KEY_ID: string;
  R2_SECRET_ACCESS_KEY: string;
  R2_BUCKET_NAME: string;
}

interface R2ConfigOverride {
  accountId?: string;
  accessKeyId?: string;
  secretAccessKey?: string;
  bucketName?: string;
  updatedAt?: number;
}

const R2_CONFIG_KV_KEY = "hefimer_r2_config_v1";

export async function resolveR2Env(env: R2Env): Promise<{
  accountId: string;
  accessKeyId: string;
  secretAccessKey: string;
  bucketName: string;
  source: "kv" | "env";
}> {
  let kvCfg: R2ConfigOverride | null = null;
  if (env.PAIR_TOKENS) {
    kvCfg = await env.PAIR_TOKENS.get<R2ConfigOverride>(R2_CONFIG_KV_KEY, "json").catch(() => null);
  }
  const accountId = (kvCfg?.accountId || env.R2_ACCOUNT_ID || "").trim();
  const accessKeyId = (kvCfg?.accessKeyId || env.R2_ACCESS_KEY_ID || "").trim();
  const secretAccessKey = (kvCfg?.secretAccessKey || env.R2_SECRET_ACCESS_KEY || "").trim();
  const bucketName = (kvCfg?.bucketName || env.R2_BUCKET_NAME || "").trim();
  const hasKv = Boolean(kvCfg?.accountId || kvCfg?.accessKeyId || kvCfg?.secretAccessKey || kvCfg?.bucketName);
  return {
    accountId,
    accessKeyId,
    secretAccessKey,
    bucketName,
    source: hasKv ? "kv" : "env",
  };
}

export async function getR2ConfigSummary(env: R2Env) {
  const cfg = await resolveR2Env(env);
  return {
    accountId: cfg.accountId,
    accessKeyId: cfg.accessKeyId,
    secretAccessKey: cfg.secretAccessKey ? `${"•".repeat(8)}${cfg.secretAccessKey.slice(-4)}` : "",
    bucketName: cfg.bucketName,
    source: cfg.source,
    configured: Boolean(cfg.accountId && cfg.accessKeyId && cfg.secretAccessKey && cfg.bucketName),
  };
}

export async function saveR2ConfigOverride(env: R2Env, input: R2ConfigOverride) {
  if (!env.PAIR_TOKENS) throw new Error("PAIR_TOKENS KV is not bound");
  const current = await resolveR2Env(env);
  const next: R2ConfigOverride = {
    accountId: input.accountId?.trim() || current.accountId,
    accessKeyId: input.accessKeyId?.trim() || current.accessKeyId,
    secretAccessKey:
      input.secretAccessKey && !input.secretAccessKey.includes("•")
        ? input.secretAccessKey.trim()
        : current.secretAccessKey,
    bucketName: input.bucketName?.trim() || current.bucketName,
    updatedAt: Date.now(),
  };
  if (!next.accountId || !next.accessKeyId || !next.secretAccessKey || !next.bucketName) {
    throw new Error("All 4 R2 credentials are required");
  }
  await env.PAIR_TOKENS.put(R2_CONFIG_KV_KEY, JSON.stringify(next));
  return getR2ConfigSummary(env);
}

export function getSafeFilename(value: string) {
  const filename = value.normalize("NFKC").trim();
  if (!filename || filename.length > 180 || filename.includes("..") || /[\\/\u0000-\u001f\u007f]/.test(filename)) return null;
  return filename;
}

export function isSafeContentType(value: string) {
  return /^[a-z][a-z0-9!#$&^_.+-]{0,126}\/[a-z0-9!#$&^_.+-]+(?:;\s*charset=[a-z0-9._-]+)?$/i.test(value);
}

export function isSafeObjectKey(value: string) {
  const match = /^hefimer\/[0-9a-f-]{36}\/(.+)$/i.exec(value);
  return Boolean(match && getSafeFilename(match[1]));
}

export function filenameFromObjectKey(value: string) {
  const match = /^hefimer\/[0-9a-f-]{36}\/(.+)$/i.exec(value);
  return match ? getSafeFilename(match[1]) : null;
}

async function createClient(env: R2Env) {
  const cfg = await resolveR2Env(env);
  if (!cfg.accountId || !cfg.accessKeyId || !cfg.secretAccessKey || !cfg.bucketName) {
    throw new Error("Secret storage is not configured.");
  }
  return {
    bucket: cfg.bucketName,
    s3: new S3Client({
      region: "auto",
      endpoint: `https://${cfg.accountId}.r2.cloudflarestorage.com`,
      credentials: {
        accessKeyId: cfg.accessKeyId,
        secretAccessKey: cfg.secretAccessKey,
      },
    }),
  };
}

export async function createUploadUrl(env: R2Env, filename: string, contentType: string) {
  const { s3, bucket } = await createClient(env);
  const objectKey = `hefimer/${crypto.randomUUID()}/${filename}`;
  const url = await getSignedUrl(s3, new PutObjectCommand({
    Bucket: bucket, Key: objectKey, ContentType: contentType,
  }), { expiresIn: 900 });
  return { objectKey, url };
}

export async function createDownloadUrl(env: R2Env, objectKey: string, filename: string) {
  const { s3, bucket } = await createClient(env);
  return getSignedUrl(s3, new GetObjectCommand({
    Bucket: bucket,
    Key: objectKey,
    ResponseContentDisposition: `attachment; filename*=UTF-8''${encodeURIComponent(filename)}`,
  }), { expiresIn: 900 });
}

export async function deleteR2Object(env: R2Env, objectKey: string) {
  const { s3, bucket } = await createClient(env);
  await s3.send(new DeleteObjectCommand({ Bucket: bucket, Key: objectKey }));
}

export function isSafeUploadId(value: string) {
  return /^[A-Za-z0-9+/_=.-]{1,2048}$/.test(value);
}

export async function createMultipartUpload(env: R2Env, filename: string, contentType: string) {
  const { s3, bucket } = await createClient(env);
  const objectKey = `hefimer/${crypto.randomUUID()}/${filename}`;
  const result = await s3.send(new CreateMultipartUploadCommand({
    Bucket: bucket, Key: objectKey, ContentType: contentType,
  }));
  if (!result.UploadId) throw new Error("Could not start multipart upload");
  return { objectKey, uploadId: result.UploadId };
}

export async function createMultipartPartUrl(env: R2Env, objectKey: string, uploadId: string, partNumber: number) {
  const { s3, bucket } = await createClient(env);
  return getSignedUrl(s3, new UploadPartCommand({
    Bucket: bucket, Key: objectKey, UploadId: uploadId, PartNumber: partNumber,
  }), { expiresIn: 3600 });
}

export async function completeMultipartUpload(
  env: R2Env,
  objectKey: string,
  uploadId: string,
  parts: { ETag: string; PartNumber: number }[],
) {
  const { s3, bucket } = await createClient(env);
  await s3.send(new CompleteMultipartUploadCommand({
    Bucket: bucket,
    Key: objectKey,
    UploadId: uploadId,
    MultipartUpload: { Parts: parts.sort((a, b) => a.PartNumber - b.PartNumber) },
  }));
}

export async function abortMultipartUpload(env: R2Env, objectKey: string, uploadId: string) {
  const { s3, bucket } = await createClient(env);
  await s3.send(new AbortMultipartUploadCommand({
    Bucket: bucket, Key: objectKey, UploadId: uploadId,
  }));
}

export async function uploadObjectProxy(
  env: R2Env,
  objectKey: string,
  body: any,
  contentType: string,
  contentLength?: number,
) {
  const { s3, bucket } = await createClient(env);
  await s3.send(new PutObjectCommand({
    Bucket: bucket,
    Key: objectKey,
    ContentType: contentType,
    Body: body,
    ContentLength: contentLength,
  }));
}

export async function uploadMultipartPartProxy(
  env: R2Env,
  objectKey: string,
  uploadId: string,
  partNumber: number,
  body: any,
  contentLength?: number,
) {
  const { s3, bucket } = await createClient(env);
  const result = await s3.send(new UploadPartCommand({
    Bucket: bucket,
    Key: objectKey,
    UploadId: uploadId,
    PartNumber: partNumber,
    Body: body,
    ContentLength: contentLength,
  }));
  return result.ETag;
}
