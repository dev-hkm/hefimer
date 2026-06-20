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
  R2_ACCOUNT_ID: string;
  R2_ACCESS_KEY_ID: string;
  R2_SECRET_ACCESS_KEY: string;
  R2_BUCKET_NAME: string;
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

function client(env: R2Env) {
  if (!env.R2_ACCOUNT_ID || !env.R2_ACCESS_KEY_ID || !env.R2_SECRET_ACCESS_KEY || !env.R2_BUCKET_NAME) {
    throw new Error("Secret storage is not configured.");
  }
  return new S3Client({
    region: "auto",
    endpoint: `https://${env.R2_ACCOUNT_ID.trim()}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: env.R2_ACCESS_KEY_ID.trim(),
      secretAccessKey: env.R2_SECRET_ACCESS_KEY.trim(),
    },
  });
}

export async function createUploadUrl(env: R2Env, filename: string, contentType: string) {
  const objectKey = `hefimer/${crypto.randomUUID()}/${filename}`;
  const url = await getSignedUrl(client(env), new PutObjectCommand({
    Bucket: env.R2_BUCKET_NAME.trim(), Key: objectKey, ContentType: contentType,
  }), { expiresIn: 900 });
  return { objectKey, url };
}

export async function createDownloadUrl(env: R2Env, objectKey: string, filename: string) {
  return getSignedUrl(client(env), new GetObjectCommand({
    Bucket: env.R2_BUCKET_NAME.trim(),
    Key: objectKey,
    ResponseContentDisposition: `attachment; filename*=UTF-8''${encodeURIComponent(filename)}`,
  }), { expiresIn: 900 });
}

export async function deleteR2Object(env: R2Env, objectKey: string) {
  await client(env).send(new DeleteObjectCommand({ Bucket: env.R2_BUCKET_NAME.trim(), Key: objectKey }));
}

export function isSafeUploadId(value: string) {
  return /^[A-Za-z0-9+/_=.-]{1,2048}$/.test(value);
}

export async function createMultipartUpload(env: R2Env, filename: string, contentType: string) {
  const objectKey = `hefimer/${crypto.randomUUID()}/${filename}`;
  const result = await client(env).send(new CreateMultipartUploadCommand({
    Bucket: env.R2_BUCKET_NAME.trim(), Key: objectKey, ContentType: contentType,
  }));
  if (!result.UploadId) throw new Error("Could not start multipart upload");
  return { objectKey, uploadId: result.UploadId };
}

export async function createMultipartPartUrl(env: R2Env, objectKey: string, uploadId: string, partNumber: number) {
  return getSignedUrl(client(env), new UploadPartCommand({
    Bucket: env.R2_BUCKET_NAME.trim(), Key: objectKey, UploadId: uploadId, PartNumber: partNumber,
  }), { expiresIn: 3600 });
}

export async function completeMultipartUpload(
  env: R2Env,
  objectKey: string,
  uploadId: string,
  parts: { ETag: string; PartNumber: number }[],
) {
  await client(env).send(new CompleteMultipartUploadCommand({
    Bucket: env.R2_BUCKET_NAME.trim(),
    Key: objectKey,
    UploadId: uploadId,
    MultipartUpload: { Parts: parts.sort((a, b) => a.PartNumber - b.PartNumber) },
  }));
}

export async function abortMultipartUpload(env: R2Env, objectKey: string, uploadId: string) {
  await client(env).send(new AbortMultipartUploadCommand({
    Bucket: env.R2_BUCKET_NAME.trim(), Key: objectKey, UploadId: uploadId,
  }));
}

export async function uploadObjectProxy(
  env: R2Env,
  objectKey: string,
  body: any,
  contentType: string,
  contentLength?: number,
) {
  await client(env).send(new PutObjectCommand({
    Bucket: env.R2_BUCKET_NAME.trim(),
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
  const result = await client(env).send(new UploadPartCommand({
    Bucket: env.R2_BUCKET_NAME.trim(),
    Key: objectKey,
    UploadId: uploadId,
    PartNumber: partNumber,
    Body: body,
    ContentLength: contentLength,
  }));
  return result.ETag;
}
