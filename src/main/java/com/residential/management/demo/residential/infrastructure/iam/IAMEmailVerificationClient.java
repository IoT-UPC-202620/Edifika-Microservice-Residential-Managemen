package com.residential.management.demo.residential.infrastructure.iam;

import com.residential.management.demo.residential.domain.exceptions.EmailVerificationUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Cliente del endpoint interno de IAM para el pre-chequeo E3 (registro de residente).
 * Ver docs/adr/0001-vinculo-por-email-id-user-diferido.md.
 *
 * <p>Contrato: GET {iam.base-url}/api/v1/authentication/internal/users/email-exists?email=...
 * con header X-Internal-Token; 200 {"exists": bool}. Timeout 2 s (conexión y lectura).
 * Cualquier fallo (timeout, IAM caído, no-2xx) → EmailVerificationUnavailableException
 * (fail closed): nunca se crea el vínculo sobre un correo que no se pudo verificar.
 * El correo no se loguea (dato personal).
 */
@Component
public class IAMEmailVerificationClient {

    private static final Logger log = LoggerFactory.getLogger(IAMEmailVerificationClient.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final String EMAIL_EXISTS_PATH = "/api/v1/authentication/internal/users/email-exists";

    private final RestClient restClient;
    private final String internalToken;

    public IAMEmailVerificationClient(
            @Value("${app.iam-base-url:http://localhost:8081}") String iamBaseUrl,
            @Value("${app.internal-api-token:dev-internal-token}") String internalToken) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(iamBaseUrl)
                .requestFactory(requestFactory)
                .build();
        this.internalToken = internalToken;
    }

    public boolean emailExists(String normalizedEmail) {
        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(EMAIL_EXISTS_PATH)
                            .queryParam("email", normalizedEmail)
                            .build())
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .body(EmailExistsResponse.class);
            if (response == null || response.exists() == null) {
                throw new EmailVerificationUnavailableException();
            }
            return response.exists();
        } catch (RestClientException | IllegalArgumentException e) {
            // No se loguea la URI: lleva el email como query param.
            log.warn("No se pudo verificar el correo en IAM ({})", e.getClass().getSimpleName());
            throw new EmailVerificationUnavailableException(e);
        }
    }

    /** Respuesta 200 del endpoint interno: {"exists": bool}. Boolean para fail closed ante body incompleto. */
    public record EmailExistsResponse(Boolean exists) {
    }
}
