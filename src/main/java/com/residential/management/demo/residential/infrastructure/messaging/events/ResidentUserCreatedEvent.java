package com.residential.management.demo.residential.infrastructure.messaging.events;

/**
 * Publicado por IAM en edifika.events con routing key iam.resident.user-created.
 * Completa el vínculo PENDIENTE.
 *
 * <p>{@code status}: "CREATED" (flujo feliz; también ausente en mensajes viejos del entorno
 * de test → se trata como CREATED) o "REJECTED". {@code reason} es nulo salvo rechazo;
 * hoy el único motivo es "EMAIL_ALREADY_REGISTERED" (IAM detectó el duplicado en carrera
 * con el pre-chequeo síncrono). {@code idUser} es nulo cuando status=REJECTED.
 */
public record ResidentUserCreatedEvent(
        String email,
        Long unitId,
        Long idUser,
        String messageId,
        String status,
        String reason
) {
}
