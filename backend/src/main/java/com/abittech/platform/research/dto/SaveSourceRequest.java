package com.abittech.platform.research.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A paper to save to the project's reading list (usually copied from an analysis report).
 * {@code url} must be http(s): it is shown to the student as a link, so other schemes are refused.
 */
public record SaveSourceRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 1000) String authors,
        @Min(1000) @Max(2100) Integer year,
        @Size(max = 300) String venue,
        @Size(max = 200) String doi,
        @Size(max = 1000) @Pattern(regexp = "^(?i)https?://\\S+$", message = "must be an http(s) link") String url,
        @Size(max = 50) String provider,
        @Size(max = 1000) String note) {
}
