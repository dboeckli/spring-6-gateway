package guru.springframework.spring6gateway.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

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
    void testV1RestMvcListBeers() {
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
    void testV2ReactiveListBeers() {
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
    void testV3ReactiveMongoListBeers() {
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

    @ParameterizedTest
    @MethodSource("actuatorTestArguments")
    void testActuator(String apiVersion, String uri, String expectedArtifact, String expectedName, String expectedGroup) {
        AtomicReference<Map<String, Object>> responseHolder = new AtomicReference<>();

        Mono<Map<String, Object>> response = webClient.get()
            .uri(uri)
            .header("Authorization", "Bearer " + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });

        StepVerifier.create(response)
            .consumeNextWith(responseHolder::set)
            .verifyComplete();

        Map<String, Object> storedResponse = responseHolder.get();
        log.info("{} Actuator Info response: {}", apiVersion, storedResponse);
        assertNotNull(storedResponse);

        Map<String, Object> build = (Map<String, Object>) storedResponse.get("build");
        assertNotNull(build);

        assertNotNull(build.get("version"));
        assertEquals(expectedArtifact, build.get("artifact"));
        assertEquals(expectedName, build.get("name"));
        assertEquals(expectedGroup, build.get("group"));
    }

    private static Stream<Arguments> actuatorTestArguments() {
        return Stream.of(
            Arguments.of("V1", "/api/v1/actuator/info", "spring-6-rest-mvc", "spring-6-rest-mvc", "ch.dboeckli.springframeworkguru.spring-rest-mvc"),
            Arguments.of("V2", "/api/v2/actuator/info", "spring-6-reactive", "spring-6-reactive", "guru.springframework"),
            Arguments.of("V3", "/api/v3/actuator/info", "spring-6-reactive-mongo", "spring-6-reactive-mongo", "guru.springframework"),
            Arguments.of("Auth", "/oauth2/actuator/info", "spring-6-auth-server", "spring-6-auth-server", "guru.springframework")
        );
    }

    @ParameterizedTest
    @MethodSource("openApiDocsTestArguments")
    void testOpenApiDocs(String apiVersion, String uri, String expectedTitle) {
        AtomicReference<Map<String, Object>> responseHolder = new AtomicReference<>();

        Mono<Map<String, Object>> response = webClient.get()
            .uri(uri)
            .header("Authorization", "Bearer " + authToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });

        StepVerifier.create(response)
            .consumeNextWith(responseHolder::set)
            .verifyComplete();

        Map<String, Object> storedResponse = responseHolder.get();
        log.info("{} OpenAPI Docs response: {}", apiVersion, storedResponse);
        assertNotNull(storedResponse);

        Map<String, Object> info = (Map<String, Object>) storedResponse.get("info");
        assertNotNull(info);
        assertEquals(expectedTitle, info.get("title"));
        assertNotNull(info.get("version"));
    }

    private static Stream<Arguments> openApiDocsTestArguments() {
        return Stream.of(
            Arguments.of("V1", "/api/v1/v3/api-docs", "spring-6-rest-mvc"),
            Arguments.of("V2", "/api/v2/v3/api-docs", "spring-6-reactive"),
            Arguments.of("V3", "/api/v3/v3/api-docs", "spring-6-reactive-mongo"),
            Arguments.of("Auth", "/oauth2/v3/api-docs", "spring-6-auth-server")
        );
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
        log.info("readyness check for {}", url);

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
