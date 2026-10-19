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

    @Column(name = "idempotency_key", length = 64)
    private String idempotency_key;


    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant created_at = Instant.now();


    public Transaction(TransactionType type,
                       BigDecimal amount,
                       Long sourceAccountId,
                       Long targetAccountId,
                       String idempotency_key,
                       String description) {
        this.type = type;
        this.amount = amount;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.idempotency_key = idempotency_key;
        this.description = description;
        // PENDING/FAILED become useful once async processing is added
        this.status = TransactionStatus.COMPLETED;
    }

}
