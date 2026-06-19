import express from "express";
import { createServer as createViteServer } from "vite";
import path from "path";
import cors from "cors";
import { S3Client, PutObjectCommand, GetObjectCommand, DeleteObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import dotenv from "dotenv";

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

// API: Presign Upload URL
app.get("/api/r2/presign-upload", async (req, res) => {
  try {
    const { filename, contentType } = req.query;
    if (!filename || !contentType) {
      return res.status(400).json({ error: "Missing filename or contentType" });
    }

    // Validate environment variables
    if (!process.env.R2_ACCOUNT_ID || !process.env.R2_ACCESS_KEY_ID || !process.env.R2_SECRET_ACCESS_KEY || !BUCKET_NAME) {
      console.error("R2 Configuration Missing:", {
        accountId: !!process.env.R2_ACCOUNT_ID,
        accessKey: !!process.env.R2_ACCESS_KEY_ID,
        secretKey: !!process.env.R2_SECRET_ACCESS_KEY,
        bucket: !!BUCKET_NAME
      });
      return res.status(500).json({ error: "R2 storage is not configured on the server. Please check environment variables." });
    }

    // Generate a unique key
    const uniqueId = Math.random().toString(36).substring(2, 15);
    const objectKey = `uploads/${uniqueId}-${filename}`;

    const command = new PutObjectCommand({
      Bucket: BUCKET_NAME,
      Key: objectKey,
      ContentType: contentType as string,
    });

    // Presigned URL valid for 15 minutes
    const url = await getSignedUrl(r2Client, command, { expiresIn: 900 });

    res.json({
      url,
      objectKey,
      expiresAt: Date.now() + 900 * 1000,
    });
  } catch (error) {
    console.error("Presign Upload Error:", error);
    res.status(500).json({ error: "Failed to generate upload URL" });
  }
});

// API: Presign Download URL
app.get("/api/r2/presign-download", async (req, res) => {
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
app.post("/api/r2/delete-object", async (req, res) => {
  try {
    const { objectKey } = req.body;
    if (!objectKey) {
      return res.status(400).json({ error: "Missing objectKey" });
    }

    // Validate key safety
    if (!objectKey.startsWith("uploads/") || objectKey.length > 256) {
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
