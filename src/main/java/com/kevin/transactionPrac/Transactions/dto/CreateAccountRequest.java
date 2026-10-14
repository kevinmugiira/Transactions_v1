package com.kevin.transactionPrac.Transactions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank
        @Size(max = 100)
        String ownerName,

        @Pattern(regexp = "[A-Z]{3}", message = "must be a three letter currency code like KSH")
        String currency
) {
}
