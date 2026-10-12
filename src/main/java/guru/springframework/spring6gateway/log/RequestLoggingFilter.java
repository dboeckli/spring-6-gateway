package guru.springframework.spring6gateway.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter implements WebFilter {

    private final List<String> excludePaths;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!isExcluded(path)) {
            var request = exchange.getRequest();
            log.debug("REQUEST DATA: method={}, uri={}, headers={}", request.getMethod(), request.getURI(),
                    request.getHeaders());
        }
        return chain.filter(exchange);
    }

    boolean isExcluded(String path) {
        return excludePaths.stream().anyMatch(path::startsWith);
    }

}
