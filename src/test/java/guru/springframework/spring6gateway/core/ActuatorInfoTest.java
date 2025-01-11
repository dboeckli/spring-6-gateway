package guru.springframework.spring6gateway.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
class ActuatorInfoTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorInfoTest() {
        EntityExchangeResult<byte[]> result = webTestClient.get().uri("/actuator/info")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.git.commit.id").isNotEmpty()
            .jsonPath("$.build.javaVersion").isEqualTo("21")
            .jsonPath("$.build.commit-id").isNotEmpty()
            .jsonPath("$.build.javaVendor").isNotEmpty()
            .jsonPath("$.build.artifact").isEqualTo("spring-6-gateway")
            .jsonPath("$.build.group").isEqualTo("guru.springframework")
            .returnResult();
        log.info("Response: {}", result.getResponseBody());
    }


    @Test
    void actuatorHealthTest() {
        EntityExchangeResult<byte[]> result = webTestClient.get().uri("/actuator/health/readiness")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .returnResult();
        log.info("Response: {}", result.getResponseBody());
    }
    
}
