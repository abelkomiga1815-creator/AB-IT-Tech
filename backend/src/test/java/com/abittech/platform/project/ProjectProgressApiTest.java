package com.abittech.platform.project;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class ProjectProgressApiTest extends IntegrationTestBase {

    private String progressUrl(String projectId) {
        return "/api/v1/student/projects/" + projectId + "/progress";
    }

    @Test
    void progressUpdatesAreRecordedAndMirroredOnTheProject() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Progress project");

        send(HttpMethod.POST, progressUrl(project), student.token(),
                Map.of("stage", "LITERATURE_REVIEW", "percentComplete", 40, "note", "  Read 12 papers "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stage").value("LITERATURE_REVIEW"))
                .andExpect(jsonPath("$.data.percentComplete").value(40))
                .andExpect(jsonPath("$.data.note").value("Read 12 papers"));
        send(HttpMethod.POST, progressUrl(project), student.token(),
                Map.of("stage", "WRITING", "percentComplete", 75))
                .andExpect(status().isCreated());

        send(HttpMethod.GET, "/api/v1/student/projects/" + project, student.token(), null)
                .andExpect(jsonPath("$.data.currentStage").value("WRITING"))
                .andExpect(jsonPath("$.data.progressPercent").value(75));
        send(HttpMethod.GET, progressUrl(project), student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].stage").value("WRITING"))   // newest first
                .andExpect(jsonPath("$.data[1].stage").value("LITERATURE_REVIEW"));
    }

    @Test
    void progressIsValidated() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Validation project");

        send(HttpMethod.POST, progressUrl(project), student.token(),
                Map.of("stage", "WRITING", "percentComplete", 101))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'percentComplete')]").exists());
        send(HttpMethod.POST, progressUrl(project), student.token(), Map.of("stage", "WRITING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'percentComplete')]").exists());
        send(HttpMethod.POST, progressUrl(project), student.token(),
                Map.of("stage", "DANCING", "percentComplete", 10))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void anotherStudentCannotReadOrAddProgress() throws Exception {
        Account owner = createStudentWithProfile();
        Account other = createStudentWithProfile();
        String project = createProject(owner, "Owner project");
        send(HttpMethod.POST, progressUrl(project), owner.token(),
                Map.of("stage", "PROPOSAL", "percentComplete", 20)).andExpect(status().isCreated());

        send(HttpMethod.GET, progressUrl(project), other.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        send(HttpMethod.POST, progressUrl(project), other.token(),
                Map.of("stage", "SUBMISSION", "percentComplete", 100))
                .andExpect(status().isNotFound());

        send(HttpMethod.GET, "/api/v1/student/projects/" + project, owner.token(), null)
                .andExpect(jsonPath("$.data.currentStage").value("PROPOSAL"))
                .andExpect(jsonPath("$.data.progressPercent").value(20));
    }

    @Test
    void unknownProjectAndMissingAuthenticationAreRejected() throws Exception {
        Account student = createStudentWithProfile();

        send(HttpMethod.GET, progressUrl(UUID.randomUUID().toString()), student.token(), null)
                .andExpect(status().isNotFound());
        send(HttpMethod.GET, progressUrl(UUID.randomUUID().toString()), null, null)
                .andExpect(status().isUnauthorized());
    }
}
