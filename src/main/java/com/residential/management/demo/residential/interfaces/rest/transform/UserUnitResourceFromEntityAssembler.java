package com.residential.management.demo.residential.interfaces.rest.transform;

import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.model.valueobjects.RelationType;
import com.residential.management.demo.residential.interfaces.rest.resources.UserUnitResource;

public class UserUnitResourceFromEntityAssembler {

    public static UserUnitResource toResourceFromEntity(UserUnit entity) {
        return new UserUnitResource(
                entity.getIdUserUnit(),
                entity.getUnit().getIdUnit(),
                entity.getIdUser(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus(),
                entity.getRelationType() != null ? entity.getRelationType().name() : RelationType.OWNER.name(),
                entity.getResidentEmail(),
                entity.getFailureReason()
        );
    }
}