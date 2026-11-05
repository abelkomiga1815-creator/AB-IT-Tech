package com.abittech.platform.student.dto;

import com.abittech.platform.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/v1/student/profile}: the full set of editable fields (replace semantics).
 */
public record StudentProfileUpdateRequest(
        @NotBlank @Size(max = 200) String university,
        @NotBlank @Size(max = 200) String programme,
        @Min(1) @Max(10) Integer yearOfStudy,
        @Pattern(regexp = ValidationPatterns.STUDENT_NUMBER, message = "may contain letters, digits and / . _ - only") String studentNumber,
        @Pattern(regexp = ValidationPatterns.PHONE, message = "must be a valid phone number") String phone) implements StudentProfileData {
}
