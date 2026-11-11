package com.residential.management.demo.residential.interfaces.rest;

import com.residential.management.demo.residential.domain.model.aggregates.Building;
import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.services.ResidentialCommandService;
import com.residential.management.demo.residential.domain.services.ResidentialQueryService;
import com.residential.management.demo.residential.interfaces.rest.resources.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResidentialControllerTest {

    @Mock
    private ResidentialCommandService residentialCommandService;

    @Mock
    private ResidentialQueryService residentialQueryService;

    @InjectMocks
    private ResidentialController residentialController;

    @Test
    void createBuilding() {
        var resource = new CreateBuildingResource(
                "Edificio Central",
                "Av. Principal 123",
                "San Miguel",
                "Lima"
        );

        var building = mock(Building.class);

        when(building.getIdBuilding()).thenReturn(1L);
        when(building.getName()).thenReturn("Edificio Central");
        when(building.getAddress()).thenReturn("Av. Principal 123");
        when(building.getDistrict()).thenReturn("San Miguel");
        when(building.getCity()).thenReturn("Lima");

        when(residentialCommandService.createBuilding(resource))
                .thenReturn(building);

        var result = residentialController.createBuilding(resource);

        assertNotNull(result);
        assertEquals(1L, result.idBuilding());
        assertEquals("Edificio Central", result.name());

        verify(residentialCommandService, times(1))
                .createBuilding(resource);
    }

    @Test
    void createUnit() {
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

        var building = mock(Building.class);
        var unit = mock(Unit.class);

        when(building.getIdBuilding()).thenReturn(1L);

        when(unit.getIdUnit()).thenReturn(10L);
        when(unit.getBuilding()).thenReturn(building);
        when(unit.getUnitNumber()).thenReturn(101);
        when(unit.getFloor()).thenReturn(1);
        when(unit.getCoveredArea()).thenReturn(75.0);
        when(unit.getTotalArea()).thenReturn(90.0);
        when(unit.getParticipationPercentage()).thenReturn(5.5);
        when(unit.getDistributionPercentage()).thenReturn(4.8);
        when(unit.getStatus()).thenReturn("ACTIVE");

        when(residentialCommandService.createUnit(resource))
                .thenReturn(unit);

        var result = residentialController.createUnit(resource);

        assertNotNull(result);
        assertEquals(10L, result.idUnit());
        assertEquals(101, result.unitNumber());

        verify(residentialCommandService, times(1))
                .createUnit(resource);
    }

    @Test
    void assignUserToUnit() {
        var resource = new AssignUserToUnitResource(
                10L,
                1L,
                LocalDateTime.now(),
                null,
                "ACTIVE"
        );

        var unit = mock(Unit.class);
        var userUnit = mock(UserUnit.class);

        when(unit.getIdUnit()).thenReturn(10L);

        when(userUnit.getIdUserUnit()).thenReturn(100L);
        when(userUnit.getUnit()).thenReturn(unit);
        when(userUnit.getIdUser()).thenReturn(1L);
        when(userUnit.getStartDate()).thenReturn(resource.startDate());
        when(userUnit.getEndDate()).thenReturn(null);
        when(userUnit.getStatus()).thenReturn("ACTIVE");

        when(residentialCommandService.assignUserToUnit(resource))
                .thenReturn(userUnit);

        var result = residentialController.assignUserToUnit(resource);

        assertNotNull(result);
        assertEquals(100L, result.idUserUnit());
        assertEquals(1L, result.idUser());

        verify(residentialCommandService, times(1))
                .assignUserToUnit(resource);
    }

    @Test
    void moveUserToUnit() {
        var moveDate = LocalDateTime.now();

        var resource = new MoveUserToUnitResource(
                1L,
                20L,
                moveDate
        );

        var unit = mock(Unit.class);
        var userUnit = mock(UserUnit.class);

        when(unit.getIdUnit()).thenReturn(20L);

        when(userUnit.getIdUserUnit()).thenReturn(200L);
        when(userUnit.getUnit()).thenReturn(unit);
        when(userUnit.getIdUser()).thenReturn(1L);
        when(userUnit.getStartDate()).thenReturn(moveDate);
        when(userUnit.getEndDate()).thenReturn(null);
        when(userUnit.getStatus()).thenReturn("ACTIVE");

        when(residentialCommandService.moveUserToUnit(resource))
                .thenReturn(userUnit);

        var result = residentialController.moveUserToUnit(resource);

        assertNotNull(result);
        assertEquals(200L, result.idUserUnit());
        assertEquals(1L, result.idUser());

        verify(residentialCommandService, times(1))
                .moveUserToUnit(resource);
    }

    @Test
    void getAllBuildings() {
        var building = mock(Building.class);

        when(building.getIdBuilding()).thenReturn(1L);
        when(building.getName()).thenReturn("Edificio Central");
        when(building.getAddress()).thenReturn("Av. Principal 123");
        when(building.getDistrict()).thenReturn("San Miguel");
        when(building.getCity()).thenReturn("Lima");

        when(residentialQueryService.getAllBuildings())
                .thenReturn(List.of(building));

        var result = residentialController.getAllBuildings();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Edificio Central", result.get(0).name());

        verify(residentialQueryService, times(1))
                .getAllBuildings();
    }

    @Test
    void getUnitsByBuildingId() {
        var building = mock(Building.class);
        var unit = mock(Unit.class);

        when(building.getIdBuilding()).thenReturn(1L);

        when(unit.getIdUnit()).thenReturn(10L);
        when(unit.getBuilding()).thenReturn(building);
        when(unit.getUnitNumber()).thenReturn(101);
        when(unit.getFloor()).thenReturn(1);
        when(unit.getCoveredArea()).thenReturn(75.0);
        when(unit.getTotalArea()).thenReturn(90.0);
        when(unit.getParticipationPercentage()).thenReturn(5.5);
        when(unit.getDistributionPercentage()).thenReturn(4.8);
        when(unit.getStatus()).thenReturn("ACTIVE");

        when(residentialQueryService.getUnitsByBuildingId(1L))
                .thenReturn(List.of(unit));

        var result = residentialController.getUnitsByBuildingId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(101, result.get(0).unitNumber());

        verify(residentialQueryService, times(1))
                .getUnitsByBuildingId(1L);
    }

    @Test
    void getResidentsByBuildingId() {
        var unit = mock(Unit.class);
        var userUnit = mock(UserUnit.class);

        when(unit.getIdUnit()).thenReturn(10L);

        when(userUnit.getIdUserUnit()).thenReturn(100L);
        when(userUnit.getUnit()).thenReturn(unit);
        when(userUnit.getIdUser()).thenReturn(1L);
        when(userUnit.getStartDate()).thenReturn(LocalDateTime.now());
        when(userUnit.getEndDate()).thenReturn(null);
        when(userUnit.getStatus()).thenReturn("ACTIVE");

        when(residentialQueryService.getResidentsByBuildingId(1L))
                .thenReturn(List.of(userUnit));

        var result = residentialController.getResidentsByBuildingId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).idUser());

        verify(residentialQueryService, times(1))
                .getResidentsByBuildingId(1L);
    }

    @Test
    void getBuildingById() {
        var building = mock(Building.class);
        when(building.getIdBuilding()).thenReturn(1L);
        when(building.getName()).thenReturn("Edificio Central");
        when(residentialQueryService.getBuildingById(1L)).thenReturn(java.util.Optional.of(building));

        var result = residentialController.getBuildingById(1L);

        assertEquals(1L, result.idBuilding());
        assertEquals("Edificio Central", result.name());
    }

    @Test
    void getBuildingByIdNotFound() {
        when(residentialQueryService.getBuildingById(99L)).thenReturn(java.util.Optional.empty());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> residentialController.getBuildingById(99L));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void getUnitById() {
        var building = mock(Building.class);
        var unit = mock(Unit.class);
        when(building.getIdBuilding()).thenReturn(1L);
        when(unit.getIdUnit()).thenReturn(10L);
        when(unit.getBuilding()).thenReturn(building);
        when(unit.getUnitNumber()).thenReturn(101);
        when(residentialQueryService.getUnitById(10L)).thenReturn(java.util.Optional.of(unit));

        var result = residentialController.getUnitById(10L);

        assertEquals(10L, result.idUnit());
        assertEquals(101, result.unitNumber());
    }

    @Test
    void getActiveUnitOfUserIncludesBuilding() {
        var building = mock(Building.class);
        var unit = mock(Unit.class);
        var userUnit = mock(UserUnit.class);
        when(building.getIdBuilding()).thenReturn(1L);
        when(unit.getIdUnit()).thenReturn(10L);
        when(unit.getBuilding()).thenReturn(building);
        when(userUnit.getIdUserUnit()).thenReturn(100L);
        when(userUnit.getUnit()).thenReturn(unit);
        when(userUnit.getIdUser()).thenReturn(7L);
        when(userUnit.getStatus()).thenReturn("ACTIVE");
        when(residentialQueryService.getActiveUserUnitByUserId(7L)).thenReturn(java.util.Optional.of(userUnit));

        var result = residentialController.getActiveUnitOfUser(7L);

        assertEquals(10L, result.idUnit());
        assertEquals(1L, result.idBuilding());
        assertEquals("ACTIVE", result.status());
    }

    @Test
    void getActiveUnitOfUserNotFound() {
        when(residentialQueryService.getActiveUserUnitByUserId(7L)).thenReturn(java.util.Optional.empty());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> residentialController.getActiveUnitOfUser(7L));
        assertEquals(404, error.getStatusCode().value());
    }
}