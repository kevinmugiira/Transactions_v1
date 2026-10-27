package org.kevin.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionCompletedEvent(
        String eventId,
        Long transactionId,
        String type,
        BigDecimal amount,
        Long sourceAccountId,
        Long targetAccountId,
        Instant occuredAt) {
}
