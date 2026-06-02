-- schema inicial
CREATE TABLE accounts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_account_id BIGINT NOT NULL,
    destination_account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transfer_source FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transfer_destination FOREIGN KEY (destination_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_transfer_amount_positive CHECK (amount > 0)
);

-- notificacoes pos-transferencia
CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  transfer_id BIGINT NOT NULL,
  message VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notification_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT fk_notification_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id)
);

CREATE INDEX idx_transfers_source ON transfers(source_account_id);
CREATE INDEX idx_transfers_destination ON transfers(destination_account_id);
CREATE INDEX idx_transfers_created_at ON transfers(created_at);
CREATE INDEX idx_notifications_account ON notifications(account_id);
