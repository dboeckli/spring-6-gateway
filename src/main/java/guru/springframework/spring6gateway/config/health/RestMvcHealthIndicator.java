package guru.springframework.spring6gateway.config.health;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class RestMvcHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String restMvcUrl;

    public RestMvcHealthIndicator(WebClient.Builder webClientBuilder,
                                  @Value("${security.mvc-health-url}") String restMvcUrl) {
        this.webClient = webClientBuilder.build();
        this.restMvcUrl = restMvcUrl;
    }

    @Override
    public @NonNull Mono<Health> health() {
        return webClient.get()
            .uri(restMvcUrl + "/actuator/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                if (response.contains("\"status\":\"UP\"")) {
                    return Health.up().build();
                } else {
                    log.warn("MVC server is not reporting UP status at {}", restMvcUrl);
                    return Health.down().build();
                }
            })
            .onErrorResume(e -> {
                log.warn("MVC server is not reachable at {}", restMvcUrl, e);
                return Mono.just(Health.down(e).build());
            });
    }
}