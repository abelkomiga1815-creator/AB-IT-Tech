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
 * A paper a student saved to their project's reading list.
 */
@Entity
@Table(name = "research_sources")
public class ResearchSource extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "authors", length = 1000)
    private String authors;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "venue", length = 300)
    private String venue;

    @Column(name = "doi", length = 200)
    private String doi;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "note", length = 1000)
    private String note;

    protected ResearchSource() {
        // required by JPA
    }

    public ResearchSource(Project project, String title, String authors, Integer publicationYear, String venue,
                          String doi, String url, String provider, String note) {
        this.project = project;
        this.title = title;
        this.authors = authors;
        this.publicationYear = publicationYear;
        this.venue = venue;
        this.doi = doi;
        this.url = url;
        this.provider = provider;
        this.note = note;
    }

    public Project getProject() {
        return project;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public String getVenue() {
        return venue;
    }

    public String getDoi() {
        return doi;
    }

    public String getUrl() {
        return url;
    }

    public String getProvider() {
        return provider;
    }

    public String getNote() {
        return note;
    }
}
