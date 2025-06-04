package guru.springframework.spring6gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@OpenAPIDefinition(
    info = @Info(
        title = "TODO",
        description = "Some long and useful description",
        version = "TODO",
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    )
)
@SecurityScheme(
    name = OpenApiConfiguration.SECURITY_SCHEME_NAME,
    type = SecuritySchemeType.OAUTH2,
    bearerFormat = "JWT",
    scheme = "bearer",
    flows = @OAuthFlows(
        clientCredentials = @OAuthFlow(
            authorizationUrl = "http://host.docker.internal:9000/oauth2/auth",
            tokenUrl = "http://host.docker.internal:9000/oauth2/token",
            refreshUrl = "http://host.docker.internal:9000/oauth2/refresh-token",
            scopes = {
                @OAuthScope(name = "message.read"),
                @OAuthScope(name = "message.write")
            })
    )
)
@Configuration
@RequiredArgsConstructor
@Slf4j
public class OpenApiConfiguration {

    private final BuildProperties buildProperties;

    private final Environment environment;

    @Value("${security.authorization-url:http://host.docker.internal:9000/oauth2/auth}")
    private String authorizationUrl;
    @Value("${security.token-url:http://host.docker.internal:9000/oauth2/token}")
    private String tokenUrl;
    @Value("${security.refresh-url:http://host.docker.internal:9000/oauth2/refresh-token}")
    private String refreshUrl;

    public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    @Qualifier("customGlobalHeaderOpenApiCustomizer")
    public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer() {
        return openApi -> {
            io.swagger.v3.oas.models.info.Info info = openApi.getInfo();
            info.setTitle(buildProperties.getName());
            info.setVersion(buildProperties.getVersion());

            // Update OAuth URLs
            openApi.getComponents().getSecuritySchemes().values().stream()
                .filter(scheme -> scheme.getType() == io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2)
                .forEach(scheme -> {
                    io.swagger.v3.oas.models.security.OAuthFlows flows = scheme.getFlows();
                    if (flows.getClientCredentials() != null) {
                        flows.getClientCredentials()
                            .authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .refreshUrl(refreshUrl);
                    }
                });
        };
    }

    @Bean
    public GroupedOpenApi mvcRestApi(@Qualifier("customGlobalHeaderOpenApiCustomizer") OpenApiCustomizer customGlobalHeaderOpenApiCustomizer) {
        return GroupedOpenApi.builder()
            .group("spring-6-rest-mvc-2")
            .addOpenApiCustomizer(customGlobalHeaderOpenApiCustomizer)
            .displayName("Spring 6 Rest MVC Rest API 2")
            .pathsToMatch("/api/v1/v3/api-docs")
            .addOpenApiCustomizer(openApi -> {
                try {
                    String port = environment.getProperty("local.server.port");

                    URL url = new URL(openApi.getServers().getFirst().getUrl() + "/api/v1/v3/api-docs");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    // Read the response
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                    }

                    OpenAPI externalOpenApi = new OpenAPIV3Parser().readContents(content.toString()).getOpenAPI();

                    // Set the correct server URL
                    externalOpenApi.getServers().clear();
                    externalOpenApi.addServersItem(new Server().url(url.getProtocol() + "://" + url.getHost() + ":" + port));

                    // Merge paths and components from external OpenAPI into the current one
                    openApi.getPaths().putAll(externalOpenApi.getPaths());
                    if (externalOpenApi.getComponents() != null) {
                        if (openApi.getComponents() == null) {
                            openApi.setComponents(new Components());
                        }
                        // TODO: TO BE REVISED
                        //openApi.getComponents().getSchemas().putAll(externalOpenApi.getComponents().getSchemas());
                    }

                    openApi.setServers(externalOpenApi.getServers());

                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load camunda OpenAPI definition." ,ex);
                }
            })
            .build();
    }
}
