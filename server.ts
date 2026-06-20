import express from "express";
import { createServer as createViteServer } from "vite";
import path from "path";
import cors from "cors";
import {
  S3Client,
  PutObjectCommand,
  GetObjectCommand,
  DeleteObjectCommand,
  CreateMultipartUploadCommand,
  UploadPartCommand,
  CompleteMultipartUploadCommand,
  AbortMultipartUploadCommand
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import dotenv from "dotenv";
import { randomUUID } from "crypto";

dotenv.config();

const app = express();
const PORT = Number(process.env.PORT) || 3000;

app.use(cors());
app.use(express.json());

// R2 Configuration
const r2Client = new S3Client({
  region: "auto",
  endpoint: `https://${(process.env.R2_ACCOUNT_ID || "").trim()}.r2.cloudflarestorage.com`,
  credentials: {
    accessKeyId: (process.env.R2_ACCESS_KEY_ID || "").trim(),
    secretAccessKey: (process.env.R2_SECRET_ACCESS_KEY || "").trim(),
  },
});

const BUCKET_NAME = (process.env.R2_BUCKET_NAME || "").trim();

/**
 * RECOMMENDED R2 CORS POLICY:
 * [
 *   {
 *     "AllowedOrigins": ["*"], // Or your specific domain
 *     "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
 *     "AllowedHeaders": ["Content-Type", "x-amz-content-sha256", "x-amz-date", "authorization"],
 *     "ExposeHeaders": ["ETag"],
 *     "MaxAgeSeconds": 3600
 *   }
 * ]
 */

// API: Create a short-lived direct R2 upload URL for local development.
app.get("/api/r2/upload-url", async (req, res) => {
  try {
    const { filename, contentType } = req.query;
    if (!filename) {
      return res.status(400).json({ error: "Missing filename" });
    }
    const objectKey = `hefimer/${randomUUID()}/${filename}`;
    const url = `/api/r2/upload?objectKey=${encodeURIComponent(objectKey)}&contentType=${encodeURIComponent((contentType as string) || "application/octet-stream")}`;
    res.json({ objectKey, url });
  } catch (error) {
    console.error("Upload URL Error:", error);
    res.status(500).json({ error: "Failed to generate upload URL" });
  }
});

// API: Local R2 Single Upload Proxy
app.put("/api/r2/upload", async (req, res) => {
  try {
    const { objectKey, contentType } = req.query;
    if (!objectKey) {
      return res.status(400).json({ error: "Missing objectKey" });
    }

    const contentLength = parseInt(req.headers["content-length"] || "", 10);
    await r2Client.send(new PutObjectCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey as string,
      ContentType: (contentType as string) || "application/octet-stream",
      Body: req,
      ContentLength: isNaN(contentLength) ? undefined : contentLength,
    }));

    res.json({ success: true, objectKey });
  } catch (error: any) {
    console.error("Local R2 Upload Error:", error);
    res.status(500).json({ error: error.message || "Failed to upload to R2" });
  }
});

// API: Init R2 Multipart Upload
app.post("/api/r2/multipart/init", async (req, res) => {
  try {
    const { filename, contentType } = req.body;
    if (!filename || !contentType) {
      return res.status(400).json({ error: "Missing filename or contentType" });
    }
    const objectKey = `hefimer/${randomUUID()}/${filename}`;
    const result = await r2Client.send(new CreateMultipartUploadCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey,
      ContentType: contentType,
    }));
    res.json({ objectKey, uploadId: result.UploadId });
  } catch (error: any) {
    console.error("Local R2 Multipart Init Error:", error);
    res.status(500).json({ error: error.message || "Failed to initialize R2 multipart upload" });
  }
});

// API: R2 Multipart Part URL
app.post("/api/r2/multipart/part-url", async (req, res) => {
  const { objectKey, uploadId, partNumber } = req.body;
  if (!objectKey || !uploadId || !partNumber) {
    return res.status(400).json({ error: "Missing objectKey, uploadId, or partNumber" });
  }
  const partUrl = `/api/r2/multipart/upload-part?objectKey=${encodeURIComponent(objectKey)}&uploadId=${encodeURIComponent(uploadId)}&partNumber=${partNumber}`;
  res.json({ url: partUrl });
});

