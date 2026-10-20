package guru.springframework.spring6gateway.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springdoc.core.utils.Constants.DEFAULT_API_DOCS_URL;

class OpenApiConfigurationTest {

    private static final String RUNTIME_SERVER = "http://runtime:8080";

    private static final String EXTERNAL_SPEC = """
            {
              "openapi": "3.0.1",
              "info": { "title": "external", "version": "1" },
              "servers": [ { "url": "http://external:1234" } ],
              "paths": {
                "/beer": { "get": { "responses": { "200": { "description": "ok" } } } },
                "/actuator/health": { "get": { "responses": { "200": { "description": "ok" } } } }
              },
              "components": {
                "securitySchemes": {
                  "Bearer Authentication": {
                    "type": "oauth2",
                    "flows": {
                      "clientCredentials": { "tokenUrl": "http://old/token", "scopes": {} }
                    }
                  }
                }
              },
              "security": [ { "Bearer Authentication": [] } ]
            }
            """;

    private static final String MINIMAL_SPEC = """
            {
              "openapi": "3.0.1",
              "info": { "title": "minimal", "version": "1" },
              "paths": {
                "/beer": { "get": { "responses": { "200": { "description": "ok" } } } }
              }
            }
            """;

    private static WireMockServer server;

    @BeforeAll
    static void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void resetStubs() {
        server.resetAll();
    }

    @Test
    void initUsesDefaultApiDocsPathWhenUnset() {
        OpenApiConfiguration configuration = newConfiguration(server.baseUrl());

        configuration.apiDocsPath = null;
        configuration.init();
        assertThat(configuration.apiDocsPath).isEqualTo(DEFAULT_API_DOCS_URL);

        configuration.apiDocsPath = " ";
        configuration.init();
        assertThat(configuration.apiDocsPath).isEqualTo(DEFAULT_API_DOCS_URL);

        configuration.apiDocsPath = "/custom/api-docs";
        configuration.init();
        assertThat(configuration.apiDocsPath).isEqualTo("/custom/api-docs");
    }

    @Test
    void serverCustomizerKeepsRuntimeServerWhenNoExternalUrlIsConfigured() {
        OpenAPI openApi = targetOpenApi();

        newConfiguration(server.baseUrl()).openApiServerCustomizer().customise(openApi);

        assertThat(openApi.getServers()).extracting(Server::getUrl).containsExactly(RUNTIME_SERVER);
    }

    @Test
    void serverCustomizerReplacesServersWithConfiguredUrl() {
        OpenApiConfiguration configuration = newConfiguration(server.baseUrl());
        ReflectionTestUtils.setField(configuration, "openApiServerUrl", "https://api.example.com");

        OpenAPI openApi = targetOpenApi();
        configuration.openApiServerCustomizer().customise(openApi);

        assertThat(openApi.getServers()).extracting(Server::getUrl).containsExactly("https://api.example.com");
    }

    @Test
    void globalHeaderCustomizerUpdatesInfoAndOAuthUrls() {
        OpenApiConfiguration configuration = newConfiguration(server.baseUrl());
        OpenAPI openApi = targetOpenApi();
        openApi.getComponents().addSecuritySchemes("api-key", new SecurityScheme().type(SecurityScheme.Type.APIKEY));
        openApi.getComponents()
            .addSecuritySchemes("oauth-without-client-credentials",
                    new SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows()));

        configuration.customGlobalHeaderOpenApiCustomizer().customise(openApi);

