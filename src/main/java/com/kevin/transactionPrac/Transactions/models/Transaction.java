package com.kevin.transactionPrac.Transactions.models;


import sun.jvm.hotspot.debugger.cdbg.EnumType;
import jakarta.persistent.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
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
