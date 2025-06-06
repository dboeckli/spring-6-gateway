package guru.springframework.spring6gateway.config.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class RestDataRestHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String restUrl;

    public RestDataRestHealthIndicator(@Value("${security.dataRest-health-url}") String restUrl) {
        this.restClient = RestClient.create();
        this.restUrl = restUrl;
    }

    @Override
    public Health health() {
        try {
            String response = restClient.get()
                .uri(restUrl + "/actuator/health")
                .retrieve()
                .body(String.class);
            if (response != null && response.contains("\"status\":\"UP\"")) {
                return Health.up().build();
            } else {
                log.warn("Data-Rest server is not reporting UP status at {}", restUrl);
                return Health.down().build();
            }
        } catch (Exception e) {
            log.warn("Data-Rest server is not reachable at {}", restUrl, e);
            return Health.down(e).build();
        }
    }

}
