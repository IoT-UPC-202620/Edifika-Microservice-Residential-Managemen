package com.residential.management.demo.residential.interfaces.rest;

import com.residential.management.demo.residential.application.internal.services.ResidentAccessService;
import com.residential.management.demo.residential.infrastructure.authorization.sfs.CurrentUser;
import com.residential.management.demo.residential.interfaces.rest.resources.AssignResidentResource;
import com.residential.management.demo.residential.interfaces.rest.resources.BuildingResource;
import com.residential.management.demo.residential.interfaces.rest.resources.ResidenceResource;
import com.residential.management.demo.residential.interfaces.rest.resources.UnitResource;
import com.residential.management.demo.residential.interfaces.rest.resources.UserUnitResource;
import com.residential.management.demo.residential.interfaces.rest.transform.BuildingResourceFromEntityAssembler;
import com.residential.management.demo.residential.interfaces.rest.transform.UnitResourceFromEntityAssembler;
import com.residential.management.demo.residential.interfaces.rest.transform.UserUnitResourceFromEntityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Asignacion de residentes existentes y lecturas con control de acceso por unidad (evita IDOR).
 * Los 404 se devuelven como respuesta (no excepcion) porque /error no es publico.
 */
@RestController
@RequestMapping("/api/v1/residential")
public class ResidentAccessController {

    private final ResidentAccessService service;

    public ResidentAccessController(ResidentAccessService service) {
        this.service = service;
    }

    /** Solo ADMIN: asigna un usuario OWNER/TENANT existente a una unidad. */
    @PostMapping("/residents/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserUnitResource> assign(@RequestBody AssignResidentResource resource) {
        var link = service.assign(resource.userId(), resource.buildingId(), resource.unitId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserUnitResourceFromEntityAssembler.toResourceFromEntity(link));
    }

    /** Unidad activa del usuario del token; 404 si no tiene. */
    @GetMapping("/residents/me")
    public ResponseEntity<?> me() {
        return service.currentResidence(CurrentUser.id())
                .<ResponseEntity<?>>map(link -> ResponseEntity.ok(new ResidenceResource(
                        link.getUnit().getBuilding().getIdBuilding(),
                        link.getUnit().getBuilding().getName(),
                        link.getUnit().getIdUnit(),
                        link.getUnit().getUnitNumber(),
                        link.getRelationType() == null ? null : link.getRelationType().name())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "El usuario no tiene una unidad asignada")));
    }

    @GetMapping("/buildings/{idBuilding}")
    public ResponseEntity<BuildingResource> building(@PathVariable Long idBuilding) {
        return service.building(idBuilding, CurrentUser.id(), CurrentUser.isAdmin())
                .map(b -> ResponseEntity.ok(BuildingResourceFromEntityAssembler.toResourceFromEntity(b)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/units")
    public List<UnitResource> units() {
        return service.units(CurrentUser.id(), CurrentUser.isAdmin()).stream()
                .map(UnitResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @GetMapping("/units/{idUnit}")
    public ResponseEntity<UnitResource> unit(@PathVariable Long idUnit) {
        return service.unit(idUnit, CurrentUser.id(), CurrentUser.isAdmin())
                .map(u -> ResponseEntity.ok(UnitResourceFromEntityAssembler.toResourceFromEntity(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user-units")
    public List<UserUnitResource> userUnits() {
        return service.userUnits(CurrentUser.id(), CurrentUser.isAdmin()).stream()
                .map(UserUnitResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
