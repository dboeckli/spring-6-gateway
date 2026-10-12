package guru.springframework.spring6gateway.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(RequestLoggingConfig.class);

    @ParameterizedTest
    @ValueSource(strings = { "/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness" })
    void excludesRequestsMatchingAnExcludedPath(String path) {
        RequestLoggingFilter filter = new RequestLoggingFilter(List.of("/actuator/health"));

        assertThat(filter.isExcluded(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/beer", "/actuator/info", "/oauth2/token" })
    void logsRequestsNotMatchingAnExcludedPath(String path) {
        RequestLoggingFilter filter = new RequestLoggingFilter(List.of("/actuator/health"));

        assertThat(filter.isExcluded(path)).isFalse();
    }

    @Test
    void logsEverythingWhenNoExcludedPathsAreConfigured() {
        RequestLoggingFilter filter = new RequestLoggingFilter(List.of());

        assertThat(filter.isExcluded("/actuator/health")).isFalse();
    }

    @Test
    void registersFilterByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(RequestLoggingFilter.class));
    }

    @Test
    void doesNotRegisterFilterWhenDisabled() {
        contextRunner.withPropertyValues("appconfig.request-logging.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(RequestLoggingFilter.class));
    }

    @Test
    void bindsExcludedPathsFromConfiguration() {
        contextRunner.withPropertyValues("appconfig.request-logging.exclude-paths[0]=/actuator/health").run(context -> {
            RequestLoggingFilter filter = context.getBean(RequestLoggingFilter.class);

            assertThat(filter.isExcluded("/actuator/health/readiness")).isTrue();
            assertThat(filter.isExcluded("/api/v1/beer")).isFalse();
        });
    }

}
