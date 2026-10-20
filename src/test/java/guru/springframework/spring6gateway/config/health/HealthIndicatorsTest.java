package guru.springframework.spring6gateway.config.health;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class HealthIndicatorsTest {

    private static WireMockServer server;

    @BeforeAll
    static void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void resetStubs() {
        server.resetAll();
    }

    static Stream<Arguments> indicators() {
        return Stream.of(
                Arguments.of("auth-server",
                        (Function<String, ReactiveHealthIndicator>) url -> new AuthServerHealthIndicator(
                                WebClient.builder(), url)),
                Arguments.of("rest-mvc",
                        (Function<String, ReactiveHealthIndicator>) url -> new RestMvcHealthIndicator(
                                WebClient.builder(), url)),
                Arguments.of("rest-reactive",
                        (Function<String, ReactiveHealthIndicator>) url -> new RestReactiveHealthIndicator(
                                WebClient.builder(), url)),
                Arguments.of("rest-reactive-mongo",
                        (Function<String, ReactiveHealthIndicator>) url -> new RestReactiveMongoHealthIndicator(
                                WebClient.builder(), url)),
                Arguments.of("rest-data-rest",
                        (Function<String, ReactiveHealthIndicator>) url -> new RestDataRestHealthIndicator(
                                WebClient.builder(), url)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("indicators")
    void reportsUp(String name, Function<String, ReactiveHealthIndicator> indicatorFactory) {
        stubHealth("{\"status\":\"UP\"}");

        Health health = indicatorFactory.apply(server.baseUrl()).health().block();

        assertThat(health).isNotNull();
        assertThat(health.getStatus().getCode()).isEqualTo("UP");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("indicators")
    void reportsDownWhenStatusIsNotUp(String name, Function<String, ReactiveHealthIndicator> indicatorFactory) {
        stubHealth("{\"status\":\"DOWN\"}");

        Health health = indicatorFactory.apply(server.baseUrl()).health().block();

        assertThat(health).isNotNull();
        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("indicators")
    void reportsDownWhenServerFails(String name, Function<String, ReactiveHealthIndicator> indicatorFactory) {
        server.stubFor(get(urlEqualTo("/actuator/health")).willReturn(aResponse().withStatus(500)));

        Health health = indicatorFactory.apply(server.baseUrl()).health().block();

        assertThat(health).isNotNull();
        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }

    private void stubHealth(String body) {
        server.stubFor(get(urlEqualTo("/actuator/health"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
    }

}
