# Hefimer Transfer Tunnel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the first-visit Hefimer landing page with a premium, multi-color, scroll-driven 3D transfer tunnel without changing product logic.

**Architecture:** Add an isolated visual module for the decorative tunnel and feature stations, with all styles scoped under `.hefimer-landing`. Keep the existing `LandingPage` function in `App.tsx` as the integration boundary so its callbacks, FAQ, likes, policy links, and history behavior remain unchanged.

**Tech Stack:** React 19, TypeScript, Motion, Lucide React, Tailwind CSS 4, scoped CSS

## Global Constraints

- Do not add a runtime dependency.
- Do not modify send, receive, Firebase, providers, R2, authentication, admin, chat, board, space, Pages Functions, or cleanup logic.
- Use only existing factual product capabilities and no invented metrics.
- Preserve responsive behavior, keyboard focus, and `prefers-reduced-motion` support.

---

### Task 1: Isolated Tunnel Visuals

**Files:**
- Create: `src/landing/TransferTunnel.tsx`
- Create: `src/landing/transfer-tunnel.css`

**Interfaces:**
- Produces: `TransferTunnel` with no props and `FeatureStationVisual` with `kind: "file" | "text" | "chat" | "board"`.
- Consumes: only Motion, Lucide icons, React refs, and landing-scoped CSS.

- [ ] **Step 1: Add visual component contracts**

Create exported `TransferTunnel` and `FeatureStationVisual` components. Keep all decorative labels limited to `SEND`, `5-DIGIT CODE`, `RECEIVE`, and `DISAPPEAR`.

- [ ] **Step 2: Build the scroll tunnel**

Use a sticky visual inside a tall scroll track. Drive orbit depth, packet position, code-slot reveal, receiver state, and particles from Motion `useScroll` and `useTransform` values.

- [ ] **Step 3: Add scoped visual styling**

Define the six design tokens on `.hefimer-landing`, build CSS 3D rings, perspective layers, signal trails, feature miniatures, pointer hover depth, responsive breakpoints, visible focus states, and a reduced-motion fallback.

- [ ] **Step 4: Verify the isolated module**

Run: `npm.cmd run lint`

Expected: TypeScript exits with code 0 and neither component imports from Firebase or application logic.

### Task 2: Landing Integration

**Files:**
- Modify: `src/App.tsx:8073-8666`

**Interfaces:**
- Consumes: `TransferTunnel` and `FeatureStationVisual` from `src/landing/TransferTunnel.tsx`.
- Preserves: `LandingPage({ setActiveTab, onOpenHistory, likesCount, showToast, setShowPolicy, developer })`.

- [ ] **Step 1: Replace the old mockup and generic landing grid**

Remove `MockupAppWindow` and replace only the `LandingPage` JSX with the new hero, tunnel, four feature stations, final CTA, and existing supporting content.

- [ ] **Step 2: Preserve every action boundary**

Wire Start sharing and File to `setActiveTab("file")`, Text to `setActiveTab("text")`, Chat to `setActiveTab("chat")`, Board to `setActiveTab("board")`, and History to `onOpenHistory()`. Keep `FAQSection`, `LikeWidget`, policy callbacks, and developer credit.

- [ ] **Step 3: Keep copy factual**

Describe only temporary sharing, the five-digit access code, no required signup, expiration, and the four existing content modes. Do not include capacity, benchmark, encryption, or usage claims.

- [ ] **Step 4: Verify application types and production output**

Run: `npm.cmd run lint`

Expected: exit code 0.

Run: `npm.cmd run build`

Expected: Vite production build completes and writes `dist/`.

### Task 3: Regression and Visual Verification

**Files:**
- Modify only if required by a verified landing defect: `src/landing/TransferTunnel.tsx`, `src/landing/transfer-tunnel.css`, `src/App.tsx`

**Interfaces:**
- Verifies the public landing UI while treating all transfer logic as read-only.

- [ ] **Step 1: Audit the final diff boundary**

Run: `git diff --stat e09ec92..HEAD` and `git diff --check`.

Expected: product code changes are limited to the landing module, landing CSS, and the landing section/imports in `App.tsx`.

- [ ] **Step 2: Check desktop and mobile rendering**

Open `http://localhost:3000` at desktop and mobile widths. Confirm no horizontal overflow, readable copy, complete tunnel stages, and working reduced layout.

- [ ] **Step 3: Check interactions**

Confirm Start sharing, History, four feature stations, FAQ toggles, like widget, Privacy Policy, and Terms of Service respond without console errors.

- [ ] **Step 4: Final verification and checkpoint**

Run: `npm.cmd run lint`

Run: `npm.cmd run build`

Expected: both commands exit with code 0. Commit only the verified landing files and documentation with `feat: redesign landing as transfer tunnel`.
