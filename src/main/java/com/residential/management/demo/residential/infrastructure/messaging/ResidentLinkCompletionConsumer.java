package com.residential.management.demo.residential.infrastructure.messaging;

import com.residential.management.demo.residential.infrastructure.messaging.events.ResidentUserCreatedEvent;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UserUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completa el vínculo PENDIENTE cuando IAM publica iam.resident.user-created.
 * Idempotente: reentregas del mismo evento son no-ops si el link ya está ACTIVE
 * o no existe PENDIENTE para (email, unitId).
 */
@Component
public class ResidentLinkCompletionConsumer {

    private static final Logger log = LoggerFactory.getLogger(ResidentLinkCompletionConsumer.class);

    private final UserUnitRepository userUnitRepository;

    public ResidentLinkCompletionConsumer(UserUnitRepository userUnitRepository) {
        this.userUnitRepository = userUnitRepository;
    }

    @RabbitListener(queues = RabbitMqConfiguration.LINK_COMPLETION_QUEUE)
    @Transactional
    public void onResidentUserCreated(ResidentUserCreatedEvent event) {
        log.info("Received iam.resident.user-created (email={}, unitId={}, idUser={}, messageId={})",
                event.email(), event.unitId(), event.idUser(), event.messageId());

        var pending = userUnitRepository
                .findByResidentEmailAndUnitIdUnitAndStatus(event.email(), event.unitId(), "PENDIENTE");

        if (pending.isEmpty()) {
            log.info("No PENDIENTE link for email={} unitId={} — redelivery or already completed, ack no-op",
                    event.email(), event.unitId());
            return;
        }

        pending.forEach(link -> {
            link.setIdUser(event.idUser());
            link.setStatus("ACTIVE");
            userUnitRepository.save(link);
            log.info("Link {} activated for user {} (unit {})", link.getIdUserUnit(), event.idUser(), event.unitId());
        });
    }
}
