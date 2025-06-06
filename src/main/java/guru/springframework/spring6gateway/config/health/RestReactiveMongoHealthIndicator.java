package guru.springframework.spring6gateway.config.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class RestReactiveMongoHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String restReactiveMongoUrl;

    public RestReactiveMongoHealthIndicator(@Value("${security.reactiveMongo-health-url}") String restReactiveMongoUrl) {
        this.restClient = RestClient.create();
        this.restReactiveMongoUrl = restReactiveMongoUrl;
    }

    @Override
    public Health health() {
        try {
            String response = restClient.get()
                .uri(restReactiveMongoUrl + "/actuator/health")
                .retrieve()
                .body(String.class);
            if (response != null && response.contains("\"status\":\"UP\"")) {
                return Health.up().build();
            } else {
                log.warn("Reactive Mongo server is not reporting UP status at {}", restReactiveMongoUrl);
                return Health.down().build();
            }
        } catch (Exception e) {
            log.warn("Reactive Mongo  server is not reachable at {}", restReactiveMongoUrl, e);
            return Health.down(e).build();
        }
    }

}
