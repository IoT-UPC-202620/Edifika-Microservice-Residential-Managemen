package com.residential.management.demo.residential.interfaces.rest;

import com.residential.management.demo.residential.domain.services.ResidentialCommandService;
import com.residential.management.demo.residential.domain.services.ResidentialQueryService;
import com.residential.management.demo.residential.interfaces.rest.resources.*;
import com.residential.management.demo.residential.interfaces.rest.transform.BuildingResourceFromEntityAssembler;
import com.residential.management.demo.residential.interfaces.rest.transform.UnitResourceFromEntityAssembler;
import com.residential.management.demo.residential.interfaces.rest.transform.UserUnitResourceFromEntityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/residential")
public class ResidentialController {

    private final ResidentialCommandService residentialCommandService;
    private final ResidentialQueryService residentialQueryService;

    public ResidentialController(
            ResidentialCommandService residentialCommandService,
            ResidentialQueryService residentialQueryService
    ) {
        this.residentialCommandService = residentialCommandService;
        this.residentialQueryService = residentialQueryService;
    }

    @PostMapping("/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResource createBuilding(@RequestBody CreateBuildingResource resource) {
        var building = residentialCommandService.createBuilding(resource);
        return BuildingResourceFromEntityAssembler.toResourceFromEntity(building);
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitResource createUnit(@RequestBody CreateUnitResource resource) {
        var unit = residentialCommandService.createUnit(resource);
        return UnitResourceFromEntityAssembler.toResourceFromEntity(unit);
    }

    @PostMapping("/user-units")
    @ResponseStatus(HttpStatus.CREATED)
    public UserUnitResource assignUserToUnit(@RequestBody AssignUserToUnitResource resource) {
        var userUnit = residentialCommandService.assignUserToUnit(resource);
        return UserUnitResourceFromEntityAssembler.toResourceFromEntity(userUnit);
    }

    @PostMapping("/residents")
    @ResponseStatus(HttpStatus.CREATED)
    public UserUnitResource registerResident(@RequestBody RegisterResidentResource resource) {
        var userUnit = residentialCommandService.registerResident(resource);
        return UserUnitResourceFromEntityAssembler.toResourceFromEntity(userUnit);
    }

    @PutMapping("/user-units/move")
    public UserUnitResource moveUserToUnit(@RequestBody MoveUserToUnitResource resource) {
        var userUnit = residentialCommandService.moveUserToUnit(resource);
        return UserUnitResourceFromEntityAssembler.toResourceFromEntity(userUnit);
    }

    @GetMapping("/buildings")
    public List<BuildingResource> getAllBuildings() {
        return residentialQueryService.getAllBuildings()
                .stream()
                .map(BuildingResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @GetMapping("/buildings/{idBuilding}")
    public BuildingResource getBuildingById(@PathVariable Long idBuilding) {
        return residentialQueryService.getBuildingById(idBuilding)
                .map(BuildingResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));
    }

    @GetMapping("/units/{idUnit}")
    public UnitResource getUnitById(@PathVariable Long idUnit) {
        return residentialQueryService.getUnitById(idUnit)
                .map(UnitResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));
    }

    /**
     * Residencia actual del usuario (incluye idBuilding). La usa la app móvil al iniciar
     * sesión, porque el JWT del IAM no conoce edificios.
     */
    @GetMapping("/users/{idUser}/active-unit")
    public UserUnitResource getActiveUnitOfUser(@PathVariable Long idUser) {
        return residentialQueryService.getActiveUserUnitByUserId(idUser)
                .map(UserUnitResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active unit not found for this user"));
    }

    @GetMapping("/buildings/{idBuilding}/units")
    public List<UnitResource> getUnitsByBuildingId(@PathVariable Long idBuilding) {
        return residentialQueryService.getUnitsByBuildingId(idBuilding)
                .stream()
                .map(UnitResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @GetMapping("/buildings/{idBuilding}/residents")
    public List<UserUnitResource> getResidentsByBuildingId(@PathVariable Long idBuilding) {
        return residentialQueryService.getResidentsByBuildingId(idBuilding)
                .stream()
                .map(UserUnitResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
    @PutMapping("/units/{idUnit}")
    public UnitResource updateUnit(
            @PathVariable Long idUnit,
            @RequestBody UpdateUnitResource resource
    ) {
        var unit = residentialCommandService.updateUnit(idUnit, resource);
        return UnitResourceFromEntityAssembler.toResourceFromEntity(unit);
    }
}