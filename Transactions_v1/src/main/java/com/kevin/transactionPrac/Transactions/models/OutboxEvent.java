package com.kevin.transactionPrac.Transactions.models;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 64)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;


    protected OutboxEvent() {}

    public OutboxEvent(
            String eventId,
            String topic,
            String eventKey,
            String eventType,
            String payload) {
        this.eventId = eventId;
        this.topic = topic;
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.payload = payload;
    }
}
