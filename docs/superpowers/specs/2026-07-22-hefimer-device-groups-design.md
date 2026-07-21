# Hefimer Device Groups

## Goal

Add accountless, persistent device pairing without changing the existing five-digit-code, provider upload, R2 secret-mode, chat, board, or space flows.

## Product model

- A device owns a locally generated ECDSA P-256 identity.
- A device can create or join one active Hefimer Group.
- Pairing uses a single-use `hfm-` token containing 36 random alphanumeric characters. QR codes encode the same token in a Hefimer URL.
- Group lifetime options are 12 hours, 24 hours, 7 days, 30 days, and never.
- Every successful file or text drop is offered to the sender's group after the legacy drop has been saved.
- Every other device decides independently: approve, decline, or auto-approve.
- Approved recipients can open the existing receive flow. The sender sees per-device delivery state and can cancel a transfer.
- Web Push announces new offers while the app is closed. Foreground clients poll for durable state so missed events recover automatically.

## Backend

- Cloudflare Pages Functions provide the signed device API.
- D1 stores devices, groups, memberships, transfers, recipients, push subscriptions, invite audit rows, and replay nonces.
- KV stores short-lived invite-token lookups with TTL; D1 remains authoritative for single-use consumption.
- Mutation requests are signed with the device's ECDSA private key. The server stores only the public key.
- Transfer payload is returned only to approved recipients, the sender, or the group owner.

## Compatibility boundary

The feature listens for `hefimer:drop-created` events emitted only after existing Firebase writes succeed. Device API failures never fail or roll back an existing upload. No provider upload or download implementation is replaced.

