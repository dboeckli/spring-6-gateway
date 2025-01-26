package guru.springframework.spring6gateway.route;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("with_docker_compose")
@Slf4j
class RouterConfigIT {

    @LocalServerPort
    private int port;

    private WebClient webClient;
    private String authToken;

    @BeforeEach
    public void setup() {
        this.webClient = WebClient.create("http://localhost:" + port);
        this.authToken = getAuthToken(webClient);
    }

    @Test
    void testV1ListBeers() {
        AtomicReference<Map<String, Object>> responseHolder = new AtomicReference<>();

        Mono<Map<String, Object>> response = webClient.get()
            .uri("/api/v1/beer/listBeers")
            .header("Authorization", "Bearer " + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});

        StepVerifier.create(response)
            .consumeNextWith(responseHolder::set)
            .verifyComplete();

        Map<String, Object> storedResponse = responseHolder.get();
        log.info("Full response:" + storedResponse);
    
        Integer totalElements = (Integer) storedResponse.get("totalElements");
        assertEquals(2413, totalElements);
    }

    private String getAuthToken(WebClient authClient) {
        String credentials = "messaging-client:secret";
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return authClient.post()
            .uri("/oauth2/token")
            .header("Authorization", "Basic " + encodedCredentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                .with("scope", "message.read message.write"))
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> System.err.println("Error: " + error.getMessage()))
            .map(response -> {
                // Parse the JSON response to extract the access token
                // This is a simplified example and should be more robust in practice
                int start = response.indexOf("\"access_token\":\"") + 16;
                int end = response.indexOf("\"", start);
                return response.substring(start, end);
            })
            .block();
    }
}
