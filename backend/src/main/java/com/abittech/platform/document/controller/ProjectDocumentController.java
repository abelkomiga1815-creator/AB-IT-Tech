package com.abittech.platform.document.controller;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.document.dto.DocumentResponse;
import com.abittech.platform.document.entity.DocumentType;
import com.abittech.platform.document.service.DocumentService;
import com.abittech.platform.document.service.DocumentService.DocumentDownload;
import com.abittech.platform.security.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Documents of one of the student's own projects. Uploads are multipart form data
 * (part {@code file}, optional {@code type} and {@code description}).
 */
@RestController
@RequestMapping("/api/v1/student/projects/{projectId}/documents")
public class ProjectDocumentController {

    private final DocumentService documentService;

    public ProjectDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @PathVariable UUID projectId,
                                                @RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "type", required = false) DocumentType type,
                                                @RequestParam(value = "description", required = false) String description) {
        return ApiResponse.ok(documentService.upload(currentUser.id(), projectId, file, type, description));
    }

    @GetMapping
    public ApiResponse<List<DocumentResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                    @PathVariable UUID projectId) {
        return ApiResponse.ok(documentService.list(currentUser.id(), projectId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @PathVariable UUID projectId,
                                             @PathVariable UUID documentId) {
        DocumentDownload file = documentService.download(currentUser.id(), projectId, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(file.resource());
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable UUID projectId,
                                    @PathVariable UUID documentId) {
        documentService.delete(currentUser.id(), projectId, documentId);
        return ApiResponse.ok();
    }
}
