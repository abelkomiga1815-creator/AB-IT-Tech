package com.abittech.platform.project;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class StudentProjectApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/student/projects";

    private Map<String, Object> project(String title) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "A study of how students use mobile money.");
        body.put("researchArea", "Information Systems");
        body.put("supervisorName", "Dr. Example");
        body.put("deadline", "2027-06-30");
        return body;
    }

    @Test
    void studentCanCreateAProject() throws Exception {
        Account student = createStudentWithProfile();

        send(HttpMethod.POST, URL, student.token(), project("  Mobile Money Adoption "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("Mobile Money Adoption"))
                .andExpect(jsonPath("$.data.researchArea").value("Information Systems"))
                .andExpect(jsonPath("$.data.deadline").value("2027-06-30"))
                .andExpect(jsonPath("$.data.status").value("PLANNING"))
                .andExpect(jsonPath("$.data.progressPercent").value(0))
                .andExpect(jsonPath("$.data.currentStage").doesNotExist());
    }

    @Test
    void onlyTheTitleIsMandatory() throws Exception {
        Account student = createStudentWithProfile();

        send(HttpMethod.POST, URL, student.token(), Map.of("title", "Minimal project"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.description").doesNotExist())
                .andExpect(jsonPath("$.data.deadline").doesNotExist());
    }

    @Test
    void studentCanListAndReadOwnProjects() throws Exception {
        Account student = createStudentWithProfile();
        String first = createProject(student, "First project");
        createProject(student, "Second project");

        send(HttpMethod.GET, URL, student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Second project"));   // newest first
        send(HttpMethod.GET, URL + "/" + first, student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("First project"));
    }

    @Test
    void studentCanUpdateAndArchiveAProject() throws Exception {
        Account student = createStudentWithProfile();
        String id = createProject(student, "Original title");

        Map<String, Object> update = project("Updated title");
        update.put("status", "IN_PROGRESS");
        send(HttpMethod.PUT, URL + "/" + id, student.token(), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated title"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        update.put("status", "ARCHIVED");
        send(HttpMethod.PUT, URL + "/" + id, student.token(), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        send(HttpMethod.GET, URL + "/" + id, student.token(), null)
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    @Test
    void projectsAreValidated() throws Exception {
        Account student = createStudentWithProfile();
        Map<String, Object> body = new HashMap<>();
        body.put("title", "");
        body.put("description", "x".repeat(4001));

        send(HttpMethod.POST, URL, student.token(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'description')]").exists());
        send(HttpMethod.PUT, URL + "/" + createProject(student, "Valid"), student.token(), project("No status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'status')]").exists());
    }

    @Test
    void studentNeedsAStudentProfileFirst() throws Exception {
        Account student = createAccount("STUDENT");

        send(HttpMethod.POST, URL, student.token(), Map.of("title", "No profile yet"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROFILE_NOT_FOUND"));
        send(HttpMethod.GET, URL, student.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROFILE_NOT_FOUND"));
    }

    @Test
    void studentACannotSeeOrChangeStudentBsProjects() throws Exception {
        Account studentA = createStudentWithProfile();
        Account studentB = createStudentWithProfile();
        String bProject = createProject(studentB, "B private project");

        send(HttpMethod.GET, URL, studentA.token(), null)
                .andExpect(jsonPath("$.data.length()").value(0));
        send(HttpMethod.GET, URL + "/" + bProject, studentA.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        Map<String, Object> attack = project("Hacked");
        attack.put("status", "ARCHIVED");
        send(HttpMethod.PUT, URL + "/" + bProject, studentA.token(), attack)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

        send(HttpMethod.GET, URL + "/" + bProject, studentB.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("B private project"))
                .andExpect(jsonPath("$.data.status").value("PLANNING"));
    }

    @Test
    void unknownProjectReturnsNotFound() throws Exception {
        Account student = createStudentWithProfile();

        send(HttpMethod.GET, URL + "/" + UUID.randomUUID(), student.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void endpointsRequireTheStudentRoleAndAuthentication() throws Exception {
        Account customer = createCustomerWithProfile();

        send(HttpMethod.GET, URL, customer.token(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        send(HttpMethod.POST, URL, customer.token(), Map.of("title", "Nope")).andExpect(status().isForbidden());
        send(HttpMethod.GET, URL, null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
