package com.abittech.platform.research.entity;

import com.abittech.platform.common.persistence.AuditedEntity;
import com.abittech.platform.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One saved run of the AI research analysis for a project. The full report is kept as JSON text.
 */
@Entity
@Table(name = "research_analyses")
public class ResearchAnalysis extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    /** The title at the time of the analysis (the project may be renamed later). */
    @Column(name = "project_title", nullable = false, updatable = false, length = 300)
    private String projectTitle;

    @Column(name = "similarity_level", nullable = false, updatable = false, length = 20)
    private String similarityLevel;

    @Column(name = "papers_found", nullable = false, updatable = false)
    private int papersFound;

    @Column(name = "report_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String reportJson;

    protected ResearchAnalysis() {
        // required by JPA
    }

    public ResearchAnalysis(Project project, String projectTitle, String similarityLevel, int papersFound, String reportJson) {
        this.project = project;
        this.projectTitle = projectTitle;
        this.similarityLevel = similarityLevel;
        this.papersFound = papersFound;
        this.reportJson = reportJson;
    }

    public Project getProject() {
        return project;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getSimilarityLevel() {
        return similarityLevel;
    }

    public int getPapersFound() {
        return papersFound;
    }

    public String getReportJson() {
        return reportJson;
    }
}
