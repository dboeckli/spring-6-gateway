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
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.springdoc.core.utils.Constants.DEFAULT_API_DOCS_URL;

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

    @Value("${security.authorization-url-for-openapi}")
    private String authorizationUrl;
    @Value("${security.token-url-for-openapi}")
    private String tokenUrl;
    @Value("${security.refresh-url-for-openapi}")
    private String refreshUrl;

    @Value("${security.mvc-health-url}")
    private String mvcUrl;
    @Value("${security.auth-server-health-url}")
    private String authServerUrl;
    @Value("${security.reactiveMongo-health-url}")
    private String reactiveMongoUrl;
    @Value("${security.reactive-health-url}")
    private String reactiveUrl;
    @Value("${security.dataRest-health-url}")
    private String dataRestUrl;

    @Value("${springdoc.api-docs.path:#{null}}")
    public String apiDocsPath;

    public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @PostConstruct
    public void init() {
        if (apiDocsPath == null || apiDocsPath.trim().isEmpty()) {
            apiDocsPath = DEFAULT_API_DOCS_URL;
        }
    }

    @Bean
    @Qualifier("customGlobalHeaderOpenApiCustomizer")
    public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer() {
        log.info("### authorizationUrl: " + authorizationUrl);
        log.info("### tokenUrl: " + authorizationUrl);
        log.info("### refreshUrl: " + authorizationUrl);

        log.info("### apiDocsPath: " + apiDocsPath);

        log.info("### mvcUrl: " + mvcUrl);
        log.info("### authServerUrl: " + authServerUrl);
        log.info("### reactiveMongoUrl: " + reactiveMongoUrl);
        log.info("### reactiveUrl: " + reactiveUrl);
        log.info("### dataRestUrl: " + dataRestUrl);


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
            .group("spring-6-rest-mvc")
            .addOpenApiCustomizer(customGlobalHeaderOpenApiCustomizer)
            .displayName("Spring 6 Rest MVC Rest-API")
            .pathsToMatch("/api/v1" + apiDocsPath)
            .addOpenApiCustomizer(openApi -> {
                try {
                    URL restMvcOpenApiUrl = new URL(mvcUrl + apiDocsPath);
                    StringBuilder content = readExternalOpenApiContent(restMvcOpenApiUrl);
                    setOpenApiServer(openApi, "Rest MVC", content);
                    correctActuatorPath(openApi, "/api");
                    setOauth2Url(openApi);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load OpenAPI definition." ,ex);
                }
            })
            .build();
    }

    @Bean
    public GroupedOpenApi reactiveRestApi(@Qualifier("customGlobalHeaderOpenApiCustomizer") OpenApiCustomizer customGlobalHeaderOpenApiCustomizer) {
        return GroupedOpenApi.builder()
            .group("spring-6-reactive")
            .addOpenApiCustomizer(customGlobalHeaderOpenApiCustomizer)
            .displayName("Spring 6 Reactive Rest-API")
            .pathsToMatch("/api/v2" + apiDocsPath)
            .addOpenApiCustomizer(openApi -> {
                try {
                    URL restMvcOpenApiUrl = new URL(reactiveUrl + apiDocsPath);
                    StringBuilder content = readExternalOpenApiContent(restMvcOpenApiUrl);
                    setOpenApiServer(openApi, "Rest Reactive", content);
                    correctActuatorPath(openApi, "/api");
                    setOauth2Url(openApi);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load OpenAPI definition." ,ex);
                }
            })
            .build();
    }

    @Bean
    public GroupedOpenApi reactiveMongoRestApi(@Qualifier("customGlobalHeaderOpenApiCustomizer") OpenApiCustomizer customGlobalHeaderOpenApiCustomizer) {
        return GroupedOpenApi.builder()
            .group("spring-6-reactive-mongo")
            .addOpenApiCustomizer(customGlobalHeaderOpenApiCustomizer)
            .displayName("Spring 6 Reactive Mongo Rest-API")
            .pathsToMatch("/api/v3" + apiDocsPath)
            .addOpenApiCustomizer(openApi -> {
                try {
                    URL restMvcOpenApiUrl = new URL(reactiveMongoUrl + apiDocsPath);
                    StringBuilder content = readExternalOpenApiContent(restMvcOpenApiUrl);
                    setOpenApiServer(openApi, "Rest Reactive Mongo", content);
                    correctActuatorPath(openApi, "/api");
                    setOauth2Url(openApi);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load OpenAPI definition." ,ex);
                }
            })
            .build();
    }

    @Bean
    public GroupedOpenApi dataRestMongoRestApi(@Qualifier("customGlobalHeaderOpenApiCustomizer") OpenApiCustomizer customGlobalHeaderOpenApiCustomizer) {
        return GroupedOpenApi.builder()
            .group("spring-6-data-rest")
            .addOpenApiCustomizer(customGlobalHeaderOpenApiCustomizer)
            .displayName("Spring 6 Data Rest Rest-API")
            .pathsToMatch("/api/v4" + apiDocsPath)
            .addOpenApiCustomizer(openApi -> {
                try {
                    URL restMvcOpenApiUrl = new URL(dataRestUrl + apiDocsPath);
                    StringBuilder content = readExternalOpenApiContent(restMvcOpenApiUrl);
                    setOpenApiServer(openApi, "Rest Data-Rest", content);
                    correctActuatorPath(openApi, "/api");
                    setOauth2Url(openApi);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load OpenAPI definition." ,ex);
                }
            })
            .build();
    }


    @Bean
    public GroupedOpenApi authRestApi(@Qualifier("customGlobalHeaderOpenApiCustomizer") OpenApiCustomizer customGlobalHeaderOpenApiCustomizer) {
        return GroupedOpenApi.builder()
            .group("spring-6-auth-server")
            .addOpenApiCustomizer(customGlobalHeaderOpenApiCustomizer)
            .displayName("Spring 6 Auth Server Rest-API")
            .pathsToMatch("/oauth2/v3" + apiDocsPath)
            .addOpenApiCustomizer(openApi -> {
                try {
                    URL restAuthOpenApiUrl = new URL(authServerUrl + apiDocsPath);
                    StringBuilder content = readExternalOpenApiContent(restAuthOpenApiUrl);
                    setOpenApiServer(openApi, "Rest Auth Server", content);
                    correctActuatorPath(openApi, "/oauth2");
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load OpenAPI definition.", ex);
                }
            })
            .build();
    }

    private void correctActuatorPath(OpenAPI openApi, String pathPrefix) {
        Paths updatedPaths = new Paths();
        boolean actuatorPathFound = false;

        for (Map.Entry<String, PathItem> entry : openApi.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            if (path.startsWith("/actuator")) {
                String newPath = pathPrefix + path;
                updatedPaths.addPathItem(newPath, pathItem);
                actuatorPathFound = true;
            } else {
                updatedPaths.addPathItem(path, pathItem);
            }
        }
        if (actuatorPathFound) {
            openApi.setPaths(updatedPaths);
            log.info("Actuator paths updated for Auth Server");
        } else {
            log.info("No Actuator paths found for Auth Server, paths remain unchanged");
        }
    }

    private void setOpenApiServer(OpenAPI openApi, String description, StringBuilder content) {
        OpenAPI externalOpenApi = new OpenAPIV3Parser().readContents(content.toString()).getOpenAPI();

        // Set the correct server URL
        externalOpenApi.getServers().clear();
        Server server = new Server().url(openApi.getServers().getFirst().getUrl());
        server.description(description);
        log.info("Setting openapi server for rest service {}: {}", description, server);
        externalOpenApi.addServersItem(server);
        log.info("set openapi server for rest service done");

        openApi.getPaths().putAll(externalOpenApi.getPaths());
        if (externalOpenApi.getComponents() != null) {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            if (externalOpenApi.getComponents().getSecuritySchemes() != null) {
                openApi.getComponents().setSecuritySchemes(externalOpenApi.getComponents().getSecuritySchemes());
            }
            if (externalOpenApi.getComponents().getSchemas() != null) {
                if (openApi.getComponents().getSchemas() == null) {
                    openApi.getComponents().setSchemas(new HashMap<>());
                }
                openApi.getComponents().getSchemas().putAll(externalOpenApi.getComponents().getSchemas());
            }
        }
        // Transfer global security requirements
        if (externalOpenApi.getSecurity() != null) {
            openApi.setSecurity(externalOpenApi.getSecurity());
        }

        openApi.setServers(externalOpenApi.getServers());
    }

    private StringBuilder readExternalOpenApiContent(URL externalUrl) throws IOException {
        log.info("Reading openapi definition from rest service: " + externalUrl);
        HttpURLConnection connection = (HttpURLConnection) externalUrl.openConnection();
        log.info("Reading openapi definition from rest service done");

        // Read the response
        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        }
        return content;
    }

    private void setOauth2Url(OpenAPI openApi) {
        openApi.getComponents().getSecuritySchemes().values().stream()
            .filter(scheme -> scheme.getType() == io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2)
            .forEach(scheme -> {
                io.swagger.v3.oas.models.security.OAuthFlows flows = scheme.getFlows();
                if (flows.getClientCredentials() != null) {
                    flows.getClientCredentials().tokenUrl(tokenUrl);
                    flows.getClientCredentials().authorizationUrl(authorizationUrl);
                    flows.getClientCredentials().refreshUrl(refreshUrl);
                }
            });
    }
}
