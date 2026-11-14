package com.residential.management.demo.residential.interfaces.rest.resources;

/** GET /residential/residents/me */
public record ResidenceResource(Long buildingId, String buildingName, Long unitId, Integer unitNumber,
                                String relationType) {
}
