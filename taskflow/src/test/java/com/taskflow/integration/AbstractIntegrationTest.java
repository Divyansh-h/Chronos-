package com.taskflow.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.taskflow.engine.WorkerSimulator;
// import org.testcontainers.containers.GenericContainer;
// import org.testcontainers.containers.PostgreSQLContainer;
// import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @Testcontainers
public abstract class AbstractIntegrationTest {

    @MockBean
    private WorkerSimulator workerSimulator;

    // public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    //         .withDatabaseName("taskflow_test")
    //         .withUsername("test")
    //         .withPassword("test");

    // public static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    //         .withExposedPorts(6379);

    @BeforeAll
    static void beforeAll() {
        // postgres.start();
        // redis.start();
    }

    // @DynamicPropertySource
    // static void configureProperties(DynamicPropertyRegistry registry) {
    //     registry.add("spring.datasource.url", postgres::getJdbcUrl);
    //     registry.add("spring.datasource.username", postgres::getUsername);
    //     registry.add("spring.datasource.password", postgres::getPassword);
    //     registry.add("spring.data.redis.host", redis::getHost);
    //     registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    // }
}
