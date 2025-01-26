package guru.springframework.spring6gateway.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("with_docker_compose")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
class RouterConfigIT {

    @LocalServerPort
    private int port;

    private WebClient webClient;
    private String authToken;

    @BeforeEach
    public void setup() {
        checkAppReady("http://localhost:8081"); // spring-6-rest-mvc
        checkAppReady("http://localhost:8082"); // spring-6-reactive
        checkAppReady("http://localhost:8083"); // spring-6-reactive-mongo
        this.webClient = WebClient.create("http://localhost:" + port);
        this.authToken = getAuthToken();
    }

    @Test
    void testV1ListBeers() {
        AtomicReference<Map<String, Object>> responseHolder = new AtomicReference<>();

        Mono<Map<String, Object>> response = webClient.get()
            .uri("/api/v1/beer/listBeers")
            .header("Authorization", "Bearer " + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });

        StepVerifier.create(response)
            .consumeNextWith(responseHolder::set)
            .verifyComplete();

        Map<String, Object> storedResponse = responseHolder.get();
        log.info("V1 Full response:" + storedResponse);

        Integer totalElements = (Integer) storedResponse.get("totalElements");
        assertEquals(2413, totalElements);
    }

    @Test
    void testV2ListBeers() {
        AtomicReference<List<Map<String, Object>>> responseHolder = new AtomicReference<>();

        Mono<List<Map<String, Object>>> response = webClient.get()
            .uri("/api/v2/beer")
            .header("Authorization", "Bearer " + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });

        StepVerifier.create(response)
            .consumeNextWith(responseHolder::set)
            .verifyComplete();

        List<Map<String, Object>> storedResponse = responseHolder.get();
        log.info("V2 Full response: {}", storedResponse);

        assertFalse(storedResponse.isEmpty());
        assertEquals(3, storedResponse.size());

        // Überprüfen Sie das erste Element in der Liste
        Map<String, Object> firstBeer = storedResponse.getFirst();
        assertNotNull(firstBeer.get("id"));
        assertNotNull(firstBeer.get("beerName"));
    }

    @Test
    void testV3ListBeers() {
        AtomicReference<List<Map<String, Object>>> responseHolder = new AtomicReference<>();

        Mono<List<Map<String, Object>>> response = webClient.get()
            .uri("/api/v3/beer")
            .header("Authorization", "Bearer " + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });

        StepVerifier.create(response)
            .consumeNextWith(responseHolder::set)
            .verifyComplete();

        List<Map<String, Object>> storedResponse = responseHolder.get();
        log.info("V3 Full response: {}", storedResponse);

        assertFalse(storedResponse.isEmpty());
        assertEquals(3, storedResponse.size());

        // Überprüfen Sie das erste Element in der Liste
        Map<String, Object> firstBeer = storedResponse.getFirst();
        assertNotNull(firstBeer.get("id"));
        assertNotNull(firstBeer.get("beerName"));
    }

    private String getAuthToken() {
        String credentials = "messaging-client:secret";
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return webClient.post()
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

    public static void checkAppReady(String url) {
        WebClient webClientForActuator = WebClient.create(url);

        Awaitility.await()
            .atMost(90, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                try {
                    return webClientForActuator.get()
                        .uri(url + "/actuator/health/readiness")
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(body -> {
                            log.info("Readiness check response: {}", body);
                            JsonNode jsonNode;
                            try {
                                jsonNode = new ObjectMapper().readTree(body);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                            String status = jsonNode.path("status").asText();
                            log.info("Application status: {}", status);
                            return "UP".equals(status);
                        })
                        .onErrorReturn(false)
                        .block(Duration.ofSeconds(5));
                } catch (Exception e) {
                    log.warn("Error checking MVC readiness: ", e);
                    return false;
                }
            });
        log.info("MVC application is ready.");
    }
}
