package com.abittech.platform.student.repository;

import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.student.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    Optional<StudentProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByUniversityIgnoreCaseAndStudentNumberIgnoreCase(String university, String studentNumber);

    boolean existsByUniversityIgnoreCaseAndStudentNumberIgnoreCaseAndIdNot(
            String university, String studentNumber, UUID id);
}
