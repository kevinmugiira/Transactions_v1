package com.kevin.transactionPrac.Transactions.dto;

import com.kevin.transactionPrac.Transactions.models.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String ownerId,
        String ownerName,
        BigDecimal balance,
        String currency,
        Instant createdAt) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getOwnerId(),
                a.getOwnerName(),
                a.getBalance(),
                a.getCurrency(),
                a.getCreatedAt());
    }
}
