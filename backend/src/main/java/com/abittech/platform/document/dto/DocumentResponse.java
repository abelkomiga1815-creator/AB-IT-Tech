package com.abittech.platform.document.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.document.entity.DocumentType;
import com.abittech.platform.document.entity.ProjectDocument;

/**
 * Document metadata. The internal storage key is never exposed.
 */
public record DocumentResponse(
        UUID id,
        DocumentType type,
        String fileName,
        String contentType,
        long sizeBytes,
        String description,
        Instant createdAt) {

    public static DocumentResponse from(ProjectDocument document) {
        return new DocumentResponse(document.getId(), document.getType(), document.getOriginalFilename(),
                document.getContentType(), document.getSizeBytes(), document.getDescription(),
                document.getCreatedAt());
    }
}
