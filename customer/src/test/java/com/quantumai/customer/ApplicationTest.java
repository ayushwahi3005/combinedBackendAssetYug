package com.quantumai.customer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=3.5.5",
    "spring.data.mongodb.database=test_db"
})
class ApplicationTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring Boot application context loads successfully
        // If the context fails to load, this test will fail
    }
}
