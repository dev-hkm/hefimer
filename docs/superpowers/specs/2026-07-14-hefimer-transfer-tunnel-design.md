# Hefimer Transfer Tunnel Landing Design

## Goal

Redesign only the first-visit landing experience so Hefimer feels distinctive, premium, three-dimensional, and kinetic while preserving every existing product behavior. The page must communicate only capabilities already present in the app: file transfer, text/code sharing, temporary chat rooms, shared boards, five-digit access codes, optional expiry, and no required account.

## Safety Boundary

- Change the `LandingPage` presentation, its decorative subcomponents, and landing-scoped CSS only.
- Preserve the existing `setActiveTab`, history, likes, policy, FAQ, and developer-name integrations.
- Do not modify send, receive, Firebase, providers, R2, authentication, admin, chat, board, space, Pages Functions, or cleanup logic.
- Do not add a runtime dependency. Use the existing React, Motion, Lucide, SVG, and CSS stack.

## Visual Direction

The page is a cinematic vertical journey through `Send -> Code -> Receive -> Disappear`. Its signature is a tunnel built from the Hefimer orbit mark rather than a generic neon corridor.

Palette:

- Carbon: `#050505`
- Paper: `#F5F4EF`
- Cyan signal: `#55D6FF`
- Violet signal: `#8B78FF`
- Coral signal: `#FF745C`
- Amber signal: `#FFD166`

Typography:

- Display: Syne
- Body: Manrope
- Codes and utility labels: IBM Plex Mono

The accents represent real content lanes rather than decorative gradients: file, text, chat, and board. Black and off-white remain the dominant brand colors.

## Page Structure

1. Full-height hero introduces Hefimer and the real product promise, with primary Start sharing and secondary History actions.
2. A scroll-driven sticky tunnel visualizes a transfer entering the orbit, becoming a five-slot code, arriving at Receive, and disappearing after its timer.
3. Four feature stations explain File, Text, Chat, and Board with small illustrative animations but no invented metrics.
4. The existing FAQ, like widget, policies, developer credit, and footer remain available in the final section.

## Motion

- Orchestrated hero reveal: label, headline, copy, actions, then tunnel.
- Scroll progress drives depth, orbit scale, packet position, five code slots, and disappearance particles.
- Feature stations reveal with staggered clipping and restrained pointer tilt.
- Mobile uses a lighter vertical sequence with reduced 3D depth.
- `prefers-reduced-motion` removes continuous movement and presents every state clearly as a static composition.

## Content Rules

- Reuse or tighten existing factual copy only.
- Do not claim encryption, permanent privacy guarantees, speed benchmarks, usage counts, storage capacity, or unsupported file limits.
- Decorative visualizations must be labeled by function, not presented as measured analytics.

## Verification

- TypeScript check and production build must pass.
- First-visit landing must render at desktop and mobile widths without horizontal overflow.
- Start sharing, History, all four feature actions, FAQ, policy links, and like widget must remain interactive.
- Existing send/receive/provider code must remain unchanged in the final diff.
