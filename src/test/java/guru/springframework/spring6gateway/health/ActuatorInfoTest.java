package guru.springframework.spring6gateway.health;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureMetrics
@Slf4j
class ActuatorInfoTest {

    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    BuildProperties buildProperties;
    
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void actuatorInfoTest() {
        webTestClient.get().uri("/actuator/info")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(result -> {
                String jsonResponse = new String(Objects.requireNonNull(result.getResponseBody()));
                log.info("Response:\n{}", pretty(jsonResponse));
            })
            .jsonPath("$.git.commit.id.abbrev").isNotEmpty()
            .jsonPath("$.java.version").value((String version) -> assertThat(version).startsWith("21"))
            .jsonPath("$.build.artifact").isEqualTo(buildProperties.getArtifact())
            .jsonPath("$.build.group").isEqualTo(buildProperties.getGroup())
            .consumeWith(result -> {
                String jsonResponse = new String(Objects.requireNonNull(result.getResponseBody()));
                log.info("Response:\n{}", pretty(jsonResponse));
            });
    }


    @Test
    void actuatorHealthTest() {
        webTestClient.get().uri("/actuator/health/readiness")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(result -> {
                String jsonResponse = new String(Objects.requireNonNull(result.getResponseBody()));
                log.info("Response:\n{}", pretty(jsonResponse));
            })
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void actuatorPrometheusTest() {
        webTestClient.get().uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(result -> {
                String jsonResponse = new String(Objects.requireNonNull(result.getResponseBody()));
                log.info("Response:\n{}", jsonResponse);
            });
    }

    private String pretty(String jsonResponse) {
        try {
            Object json = objectMapper.readValue(jsonResponse, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            // Falls kein valides JSON: unverändert zurückgeben
            return jsonResponse;
        }
    }
    
}
