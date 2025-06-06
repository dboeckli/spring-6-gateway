package guru.springframework.spring6gateway.config.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthServerHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String authServerUrl;

    public AuthServerHealthIndicator(WebClient.Builder webClientBuilder,
                                     @Value("${security.auth-server-health-url}") String authServerUrl) {
        this.webClient = webClientBuilder.build();
        this.authServerUrl = authServerUrl;
    }

    @Override
    public Mono<Health> health() {
        return checkAuthServerHealth()
            .map(status -> status ? Health.up().build() : Health.down().build())
            .doOnNext(health -> {
                if (health.getStatus().equals(Health.down().build().getStatus())) {
                    log.warn("Auth server is not reachable at {}", authServerUrl);
                }
            });
    }

    private Mono<Boolean> checkAuthServerHealth() {
        return webClient.get()
            .uri(authServerUrl + "/actuator/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> response.contains("\"status\":\"UP\""))
            .onErrorResume(e -> Mono.just(false));
    }
}
