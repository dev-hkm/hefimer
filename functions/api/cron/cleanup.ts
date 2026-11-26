import { deleteR2Object, type R2Env } from "../../lib/r2";

interface Env extends R2Env {
  CRON_SECRET?: string;
}

const FIREBASE_URL = "https://hefimer-default-rtdb.asia-southeast1.firebasedatabase.app";
const EXPIRING_COLLECTIONS = ["drops", "polls", "spaces", "chats", "boards", "link_codes"] as const;
type ExpiringCollection = (typeof EXPIRING_COLLECTIONS)[number];

function extractDropObjectKeys(data: any): string[] {
  return [
    data?.objectKey,
    ...(Array.isArray(data?.files)
      ? data.files.map((fileEntry: any) => fileEntry?.objectKey)
      : []),
  ].filter((key): key is string => typeof key === "string" && key.length > 0);
}

async function cleanupCollection(
  env: Env,
  collection: ExpiringCollection,
  now: number,
  errors: string[],
): Promise<number> {
  const queryUrl = `${FIREBASE_URL}/${collection}.json?orderBy=${encodeURIComponent('"expiresAt"')}&endAt=${now}`;
  const res = await fetch(queryUrl);
  if (!res.ok) {
    errors.push(`Failed to fetch ${collection} from Firebase (${res.status})`);
    return 0;
  }

  const entries = await res.json();
  if (!entries || typeof entries !== "object") {
    return 0;
  }

  let deletedCount = 0;

  for (const [id, data] of Object.entries(entries) as [string, any][]) {
    if (typeof data?.expiresAt !== "number" || data.expiresAt <= 0 || data.expiresAt > now) {
      continue;
    }

    let r2DeleteSuccess = true;
    if (collection === "drops") {
      const objectKeys = extractDropObjectKeys(data);
      // If it has R2-backed files, delete all objects before removing Firebase metadata.
      for (const objectKey of objectKeys) {
        try {
          await deleteR2Object(env, objectKey);
        } catch (r2Err: any) {
          console.error(`R2 delete failed for ${id}:`, r2Err);
          r2DeleteSuccess = false;
          errors.push(`R2 delete failed for ${id}/${objectKey}: ${r2Err.message}`);
        }
      }
    }

    if (r2DeleteSuccess) {
      const fbDeleteRes = await fetch(`${FIREBASE_URL}/${collection}/${id}.json`, {
        method: "DELETE",
      });

      if (fbDeleteRes.ok) {
        deletedCount++;
      } else {
        errors.push(`Firebase delete failed for ${collection}/${id}`);
      }
    }
  }

  return deletedCount;
}

export const onRequest: any = async (context: any) => {
  const { request, env } = context;

  // 1. Verify Secret
  const url = new URL(request.url);
  const secret = url.searchParams.get("secret");

  if (!env.CRON_SECRET) {
    return new Response(JSON.stringify({ error: "CRON_SECRET is not configured on the server." }), {
      status: 503,
      headers: { "Content-Type": "application/json" },
    });
  }

  if (secret !== env.CRON_SECRET) {
    return new Response(JSON.stringify({ error: "Unauthorized. Invalid secret." }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }

  try {
    const now = Date.now();
    let deletedCount = 0;
    const deletedByCollection: Record<string, number> = {};
    const errors: string[] = [];

    for (const collection of EXPIRING_COLLECTIONS) {
      const count = await cleanupCollection(env, collection, now, errors);
      deletedByCollection[collection] = count;
      deletedCount += count;
    }

    return new Response(JSON.stringify({
      success: true,
      deletedCount,
      deletedByCollection,
      errors: errors.length > 0 ? errors : undefined,
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (error: any) {
    console.error("Cron execution error:", error);
    return new Response(JSON.stringify({ error: "Internal Server Error", details: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
};
