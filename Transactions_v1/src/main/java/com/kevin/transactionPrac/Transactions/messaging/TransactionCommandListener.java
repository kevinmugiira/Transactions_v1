package com.kevin.transactionPrac.Transactions.messaging;


import com.kevin.transactionPrac.Transactions.config.KafkaConfig;
import com.kevin.transactionPrac.Transactions.services.TransactionProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


/**
 * The async worker. Each message is just the id of a PENDING transaction.
 * With 3 partitions and concurrency=3, up to 3 transactions are processed at the same time, but
 * everything keyed by the same account is handled in order on one thread.
 */

@Component
@RequiredArgsConstructor
public class TransactionCommandListener {

    private final TransactionProcessor processor;


    @KafkaListener(topics = KafkaConfig.COMMANDS, groupId = "transaction-processor")
    public void onCommand(String transctionId) {
        processor.process(Long.parseLong(transctionId));
    }
}
