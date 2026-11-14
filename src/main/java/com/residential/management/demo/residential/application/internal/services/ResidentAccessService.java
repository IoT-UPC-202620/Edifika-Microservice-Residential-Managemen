package com.residential.management.demo.residential.application.internal.services;

import com.residential.management.demo.residential.domain.exceptions.TitularActivoException;
import com.residential.management.demo.residential.domain.model.aggregates.Building;
import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.model.valueobjects.RelationType;
import com.residential.management.demo.residential.infrastructure.iam.IAMUserClient;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.BuildingRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UnitRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UserUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Asignacion de residentes existentes a unidades y lecturas con control de acceso:
 * ADMIN ve todo; OWNER/TENANT solo lo de sus unidades activas.
 */
@Service
public class ResidentAccessService {

    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> RESIDENT_ROLES = Set.of("OWNER", "TENANT");

    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final UserUnitRepository userUnitRepository;
    private final IAMUserClient iamUserClient;

    public ResidentAccessService(BuildingRepository buildingRepository, UnitRepository unitRepository,
                                 UserUnitRepository userUnitRepository, IAMUserClient iamUserClient) {
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.userUnitRepository = userUnitRepository;
        this.iamUserClient = iamUserClient;
    }

    /**
     * relationType = rol del usuario en IAM (OWNER/TENANT). No crea usuarios en IAM.
     * La regla "ya hay un titular activo" aplica solo a OWNER.
     */
    @Transactional
    public UserUnit assign(Long userId, Long buildingId, Long unitId) {
        if (userId == null || buildingId == null || unitId == null) {
            throw new IllegalArgumentException("userId, buildingId y unitId son obligatorios");
        }
        var unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("La unidad no existe"));
        if (!Objects.equals(unit.getBuilding().getIdBuilding(), buildingId)) {
            throw new IllegalArgumentException("La unidad no pertenece a la torre seleccionada");
        }
        var user = iamUserClient.findUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe en IAM"));
        var role = user.roles() == null ? null
                : user.roles().stream().filter(RESIDENT_ROLES::contains).findFirst().orElse(null);
        if (role == null) {
            throw new IllegalArgumentException("Solo se pueden asignar usuarios OWNER o TENANT");
        }
        var relationType = RelationType.valueOf(role);
        var existing = userUnitRepository.findByIdUserAndUnitIdUnit(userId, unitId);
        if (existing.isPresent() && ACTIVE.equalsIgnoreCase(existing.get().getStatus())) {
            throw new IllegalArgumentException("El usuario ya esta asignado a esta unidad");
        }
        if (relationType == RelationType.OWNER && hasActiveOwner(unitId)) {
            throw new TitularActivoException();
        }
        return userUnitRepository.save(new UserUnit(unit, userId, LocalDateTime.now(), null, ACTIVE, relationType));
    }

    /** Vinculo activo mas reciente del usuario. */
    public Optional<UserUnit> currentResidence(Long userId) {
        if (userId == null) return Optional.empty();
        return userUnitRepository.findByIdUserAndStatusOrderByStartDateDesc(userId, ACTIVE).stream()
                .filter(link -> link.getEndDate() == null)
                .findFirst();
    }

    public Optional<Building> building(Long buildingId, Long userId, boolean admin) {
        if (!admin && activeLinks(userId).stream().noneMatch(l -> Objects.equals(l.getUnit().getBuilding().getIdBuilding(), buildingId))) {
            return Optional.empty();
        }
        return buildingRepository.findById(buildingId);
    }

    public List<Unit> units(Long userId, boolean admin) {
        return admin ? unitRepository.findAll() : activeLinks(userId).stream().map(UserUnit::getUnit).distinct().toList();
    }

    public Optional<Unit> unit(Long unitId, Long userId, boolean admin) {
        if (!admin && activeLinks(userId).stream().noneMatch(l -> Objects.equals(l.getUnit().getIdUnit(), unitId))) {
            return Optional.empty();
        }
        return unitRepository.findById(unitId);
    }

    public List<UserUnit> userUnits(Long userId, boolean admin) {
        if (admin) return userUnitRepository.findAll();
        return userId == null ? List.of() : userUnitRepository.findByIdUser(userId);
    }

    private List<UserUnit> activeLinks(Long userId) {
        if (userId == null) return List.of();
        return userUnitRepository.findByIdUserAndStatusOrderByStartDateDesc(userId, ACTIVE).stream()
                .filter(link -> link.getEndDate() == null)
                .toList();
    }

    private boolean hasActiveOwner(Long unitId) {
        return userUnitRepository.findByUnitIdUnitAndStatus(unitId, ACTIVE).stream()
                .anyMatch(l -> l.getEndDate() == null && l.getRelationType() == RelationType.OWNER);
    }
}
