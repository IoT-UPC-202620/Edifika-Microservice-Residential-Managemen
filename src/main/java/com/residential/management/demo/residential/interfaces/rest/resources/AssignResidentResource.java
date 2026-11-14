package com.residential.management.demo.residential.interfaces.rest.resources;

/** Asignar un usuario existente de IAM (OWNER/TENANT) a una unidad. */
public record AssignResidentResource(Long userId, Long buildingId, Long unitId) {
}
