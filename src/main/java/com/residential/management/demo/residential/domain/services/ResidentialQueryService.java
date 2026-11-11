package com.residential.management.demo.residential.domain.services;

import com.residential.management.demo.residential.domain.model.aggregates.Building;
import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;

import java.util.List;
import java.util.Optional;

public interface ResidentialQueryService {

    List<Building> getAllBuildings();

    List<Unit> getUnitsByBuildingId(Long idBuilding);

    List<UserUnit> getResidentsByBuildingId(Long idBuilding);

    Optional<Building> getBuildingById(Long idBuilding);

    Optional<Unit> getUnitById(Long idUnit);

    /** Relación ACTIVE del usuario con una unidad (un usuario vive en una sola unidad a la vez). */
    Optional<UserUnit> getActiveUserUnitByUserId(Long idUser);
}