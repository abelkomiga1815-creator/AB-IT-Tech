package com.abittech.platform.project.entity;

import java.time.LocalDate;

import com.abittech.platform.common.persistence.AuditedEntity;
import com.abittech.platform.student.entity.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A student's research project. A student profile can have many.
 * {@code currentStage} and {@code progressPercent} mirror the latest progress entry.
 */
@Entity
@Table(name = "projects")
public class Project extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false, updatable = false)
    private StudentProfile studentProfile;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "research_area", length = 150)
    private String researchArea;

    @Column(name = "supervisor_name", length = 150)
    private String supervisorName;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", length = 30)
    private ProjectStage currentStage;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    protected Project() {
        // required by JPA
    }

    public Project(StudentProfile studentProfile, String title) {
        this.studentProfile = studentProfile;
        this.title = title;
    }

    public void recordProgress(ProjectStage stage, int percent) {
        this.currentStage = stage;
        this.progressPercent = percent;
    }

    public StudentProfile getStudentProfile() {
        return studentProfile;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResearchArea() {
        return researchArea;
    }

    public void setResearchArea(String researchArea) {
        this.researchArea = researchArea;
    }

    public String getSupervisorName() {
        return supervisorName;
    }

    public void setSupervisorName(String supervisorName) {
        this.supervisorName = supervisorName;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public ProjectStage getCurrentStage() {
        return currentStage;
    }

    public int getProgressPercent() {
        return progressPercent;
    }
}
