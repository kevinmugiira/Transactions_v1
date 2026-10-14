package com.kevin.transactionPrac.Transactions.dto;

import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
                                    Long id,
                                    TransactionType type,
                                    BigDecimal amount,
                                    Long sourceAccountId,
                                    Long targetAccountId,
                                    String Description,
                                    Instant createdAt) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getSourceAccountId(),
                t.getTargetAccountId(),
                t.getDescription(),
                t.getCreated_at()
        );
    }
}
