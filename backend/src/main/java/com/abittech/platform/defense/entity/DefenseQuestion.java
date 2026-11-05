package com.abittech.platform.defense.entity;

import com.abittech.platform.common.persistence.AuditedEntity;
import com.abittech.platform.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A question a student expects at the defense, with the student's prepared answer.
 */
@Entity
@Table(name = "defense_questions")
public class DefenseQuestion extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "question", nullable = false, length = 1000)
    private String question;

    @Column(name = "answer", length = 4000)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private DefenseCategory category = DefenseCategory.GENERAL;

    @Column(name = "prepared", nullable = false)
    private boolean prepared;

    protected DefenseQuestion() {
        // required by JPA
    }

    public DefenseQuestion(Project project, String question) {
        this.project = project;
        this.question = question;
    }

    public Project getProject() {
        return project;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public DefenseCategory getCategory() {
        return category;
    }

    public void setCategory(DefenseCategory category) {
        this.category = category;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }
}