        assertThat(openApi.getInfo().getTitle()).isEqualTo("spring-6-gateway-test");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.2.3");
        OAuthFlow clientCredentials = openApi.getComponents()
            .getSecuritySchemes()
            .get("Bearer Authentication")
            .getFlows()
            .getClientCredentials();
        assertThat(clientCredentials.getTokenUrl()).isEqualTo(server.baseUrl() + "/oauth2/token");
        assertThat(clientCredentials.getAuthorizationUrl()).isEqualTo(server.baseUrl() + "/oauth2/auth");
        assertThat(clientCredentials.getRefreshUrl()).isEqualTo(server.baseUrl() + "/oauth2/refresh");
    }

    static Stream<Arguments> groups() {
        return Stream.of(Arguments.of("rest-mvc",
                (BiFunction<OpenApiConfiguration, OpenApiCustomizer, GroupedOpenApi>) OpenApiConfiguration::mvcRestApi,
                "/api/actuator/health", true),
                Arguments.of("reactive",
                        (BiFunction<OpenApiConfiguration, OpenApiCustomizer, GroupedOpenApi>) OpenApiConfiguration::reactiveRestApi,
                        "/api/actuator/health", true),
                Arguments.of("reactive-mongo",
                        (BiFunction<OpenApiConfiguration, OpenApiCustomizer, GroupedOpenApi>) OpenApiConfiguration::reactiveMongoRestApi,
                        "/api/actuator/health", true),
                Arguments.of("data-rest",
                        (BiFunction<OpenApiConfiguration, OpenApiCustomizer, GroupedOpenApi>) OpenApiConfiguration::dataRestMongoRestApi,
                        "/api/actuator/health", true),
                Arguments.of("auth-server",
                        (BiFunction<OpenApiConfiguration, OpenApiCustomizer, GroupedOpenApi>) OpenApiConfiguration::authRestApi,
                        "/oauth2/actuator/health", false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("groups")
    void groupCustomizerMergesExternalSpec(String name,
            BiFunction<OpenApiConfiguration, OpenApiCustomizer, GroupedOpenApi> groupFactory, String actuatorPath,
            boolean rewritesOAuthUrls) {
        stubSpec(EXTERNAL_SPEC);
        OpenApiConfiguration configuration = newConfiguration(server.baseUrl());
        GroupedOpenApi group = groupFactory.apply(configuration, configuration.customGlobalHeaderOpenApiCustomizer());

        OpenAPI openApi = targetOpenApi();
        group.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi));

        assertThat(openApi.getPaths()).containsKey("/beer").containsKey(actuatorPath);
        assertThat(openApi.getServers()).extracting(Server::getUrl).containsExactly(RUNTIME_SERVER);
        assertThat(openApi.getSecurity()).isNotEmpty();
        if (rewritesOAuthUrls) {
            OAuthFlow clientCredentials = openApi.getComponents()
                .getSecuritySchemes()
                .get("Bearer Authentication")
                .getFlows()
                .getClientCredentials();
            assertThat(clientCredentials.getTokenUrl()).isEqualTo(server.baseUrl() + "/oauth2/token");
            assertThat(clientCredentials.getAuthorizationUrl()).isEqualTo(server.baseUrl() + "/oauth2/auth");
            assertThat(clientCredentials.getRefreshUrl()).isEqualTo(server.baseUrl() + "/oauth2/refresh");
        }
    }

    @Test
    void groupCustomizerUsesConfiguredServerUrl() {
        stubSpec(EXTERNAL_SPEC);
        OpenApiConfiguration configuration = newConfiguration(server.baseUrl());
        ReflectionTestUtils.setField(configuration, "openApiServerUrl", "https://api.example.com");
        GroupedOpenApi group = configuration.dataRestMongoRestApi(configuration.customGlobalHeaderOpenApiCustomizer());

        OpenAPI openApi = targetOpenApi();
        group.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi));

        assertThat(openApi.getServers()).extracting(Server::getUrl).containsExactly("https://api.example.com");
        assertThat(openApi.getServers().getFirst().getDescription()).isEqualTo("Rest Data-Rest");
    }

    @Test
    void groupCustomizerLeavesPathsUntouchedWhenExternalSpecHasNoActuatorPaths() {
        stubSpec(MINIMAL_SPEC);
        OpenApiConfiguration configuration = newConfiguration(server.baseUrl());
        GroupedOpenApi group = configuration.mvcRestApi(configuration.customGlobalHeaderOpenApiCustomizer());

        OpenAPI openApi = targetOpenApi();
        group.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi));

        assertThat(openApi.getPaths()).containsKey("/beer").doesNotContainKey("/api/actuator/health");
        assertThat(openApi.getComponents()).isNotNull();
    }

    @Test
    void groupCustomizerWrapsFetchErrors() {
        OpenApiConfiguration configuration = newConfiguration("http://127.0.0.1:1");
        GroupedOpenApi group = configuration.mvcRestApi(configuration.customGlobalHeaderOpenApiCustomizer());

        OpenAPI openApi = targetOpenApi();

        assertThatThrownBy(() -> group.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to load OpenAPI definition.");
    }

    private void stubSpec(String body) {
        server.stubFor(get(urlEqualTo("/v3/api-docs"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
    }

    private OpenApiConfiguration newConfiguration(String baseUrl) {
        Properties properties = new Properties();
        properties.setProperty("name", "spring-6-gateway-test");
        properties.setProperty("version", "1.2.3");
        OpenApiConfiguration configuration = new OpenApiConfiguration(new BuildProperties(properties));
        ReflectionTestUtils.setField(configuration, "authorizationUrl", baseUrl + "/oauth2/auth");
        ReflectionTestUtils.setField(configuration, "tokenUrl", baseUrl + "/oauth2/token");
        ReflectionTestUtils.setField(configuration, "refreshUrl", baseUrl + "/oauth2/refresh");
        ReflectionTestUtils.setField(configuration, "mvcUrl", baseUrl);
        ReflectionTestUtils.setField(configuration, "authServerUrl", baseUrl);
        ReflectionTestUtils.setField(configuration, "reactiveMongoUrl", baseUrl);
        ReflectionTestUtils.setField(configuration, "reactiveUrl", baseUrl);
        ReflectionTestUtils.setField(configuration, "dataRestUrl", baseUrl);
        ReflectionTestUtils.setField(configuration, "openApiServerUrl", "");
        configuration.apiDocsPath = "/v3/api-docs";
        return configuration;
    }

    private OpenAPI targetOpenApi() {
        return new OpenAPI().info(new Info().title("old").version("old"))
            .paths(new Paths())
            .components(new Components().addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().clientCredentials(new OAuthFlow()))))
            .servers(new ArrayList<>(List.of(new Server().url(RUNTIME_SERVER))));
    }

}
