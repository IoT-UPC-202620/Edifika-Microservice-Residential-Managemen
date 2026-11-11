package com.residential.management.demo.residential.domain.exceptions;

public class TitularActivoException extends RuntimeException {

    public static final String MESSAGE = "La unidad ya tiene un titular activo";

    public TitularActivoException() {
        super(MESSAGE);
    }
}
