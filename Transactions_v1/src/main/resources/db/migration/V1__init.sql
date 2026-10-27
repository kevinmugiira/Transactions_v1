CREATE TABLE accounts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_name   VARCHAR(100)  NOT NULL,
    balance      DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency     CHAR(3)       NOT NULL DEFAULT 'KSH',
    version      BIGINT        NOT NULL DEFAULT 0,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_balance CHECK ( balance >= 0 )
) ENGINE=InnoDB;


CREATE TABLE transactions (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    type                 VARCHAR(20)    NOT NULL,
    amount               DECIMAL(19,4)  NOT NULL,
    source_account_id    BIGINT NULL,
    target_account_id    BIGINT NULL,
    status               VARCHAR(20)    NOT NULL,
    idempotency_key      VARCHAR(64)    NULL,
    description          VARCHAR(255)   NULL,
    created_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_amount CHECK ( amount > 0 ),
    CONSTRAINT uq_idem UNIQUE (idempotency_key),
    CONSTRAINT fk_src FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_tgt FOREIGN KEY (target_account_id) REFERENCES accounts(id),
    INDEX idx_tx_source (source_account_id, created_at),
    INDEX idx_tx_target (target_account_id, created_at)
) ENGINE=InnoDB;