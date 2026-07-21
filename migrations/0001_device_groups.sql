PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS devices (
  id TEXT PRIMARY KEY,
  public_key TEXT NOT NULL,
  name TEXT NOT NULL,
  platform TEXT NOT NULL DEFAULT 'Web',
  created_at INTEGER NOT NULL,
  last_seen_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS device_groups (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  owner_device_id TEXT NOT NULL,
  expires_at INTEGER,
  created_at INTEGER NOT NULL,
  FOREIGN KEY (owner_device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_memberships (
  group_id TEXT NOT NULL,
  device_id TEXT NOT NULL,
  auto_approve INTEGER NOT NULL DEFAULT 0,
  joined_at INTEGER NOT NULL,
  PRIMARY KEY (group_id, device_id),
  FOREIGN KEY (group_id) REFERENCES device_groups(id) ON DELETE CASCADE,
  FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS pair_invites (
  token_hash TEXT PRIMARY KEY,
  group_id TEXT NOT NULL,
  created_by TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  consumed_at INTEGER,
  consumed_by TEXT,
  created_at INTEGER NOT NULL,
  FOREIGN KEY (group_id) REFERENCES device_groups(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transfers (
  id TEXT PRIMARY KEY,
  group_id TEXT NOT NULL,
  sender_device_id TEXT NOT NULL,
  kind TEXT NOT NULL CHECK (kind IN ('file', 'text')),
  name TEXT NOT NULL,
  provider TEXT,
  size_bytes INTEGER NOT NULL DEFAULT 0,
  drop_code TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'cancelled', 'expired')),
  created_at INTEGER NOT NULL,
  cancelled_at INTEGER,
  FOREIGN KEY (group_id) REFERENCES device_groups(id) ON DELETE CASCADE,
  FOREIGN KEY (sender_device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transfer_recipients (
  transfer_id TEXT NOT NULL,
  device_id TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'declined', 'downloading', 'received', 'failed')),
  decision_at INTEGER,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (transfer_id, device_id),
  FOREIGN KEY (transfer_id) REFERENCES transfers(id) ON DELETE CASCADE,
  FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS push_subscriptions (
  id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  endpoint TEXT NOT NULL UNIQUE,
  p256dh TEXT NOT NULL,
  auth TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  last_used_at INTEGER NOT NULL,
  FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auth_nonces (
  nonce TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  expires_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memberships_device ON group_memberships(device_id);
CREATE INDEX IF NOT EXISTS idx_transfers_group_created ON transfers(group_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_recipients_device_updated ON transfer_recipients(device_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_invites_expiry ON pair_invites(expires_at);
CREATE INDEX IF NOT EXISTS idx_nonces_expiry ON auth_nonces(expires_at);
