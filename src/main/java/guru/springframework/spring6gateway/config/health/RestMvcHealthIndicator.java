package guru.springframework.spring6gateway.config.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class RestMvcHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String restMvcUrl;

    public RestMvcHealthIndicator(@Value("${security.mvc-health-url}") String restMvcUrl) {
        this.restClient = RestClient.create();
        this.restMvcUrl = restMvcUrl;
    }

    @Override
    public Health health() {
        try {
            String response = restClient.get()
                .uri(restMvcUrl + "/actuator/health")
                .retrieve()
                .body(String.class);
            if (response != null && response.contains("\"status\":\"UP\"")) {
                return Health.up().build();
            } else {
                log.warn("MVC server is not reporting UP status at {}", restMvcUrl);
                return Health.down().build();
            }
        } catch (Exception e) {
            log.warn("MVC server is not reachable at {}", restMvcUrl, e);
            return Health.down(e).build();
        }
    }

}
