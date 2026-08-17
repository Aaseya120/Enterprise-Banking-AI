package com.bank.transfer.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@SuppressWarnings("null")
public class KafkaProducerConfig {

    // -----------------------------------------------------------------------
    // String-value producer — used by OutboxPublisher which sends
    // pre-serialised JSON strings from the outbox table.
    // -----------------------------------------------------------------------

    @Bean
    public ProducerFactory<String, String> stringProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Idempotent producer -- required so outbox retries can never double-publish
        // the same event as two distinct Kafka records (section 13/14).
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @Qualifier("kafkaStringTemplate")
    public KafkaTemplate<String, String> kafkaStringTemplate(
            @Qualifier("stringProducerFactory") ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }

    // -----------------------------------------------------------------------
    // Object-value producer — used by AuditEventPublisher (common-events)
    // which injects KafkaTemplate<String, Object> and sends POJOs.
    // -----------------------------------------------------------------------

    @Bean
    public ProducerFactory<String, Object> objectProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @Qualifier("kafkaObjectTemplate")
    public KafkaTemplate<String, Object> kafkaObjectTemplate(
            @Qualifier("objectProducerFactory") ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}

