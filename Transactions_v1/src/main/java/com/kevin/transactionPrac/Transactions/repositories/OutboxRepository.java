package com.kevin.transactionPrac.Transactions.repositories;

import com.kevin.transactionPrac.Transactions.models.OutboxEvent;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50BySentAtIsNullOrderByIdAsc();

    @Transactional
    @Modifying
    @Query("update OutboxEvent e set e.sentAt = :sentAt where e.id = :id")
    int markSent(@Param("id") Long id, @Param("sentAt") Instant sentAt);
}
