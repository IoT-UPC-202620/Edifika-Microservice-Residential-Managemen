package com.residential.management.demo.residential.domain.model.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_unit")
public class UserUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user_unit")
    private Long idUserUnit;

    @ManyToOne
    @JoinColumn(name = "id_unit", nullable = false)
    private Unit unit;

    @Column(name = "id_user")
    private Long idUser;

    /**
     * Correo del residente. Permite crear el vínculo en estado PENDIENTE antes de que
     * IAM cree el usuario (id_user se completa cuando llega iam.resident.user-created).
     * Registros existentes creados por assignUserToUnit tienen correo nulo.
     */
    @Column(name = "resident_email", length = 320)
    private String residentEmail;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status", nullable = false, length = 80)
    private String status;

    public UserUnit() {
    }

    public UserUnit(Unit unit, Long idUser, LocalDateTime startDate, LocalDateTime endDate, String status) {
        this.unit = unit;
        this.idUser = idUser;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public UserUnit(Unit unit, String residentEmail) {
        this.unit = unit;
        this.residentEmail = residentEmail;
        this.startDate = LocalDateTime.now();
        this.status = "PENDIENTE";
    }

    public Long getIdUserUnit() {
        return idUserUnit;
    }

    public Unit getUnit() {
        return unit;
    }

    public Long getIdUser() {
        return idUser;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }

    public String getResidentEmail() {
        return residentEmail;
    }

    public void setResidentEmail(String residentEmail) {
        this.residentEmail = residentEmail;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
