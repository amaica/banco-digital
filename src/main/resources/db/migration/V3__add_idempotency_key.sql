-- idempotencia (evita transferir 2x se cliente der retry)
ALTER TABLE transfers ADD COLUMN idempotency_key VARCHAR(255) NULL;

CREATE UNIQUE INDEX idx_transfers_idempotency_key ON transfers(idempotency_key);
