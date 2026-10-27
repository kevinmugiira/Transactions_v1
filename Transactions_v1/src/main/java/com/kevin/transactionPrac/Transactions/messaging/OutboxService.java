package com.kevin.transactionPrac.Transactions.messaging;


import com.kevin.transactionPrac.Transactions.models.OutboxEvent;
import com.kevin.transactionPrac.Transactions.repositories.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outbox;


    /**
     * MANDATORY means "there must already be a database transaction running, or fail". That makes
     * it impossible to stage an event without it committing or rolling back together with the
     * business change that caused it. That guarantee is the whole point of the outbox pattern.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void stage(
            String eventId,
            String topic,
            String key,
            String eventType,
            String payload
    ) {
        outbox.save(new OutboxEvent(eventId, topic, key, eventType, payload));
    }
}
