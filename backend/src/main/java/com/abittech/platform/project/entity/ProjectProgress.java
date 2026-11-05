package com.abittech.platform.project.entity;

import com.abittech.platform.common.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One entry in a project's progress history: "I am at stage X, N percent done".
 */
@Entity
@Table(name = "project_progress")
public class ProjectProgress extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private ProjectStage stage;

    @Column(name = "percent_complete", nullable = false)
    private int percentComplete;

    @Column(name = "note", length = 2000)
    private String note;

    protected ProjectProgress() {
        // required by JPA
    }

    public ProjectProgress(Project project, ProjectStage stage, int percentComplete, String note) {
        this.project = project;
        this.stage = stage;
        this.percentComplete = percentComplete;
        this.note = note;
    }

    public Project getProject() {
        return project;
    }

    public ProjectStage getStage() {
        return stage;
    }

    public int getPercentComplete() {
        return percentComplete;
    }

    public String getNote() {
        return note;
    }
}
