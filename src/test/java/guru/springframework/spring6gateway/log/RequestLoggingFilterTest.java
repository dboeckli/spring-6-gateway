package guru.springframework.spring6gateway.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.server.WebFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(RequestLoggingFilter.class);

    @ParameterizedTest
    @ValueSource(strings = { "/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness" })
    void excludesRequestsMatchingAnExcludedPath(String path) {
        assertThat(RequestLoggingFilter.isExcluded(path, List.of("/actuator/health"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/beer", "/actuator/info", "/actuator/healthcheck" })
    void logsRequestsNotMatchingAnExcludedPath(String path) {
        assertThat(RequestLoggingFilter.isExcluded(path, List.of("/actuator/health"))).isFalse();
    }

    @Test
    void logsEverythingWhenNoExcludedPathsAreConfigured() {
        assertThat(RequestLoggingFilter.isExcluded("/actuator/health", List.of())).isFalse();
    }

    @Test
    void logsEverythingWhenExcludedPathsIsNull() {
        assertThat(RequestLoggingFilter.isExcluded("/actuator/health", null)).isFalse();
    }

    @Test
    void registersFilterByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(WebFilter.class));
    }

    @Test
    void doesNotRegisterFilterWhenDisabled() {
        contextRunner.withPropertyValues("appconfig.request-logging.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(WebFilter.class));
    }

}
