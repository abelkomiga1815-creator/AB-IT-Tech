package com.abittech.platform.defense.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.defense.entity.DefenseCategory;
import com.abittech.platform.defense.entity.DefenseQuestion;

public record DefenseQuestionResponse(
        UUID id,
        String question,
        String answer,
        DefenseCategory category,
        boolean prepared,
        Instant createdAt,
        Instant updatedAt) {

    public static DefenseQuestionResponse from(DefenseQuestion question) {
        return new DefenseQuestionResponse(question.getId(), question.getQuestion(), question.getAnswer(),
                question.getCategory(), question.isPrepared(), question.getCreatedAt(), question.getUpdatedAt());
    }
}
