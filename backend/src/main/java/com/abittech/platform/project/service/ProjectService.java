package com.abittech.platform.project.service;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.project.dto.ProjectData;
import com.abittech.platform.project.dto.ProjectResponse;
import com.abittech.platform.project.dto.UpdateProjectRequest;
import com.abittech.platform.project.entity.Project;
import com.abittech.platform.project.repository.ProjectRepository;
import com.abittech.platform.student.entity.StudentProfile;
import com.abittech.platform.student.repository.StudentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Student project use cases. Every operation is scoped to the authenticated student: a project that
 * belongs to someone else is reported as not found.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final StudentProfileRepository studentProfileRepository;

    public ProjectService(ProjectRepository projectRepository, StudentProfileRepository studentProfileRepository) {
        this.projectRepository = projectRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional
    public ProjectResponse create(UUID userId, ProjectData data) {
        StudentProfile profile = requireProfile(userId);
        Project project = new Project(profile, data.title().trim());
        apply(project, data);
        Project saved = projectRepository.save(project);
        log.info("Project created: projectId={}, userId={}", saved.getId(), userId);
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID userId) {
        StudentProfile profile = requireProfile(userId);
        return projectRepository.findByStudentProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID userId, UUID projectId) {
        return ProjectResponse.from(requireOwned(userId, projectId));
    }

    @Transactional
    public ProjectResponse update(UUID userId, UUID projectId, UpdateProjectRequest request) {
        Project project = requireOwned(userId, projectId);
        project.setTitle(request.title().trim());
        apply(project, request);
        project.setStatus(request.status());
        Project saved = projectRepository.saveAndFlush(project);
        log.info("Project updated: projectId={}, userId={}", projectId, userId);
        return ProjectResponse.from(saved);
    }

    /**
     * Loads a project only if it belongs to the given user's student profile. Used by the progress,
     * document and defense services so ownership is checked in exactly one place.
     * Must be called inside a transaction (all callers are).
     *
     * @throws ApiException {@code STUDENT_PROFILE_NOT_FOUND} or {@code PROJECT_NOT_FOUND}
     */
    @Transactional(readOnly = true)
    public Project requireOwned(UUID userId, UUID projectId) {
        StudentProfile profile = requireProfile(userId);
        return projectRepository.findByIdAndStudentProfileId(projectId, profile.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private StudentProfile requireProfile(UUID userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
    }

    private void apply(Project project, ProjectData data) {
        project.setDescription(Strings.trimToNull(data.description()));
        project.setResearchArea(Strings.trimToNull(data.researchArea()));
        project.setSupervisorName(Strings.trimToNull(data.supervisorName()));
        project.setDeadline(data.deadline());
    }
}
