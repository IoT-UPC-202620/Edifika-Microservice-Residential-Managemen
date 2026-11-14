package com.residential.management.demo.residential.infrastructure.iam;

import com.residential.management.demo.residential.domain.exceptions.EmailVerificationUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * GET {iam.base-url}/api/v1/authentication/internal/users/{id} con X-Internal-Token.
 * 404 -> Optional.empty(); otro fallo -> EmailVerificationUnavailableException (503, fail closed).
 */
@Component
public class IAMUserClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final RestClient restClient;
    private final String internalToken;

    public IAMUserClient(@Value("${app.iam-base-url:http://localhost:8081}") String iamBaseUrl,
                         @Value("${app.internal-api-token:dev-internal-token}") String internalToken) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        this.restClient = RestClient.builder().baseUrl(iamBaseUrl).requestFactory(requestFactory).build();
        this.internalToken = internalToken;
    }

    public Optional<IamUser> findUser(Long id) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/v1/authentication/internal/users/{id}", id)
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .body(IamUser.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new EmailVerificationUnavailableException(e);
        }
    }

    public record IamUser(Long id, String email, String fullName, List<String> roles) {
    }
}
