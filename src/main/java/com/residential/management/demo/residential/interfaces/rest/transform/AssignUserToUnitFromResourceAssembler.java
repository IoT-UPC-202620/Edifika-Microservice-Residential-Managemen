package com.residential.management.demo.residential.interfaces.rest.transform;

import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.model.valueobjects.RelationType;
import com.residential.management.demo.residential.interfaces.rest.resources.AssignUserToUnitResource;

public class AssignUserToUnitFromResourceAssembler {

    public static UserUnit toEntityFromResource(AssignUserToUnitResource resource, Unit unit) {
        return new UserUnit(
                unit,
                resource.idUser(),
                resource.startDate(),
                resource.endDate(),
                resource.status(),
                parseRelationType(resource.relationType())
        );
    }

    public static RelationType parseRelationType(String relationType) {
        if (relationType == null || relationType.isBlank()) {
            return RelationType.OWNER;
        }
        try {
            return RelationType.valueOf(relationType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de relación inválido: " + relationType);
        }
    }
}