// API: Local R2 Multipart Upload Part Proxy
app.put("/api/r2/multipart/upload-part", async (req, res) => {
  try {
    const { objectKey, uploadId, partNumber } = req.query;
    if (!objectKey || !uploadId || !partNumber) {
      return res.status(400).json({ error: "Missing objectKey, uploadId, or partNumber" });
    }

    const partNum = parseInt(partNumber as string, 10);
    const contentLength = parseInt(req.headers["content-length"] || "", 10);

    const result = await r2Client.send(new UploadPartCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey as string,
      UploadId: uploadId as string,
      PartNumber: partNum,
      Body: req,
      ContentLength: isNaN(contentLength) ? undefined : contentLength,
    }));

    res.setHeader("ETag", result.ETag || "");
    res.json({ success: true });
  } catch (error: any) {
    console.error("Local R2 Upload Part Error:", error);
    res.status(500).json({ error: error.message || "Failed to upload R2 part" });
  }
});

// API: Local R2 Multipart Complete
app.post("/api/r2/multipart/complete", async (req, res) => {
  try {
    const { objectKey, uploadId, parts } = req.body;
    if (!objectKey || !uploadId || !parts) {
      return res.status(400).json({ error: "Missing objectKey, uploadId, or parts" });
    }
    await r2Client.send(new CompleteMultipartUploadCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey,
      UploadId: uploadId,
      MultipartUpload: { Parts: parts.sort((a: any, b: any) => a.PartNumber - b.PartNumber) },
    }));
    res.json({ success: true });
  } catch (error: any) {
    console.error("Local R2 Multipart Complete Error:", error);
    res.status(500).json({ error: error.message || "Failed to complete R2 multipart upload" });
  }
});

// API: Local R2 Multipart Abort
app.post("/api/r2/multipart/abort", async (req, res) => {
  try {
    const { objectKey, uploadId } = req.body;
    if (!objectKey || !uploadId) {
      return res.status(400).json({ error: "Missing objectKey or uploadId" });
    }
    await r2Client.send(new AbortMultipartUploadCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey,
      UploadId: uploadId,
    }));
    res.json({ success: true });
  } catch (error: any) {
    console.error("Local R2 Multipart Abort Error:", error);
    res.status(500).json({ error: error.message || "Failed to abort R2 multipart upload" });
  }
});

// API: Create a short-lived direct R2 download URL for local development.
app.get("/api/r2/download-url", async (req, res) => {
  try {
    const { objectKey } = req.query;
    if (!objectKey) {
      return res.status(400).json({ error: "Missing objectKey" });
    }

    const command = new GetObjectCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey as string,
      ResponseContentDisposition: `attachment; filename="${(objectKey as string).split('-').slice(1).join('-') || 'download'}"`,
    });

    // Presigned URL valid for 10 minutes
    const url = await getSignedUrl(r2Client, command, { expiresIn: 600 });

    res.json({ url });
  } catch (error) {
    console.error("Presign Download Error:", error);
    res.status(500).json({ error: "Failed to generate download URL" });
  }
});

// API: Delete Object from R2
app.post("/api/r2/delete", async (req, res) => {
  try {
    const { objectKey } = req.body;
    if (!objectKey) {
      return res.status(400).json({ error: "Missing objectKey" });
    }

    // Validate key safety
    if (!objectKey.startsWith("hefimer/") || objectKey.length > 256) {
      return res.status(400).json({ error: "Invalid object key" });
    }

    // Validate environment variables
    if (!process.env.R2_ACCOUNT_ID || !process.env.R2_ACCESS_KEY_ID || !process.env.R2_SECRET_ACCESS_KEY || !BUCKET_NAME) {
      return res.status(500).json({ error: "R2 storage is not configured." });
    }

    const command = new DeleteObjectCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey,
    });

    await r2Client.send(command);
    res.json({ success: true });
  } catch (error) {
    console.error("Delete Object Error:", error);
    res.status(500).json({ error: "Failed to delete object from R2" });
  }
});

