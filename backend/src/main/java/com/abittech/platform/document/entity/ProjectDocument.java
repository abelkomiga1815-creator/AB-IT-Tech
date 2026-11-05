package com.abittech.platform.document.entity;

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
 * Metadata of a file uploaded to a project. The bytes are stored on disk under {@code storageKey}.
 */
@Entity
@Table(name = "project_documents")
public class ProjectDocument extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType type;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, updatable = false, unique = true, length = 100)
    private String storageKey;

    @Column(name = "description", length = 500)
    private String description;

    protected ProjectDocument() {
        // required by JPA
    }

    public ProjectDocument(Project project, DocumentType type, String originalFilename, String contentType,
                           long sizeBytes, String storageKey, String description) {
        this.project = project;
        this.type = type;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.description = description;
    }

    public Project getProject() {
        return project;
    }

    public DocumentType getType() {
        return type;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getDescription() {
        return description;
    }
}
