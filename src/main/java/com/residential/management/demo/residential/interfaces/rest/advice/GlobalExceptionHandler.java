package com.residential.management.demo.residential.interfaces.rest.advice;

import com.residential.management.demo.residential.domain.exceptions.EmailAlreadyRegisteredException;
import com.residential.management.demo.residential.domain.exceptions.EmailVerificationUnavailableException;
import com.residential.management.demo.residential.domain.exceptions.TitularActivoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TitularActivoException.class)
    public ResponseEntity<Map<String, String>> handleTitularActivo(TitularActivoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(EmailVerificationUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleEmailVerificationUnavailable(EmailVerificationUnavailableException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", ex.getMessage()));
    }
}
