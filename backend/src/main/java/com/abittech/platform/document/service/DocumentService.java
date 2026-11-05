package com.abittech.platform.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.config.StorageProperties;
import com.abittech.platform.document.dto.DocumentResponse;
import com.abittech.platform.document.entity.DocumentType;
import com.abittech.platform.document.entity.ProjectDocument;
import com.abittech.platform.document.repository.ProjectDocumentRepository;
import com.abittech.platform.project.entity.Project;
import com.abittech.platform.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Project documents: upload, list, download and delete, always inside the owning student's projects.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ProjectService projectService;
    private final ProjectDocumentRepository documentRepository;
    private final LocalDocumentStorage storage;
    private final StorageProperties storageProperties;

    public DocumentService(ProjectService projectService,
                           ProjectDocumentRepository documentRepository,
                           LocalDocumentStorage storage,
                           StorageProperties storageProperties) {
        this.projectService = projectService;
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.storageProperties = storageProperties;
    }

    /** A file ready to be streamed to the client. */
    public record DocumentDownload(Resource resource, String fileName, String contentType, long sizeBytes) {
    }

    @Transactional
    public DocumentResponse upload(UUID userId, UUID projectId, MultipartFile file, DocumentType type, String description) {
        Project project = projectService.requireOwned(userId, projectId);
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_FILE);
        }
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE);
        }
        String extension = DocumentFileTypes.allowedExtension(file.getOriginalFilename())
                .orElseThrow(() -> new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE));
        String cleanDescription = Strings.trimToNull(description);
        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        String key = UUID.randomUUID() + "." + extension;
        try (InputStream content = file.getInputStream()) {
            storage.store(key, content);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read the uploaded file", ex);
        }
        try {
            String contentType = DocumentFileTypes.contentTypeFor(extension)
                    .orElse("application/octet-stream");
            ProjectDocument saved = documentRepository.saveAndFlush(new ProjectDocument(
                    project,
                    type == null ? DocumentType.OTHER : type,
                    DocumentFileTypes.sanitizeDisplayName(file.getOriginalFilename(), extension),
                    contentType,
                    file.getSize(),
                    key,
                    cleanDescription));
            log.info("Document uploaded: documentId={}, projectId={}, sizeBytes={}",
                    saved.getId(), projectId, saved.getSizeBytes());
            return DocumentResponse.from(saved);
        } catch (RuntimeException ex) {
            storage.delete(key);   // do not leave an orphan file behind
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID userId, UUID projectId) {
        Project project = projectService.requireOwned(userId, projectId);
        return documentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(UUID userId, UUID projectId, UUID documentId) {
        ProjectDocument document = findOwned(userId, projectId, documentId);
        Resource resource = storage.load(document.getStorageKey());
        if (resource == null) {
            log.error("Stored file is missing for documentId={}", documentId);
            throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return new DocumentDownload(resource, document.getOriginalFilename(), document.getContentType(),
                document.getSizeBytes());
    }

    /** Removes one of the student's own documents (database row and file). */
    @Transactional
    public void delete(UUID userId, UUID projectId, UUID documentId) {
        ProjectDocument document = findOwned(userId, projectId, documentId);
        documentRepository.delete(document);
        documentRepository.flush();
        storage.delete(document.getStorageKey());
        log.info("Document deleted: documentId={}, projectId={}", documentId, projectId);
    }

    private ProjectDocument findOwned(UUID userId, UUID projectId, UUID documentId) {
        Project project = projectService.requireOwned(userId, projectId);
        return documentRepository.findByIdAndProjectId(documentId, project.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
    }
}
