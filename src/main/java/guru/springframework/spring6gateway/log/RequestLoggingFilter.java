package guru.springframework.spring6gateway.log;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
@EnableConfigurationProperties(RequestLoggingFilter.RequestLoggingProperties.class)
public class RequestLoggingFilter {

    static boolean isExcluded(String path, List<String> excludePaths) {
        if (path == null || excludePaths == null || excludePaths.isEmpty()) {
            return false;
        }
        return excludePaths.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));
    }

    @Bean
    @ConditionalOnProperty(prefix = "appconfig.request-logging", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public WebFilter logFilter(RequestLoggingProperties requestLoggingProperties) {
        List<String> excludePaths = requestLoggingProperties.getExcludePaths();
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            if (!isExcluded(request.getURI().getPath(), excludePaths)) {
                log.debug("REQUEST DATA: method={}, uri={}, headers={}", request.getMethod(), request.getURI(),
                        request.getHeaders());
            }
            return chain.filter(exchange);
        };
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "appconfig.request-logging")
    public static class RequestLoggingProperties {

        /**
         * Whether the request logging filter is registered at all. When set to false no
         * filter is added to the web filter chain.
         */
        private boolean enabled = true;

        /**
         * Request URI prefixes that are excluded from request logging (e.g.
         * /actuator/health).
         */
        private List<String> excludePaths = new ArrayList<>();

    }

}
