package com.residential.management.demo.residential.application.internal.queryservices;

import com.residential.management.demo.residential.domain.model.aggregates.Building;
import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.services.ResidentialQueryService;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.BuildingRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UnitRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UserUnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResidentialQueryServiceImpl implements ResidentialQueryService {

    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final UserUnitRepository userUnitRepository;

    public ResidentialQueryServiceImpl(
            BuildingRepository buildingRepository,
            UnitRepository unitRepository,
            UserUnitRepository userUnitRepository
    ) {
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.userUnitRepository = userUnitRepository;
    }

    @Override
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @Override
    public List<Unit> getUnitsByBuildingId(Long idBuilding) {
        return unitRepository.findByBuildingIdBuilding(idBuilding);
    }

    @Override
    public List<UserUnit> getResidentsByBuildingId(Long idBuilding) {
        return userUnitRepository.findByUnitBuildingIdBuilding(idBuilding);
    }

    @Override
    public Optional<Building> getBuildingById(Long idBuilding) {
        return buildingRepository.findById(idBuilding);
    }

    @Override
    public Optional<Unit> getUnitById(Long idUnit) {
        return unitRepository.findById(idUnit);
    }

    @Override
    public Optional<UserUnit> getActiveUserUnitByUserId(Long idUser) {
        return userUnitRepository.findByIdUserAndStatus(idUser, "ACTIVE");
    }
}