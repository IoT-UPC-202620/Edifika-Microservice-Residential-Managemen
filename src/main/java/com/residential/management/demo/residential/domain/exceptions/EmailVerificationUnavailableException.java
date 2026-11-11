package com.residential.management.demo.residential.domain.exceptions;

/**
 * E3 fail closed: IAM no respondió, tardó más de 2 s o devolvió no-2xx al verificar
 * el correo. El vínculo NO se crea; el admin recibe 503 y puede reintentar.
 */
public class EmailVerificationUnavailableException extends RuntimeException {

    public static final String MESSAGE = "No se pudo verificar el correo; inténtalo de nuevo";

    public EmailVerificationUnavailableException() {
        super(MESSAGE);
    }

    public EmailVerificationUnavailableException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
