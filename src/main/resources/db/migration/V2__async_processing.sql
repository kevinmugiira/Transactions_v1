ALTER TABLE transactions
    ADD COLUMN failure_reason VARCHAR(255) NULL,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

CREATE TABLE outbox_events (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id    VARCHAR(36) NOT NULL,
    topic       VARCHAR(100) NOT NULL,
    event_key   VARCHAR(64) NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    payload     TEXT NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at     DATETIME(6) NULL,
    CONSTRAINT uq_outbox_event_id UNIQUE  (event_id),
    INDEX idx_outbox_unsent (sent_at, id)
) ENGINE = InnoDB;