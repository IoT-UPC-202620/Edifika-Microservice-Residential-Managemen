package com.residential.management.demo.residential.infrastructure.messaging;

import com.residential.management.demo.residential.domain.services.ResidentAssignmentPublisher;
import com.residential.management.demo.residential.infrastructure.messaging.events.ResidentAssignedToUnitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica residential.resident.assigned en edifika.events (JSON, durable en la cola IAM).
 * Sin publisher confirms: patrón del repo (DLQ + consumer idempotente).
 */
@Component
public class RabbitResidentAssignmentPublisher implements ResidentAssignmentPublisher {

    public static final String EXCHANGE = "edifika.events";
    public static final String ROUTING_KEY = "residential.resident.assigned";

    private static final Logger log = LoggerFactory.getLogger(RabbitResidentAssignmentPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitResidentAssignmentPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(ResidentAssignedToUnitEvent event) {
        log.info("Publishing {} to {} (unitId={}, email={})", ROUTING_KEY, EXCHANGE, event.unitId(), event.email());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
