package com.abittech.platform.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.abittech.platform.support.IntegrationTestBase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationTest extends IntegrationTestBase {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allMigrationsAreAppliedInOrderAtStartup() {
        List<String> applied = Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().getVersion())
                .toList();
        assertThat(applied).startsWith("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void baselineAndProfileTablesExist() {
        String stage = jdbcTemplate.queryForObject(
                "SELECT meta_value FROM app_metadata WHERE meta_key = 'foundation.stage'", String.class);
        assertThat(stage).isEqualTo("GROUP-3-BACKEND-DATABASE-FOUNDATION");
        for (String table : List.of("users", "customer_profiles", "student_profiles", "services",
                "customer_inquiries", "service_requests", "projects", "project_progress", "project_documents",
                "defense_questions", "research_analyses", "research_sources")) {
            Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            assertThat(rows).isNotNull();
        }
    }
}
