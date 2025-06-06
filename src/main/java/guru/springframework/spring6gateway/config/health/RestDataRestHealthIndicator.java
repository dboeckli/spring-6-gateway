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
public class RestDataRestHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String restUrl;

    public RestDataRestHealthIndicator(WebClient.Builder webClientBuilder,
                                       @Value("${security.dataRest-health-url}") String restUrl) {
        this.webClient = webClientBuilder.build();
        this.restUrl = restUrl;
    }

    @Override
    public Mono<Health> health() {
        return webClient.get()
            .uri(restUrl + "/actuator/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                if (response != null && response.contains("\"status\":\"UP\"")) {
                    return Health.up().build();
                } else {
                    log.warn("Data-Rest server is not reporting UP status at {}", restUrl);
                    return Health.down().build();
                }
            })
            .onErrorResume(e -> {
                log.warn("Data-Rest server is not reachable at {}", restUrl, e);
                return Mono.just(Health.down(e).build());
            });
    }
}