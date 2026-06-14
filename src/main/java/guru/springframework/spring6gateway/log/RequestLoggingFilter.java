package guru.springframework.spring6gateway.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
@Slf4j
public class RequestLoggingFilter {

    @Bean
    public WebFilter logFilter() {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            log.debug("REQUEST DATA: method={}, uri={}, headers={}", request.getMethod(), request.getURI(),
                    request.getHeaders());
            return chain.filter(exchange);
        };
    }

}
