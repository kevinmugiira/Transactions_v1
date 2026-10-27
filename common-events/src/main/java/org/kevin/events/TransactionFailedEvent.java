package org.kevin.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Sent with Kafka header eventType=TransactionFailed. */
public record TransactionFailedEvent(
        String eventId,
        Long transactionId,
        String type,
        BigDecimal amount,
        Long sourceAccountId,
        Long targetAccountId,
        String reason,
        Instant occurredAt) {
}
