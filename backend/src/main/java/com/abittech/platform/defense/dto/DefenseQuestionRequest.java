package com.abittech.platform.defense.dto;

import com.abittech.platform.defense.entity.DefenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for creating and (replace semantics) updating a defense question.
 *
 * @param category optional, defaults to GENERAL
 * @param prepared optional, defaults to false
 */
public record DefenseQuestionRequest(
        @NotBlank @Size(max = 1000) String question,
        @Size(max = 4000) String answer,
        DefenseCategory category,
        Boolean prepared) {
}
