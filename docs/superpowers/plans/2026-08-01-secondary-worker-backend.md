# Secondary Worker Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy an isolated Cloudflare Worker backend and Firebase Hosting mirror without changing the existing Cloudflare Pages production deployment.

**Architecture:** Keep relative API URLs as the default for Pages and introduce a Firebase-only API base URL. Route Firebase API traffic to a standalone Worker that reuses existing handler modules but binds to new D1/KV resources and explicitly disables Secret R2.

**Tech Stack:** React 19, TypeScript, Vite, Cloudflare Workers, D1, KV, Wrangler, Firebase Hosting, Node test runner.

## Global Constraints

- Do not modify or deploy the Cloudflare Pages project `hefimer`.
- Do not bind the Worker to production D1 `hefimer-devices`, production KV, or production R2.
- Work only in `D:\Downloads\Web Projects\Hefimer-worker-backend` on `codex/firebase-worker-backend`.
- Allow CORS only for the two Firebase Hosting origins and an explicit local development origin.
- Preserve relative `/api` behavior when `VITE_API_BASE_URL` is absent.

---

### Task 1: Environment-aware API URLs

**Files:**
- Create: `src/api-url.ts`
- Create: `tests/api-url.test.ts`
- Modify: `src/downloads.ts`
- Modify: `src/devices/device-api.ts`
- Modify: `src/App.tsx`
- Modify: `src/Space.tsx`

**Interfaces:**
- Produces: `apiUrl(path: string): string` and `apiPath(path: string): string`.
- Consumes: optional `import.meta.env.VITE_API_BASE_URL`.

- [ ] Write tests proving an empty base returns `/api/...` unchanged and a Worker base returns an absolute Worker URL.
- [ ] Run `npm.cmd test` and confirm the new test fails because `src/api-url.ts` does not exist.
- [ ] Implement normalization that removes trailing slashes and rejects non-HTTPS production bases.
- [ ] Replace each frontend `/api/...` fetch/XHR construction with `apiUrl(...)` while leaving non-API URLs untouched.
- [ ] Update device signing to sign the pathname and query of the resolved Worker URL.
- [ ] Run `npm.cmd test` and `npm.cmd run lint`; expect all checks to pass.
- [ ] Commit with `feat: add configurable secondary api origin`.

### Task 2: Standalone Worker adapter and CORS

**Files:**
- Create: `secondary-worker/src/index.ts`
- Create: `secondary-worker/wrangler.toml`
- Create: `tests/secondary-worker.test.ts`

**Interfaces:**
- Consumes existing `onRequest`, `onRequestGet`, and `onRequestPost` exports under `functions/api`.
- Produces Worker `fetch(request, env, ctx)` and `GET /health`.

- [ ] Write tests for health, blocked origins, allowed preflight, unknown routes, and disabled `/api/r2/*`.
- [ ] Run the targeted test and confirm it fails because the Worker module does not exist.
- [ ] Implement an exact route table for devices, download, Litterbox, tmpfiles, and storage.to handlers.
- [ ] Validate `Origin` before reading request bodies; return `403` for unknown origins.
- [ ] Add exact CORS headers to normal, error, streaming, and preflight responses.
- [ ] Adapt the validated request origin for existing same-origin guards without forwarding any client-supplied internal trust marker.
- [ ] Return `503` for every `/api/r2/*` route.
- [ ] Run all tests and TypeScript checks.
- [ ] Commit with `feat: add isolated secondary worker api`.

### Task 3: Isolated Cloudflare resources

**Files:**
- Modify: `secondary-worker/wrangler.toml`
- Reuse: `migrations/0001_device_groups.sql`

**Interfaces:**
- Worker binding `HEFIMER_DB` points only to `hefimer-secondary-devices`.
- Worker binding `PAIR_TOKENS` points only to a newly created KV namespace.

- [ ] Create D1 database `hefimer-secondary-devices` and record its returned ID in the secondary Worker config.
- [ ] Create a new KV namespace for the secondary Worker and record only that ID.
- [ ] Apply `migrations/0001_device_groups.sql` to the secondary D1.
- [ ] Query `sqlite_master` and verify the expected seven tables exist only in the secondary database.
- [ ] Run `npx.cmd wrangler deploy --dry-run --config secondary-worker/wrangler.toml` and expect a successful bundle.
- [ ] Commit with `chore: bind isolated worker storage`.

### Task 4: Firebase build and Hosting configuration

**Files:**
- Create: `.env.firebase`
- Modify: `firebase.json`
- Create: `.firebaserc`
- Modify: `package.json`
- Modify: `README.md`

**Interfaces:**
- `.env.firebase` sets public `VITE_API_BASE_URL` to the deployed secondary Worker URL.
- Firebase Hosting serves `dist` and rewrites non-file routes to `/index.html`.

- [ ] Add `build:firebase` and `deploy:firebase` scripts.
- [ ] Add Hosting config without changing the existing database-rules config.
- [ ] Set Firebase project alias to `hefimer`.
- [ ] Build with `npm.cmd run build:firebase` and verify the output bundle contains the Worker origin.
- [ ] Build with `npm.cmd run build` and verify the default bundle does not contain the Worker origin.
- [ ] Document the two isolated deployment paths and the disabled Firebase Secret R2 mode.
- [ ] Commit with `feat: configure firebase mirror build`.

### Task 5: Live deployment and end-to-end verification

**Files:**
- Test: `scripts/test-device-groups.mjs`
- Test: `tests/secondary-worker.test.ts`

**Interfaces:**
- Deployed Worker URL is consumed by the Firebase build.
- Firebase Hosting URLs are allowed by Worker CORS.

- [ ] Deploy only `secondary-worker/wrangler.toml`; do not run any Pages deployment command.
- [ ] Verify Worker `/health` and CORS from both Firebase origins.
- [ ] Smoke-test a storage.to download through the Worker and verify bytes, MIME type, filename, and range streaming.
- [ ] Run the device-group E2E against the Worker-backed local Firebase-mode frontend and confirm isolated create, pair, auto-approve, and download.
- [ ] Deploy only Firebase Hosting with `firebase deploy --only hosting --project hefimer`.
- [ ] Run the same smoke flow against the live Firebase Hosting URL.
- [ ] Verify `https://hefimer.qzz.io`, its current JS asset, and a Pages `/api/download` request remain unchanged.
- [ ] Push branch `codex/firebase-worker-backend` without merging it into `main`.
- [ ] Commit final verification notes with `test: verify isolated firebase mirror`.
