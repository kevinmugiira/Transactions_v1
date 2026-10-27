package com.kevin.transactionPrac.Transactions.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "source_account_Id")
    private Long sourceAccountId;

    @Column(name = "target_account_id")
    private Long targetAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "initiated_by", length = 36)
    private String initiatedBy;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant created_at = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updated_at = Instant.now();


    public Transaction(TransactionType type,
                       BigDecimal amount,
                       Long sourceAccountId,
                       Long targetAccountId,
                       String initiatedBy,
                       String idempotencyKey,
                       String description) {
        this.type = type;
        this.amount = amount;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.initiatedBy = initiatedBy;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.status = TransactionStatus.PENDING;
    }

    public Transaction() {

    }

    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.updated_at = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason != null && reason.length() > 255 ? reason.substring(0, 255) : reason;
        this.updated_at = Instant.now();
    }

    /**
     * The account whose events must stay in order. It is used as the Kafka message key, which puts
     * all work for one account on the same partition, processed one message at a time.
     */
    public Long routingAccountId() {
        return sourceAccountId != null ? sourceAccountId : targetAccountId;
    }
}
