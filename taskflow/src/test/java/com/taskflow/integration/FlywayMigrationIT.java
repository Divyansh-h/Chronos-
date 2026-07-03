package com.taskflow.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrations_ShouldApplySuccessfully() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM flyway_schema_history", Integer.class);
        
        assertTrue(count != null && count > 0, "Flyway schema history should have at least one applied migration upon startup");
    }
}
