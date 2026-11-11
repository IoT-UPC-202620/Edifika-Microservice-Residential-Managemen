package com.residential.management.demo.residential.interfaces.rest.advice;

import com.residential.management.demo.residential.domain.exceptions.EmailAlreadyRegisteredException;
import com.residential.management.demo.residential.domain.exceptions.EmailVerificationUnavailableException;
import com.residential.management.demo.residential.domain.exceptions.TitularActivoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void titularActivoMapsTo409WithExactMessage() {
        var response = handler.handleTitularActivo(new TitularActivoException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(java.util.Map.of("message", "La unidad ya tiene un titular activo"), response.getBody());
    }

    @Test
    void emailAlreadyRegisteredMapsTo409WithExactMessage() {
        var response = handler.handleEmailAlreadyRegistered(new EmailAlreadyRegisteredException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(java.util.Map.of("message", "El correo ya está registrado"), response.getBody());
    }

    @Test
    void emailVerificationUnavailableMapsTo503WithExactMessage() {
        var response = handler.handleEmailVerificationUnavailable(new EmailVerificationUnavailableException());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(java.util.Map.of("message", "No se pudo verificar el correo; inténtalo de nuevo"),
                response.getBody());
    }
}
