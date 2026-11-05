package com.abittech.platform.research.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.abittech.platform.ai.AiClient;
import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.config.AiProperties;
import com.abittech.platform.project.entity.Project;
import com.abittech.platform.project.service.ProjectService;
import com.abittech.platform.research.dto.AnalysisDetailResponse;
import com.abittech.platform.research.dto.AnalysisSummaryResponse;
import com.abittech.platform.research.dto.SaveSourceRequest;
import com.abittech.platform.research.dto.SourceResponse;
import com.abittech.platform.research.entity.ResearchAnalysis;
import com.abittech.platform.research.entity.ResearchSource;
import com.abittech.platform.research.repository.ResearchAnalysisRepository;
import com.abittech.platform.research.repository.ResearchSourceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Research assistance for a student's own project: runs the AI analysis, keeps its reports and the
 * papers the student saves. Ownership is always checked through {@link ProjectService#requireOwned}.
 */
@Service
public class ResearchService {

    private static final Logger log = LoggerFactory.getLogger(ResearchService.class);
    private static final int MIN_TITLE_LENGTH = 3;          // the AI engine rejects shorter titles
    private static final int RESULTS_PER_SOURCE = 10;
    private static final int MAX_REPORT_CHARS = 2_000_000;

    private final ProjectService projectService;
    private final ResearchAnalysisRepository analysisRepository;
    private final ResearchSourceRepository sourceRepository;
    private final AiClient aiClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public ResearchService(ProjectService projectService,
                           ResearchAnalysisRepository analysisRepository,
                           ResearchSourceRepository sourceRepository,
                           AiClient aiClient,
                           AiProperties aiProperties,
                           ObjectMapper objectMapper,
                           TransactionTemplate transactions,
                           Clock clock) {
        this.projectService = projectService;
        this.analysisRepository = analysisRepository;
        this.sourceRepository = sourceRepository;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.transactions = transactions;
        this.clock = clock;
    }

    private record ProjectSnapshot(UUID id, String title, String description) {
    }

    /**
     * Runs a new analysis. The slow call to the AI engine happens OUTSIDE any database transaction, so no
     * connection is held while waiting for the external sources.
     */
    public AnalysisDetailResponse runAnalysis(UUID userId, UUID projectId) {
        ProjectSnapshot snapshot = transactions.execute(status -> {
            Project project = projectService.requireOwned(userId, projectId);
            enforceMinimumInterval(project.getId());
            return new ProjectSnapshot(project.getId(), project.getTitle(), project.getDescription());
        });
        if (snapshot == null || snapshot.title().trim().length() < MIN_TITLE_LENGTH) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "The project title needs at least " + MIN_TITLE_LENGTH + " characters before a research analysis can run");
        }

        JsonNode report = aiClient.analyze(snapshot.title(), snapshot.description(), RESULTS_PER_SOURCE);
        String reportJson = serialize(report);

        return transactions.execute(status -> {
            Project project = projectService.requireOwned(userId, projectId);
            enforceMinimumInterval(project.getId());   // closes the gap if two requests overlapped
            ResearchAnalysis saved = analysisRepository.saveAndFlush(new ResearchAnalysis(
                    project, snapshot.title(), levelOf(report), papersFoundOf(report), reportJson));
            log.info("Research analysis stored: analysisId={}, projectId={}, papers={}",
                    saved.getId(), projectId, saved.getPapersFound());
            return detail(saved, report);
        });
    }

    @Transactional(readOnly = true)
    public List<AnalysisSummaryResponse> listAnalyses(UUID userId, UUID projectId) {
        Project project = projectService.requireOwned(userId, projectId);
        return analysisRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(AnalysisSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisDetailResponse getAnalysis(UUID userId, UUID projectId, UUID analysisId) {
        Project project = projectService.requireOwned(userId, projectId);
        ResearchAnalysis analysis = analysisRepository.findByIdAndProjectId(analysisId, project.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESEARCH_ANALYSIS_NOT_FOUND));
        return detail(analysis, parse(analysis.getReportJson()));
    }

    // ---------- saved sources ----------

    @Transactional
    public SourceResponse saveSource(UUID userId, UUID projectId, SaveSourceRequest request) {
        Project project = projectService.requireOwned(userId, projectId);
        String doi = Strings.trimToNull(request.doi());
        String title = request.title().trim();
        boolean duplicate = doi != null
                ? sourceRepository.existsByProjectIdAndDoiIgnoreCase(project.getId(), doi)
                : sourceRepository.existsByProjectIdAndTitleIgnoreCase(project.getId(), title);
        if (duplicate) {
            throw new ApiException(ErrorCode.RESEARCH_SOURCE_ALREADY_EXISTS);
        }
        ResearchSource saved = sourceRepository.save(new ResearchSource(
                project, title, Strings.trimToNull(request.authors()), request.year(),
                Strings.trimToNull(request.venue()), doi == null ? null : doi.toLowerCase(),
                Strings.trimToNull(request.url()), Strings.trimToNull(request.provider()),
                Strings.trimToNull(request.note())));
        log.info("Research source saved: sourceId={}, projectId={}", saved.getId(), projectId);
        return SourceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SourceResponse> listSources(UUID userId, UUID projectId) {
        Project project = projectService.requireOwned(userId, projectId);
        return sourceRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(SourceResponse::from)
                .toList();
    }

    /** Removes a paper from the student's own reading list. */
    @Transactional
    public void deleteSource(UUID userId, UUID projectId, UUID sourceId) {
        Project project = projectService.requireOwned(userId, projectId);
        ResearchSource source = sourceRepository.findByIdAndProjectId(sourceId, project.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESEARCH_SOURCE_NOT_FOUND));
        sourceRepository.delete(source);
        log.info("Research source deleted: sourceId={}", sourceId);
    }

    // ---------- helpers ----------

    private void enforceMinimumInterval(UUID projectId) {
        int minimum = aiProperties.minSecondsBetweenAnalyses();
        if (minimum <= 0) {
            return;
        }
        analysisRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId).ifPresent(last -> {
            if (Duration.between(last.getCreatedAt(), Instant.now(clock)).getSeconds() < minimum) {
                throw new ApiException(ErrorCode.ANALYSIS_TOO_FREQUENT);
            }
        });
    }

    private AnalysisDetailResponse detail(ResearchAnalysis analysis, JsonNode report) {
        return new AnalysisDetailResponse(analysis.getId(), analysis.getProjectTitle(), analysis.getSimilarityLevel(),
                analysis.getPapersFound(), analysis.getCreatedAt(), report);
    }

    private static String levelOf(JsonNode report) {
        String level = report.path("analysis").path("similarity").path("level").asText("insufficient_data");
        return switch (level) {
            case "low", "moderate", "high", "insufficient_data" -> level;
            default -> "insufficient_data";
        };
    }

    private static int papersFoundOf(JsonNode report) {
        return Math.max(0, report.path("retrieved").path("papersFound").asInt(0));
    }

    private String serialize(JsonNode report) {
        try {
            String json = objectMapper.writeValueAsString(report);
            if (json.length() > MAX_REPORT_CHARS) {
                throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
            }
            return json;
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            log.error("Stored research report could not be read");
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
