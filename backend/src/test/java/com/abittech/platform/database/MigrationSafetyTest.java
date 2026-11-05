package com.abittech.platform.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Guards the migration rules: versioned naming and no destructive statements.
 */
class MigrationSafetyTest {

    private static final Pattern FILE_NAME = Pattern.compile("^V\\d+__[a-z0-9_]+\\.sql$");
    private static final Pattern DESTRUCTIVE = Pattern.compile(
            "\\b(DROP\\s+(DATABASE|SCHEMA|TABLE)|TRUNCATE)\\b", Pattern.CASE_INSENSITIVE);

    private Resource[] migrations() throws IOException {
        return new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/*.sql");
    }

    @Test
    void migrationsExistAndFollowNamingConvention() throws IOException {
        Resource[] migrations = migrations();
        assertThat(migrations).isNotEmpty();
        for (Resource migration : migrations) {
            assertThat(migration.getFilename()).matches(FILE_NAME);
        }
    }

    @Test
    void migrationsContainNoDestructiveStatements() throws IOException {
        for (Resource migration : migrations()) {
            String sql = migration.getContentAsString(StandardCharsets.UTF_8)
                    .replaceAll("(?m)--.*$", "")
                    .replaceAll("(?s)/\\*.*?\\*/", "");
            assertThat(DESTRUCTIVE.matcher(sql).find())
                    .as("destructive SQL in %s", migration.getFilename())
                    .isFalse();
        }
    }
}
