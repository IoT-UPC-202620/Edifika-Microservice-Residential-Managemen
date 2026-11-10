package com.residential.management.demo.residential.domain.services;

import com.residential.management.demo.residential.infrastructure.messaging.events.ResidentAssignedToUnitEvent;

/**
 * Puerto de dominio para publicar el hecho de que un residente fue asignado a una unidad.
 * La implementación vive en infrastructure/messaging (RabbitMQ).
 */
public interface ResidentAssignmentPublisher {

    void publish(ResidentAssignedToUnitEvent event);
}
