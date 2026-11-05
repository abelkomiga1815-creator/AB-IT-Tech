package com.abittech.platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.abittech.platform.document.entity.ProjectDocument;
import com.abittech.platform.document.repository.ProjectDocumentRepository;
import com.abittech.platform.document.service.LocalDocumentStorage;
import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class ProjectDocumentApiTest extends IntegrationTestBase {

    private static final byte[] PDF_BYTES = "%PDF-1.4 test document".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private ProjectDocumentRepository documentRepository;
    @Autowired
    private LocalDocumentStorage storage;

    private String url(String projectId) {
        return "/api/v1/student/projects/" + projectId + "/documents";
    }

    private ResultActions upload(Account student, String projectId, String filename, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart(url(projectId))
                .file(new MockMultipartFile("file", filename, "application/octet-stream", bytes))
                .param("type", "PROPOSAL")
                .param("description", "  First version ")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + student.token()));
    }

    private String uploadOk(Account student, String projectId, String filename) throws Exception {
        return read(upload(student, projectId, filename, PDF_BYTES).andExpect(status().isCreated()).andReturn())
                .at("/data/id").asText();
    }

    @Test
    void studentCanUploadListAndDownloadADocument() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Documents project");

        upload(student, project, "proposal.pdf", PDF_BYTES)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").value("proposal.pdf"))
                .andExpect(jsonPath("$.data.type").value("PROPOSAL"))
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data.sizeBytes").value(PDF_BYTES.length))
                .andExpect(jsonPath("$.data.description").value("First version"))
                .andExpect(jsonPath("$.data.storageKey").doesNotExist());
        String id = uploadOk(student, project, "second.pdf");

        send(HttpMethod.GET, url(project), student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fileName").value("second.pdf"));   // newest first

        mockMvc.perform(get(url(project) + "/" + id + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student.token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("second.pdf")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PDF_BYTES));
    }

    @Test
    void contentTypeComesFromTheExtensionNotFromTheClient() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Documents project");

        upload(student, project, "notes.txt", "plain notes".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contentType").value("text/plain"));
    }

    @Test
    void directoryPartsInTheFilenameAreStripped() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Documents project");

        upload(student, project, "../../etc/evil.pdf", PDF_BYTES)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").value("evil.pdf"));
    }

    @Test
    void dangerousOrUnknownFileTypesAreRejected() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Documents project");

        for (String name : new String[] {"malware.exe", "page.html", "script.js", "image.svg", "archive.zip", "README"}) {
            upload(student, project, name, PDF_BYTES)
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
        }
        send(HttpMethod.GET, url(project), student.token(), null)
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void emptyFilesAreRejected() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Documents project");

        upload(student, project, "empty.pdf", new byte[0])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
    }

    @Test
    void filesOverTheConfiguredLimitAreRejected() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Documents project");

        upload(student, project, "big.pdf", new byte[1_048_577])   // test limit is 1 MB
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void studentCanDeleteADocumentAndItsFile() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Documents project");
        String id = uploadOk(student, project, "delete-me.pdf");
        ProjectDocument stored = documentRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(storage.load(stored.getStorageKey())).isNotNull();

        send(HttpMethod.DELETE, url(project) + "/" + id, student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(storage.load(stored.getStorageKey())).isNull();
        send(HttpMethod.GET, url(project) + "/" + id + "/download", student.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void anotherStudentCannotListDownloadUploadOrDelete() throws Exception {
        Account owner = createStudentWithProfile();
        Account other = createStudentWithProfile();
        String project = createProject(owner, "Owner project");
        String id = uploadOk(owner, project, "private.pdf");
        String otherProject = createProject(other, "Other project");

        send(HttpMethod.GET, url(project), other.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        send(HttpMethod.GET, url(project) + "/" + id + "/download", other.token(), null)
                .andExpect(status().isNotFound());
        send(HttpMethod.DELETE, url(project) + "/" + id, other.token(), null)
                .andExpect(status().isNotFound());
        upload(other, project, "injected.pdf", PDF_BYTES).andExpect(status().isNotFound());
        // the owner's document id is not reachable through the attacker's own project either
        send(HttpMethod.GET, url(otherProject) + "/" + id + "/download", other.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));

        mockMvc.perform(get(url(project) + "/" + id + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF_BYTES));
    }

    @Test
    void roleAndAuthenticationAreRequired() throws Exception {
        Account customer = createCustomerWithProfile();
        String anyProject = UUID.randomUUID().toString();

        send(HttpMethod.GET, url(anyProject), customer.token(), null).andExpect(status().isForbidden());
        send(HttpMethod.GET, url(anyProject), null, null).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart(url(anyProject))
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", PDF_BYTES)))
                .andExpect(status().isUnauthorized());
    }
}