// API: Cron Cleanup
app.get("/api/cron/cleanup", async (req, res) => {
  const secret = req.query.secret;
  if (!process.env.CRON_SECRET) {
    return res.status(503).json({ error: "CRON_SECRET is not configured." });
  }
  if (secret !== process.env.CRON_SECRET) {
    return res.status(401).json({ error: "Unauthorized." });
  }

  try {
    const FIREBASE_URL = "https://hefimer-default-rtdb.asia-southeast1.firebasedatabase.app";
    const fbRes = await fetch(`${FIREBASE_URL}/drops.json`);
    if (!fbRes.ok) throw new Error("Failed to fetch Firebase drops");
    const drops = await fbRes.json();
    if (!drops) return res.json({ success: true, deletedCount: 0 });

    const now = Date.now();
    let deletedCount = 0;
    const errors: string[] = [];

    for (const [code, data] of Object.entries(drops) as [string, any][]) {
      if (data.expiresAt && data.expiresAt <= now) {
        let r2DeleteSuccess = true;
        if (data.objectKey) {
          try {
            await r2Client.send(new DeleteObjectCommand({
              Bucket: BUCKET_NAME,
              Key: data.objectKey,
            }));
          } catch (r2Err: any) {
            console.error(`R2 delete failed for ${code}:`, r2Err);
            r2DeleteSuccess = false;
            errors.push(`R2 delete failed for ${code}`);
          }
        }
        if (r2DeleteSuccess) {
          const deleteRes = await fetch(`${FIREBASE_URL}/drops/${code}.json`, { method: "DELETE" });
          if (deleteRes.ok) deletedCount++;
          else errors.push(`Firebase delete failed for ${code}`);
        }
      }
    }

    res.json({ success: true, deletedCount, errors: errors.length > 0 ? errors : undefined });
  } catch (error: any) {
    console.error("Cron Error:", error);
    res.status(500).json({ error: "Cron execution failed" });
  }
});

// API: Proxy Litterbox Upload
app.post("/api/proxy/litterbox", async (req, res) => {
  try {
    const targetUrl = "https://litterbox.catbox.moe/resources/internals/api.php";
    const contentType = req.headers["content-type"];

    const headers: Record<string, string> = {};
    if (contentType) {
      headers["content-type"] = contentType;
    }

    const response = await fetch(targetUrl, {
      method: "POST",
      headers,
      body: req as any,
      duplex: "half",
    } as any);

    const bodyText = await response.text();
    res.status(response.status).send(bodyText);
  } catch (error: any) {
    console.error("Litterbox Proxy Error:", error);
    res.status(500).json({ error: error.message || "Litterbox proxy failed" });
  }
});

// API: Proxy storage.to
app.all("/api/proxy/storageto/*", async (req, res) => {
  try {
    const subpath = req.params[0] || "";
    const targetUrl = new URL(`https://storage.to/api/${subpath}`);
    const searchParams = new URLSearchParams(req.query as any);
    targetUrl.search = searchParams.toString();

    const headers: Record<string, string> = {};
    const headerKeys = [
      "content-type",
      "x-visitor-token",
      "x-owner-token",
      "authorization",
    ];
    for (const key of headerKeys) {
      const val = req.headers[key];
      if (typeof val === "string") {
        headers[key] = val;
      }
    }

    let requestBody: any = null;
    if (req.method !== "GET" && req.method !== "HEAD") {
      const contentType = req.headers["content-type"] || "";
      if (contentType.includes("application/json")) {
        requestBody = JSON.stringify(req.body);
      } else {
        requestBody = req;
      }
    }

    const response = await fetch(targetUrl.toString(), {
      method: req.method,
      headers,
      body: requestBody,
      duplex: "half",
    } as any);

    const bodyText = await response.text();
    res.status(response.status).send(bodyText);
  } catch (error: any) {
    console.error("storage.to Proxy Error:", error);
    res.status(500).json({ error: error.message || "storage.to proxy failed" });
  }
});

// API: Proxy tmpfiles.org Upload
app.post("/api/proxy/tmpfiles", async (req, res) => {
  try {
    const targetUrl = "https://tmpfiles.org/api/v1/upload";
    const contentType = req.headers["content-type"];

    const headers: Record<string, string> = {};
    if (contentType) {
      headers["content-type"] = contentType;
    }

    const response = await fetch(targetUrl, {
      method: "POST",
      headers,
      body: req as any,
      duplex: "half",
    } as any);

    const bodyText = await response.text();
    res.status(response.status).send(bodyText);
  } catch (error: any) {
    console.error("tmpfiles Proxy Error:", error);
    res.status(500).json({ error: error.message || "tmpfiles proxy failed" });
  }
});

async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
