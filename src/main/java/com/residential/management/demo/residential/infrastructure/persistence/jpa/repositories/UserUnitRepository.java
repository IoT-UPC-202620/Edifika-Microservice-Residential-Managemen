package com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories;

import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserUnitRepository extends JpaRepository<UserUnit, Long> {

    Optional<UserUnit> findByIdUserAndUnitIdUnit(Long idUser, Long idUnit);

    List<UserUnit> findByUnitBuildingIdBuilding(Long idBuilding);

    Optional<UserUnit> findByIdUserAndStatus(Long idUser, String status);

    List<UserUnit> findByUnitIdUnitAndStatus(Long idUnit, String status);

    List<UserUnit> findByResidentEmailAndUnitIdUnitAndStatus(String residentEmail, Long idUnit, String status);

    // Vinculos de un usuario (mas reciente primero): /residents/me y control de acceso por unidad
    List<UserUnit> findByIdUserAndStatusOrderByStartDateDesc(Long idUser, String status);

    List<UserUnit> findByIdUser(Long idUser);
}