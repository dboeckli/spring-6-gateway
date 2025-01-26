package guru.springframework.spring6gateway.route;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock
@ActiveProfiles("test")
@Slf4j
class RouterConfigTest {

    @Value("${wiremock.server.baseUrl}")
    private String wireMockUrl;

    @Autowired
    private WebTestClient webTestClient;

    @PostConstruct
    public void init() {
        log.info("WireMock URL: {}", wireMockUrl);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        @Order(1)
        public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
            return http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
        }
    }

    @Test
    void testSpring6RestMvcRoute() {
        stubFor(get(urlEqualTo("/api/v1/rest-mvc/test"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Hello from mocked REST MVC!\"}")));

        webTestClient.get().uri("/api/v1/rest-mvc/test")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Content-Type", "application/json")
            .expectBody()
            .jsonPath("$.message").isEqualTo("Hello from mocked REST MVC!");
    }

    @Test
    void testSpring6ReactiveRoute() {
        stubFor(get(urlEqualTo("/api/v1/reactive/test"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Hello from mocked Reactive!\"}")));

        webTestClient.get().uri("/api/v1/reactive/test")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Content-Type", "application/json")
            .expectBody()
            .jsonPath("$.message").isEqualTo("Hello from mocked Reactive!");
    }

    @Test
    void testSpring6ReactiveMongoRoute() {
        stubFor(get(urlEqualTo("/api/v1/reactive-mongo/test"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Hello from mocked Reactive Mongo!\"}")));

        webTestClient.get().uri("/api/v1/reactive-mongo/test")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Content-Type", "application/json")
            .expectBody()
            .jsonPath("$.message").isEqualTo("Hello from mocked Reactive Mongo!");
    }

    
}
