package com.kevin.transactionPrac.Transactions.messaging;

import com.kevin.transactionPrac.Transactions.models.OutboxEvent;
import com.kevin.transactionPrac.Transactions.repositories.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.boot.availability.AvailabilityChangeEvent.publish;

@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;


    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:500}")
    public void relay() {
        for (OutboxEvent event : outbox.findTop50BySentAtIsNullOrderByIdAsc()) {
            if (!publish(event)) {
                return;
            }
            outbox.markSent(event.getId(), Instant.now());
        }
    }

    private boolean publish(OutboxEvent event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(event.getEventId(), event.getPayload());
        record.headers().add("eventId", event.getEventId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            kafka.send(record).get(5, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Could not publish outbox event {} ({}): {}", event.getId(), event.getEventType(), e.toString());
            return false;
        }
    }
}
