package com.abittech.platform.project.service;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.util.Strings;
import com.abittech.platform.project.dto.AddProgressRequest;
import com.abittech.platform.project.dto.ProgressResponse;
import com.abittech.platform.project.entity.Project;
import com.abittech.platform.project.entity.ProjectProgress;
import com.abittech.platform.project.repository.ProjectProgressRepository;
import com.abittech.platform.project.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Progress tracking: an append-only history per project. The newest entry also updates the
 * project's current stage and percentage.
 */
@Service
public class ProjectProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProjectProgressService.class);

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final ProjectProgressRepository progressRepository;

    public ProjectProgressService(ProjectService projectService,
                                  ProjectRepository projectRepository,
                                  ProjectProgressRepository progressRepository) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional
    public ProgressResponse add(UUID userId, UUID projectId, AddProgressRequest request) {
        Project project = projectService.requireOwned(userId, projectId);
        ProjectProgress saved = progressRepository.save(new ProjectProgress(
                project, request.stage(), request.percentComplete(), Strings.trimToNull(request.note())));
        project.recordProgress(request.stage(), request.percentComplete());
        projectRepository.saveAndFlush(project);
        log.info("Progress recorded: projectId={}, stage={}, percent={}",
                projectId, request.stage(), request.percentComplete());
        return ProgressResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> history(UUID userId, UUID projectId) {
        Project project = projectService.requireOwned(userId, projectId);
        return progressRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(ProgressResponse::from)
                .toList();
    }
}
