package com.residential.management.demo.residential.infrastructure.messaging;

import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.infrastructure.messaging.events.ResidentUserCreatedEvent;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UserUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Completa el vínculo PENDIENTE cuando IAM publica iam.resident.user-created.
 * Idempotente: reentregas del mismo evento son no-ops si el link ya está ACTIVE/FAILED
 * o no existe PENDIENTE para (email, unitId).
 *
 * <p>Sin @Transactional de método a propósito: cada operación de repositorio corre en su
 * propia transacción. Si el paso PENDIENTE→ACTIVE viola el índice único parcial de titular
 * (SQLState 23505), esa transacción aborta y el vínculo se marca FAILED en una transacción
 * posterior; con una transacción ambiente, PostgreSQL abortaría todo lo posterior al error
 * y el FAILED no podría persistirse.
 *
 * <p>Dos caminos:
 * <ul>
 *   <li>status=CREATED (o ausente, mensajes viejos del entorno de test) → ACTIVE.</li>
 *   <li>status=REJECTED + reason=EMAIL_ALREADY_REGISTERED → FAILED con motivo
 *       «El correo ya está registrado». Nunca ACTIVE sobre cuenta ajena.</li>
 * </ul>
 */
@Component
public class ResidentLinkCompletionConsumer {

    public static final String REASON_TITULAR_ACTIVO = "La unidad ya tiene un titular activo";
    public static final String REASON_CORREO_REGISTRADO = "El correo ya está registrado";

    private static final String STATUS_REJECTED = "REJECTED";
    private static final String REASON_EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED";
    private static final String STATUS_PENDIENTE = "PENDIENTE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_FAILED = "FAILED";

    private static final Logger log = LoggerFactory.getLogger(ResidentLinkCompletionConsumer.class);

    private final UserUnitRepository userUnitRepository;

    public ResidentLinkCompletionConsumer(UserUnitRepository userUnitRepository) {
        this.userUnitRepository = userUnitRepository;
    }

    @RabbitListener(queues = RabbitMqConfiguration.LINK_COMPLETION_QUEUE)
    public void onResidentUserCreated(ResidentUserCreatedEvent event) {
        log.info("Received iam.resident.user-created (unitId={}, messageId={}, status={})",
                event.unitId(), event.messageId(), event.status());

        var pending = userUnitRepository
                .findByResidentEmailAndUnitIdUnitAndStatus(event.email(), event.unitId(), STATUS_PENDIENTE);

        if (pending.isEmpty()) {
            log.info("No PENDIENTE link for unitId={} — redelivery or already completed, ack no-op",
                    event.unitId());
            return;
        }

        if (isEmailAlreadyRegistered(event)) {
            pending.forEach(link -> fail(link, REASON_CORREO_REGISTRADO, event));
            return;
        }

        pending.forEach(link -> activate(link, event));
    }

    private boolean isEmailAlreadyRegistered(ResidentUserCreatedEvent event) {
        return STATUS_REJECTED.equals(event.status())
                && REASON_EMAIL_ALREADY_REGISTERED.equals(event.reason());
    }

    private void fail(UserUnit link, String reason, ResidentUserCreatedEvent event) {
        link.setStatus(STATUS_FAILED);
        link.setFailureReason(reason);
        userUnitRepository.save(link);
        log.info("Link {} marked FAILED (unitId={}): {}", link.getIdUserUnit(), event.unitId(), reason);
    }

    private void activate(UserUnit link, ResidentUserCreatedEvent event) {
        link.setIdUser(event.idUser());
        link.setStatus(STATUS_ACTIVE);
        link.setFailureReason(null);
        try {
            userUnitRepository.saveAndFlush(link);
            log.info("Link {} activated for user {} (unitId={})",
                    link.getIdUserUnit(), event.idUser(), event.unitId());
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
            // Carrera doble confirmación: otro vínculo ganó el índice único parcial de titular ACTIVE OWNER.
            link.setStatus(STATUS_FAILED);
            link.setFailureReason(REASON_TITULAR_ACTIVO);
            userUnitRepository.save(link);
            log.warn("Link {} marked FAILED (unitId={}): {}",
                    link.getIdUserUnit(), event.unitId(), REASON_TITULAR_ACTIVO);
        }
    }

    private boolean isUniqueViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            var message = cause.getMessage();
            if (message == null) {
                continue;
            }
            var lower = message.toLowerCase();
            if (message.contains("23505")
                    || lower.contains("duplicate key")
                    || lower.contains("unique constraint")) {
                return true;
            }
        }
        return false;
    }
}
