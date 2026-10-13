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
    private TransactionStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private Long sourceAccountId;
    private Long targetAccountId;
    private String idempotency_key;
    private String description;
    private Instant created_at = Instant.now();


}
