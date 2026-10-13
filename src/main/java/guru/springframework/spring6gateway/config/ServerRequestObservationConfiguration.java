package guru.springframework.spring6gateway.config;

import io.micrometer.common.KeyValue;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;

@Configuration(proxyBeanMethods = false)
public class ServerRequestObservationConfiguration {

    @Bean
    ServerRequestObservationConvention serverRequestObservationConvention(ObservationProperties properties) {
        return new PathServerRequestObservationConvention(properties.getHttp().getServer().getRequests().getName());
    }

    static class PathServerRequestObservationConvention extends DefaultServerRequestObservationConvention {

        PathServerRequestObservationConvention(String name) {
            super(name);
        }

        @Override
        public String getContextualName(ServerRequestObservationContext context) {
            String contextualName = super.getContextualName(context);
            if (contextualName != null && context.getCarrier() != null && context.getPathPattern() == null) {
                return contextualName + " " + context.getCarrier().getPath().value();
            }
            return contextualName;
        }

        @Override
        protected KeyValue uri(ServerRequestObservationContext context) {
            if (context.getCarrier() != null && context.getPathPattern() == null) {
                return KeyValue.of("uri", context.getCarrier().getPath().value());
            }
            return super.uri(context);
        }

    }

}
