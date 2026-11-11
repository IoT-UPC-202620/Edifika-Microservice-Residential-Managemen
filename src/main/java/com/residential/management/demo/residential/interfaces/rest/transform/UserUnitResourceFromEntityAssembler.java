package com.residential.management.demo.residential.interfaces.rest.transform;

import com.residential.management.demo.residential.domain.model.entities.UserUnit;
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
                entity.getResidentEmail(),
                entity.getUnit().getBuilding() != null ? entity.getUnit().getBuilding().getIdBuilding() : null
        );
    }
}