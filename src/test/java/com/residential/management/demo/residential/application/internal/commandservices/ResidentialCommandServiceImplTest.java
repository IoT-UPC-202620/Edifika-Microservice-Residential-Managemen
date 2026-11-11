package com.residential.management.demo.residential.application.internal.commandservices;

import com.residential.management.demo.residential.domain.exceptions.EmailAlreadyRegisteredException;
import com.residential.management.demo.residential.domain.exceptions.EmailVerificationUnavailableException;
import com.residential.management.demo.residential.domain.exceptions.TitularActivoException;
import com.residential.management.demo.residential.domain.services.ResidentAssignmentPublisher;
import com.residential.management.demo.residential.infrastructure.iam.IAMEmailVerificationClient;
import com.residential.management.demo.residential.domain.model.aggregates.Building;
import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.model.valueobjects.RelationType;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.BuildingRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UnitRepository;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UserUnitRepository;
import com.residential.management.demo.residential.interfaces.rest.resources.AssignUserToUnitResource;
import com.residential.management.demo.residential.interfaces.rest.resources.CreateBuildingResource;
import com.residential.management.demo.residential.interfaces.rest.resources.CreateUnitResource;
import com.residential.management.demo.residential.interfaces.rest.resources.MoveUserToUnitResource;
import com.residential.management.demo.residential.interfaces.rest.resources.RegisterResidentResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResidentialCommandServiceImplTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserUnitRepository userUnitRepository;

    @Mock
    private ResidentAssignmentPublisher residentAssignmentPublisher;

    @Mock
    private IAMEmailVerificationClient iamEmailVerificationClient;

    @InjectMocks
    private ResidentialCommandServiceImpl residentialCommandService;

    @Test
    void createBuildingSuccessfully() {
        var resource = new CreateBuildingResource(
                "Edificio Central",
                "Av. Principal 123",
                "San miguel",
                "Lima"
        );

        var savedBuilding = mock(Building.class);

        when(buildingRepository.save(any(Building.class)))
                .thenReturn(savedBuilding);

        var result = residentialCommandService.createBuilding(resource);

        assertNotNull(result);
        assertEquals(savedBuilding, result);

        verify(buildingRepository, times(1))
                .save(any(Building.class));
    }

    @Test
    void createUnitSuccessfully() {
        var building = mock(Building.class);

        var resource = new CreateUnitResource(
                1L,
                101,
                1,
                75.0,
                90.0,
                5.5,
                4.8,
                "ACTIVE"
        );

        var savedUnit = mock(Unit.class);

        when(buildingRepository.findById(1L))
                .thenReturn(Optional.of(building));

        when(unitRepository.save(any(Unit.class)))
                .thenReturn(savedUnit);

        var result = residentialCommandService.createUnit(resource);

        assertNotNull(result);
        assertEquals(savedUnit, result);

        verify(buildingRepository, times(1)).findById(1L);
        verify(unitRepository, times(1)).save(any(Unit.class));
    }


    @Test
    void assignUserToUnitSuccessfully() {
        var unit = mock(Unit.class);

        var resource = new AssignUserToUnitResource(
                10L,
                1L,
                LocalDateTime.now(),
                null,
                "ACTIVE",
                "OWNER"
        );

        var savedUserUnit = mock(UserUnit.class);

        when(unitRepository.findById(10L))
                .thenReturn(Optional.of(unit));

        when(userUnitRepository.findByIdUserAndUnitIdUnit(1L, 10L))
                .thenReturn(Optional.empty());

        when(userUnitRepository.save(any(UserUnit.class)))
                .thenReturn(savedUserUnit);

        var result = residentialCommandService.assignUserToUnit(resource);

        assertNotNull(result);
        assertEquals(savedUserUnit, result);

        verify(unitRepository, times(1)).findById(10L);
        verify(userUnitRepository, times(1))
                .findByIdUserAndUnitIdUnit(1L, 10L);
        verify(userUnitRepository, times(1))
                .save(any(UserUnit.class));
    }


    @Test
    void assignUserToUnitThrowsExceptionWhenUserAlreadyAssigned() {
        var unit = mock(Unit.class);
        var existingAssignment = mock(UserUnit.class);

        var resource = new AssignUserToUnitResource(
                10L, // idUnit
                1L,  // idUser
                LocalDateTime.now(),
                null,
                "ACTIVE",
                "OWNER"
        );

        when(unitRepository.findById(10L))
                .thenReturn(Optional.of(unit));

        when(userUnitRepository.findByIdUserAndUnitIdUnit(1L, 10L))
                .thenReturn(Optional.of(existingAssignment));

        var exception = assertThrows(RuntimeException.class,
                () -> residentialCommandService.assignUserToUnit(resource));

        assertEquals("User is already assigned to this unit", exception.getMessage());

        verify(userUnitRepository, never())
                .save(any(UserUnit.class));
    }

    private static final String TITULAR_MESSAGE = "La unidad ya tiene un titular activo";

    @Test
    void assignUserToUnitBlocksWhenUnitAlreadyHasActiveTitular() {
        var unit = mock(Unit.class);
        var existingTitular = new UserUnit(unit, 99L, LocalDateTime.now(), null, "ACTIVE", RelationType.OWNER);

        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(userUnitRepository.findByIdUserAndUnitIdUnit(10L, 1L)).thenReturn(Optional.empty());
        when(userUnitRepository.findByUnitIdUnitAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(existingTitular));

        var resource = new AssignUserToUnitResource(
                1L, 10L, LocalDateTime.now(), null, "ACTIVE", "OWNER");

        var exception = assertThrows(TitularActivoException.class,
                () -> residentialCommandService.assignUserToUnit(resource));

        assertEquals(TITULAR_MESSAGE, exception.getMessage());
        verify(userUnitRepository, never()).save(any(UserUnit.class));
    }

    @Test
    void assignUserToUnitAllowsWhenExistingTitularIsInactive() {
        var unit = mock(Unit.class);

        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(userUnitRepository.findByIdUserAndUnitIdUnit(10L, 1L)).thenReturn(Optional.empty());
        when(userUnitRepository.findByUnitIdUnitAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of());
        when(userUnitRepository.save(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resource = new AssignUserToUnitResource(
                1L, 10L, LocalDateTime.now(), null, "ACTIVE", "OWNER");

        var result = residentialCommandService.assignUserToUnit(resource);

        assertNotNull(result);
        verify(userUnitRepository, times(1)).save(any(UserUnit.class));
    }

    @Test
    void assignUserToUnitAllowsTenantEvenWithActiveTitular() {
        var unit = mock(Unit.class);

        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(userUnitRepository.findByIdUserAndUnitIdUnit(10L, 1L)).thenReturn(Optional.empty());
        when(userUnitRepository.save(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resource = new AssignUserToUnitResource(
                1L, 10L, LocalDateTime.now(), null, "ACTIVE", "TENANT");

        var result = residentialCommandService.assignUserToUnit(resource);

        assertNotNull(result);
        verify(userUnitRepository, times(1)).save(any(UserUnit.class));
    }

    @Test
    void moveUserToUnitSuccessfully() {
        var currentAssignment = mock(UserUnit.class);
        var newUnit = mock(Unit.class);

        var moveDate = LocalDateTime.now();

        var resource = new MoveUserToUnitResource(
                1L,
                20L,
                moveDate
        );

        var newAssignment = mock(UserUnit.class);

        when(userUnitRepository.findByIdUserAndStatus(1L, "ACTIVE"))
                .thenReturn(Optional.of(currentAssignment));

        when(unitRepository.findById(20L))
                .thenReturn(Optional.of(newUnit));

        when(userUnitRepository.save(any(UserUnit.class)))
                .thenReturn(newAssignment);

        var result = residentialCommandService.moveUserToUnit(resource);

        assertNotNull(result);
        assertEquals(newAssignment, result);

        verify(currentAssignment).setEndDate(moveDate);
        verify(currentAssignment).setStatus("INACTIVE");

        verify(userUnitRepository, times(2))
                .save(any(UserUnit.class));
    }

    @Test
    void moveUserToUnitThrowsExceptionWhenActiveAssignmentNotFound() {
        var newUnit = mock(Unit.class);

        var resource = new MoveUserToUnitResource(
                1L,
                20L,
                LocalDateTime.now()
        );

        when(unitRepository.findById(20L))
                .thenReturn(Optional.of(newUnit));
        when(userUnitRepository.findByUnitIdUnitAndStatus(20L, "ACTIVE"))
                .thenReturn(List.of());
        when(userUnitRepository.findByIdUserAndStatus(1L, "ACTIVE"))
                .thenReturn(Optional.empty());

        var exception = assertThrows(RuntimeException.class,
                () -> residentialCommandService.moveUserToUnit(resource));

        assertEquals("Active assignment not found for this user", exception.getMessage());

        verify(userUnitRepository, never())
                .save(any(UserUnit.class));
    }

    @Test
    void registerResidentCreatesPendingLinkAndPublishesEvent() {
        var building = mock(Building.class);
        when(building.getIdBuilding()).thenReturn(1L);
        var unit = mock(Unit.class);
        when(unit.getIdUnit()).thenReturn(7L);
        when(unit.getBuilding()).thenReturn(building);
        when(unit.getUnitNumber()).thenReturn(501);
        when(unit.getFloor()).thenReturn(5);

        when(unitRepository.findById(7L)).thenReturn(Optional.of(unit));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(java.util.List.of());
        when(userUnitRepository.findByUnitIdUnitAndStatus(7L, "ACTIVE"))
                .thenReturn(List.of());
        when(iamEmailVerificationClient.emailExists("ana@example.com"))
                .thenReturn(false);
        when(userUnitRepository.save(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resource = new RegisterResidentResource(
                "Ana Pérez", "Ana@Example.com ", "999888777", "DNI", "12345678", 1L, 7L);

        var saved = residentialCommandService.registerResident(resource);

        assertEquals("PENDIENTE", saved.getStatus());
        assertEquals("ana@example.com", saved.getResidentEmail());
        assertNull(saved.getIdUser());
        verify(residentAssignmentPublisher).publish(argThat(event ->
                event.email().equals("ana@example.com")
                        && event.unitId().equals(7L)
                        && event.buildingId().equals(1L)
                        && event.messageId() != null));
    }

    private RegisterResidentResource registerResidentResource() {
        return new RegisterResidentResource(
                "Ana Pérez", "Ana@Example.com ", "999888777", "DNI", "12345678", 1L, 7L);
    }

    private Unit stubUnitAndBuilding() {
        var building = mock(Building.class);
        when(building.getIdBuilding()).thenReturn(1L);
        var unit = mock(Unit.class);
        when(unit.getIdUnit()).thenReturn(7L);
        when(unit.getBuilding()).thenReturn(building);

        when(unitRepository.findById(7L)).thenReturn(Optional.of(unit));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        return unit;
    }

    @Test
    void registerResidentBlocksWhenUnitAlreadyHasActiveTitular() {
        stubUnitAndBuilding();
        var unit = mock(Unit.class);
        var titular = new UserUnit(unit, 99L, LocalDateTime.now(), null, "ACTIVE", RelationType.OWNER);
        when(userUnitRepository.findByUnitIdUnitAndStatus(7L, "ACTIVE"))
                .thenReturn(List.of(titular));

        var exception = assertThrows(TitularActivoException.class,
                () -> residentialCommandService.registerResident(registerResidentResource()));

        assertEquals("La unidad ya tiene un titular activo", exception.getMessage());
        verify(userUnitRepository, never()).save(any(UserUnit.class));
        verify(residentAssignmentPublisher, never()).publish(any());
        verifyNoInteractions(iamEmailVerificationClient);
    }

    @Test
    void registerResidentBlocksWhenEmailAlreadyRegistered() {
        stubUnitAndBuilding();
        when(userUnitRepository.findByUnitIdUnitAndStatus(7L, "ACTIVE")).thenReturn(List.of());
        when(iamEmailVerificationClient.emailExists("ana@example.com"))
                .thenReturn(true);

        var exception = assertThrows(EmailAlreadyRegisteredException.class,
                () -> residentialCommandService.registerResident(registerResidentResource()));

        assertEquals("El correo ya está registrado", exception.getMessage());
        verify(userUnitRepository, never()).save(any(UserUnit.class));
        verify(residentAssignmentPublisher, never()).publish(any());
    }

    @Test
    void registerResidentFailsClosedWhenIamIsUnavailable() {
        stubUnitAndBuilding();
        when(userUnitRepository.findByUnitIdUnitAndStatus(7L, "ACTIVE")).thenReturn(List.of());
        when(iamEmailVerificationClient.emailExists("ana@example.com"))
                .thenThrow(new EmailVerificationUnavailableException());

        var exception = assertThrows(EmailVerificationUnavailableException.class,
                () -> residentialCommandService.registerResident(registerResidentResource()));

        assertEquals("No se pudo verificar el correo; inténtalo de nuevo", exception.getMessage());
        verify(userUnitRepository, never()).save(any(UserUnit.class));
        verify(residentAssignmentPublisher, never()).publish(any());
    }

    @Test
    void registerResidentSucceedsWhenNoActiveTitularAndEmailNotRegistered() {
        var unit = stubUnitAndBuilding();
        when(unit.getUnitNumber()).thenReturn(501);
        when(unit.getFloor()).thenReturn(5);
        when(userUnitRepository.findByUnitIdUnitAndStatus(7L, "ACTIVE")).thenReturn(List.of());
        when(iamEmailVerificationClient.emailExists("ana@example.com"))
                .thenReturn(false);
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(List.of());
        when(userUnitRepository.save(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var saved = residentialCommandService.registerResident(registerResidentResource());

        assertEquals("PENDIENTE", saved.getStatus());
        verify(userUnitRepository, times(1)).save(any(UserUnit.class));
        verify(residentAssignmentPublisher, times(1)).publish(any());
    }

    @Test
    void moveUserToUnitThrowsExceptionWhenNewUnitNotFound() {
        var resource = new MoveUserToUnitResource(
                1L,
                20L,
                LocalDateTime.now()
        );

        when(unitRepository.findById(20L))
                .thenReturn(Optional.empty());

        var exception = assertThrows(RuntimeException.class,
                () -> residentialCommandService.moveUserToUnit(resource));

        assertEquals("New unit not found", exception.getMessage());
    }
}