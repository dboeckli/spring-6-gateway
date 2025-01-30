package guru.springframework.spring6gateway.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
class OpenApiTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BuildProperties buildProperties;

    @Test
    void openapiGetJsonTest() throws JsonProcessingException {
        EntityExchangeResult<byte[]> result = webTestClient.get().uri("/v3/api-docs")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .returnResult();

        String jsonResponse = new String(Objects.requireNonNull(result.getResponseBody()));
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);

        assertThat(jsonNode.has("info")).isTrue();
        JsonNode infoNode = jsonNode.get("info");
        assertThat(infoNode.has("title")).isTrue();
        assertThat(infoNode.get("title").asText()).isEqualTo(buildProperties.getName());
        assertThat(infoNode.get("version").asText()).isEqualTo(buildProperties.getVersion());
        
        log.info("Response:\n{}", objectMapper.writeValueAsString(jsonNode));
    }

}
