package com.residential.management.demo.residential.infrastructure.messaging.events;

/**
 * Publicado en edifika.events con routing key residential.resident.assigned.
 * Lleva los datos personales porque IAM es quien los persiste al crear el usuario.
 */
public record ResidentAssignedToUnitEvent(
        String messageId,
        String fullName,
        String email,
        String phone,
        String documentType,
        String documentNumber,
        Long buildingId,
        Long unitId,
        Integer unitNumber,
        Integer unitFloor,
        String assignedAt
) {
}
