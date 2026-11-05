package com.abittech.platform.student.service;

import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.student.dto.StudentProfileData;
import com.abittech.platform.student.dto.StudentProfileResponse;
import com.abittech.platform.student.entity.StudentProfile;
import com.abittech.platform.student.repository.StudentProfileRepository;
import com.abittech.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Student profile use cases. Every method takes the id of the authenticated user, so a caller can
 * only ever reach their own profile.
 */
@Service
public class StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;

    public StudentProfileService(StudentProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(UUID userId) {
        return StudentProfileResponse.from(find(userId));
    }

    @Transactional
    public StudentProfileResponse create(UUID userId, StudentProfileData data) {
        if (profileRepository.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.STUDENT_PROFILE_ALREADY_EXISTS);
        }
        String university = data.university().trim();
        String studentNumber = Strings.trimToNull(data.studentNumber());
        if (studentNumber != null
                && profileRepository.existsByUniversityIgnoreCaseAndStudentNumberIgnoreCase(university, studentNumber)) {
            throw new ApiException(ErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }
        StudentProfile profile = new StudentProfile(userRepository.getReferenceById(userId));
        apply(profile, data);
        StudentProfile saved = profileRepository.save(profile);
        log.info("Student profile created: userId={}", userId);
        return StudentProfileResponse.from(saved);
    }

    @Transactional
    public StudentProfileResponse update(UUID userId, StudentProfileData data) {
        StudentProfile profile = find(userId);
        String university = data.university().trim();
        String studentNumber = Strings.trimToNull(data.studentNumber());
        if (studentNumber != null && profileRepository
                .existsByUniversityIgnoreCaseAndStudentNumberIgnoreCaseAndIdNot(university, studentNumber, profile.getId())) {
            throw new ApiException(ErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }
        apply(profile, data);
        StudentProfile saved = profileRepository.saveAndFlush(profile);
        log.info("Student profile updated: userId={}", userId);
        return StudentProfileResponse.from(saved);
    }

    private StudentProfile find(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
    }

    private void apply(StudentProfile profile, StudentProfileData data) {
        profile.setUniversity(data.university().trim());
        profile.setProgramme(data.programme().trim());
        profile.setYearOfStudy(data.yearOfStudy());
        profile.setStudentNumber(Strings.trimToNull(data.studentNumber()));
        profile.setPhone(Strings.trimToNull(data.phone()));
    }
}
