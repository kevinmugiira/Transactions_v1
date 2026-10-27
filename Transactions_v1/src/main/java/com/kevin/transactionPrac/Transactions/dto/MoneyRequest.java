package com.kevin.transactionPrac.Transactions.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MoneyRequest(
        @NotNull Long accountId,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(max = 255) String description) {
}
