package guru.springframework.spring6gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private static final List<String> ALLOWED_HEADERS = List.of("*");
    private static final List<String> ALLOWED_METHODS = List.of("POST", "GET", "PUT", "OPTIONS", "DELETE", "PATCH");

    private final AllowedOriginConfig allowedOriginConfig;

    @PostConstruct
    public void init() {
        log.info("### Allowed origins: {}", allowedOriginConfig);
    }

    @Bean
    @Order(1)
    public SecurityWebFilterChain actuatorSecurityFilterChain(ServerHttpSecurity http) {
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeExchange(authorize -> authorize.anyExchange().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {

        http.authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec
                .pathMatchers(
                    "/oauth2/**", 
                    "/oauth2/token",
                    "/favicon.ico",
                    
                    "/v3/api-docs",
                    "/v3/api-docs.yaml",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",

                    // spring-6-rest-mvc
                    "/api/v1/actuator/**",
                    "/api/v1/actuator",
                    "/api/v1/v3/api-docs",
                    "/api/v1/v3/api-docs.yaml",
                    "/api/v1/v3/api-docs/**",
                    "/api/v1/swagger-ui/**",
                    "/api/v1/swagger-ui.html",

                    // spring-6-reactive
                    "/api/v2/actuator/**",
                    "/api/v2/actuator",
                    "/api/v2/v3/api-docs",
                    "/api/v2/v3/api-docs.yaml",
                    "/api/v2/v3/api-docs/**",
                    "/api/v2/swagger-ui/**",
                    "/api/v2/swagger-ui.html",

                    // spring-6-reactive-mongo
                    "/api/v3/actuator/**",
                    "/api/v3/actuator",
                    "/api/v3/v3/api-docs",
                    "/api/v3/v3/api-docs.yaml",
                    "/api/v3/v3/api-docs/**",
                    "/api/v3/swagger-ui/**",
                    "/api/v3/swagger-ui.html",

                    // spring-6-data-rest
                    "/api/v4/actuator/**",
                    "/api/v4/actuator",
                    "/api/v4/v3/api-docs",
                    "/api/v4/v3/api-docs.yaml",
                    "/api/v4/v3/api-docs/**",
                    "/api/v4/swagger-ui/**",
                    "/api/v4/swagger-ui.html")
                      
                .permitAll()
                .anyExchange().permitAll())
            .oauth2ResourceServer(oAuth2ResourceServerSpec -> oAuth2ResourceServerSpec
                .jwt(Customizer.withDefaults()))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOriginConfig.getAllowedOrigins());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Component
    @ConfigurationProperties(prefix = "security.cors")
    @Data
    public static class AllowedOriginConfig {
        private List<String> allowedOrigins;
    }
}
