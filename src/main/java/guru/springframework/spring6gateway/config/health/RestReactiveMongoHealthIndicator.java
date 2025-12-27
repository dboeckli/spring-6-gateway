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
public class RestReactiveMongoHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String restReactiveMongoUrl;

    public RestReactiveMongoHealthIndicator(WebClient.Builder webClientBuilder,
                                            @Value("${security.reactiveMongo-health-url}") String restReactiveMongoUrl) {
        this.webClient = webClientBuilder.build();
        this.restReactiveMongoUrl = restReactiveMongoUrl;
    }

    @Override
    public @NonNull Mono<Health> health() {
        return webClient.get()
            .uri(restReactiveMongoUrl + "/actuator/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                if (response.contains("\"status\":\"UP\"")) {
                    return Health.up().build();
                } else {
                    log.warn("Reactive Mongo server is not reporting UP status at {}", restReactiveMongoUrl);
                    return Health.down().build();
                }
            })
            .onErrorResume(e -> {
                log.warn("Reactive Mongo server is not reachable at {}", restReactiveMongoUrl, e);
                return Mono.just(Health.down(e).build());
            });
    }
}