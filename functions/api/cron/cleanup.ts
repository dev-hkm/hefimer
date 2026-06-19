import { S3Client, DeleteObjectCommand } from "@aws-sdk/client-s3";

interface Env {
  R2_ACCOUNT_ID: string;
  R2_ACCESS_KEY_ID: string;
  R2_SECRET_ACCESS_KEY: string;
  R2_BUCKET_NAME: string;
  CRON_SECRET?: string;
}

const FIREBASE_URL = "https://hefimer-default-rtdb.asia-southeast1.firebasedatabase.app";

export const onRequest: any = async (context: any) => {
  const { request, env } = context;

  // 1. Verify Secret
  const url = new URL(request.url);
  const secret = url.searchParams.get("secret");
  
  if (!env.CRON_SECRET) {
    return new Response(JSON.stringify({ error: "CRON_SECRET is not configured on the server." }), { 
      status: 503, 
      headers: { "Content-Type": "application/json" } 
    });
  }

  if (secret !== env.CRON_SECRET) {
    return new Response(JSON.stringify({ error: "Unauthorized. Invalid secret." }), { 
      status: 401, 
      headers: { "Content-Type": "application/json" } 
    });
  }

  try {
    // 2. Fetch all drops from Firebase REST API
    const res = await fetch(`${FIREBASE_URL}/drops.json`);
    if (!res.ok) {
      return new Response(JSON.stringify({ error: "Failed to fetch from Firebase" }), { 
        status: 500, 
        headers: { "Content-Type": "application/json" } 
      });
    }

    const drops = await res.json();
    if (!drops) {
      return new Response(JSON.stringify({ success: true, message: "No drops found", deletedCount: 0 }), { 
        status: 200, 
        headers: { "Content-Type": "application/json" } 
      });
    }

    // Initialize R2 client
    let r2Client: S3Client | null = null;
    if (env.R2_ACCOUNT_ID && env.R2_ACCESS_KEY_ID && env.R2_SECRET_ACCESS_KEY && env.R2_BUCKET_NAME) {
      r2Client = new S3Client({
        region: "auto",
        endpoint: `https://${env.R2_ACCOUNT_ID.trim()}.r2.cloudflarestorage.com`,
        credentials: {
          accessKeyId: env.R2_ACCESS_KEY_ID.trim(),
          secretAccessKey: env.R2_SECRET_ACCESS_KEY.trim(),
        },
      });
    }

    const now = Date.now();
    let deletedCount = 0;
    const errors: string[] = [];

    // 3. Loop through drops and check for expiration
    for (const [code, data] of Object.entries(drops) as [string, any][]) {
      if (data.expiresAt && data.expiresAt <= now) {
        let r2DeleteSuccess = true;

        // If it's an R2 file, try to delete it first
        if (data.objectKey && r2Client) {
          try {
            await r2Client.send(new DeleteObjectCommand({
              Bucket: env.R2_BUCKET_NAME.trim(),
              Key: data.objectKey,
            }));
          } catch (r2Err: any) {
            console.error(`R2 delete failed for ${code}:`, r2Err);
            r2DeleteSuccess = false;
            errors.push(`R2 delete failed for ${code}: ${r2Err.message}`);
          }
        }

        // If R2 deletion succeeded (or it wasn't an R2 file), delete from Firebase
        if (r2DeleteSuccess) {
          const fbDeleteRes = await fetch(`${FIREBASE_URL}/drops/${code}.json`, {
            method: "DELETE",
          });
          
          if (fbDeleteRes.ok) {
            deletedCount++;
          } else {
            errors.push(`Firebase delete failed for ${code}`);
          }
        }
      }
    }

    return new Response(JSON.stringify({ 
      success: true, 
      deletedCount,
      errors: errors.length > 0 ? errors : undefined 
    }), { 
      status: 200, 
      headers: { "Content-Type": "application/json" } 
    });

  } catch (error: any) {
    console.error("Cron execution error:", error);
    return new Response(JSON.stringify({ error: "Internal Server Error", details: error.message }), { 
      status: 500, 
      headers: { "Content-Type": "application/json" } 
    });
  }
};
