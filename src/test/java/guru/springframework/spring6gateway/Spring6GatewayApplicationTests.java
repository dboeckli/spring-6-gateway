package guru.springframework.spring6gateway;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Slf4j
class Spring6GatewayApplicationTests {

    @Test
    void contextLoads() {
        log.info("Testing Spring 6 Template Application...");
    }

}
