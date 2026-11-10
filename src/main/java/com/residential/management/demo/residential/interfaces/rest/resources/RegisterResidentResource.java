package com.residential.management.demo.residential.interfaces.rest.resources;

/**
 * Alta compuesta de residente: datos personales (van en el evento a IAM, no se persisten aquí)
 * + torre + unidad. Residential guarda solo el vínculo PENDIENTE (por email) e id_user diferido.
 */
public record RegisterResidentResource(
        String fullName,
        String email,
        String phone,
        String documentType,
        String documentNumber,
        Long buildingId,
        Long unitId
) {
}
