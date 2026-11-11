package com.residential.management.demo.residential.interfaces.rest.resources;

import java.time.LocalDateTime;

public record UserUnitResource(
        Long idUserUnit,
        Long idUnit,
        Long idUser,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status,
        String residentEmail,
        Long idBuilding
) {
}