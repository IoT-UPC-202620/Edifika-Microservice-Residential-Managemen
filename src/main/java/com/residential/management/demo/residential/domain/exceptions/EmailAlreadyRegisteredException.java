package com.residential.management.demo.residential.domain.exceptions;

/**
 * E3: el correo ya está registrado en IAM (el admin debe verlo como 409, no como 201).
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public static final String MESSAGE = "El correo ya está registrado";

    public EmailAlreadyRegisteredException() {
        super(MESSAGE);
    }
}
