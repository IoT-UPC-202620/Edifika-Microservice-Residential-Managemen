package com.residential.management.demo.residential.application.internal.commandservices;

import com.residential.management.demo.residential.domain.exceptions.EmailAlreadyRegisteredException;
import com.residential.management.demo.residential.domain.exceptions.TitularActivoException;
import com.residential.management.demo.residential.domain.model.aggregates.Building;
import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.model.valueobjects.RelationType;
import com.residential.management.demo.residential.domain.services.ResidentialCommandService;
import com.residential.management.demo.residential.domain.services.ResidentAssignmentPublisher;
import com.residential.management.demo.residential.infrastructure.iam.IAMEmailVerificationClient;
import com.residential.management.demo.residential.infrastructure.messaging.events.ResidentAssignedToUnitEvent;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.BuildingRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UnitRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UserUnitRepository;
import com.residential.management.demo.residential.interfaces.rest.resources.*;
import com.residential.management.demo.residential.interfaces.rest.transform.AssignUserToUnitFromResourceAssembler;
import com.residential.management.demo.residential.interfaces.rest.transform.CreateBuildingFromResourceAssembler;
import com.residential.management.demo.residential.interfaces.rest.transform.CreateUnitFromResourceAssembler;
import com.residential.management.demo.residential.interfaces.rest.transform.UpdateUnitFromResourceAssembler;
import org.springframework.stereotype.Service;

@Service
public class ResidentialCommandServiceImpl implements ResidentialCommandService {

    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final UserUnitRepository userUnitRepository;
    private final ResidentAssignmentPublisher residentAssignmentPublisher;
    private final IAMEmailVerificationClient iamEmailVerificationClient;



    public ResidentialCommandServiceImpl(
            BuildingRepository buildingRepository,
            UnitRepository unitRepository,
            UserUnitRepository userUnitRepository,
            ResidentAssignmentPublisher residentAssignmentPublisher,
            IAMEmailVerificationClient iamEmailVerificationClient
    ) {
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.userUnitRepository = userUnitRepository;
        this.residentAssignmentPublisher = residentAssignmentPublisher;
        this.iamEmailVerificationClient = iamEmailVerificationClient;
    }

    @Override
    public Building createBuilding(CreateBuildingResource resource) {
        var building = CreateBuildingFromResourceAssembler.toEntityFromResource(resource);
        return buildingRepository.save(building);
    }

    @Override
    public Unit createUnit(CreateUnitResource resource) {
        var building = buildingRepository.findById(resource.idBuilding())
                .orElseThrow(() -> new RuntimeException("Building not found"));

        var unit = CreateUnitFromResourceAssembler.toEntityFromResource(resource, building);
        return unitRepository.save(unit);
    }

    @Override
    public UserUnit assignUserToUnit(AssignUserToUnitResource resource) {
        var unit = unitRepository.findById(resource.idUnit())
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        var existingAssignment = userUnitRepository
                .findByIdUserAndUnitIdUnit(resource.idUser(), resource.idUnit());

        if (existingAssignment.isPresent()) {
            throw new RuntimeException("User is already assigned to this unit");
        }

        var relationType = AssignUserToUnitFromResourceAssembler.parseRelationType(resource.relationType());
        var isActiveOwner = isActive(resource.status()) && relationType == RelationType.OWNER;
        if (isActiveOwner && hasActiveTitular(resource.idUnit())) {
            throw new TitularActivoException();
        }

        var userUnit = AssignUserToUnitFromResourceAssembler.toEntityFromResource(resource, unit);
        return userUnitRepository.save(userUnit);
    }


    @Override
    public UserUnit moveUserToUnit(MoveUserToUnitResource resource) {
        var newUnit = unitRepository.findById(resource.newIdUnit())
                .orElseThrow(() -> new RuntimeException("New unit not found"));

        if (hasActiveTitular(resource.newIdUnit())) {
            throw new TitularActivoException();
        }

        var currentAssignment = userUnitRepository
                .findByIdUserAndStatus(resource.idUser(), "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Active assignment not found for this user"));

