package com.abittech.platform.defense.service;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.defense.dto.DefenseQuestionRequest;
import com.abittech.platform.defense.dto.DefenseQuestionResponse;
import com.abittech.platform.defense.entity.DefenseCategory;
import com.abittech.platform.defense.entity.DefenseQuestion;
import com.abittech.platform.defense.repository.DefenseQuestionRepository;
import com.abittech.platform.project.entity.Project;
import com.abittech.platform.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Defense preparation: a student's own practice questions and answers, per project.
 */
@Service
public class DefenseQuestionService {

    private static final Logger log = LoggerFactory.getLogger(DefenseQuestionService.class);

    private final ProjectService projectService;
    private final DefenseQuestionRepository questionRepository;

    public DefenseQuestionService(ProjectService projectService, DefenseQuestionRepository questionRepository) {
        this.projectService = projectService;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public DefenseQuestionResponse create(UUID userId, UUID projectId, DefenseQuestionRequest request) {
        Project project = projectService.requireOwned(userId, projectId);
        DefenseQuestion question = new DefenseQuestion(project, request.question().trim());
        apply(question, request);
        DefenseQuestion saved = questionRepository.save(question);
        log.info("Defense question created: questionId={}, projectId={}", saved.getId(), projectId);
        return DefenseQuestionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DefenseQuestionResponse> list(UUID userId, UUID projectId) {
        Project project = projectService.requireOwned(userId, projectId);
        return questionRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .map(DefenseQuestionResponse::from)
                .toList();
    }

    @Transactional
    public DefenseQuestionResponse update(UUID userId, UUID projectId, UUID questionId, DefenseQuestionRequest request) {
        DefenseQuestion question = findOwned(userId, projectId, questionId);
        question.setQuestion(request.question().trim());
        apply(question, request);
        DefenseQuestion saved = questionRepository.saveAndFlush(question);
        log.info("Defense question updated: questionId={}", questionId);
        return DefenseQuestionResponse.from(saved);
    }

    /** Removes one of the student's own practice questions. */
    @Transactional
    public void delete(UUID userId, UUID projectId, UUID questionId) {
        questionRepository.delete(findOwned(userId, projectId, questionId));
        log.info("Defense question deleted: questionId={}", questionId);
    }

    private DefenseQuestion findOwned(UUID userId, UUID projectId, UUID questionId) {
        Project project = projectService.requireOwned(userId, projectId);
        return questionRepository.findByIdAndProjectId(questionId, project.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.DEFENSE_QUESTION_NOT_FOUND));
    }

    private void apply(DefenseQuestion question, DefenseQuestionRequest request) {
        question.setAnswer(Strings.trimToNull(request.answer()));
        question.setCategory(request.category() == null ? DefenseCategory.GENERAL : request.category());
        question.setPrepared(Boolean.TRUE.equals(request.prepared()));
    }
}
