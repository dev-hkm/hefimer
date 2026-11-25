# Hefimer

**Ephemeral 5-digit file sharing, text drops, real-time Spaces, Chat & Whiteboards, and Hefimer Link — built with a strict monochrome Apple Liquid Glass UI across Web (React + Cloudflare Pages) and Native Android (Jetpack Compose).**

---

## Core Features

- **Send & Receive (5-Digit Code + QR)**:
  - Share single files, multiple files, zipped folders, or syntax-highlighted text/code snippets (`CodeHighlighter`).
  - Configurable auto-expiry timers, optional drop passwords, and **One-Time self-destruct** mode.
  - Native **Android Share Sheet** integration (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`, including `clipData` fallback).
- **18-Host Upload Engine + Custom User R2 / S3 Buckets**:
  - Defaults to **`storage.to`** (up to 25 GB via Cloudflare R2 presigned multipart) with **direct-first device IP API calls**, batched `/upload/parts` (25 parts/request), and optional **Free `storage.to` API Token** support in Settings for unlimited daily files & bandwidth.
  - Automatic fallback across **17 backup hosts** (`sto.care`, `x0.at`, `pomf.lain.la`, `qu.ax`, `Catbox`, `Litterbox`, `Buzzheavier`, `Fileditch`, `Gofile`, `VikingFile`, `tfLink`, `TempFile`, `Filebin`, `temp.sh`, `tmpfiles.org`, `Uguu.se`, `Put.icu`) with detailed bilingual (EN/VI) specs.
  - **Custom R2 / S3 Storage (On-Device AWS SigV4)**: Add and manage multiple user-owned Cloudflare R2, AWS S3, Backblaze B2, or MinIO buckets directly in Settings with client-side HMAC-SHA256 presigning—credentials never leave your device.
- **Real-Time Collaboration**:
  - **Spaces**: Infinite shared canvas for files, sticky notes, links, and live cursors via 5-digit codes.
  - **Rooms**: Ephemeral group Chat and collaborative Whiteboards.
  - **Hefimer Link**: QR / 5-digit pairing between Android and browser for instant 1-tap transfers.
- **Native Android Design & UX**:
  - **Strict Monochrome Apple Liquid Glass** (`Dark` & `Light` modes) with `AppMotion` spring physics and a **curved-notch floating bottom navigation bar** displaying the active tab's label underneath the floating orb.
  - **Phosphor Icons** (`com.adamglin:phosphor-icon:1.0.0`) & bilingual adaptive typography (**Urbanist** for English, **Baloo Chettan 2** for Vietnamese, **JetBrains Mono** for codes & telemetry).
  - Dedicated full-screen **Settings** (`SettingsScreen.kt`) including Appearance, Language, Default Launch Tab, Storage & Custom R2/S3, Experience & Safety toggles, **FAQ**, **Privacy & Security**, and **Terms of Service**.

---

## Security Architecture

- **Firebase Realtime Database (`database.rules.json`)**:
  - Root read/write (`/.json`) and collection listing (`/drops.json`, `/spaces.json`, `/chats.json`, `/boards.json`, `/link_codes.json`) are **denied** (`401`). Only exact known IDs can be accessed, with key-format and structure `.validate` rules.
  - `stats/passwords` is completely locked (`.read: false`, `.write: false`), and public `stats/*` metadata is read-only.
- **Cloud Firestore (`firestore.rules`)**:
  - Explicit `allow read, write: if false;` (`rules_version = '2'`) across all documents.
- **Backend Admin & R2 Credentials (`functions/api/admin/[[path]].ts`, `functions/lib/admin-auth.ts`, `functions/lib/r2.ts`)**:
  - Admin login verifies against **salted PBKDF2-SHA256** hashes stored in Cloudflare KV (`PAIR_TOKENS`) and issues **HMAC-SHA256** 12-hour session tokens.
  - Authenticated Admin Console supports updating global system controls (freeze, developer credit, likes, broadcasts), rotating admin passwords, and overriding the 4 Cloudflare R2 backend credentials (`R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`) in KV.
- **Android Hardening**:
  - `android:allowBackup="false"`, `android:usesCleartextTraffic="false"`, R8 minification & resource shrinking enabled on release builds.

---

## Project Structure

```text
Hefimer/
├── android/                        # Native Kotlin + Jetpack Compose Android app
│   └── app/src/main/java/com/hefimer/app/
│       ├── data/local/             # HefimerStorage (SharedPreferences, custom S3/R2, tokens)
│       ├── data/model/             # Models (DropItem, CustomS3Provider, Space/Room/Link)
│       ├── data/network/           # UploadProviders, S3SigV4Signer, DownloadHelper, AdminApiClient, FirebaseRtdbClient
│       └── ui/
│           ├── components/         # FloatingBottomDock, AdminDashboardSheet, InfoAndLegalSheets, Dialogs
│           ├── screens/            # SendScreen, ReceiveScreen, SpaceScreen, RoomsScreen, LinkScreen, SettingsScreen
│           └── theme/              # AppleGlass, AppMotion, Color, Theme, Type (Urbanist + Baloo Chettan 2)
├── functions/                      # Cloudflare Pages Functions backend
│   ├── api/admin/[[path]].ts       # PBKDF2 Admin Auth, stats & live R2 KV config endpoints
│   ├── api/proxy/                  # CORS proxies (storage.to, filebin, put.icu, vikingfile, etc.)
│   ├── api/r2/                     # Presigned R2 upload/multipart/download routes
│   └── lib/                        # admin-auth.ts, r2.ts (KV override), request-guard.ts
├── src/                            # Web React + Vite frontend
├── database.rules.json             # Firebase RTDB security rules
├── firestore.rules                 # Cloud Firestore deny-all rules
└── wrangler.toml                   # Cloudflare Pages D1 + KV bindings
```

---

## Development & Build

### Web & Cloudflare Pages Backend
```bash
npm install
npm run build
npx wrangler pages deploy dist --project-name hefimer
```

### Android (Gradle)
```bash
cd android
gradle testDebugUnitTest assembleRelease -x lintVitalAnalyzeRelease --no-daemon
```
> Set `HEFIMER_KEYSTORE_PATH`, `HEFIMER_KEYSTORE_PASS`, `HEFIMER_KEY_ALIAS`, and `HEFIMER_KEY_PASS` environment variables to sign the Release APK.

### Firebase Rules
```bash
firebase deploy --only database,firestore:rules --project hefimer
```
