package com.abittech.platform.student.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.student.entity.StudentProfile;

public record StudentProfileResponse(
        UUID id,
        String university,
        String programme,
        Integer yearOfStudy,
        String studentNumber,
        String phone,
        Instant createdAt,
        Instant updatedAt) {

    public static StudentProfileResponse from(StudentProfile profile) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getUniversity(),
                profile.getProgramme(),
                profile.getYearOfStudy(),
                profile.getStudentNumber(),
                profile.getPhone(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
