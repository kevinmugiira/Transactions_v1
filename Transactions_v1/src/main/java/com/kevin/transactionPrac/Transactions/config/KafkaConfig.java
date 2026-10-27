package com.kevin.transactionPrac.Transactions.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /** internal work queue: "please process transaction N". Key = account id. */
    public static final String COMMANDS = "transaction.commands";

    /** commands that kept failing end up here for manual verification */
    public static final String COMMANDS_DLT = "transactions.commands.DLT";


    @Bean
    public NewTopic commandsTopic() {
        return TopicBuilder.name(COMMANDS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic commandsDeadLetterTopic() {
        return TopicBuilder.name(COMMANDS_DLT).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(Topics.TRANSACTION_EVENTS).partitions(3).replicas(1).build();
    }


    /**
     * What happens when the listener throws: retry 3 more times, 1 second apart, then park the
     * message on the dead-letter topic so it can't block everything behind it.
     * Business rejections never reach here: the processor turns them into FAILED transactions.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template, (record, exception) -> new TopicPartition(COMMANDS_DLT, -1));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(NumberFormatException.class);
        return handler;
        )
    }


}
