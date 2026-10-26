-- Accounts now belong to a user (the "sub" claim of their JWT).
-- Rows created before this migration have no owner: only admins can see them.
ALTER TABLE accounts
    ADD COLUMN owner_id VARCHAR(36) NULL,
    ADD INDEX idx_accounts_owner (owner_id);

-- Idempotency keys used to be unique across ALL users, which would let user B replay user A's key
-- and receive A's transaction back. They are now unique per initiating user.
ALTER TABLE transactions DROP INDEX uq_idem;
ALTER TABLE transactions ADD COLUMN initiated_by VARCHAR(36) NULL;
ALTER TABLE transactions ADD CONSTRAINT uq_tx_initiator_idempotency UNIQUE (initiated_by, idempotency_key);