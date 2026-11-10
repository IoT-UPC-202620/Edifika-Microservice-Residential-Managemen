package com.residential.management.demo.residential.infrastructure.messaging.events;

/**
 * Publicado por IAM en edifika.events con routing key iam.resident.user-created
 * tras crear el usuario OWNER. Completa el vínculo PENDIENTE.
 */
public record ResidentUserCreatedEvent(
        String messageId,
        String email,
        Long unitId,
        Long idUser
) {
}
