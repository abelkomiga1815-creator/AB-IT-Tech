package com.abittech.platform.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Document storage settings, bound from {@code app.storage.*} (environment: DOCUMENTS_DIR).
 *
 * @param documentsDir     directory where uploaded files are kept (created at startup if missing)
 * @param maxFileSizeBytes largest accepted upload; keep it at or below {@code spring.servlet.multipart.max-file-size}
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(@NotBlank String documentsDir, @Positive long maxFileSizeBytes) {
}
