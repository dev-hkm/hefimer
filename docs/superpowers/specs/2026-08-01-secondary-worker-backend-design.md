# Secondary Worker Backend Design

## Objective

Create a second Hefimer deployment path for Firebase Hosting backed by a standalone Cloudflare Worker. The existing Cloudflare Pages deployment at `https://hefimer.qzz.io` must remain unchanged and continue using same-origin Pages Functions.

## Safety Boundary

- Development happens only on branch `codex/firebase-worker-backend` in the isolated worktree `D:\Downloads\Web Projects\Hefimer-worker-backend`.
- The `main` branch, Pages project `hefimer`, and `hefimer.qzz.io` deployment are not modified or redeployed.
- The secondary Worker uses a new D1 database and a new KV namespace. It never binds to `hefimer-devices` or the production `PAIR_TOKENS` namespace.
- Secret R2 mode remains available only on the Pages deployment. The Firebase mirror does not receive production R2 credentials or access to the production bucket.
- Public provider uploads and downloads may use the same third-party providers because those files are already temporary and external to Hefimer storage.

## Architecture

The current frontend keeps relative `/api/...` URLs by default. A small API URL helper reads `VITE_API_BASE_URL` only for the Firebase build. Therefore:

- Cloudflare Pages build: `/api/...` continues to resolve to Pages Functions.
- Firebase Hosting build: `/api/...` resolves to the standalone Worker origin.

The Worker uses an explicit route table to adapt the existing Pages Function handlers. It validates the browser `Origin` against a fixed allowlist, handles preflight requests, preserves streaming responses, and adds CORS headers. The Worker rewrites only the internal request origin passed to existing handlers so their same-origin request guard remains effective.

## Secondary Resources

- Worker: `hefimer-secondary-api`
- D1: `hefimer-secondary-devices`
- KV: a new namespace bound as `PAIR_TOKENS`
- Firebase Hosting project: `hefimer`
- Firebase API build mode: `firebase`

The secondary D1 receives the existing device-group schema, so Hefimer Link can operate independently inside the Firebase mirror. Groups, devices, invitations, and transfer states do not cross between Firebase and Pages.

## API Scope

The Worker serves:

- `GET /health`
- `/api/devices/*`
- `GET /api/download`
- `POST /api/proxy/litterbox`
- `POST /api/proxy/tmpfiles`
- `/api/proxy/storageto/*`

Requests to `/api/r2/*` return a clear `503` response stating that Secret R2 mode is unavailable on the Firebase mirror. This prevents accidental production-bucket access.

## CORS Policy

Allowed production origins are exact matches:

- `https://hefimer.web.app`
- `https://hefimer.firebaseapp.com`

Local Worker development may additionally allow `http://127.0.0.1:4176` through a local-only Wrangler variable. Allowed request headers include `Content-Type`, `Range`, `Authorization`, `X-Visitor-Token`, `X-Owner-Token`, and the four `X-Hefimer-*` device-signing headers. Download headers are exposed to the browser.

## Firebase Hosting

Firebase Hosting serves `dist` with SPA fallback to `index.html`. The build runs in Firebase mode and embeds only the public Worker base URL. Firebase Realtime Database remains the existing project dependency for core drops, text, rooms, and boards; deploying Hosting does not deploy or change database rules.

## Failure Behavior

- Unknown Worker routes return JSON `404`.
- Disallowed origins return JSON `403` without permissive CORS headers.
- R2 routes return JSON `503` on the Firebase mirror.
- Existing provider errors retain their current status and body.
- The Worker never falls back to or proxies through the Pages production deployment.

## Verification

1. Unit tests verify API URL selection, route matching, CORS allow/deny behavior, preflight headers, and R2 isolation.
2. Existing tests and TypeScript checks remain green.
3. Worker dry-run succeeds before deployment.
4. The new D1 schema is applied only to `hefimer-secondary-devices`.
5. Live Worker health and provider download streaming are smoke-tested.
6. Firebase Hosting is deployed only after the Worker tests pass.
7. The Pages domain is checked before and after deployment to confirm its asset and API behavior are unchanged.

## Rollback

- Git restore point: branch `codex/checkpoint-pre-worker-backend` at commit `c91a319`.
- Worker rollback uses Wrangler deployment versions or deletion of the secondary Worker only.
- Firebase Hosting rollback uses Firebase release history.
- No rollback action targets the Pages project or production D1/KV resources.