        currentAssignment.setEndDate(resource.moveDate());
        currentAssignment.setStatus("INACTIVE");
        userUnitRepository.save(currentAssignment);

        var newAssignment = new UserUnit(
                newUnit,
                resource.idUser(),
                resource.moveDate(),
                null,
                "ACTIVE"
        );

        return userUnitRepository.save(newAssignment);
    }
    @Override
    public Unit updateUnit(Long idUnit, UpdateUnitResource resource) {

        var unit = unitRepository.findById(idUnit)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        var building = buildingRepository.findById(resource.idBuilding())
                .orElseThrow(() -> new RuntimeException("Building not found"));

        UpdateUnitFromResourceAssembler.toEntityFromResource(unit, resource, building);

        return unitRepository.save(unit);
    }

    @Override
    // Sin @Transactional a propósito: el save() hace commit antes de publicar el evento,
    // así el consumidor y el completado nunca ven un link que no existe.
    public UserUnit registerResident(RegisterResidentResource resource) {
        if (resource.email() == null || resource.email().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (resource.fullName() == null || resource.fullName().isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }

        var unit = unitRepository.findById(resource.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        var building = buildingRepository.findById(resource.buildingId())
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));

        if (!java.util.Objects.equals(unit.getBuilding().getIdBuilding(), building.getIdBuilding())) {
            throw new IllegalArgumentException("La unidad no pertenece a la torre seleccionada");
        }

        var normalizedEmail = resource.email().trim().toLowerCase();

        // E2: un titular ACTIVE OWNER ya vinculado bloquea el registro (PENDIENTE no bloquea).
        // Guard local antes de llamar a IAM: sin vínculo ni eventos si hay titular activo.
        if (hasActiveTitular(unit.getIdUnit())) {
            throw new TitularActivoException();
        }

        // E3 pre-chequeo síncrono contra IAM (fail closed): sin vínculo ni eventos
        // si el correo ya existe o si IAM no se pudo consultar.
        if (iamEmailVerificationClient.emailExists(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        // Idempotente a nivel de aplicación: si ya existe un link PENDIENTE para
        // (email, unitId), lo reutilizamos y republicamos el evento.
        var pending = userUnitRepository
                .findByResidentEmailAndUnitIdUnitAndStatus(normalizedEmail, unit.getIdUnit(), "PENDIENTE");
        if (!pending.isEmpty()) {
            var existing = pending.get(0);
            residentAssignmentPublisher.publish(toEvent(resource, normalizedEmail, unit));
            return existing;
        }

        var link = new UserUnit(unit, normalizedEmail, RelationType.OWNER);
        var saved = userUnitRepository.save(link);
        residentAssignmentPublisher.publish(toEvent(resource, normalizedEmail, unit));
        return saved;
    }

    private boolean isActive(String status) {
        return status != null && status.trim().equalsIgnoreCase("ACTIVE");
    }

    private boolean hasActiveTitular(Long idUnit) {
        return userUnitRepository.findByUnitIdUnitAndStatus(idUnit, "ACTIVE")
                .stream()
                .anyMatch(assignment ->
                        assignment.getEndDate() == null
                                && assignment.getRelationType() == RelationType.OWNER);
    }

    private ResidentAssignedToUnitEvent toEvent(RegisterResidentResource resource,
                                                 String normalizedEmail,
                                                 Unit unit) {
        return new ResidentAssignedToUnitEvent(
                java.util.UUID.randomUUID().toString(),
                resource.fullName().trim(),
                normalizedEmail,
                resource.phone(),
                resource.documentType(),
                resource.documentNumber(),
                unit.getBuilding().getIdBuilding(),
                unit.getIdUnit(),
                unit.getUnitNumber(),
                unit.getFloor(),
                java.time.LocalDateTime.now().toString()
        );
    }
}