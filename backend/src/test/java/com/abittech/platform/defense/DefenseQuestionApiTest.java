package com.abittech.platform.defense;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class DefenseQuestionApiTest extends IntegrationTestBase {

    private String url(String projectId) {
        return "/api/v1/student/projects/" + projectId + "/defense-questions";
    }

    private String create(Account student, String project, String question) throws Exception {
        return read(send(HttpMethod.POST, url(project), student.token(), Map.of("question", question))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();
    }

    @Test
    void studentCanCreateAndListQuestionsWithDefaults() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Defense project");

        send(HttpMethod.POST, url(project), student.token(), Map.of("question", "  Why did you choose this method? "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.question").value("Why did you choose this method?"))
                .andExpect(jsonPath("$.data.category").value("GENERAL"))
                .andExpect(jsonPath("$.data.prepared").value(false))
                .andExpect(jsonPath("$.data.answer").doesNotExist());
        create(student, project, "What are the limitations?");

        send(HttpMethod.GET, url(project), student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].question").value("Why did you choose this method?"));   // oldest first
    }

    @Test
    void studentCanAnswerCategoriseAndMarkAsPrepared() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Defense project");
        String id = create(student, project, "How did you validate the results?");

        Map<String, Object> update = new HashMap<>();
        update.put("question", "How did you validate the results?");
        update.put("answer", "I compared them with two published benchmarks.");
        update.put("category", "RESULTS");
        update.put("prepared", true);
        send(HttpMethod.PUT, url(project) + "/" + id, student.token(), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("I compared them with two published benchmarks."))
                .andExpect(jsonPath("$.data.category").value("RESULTS"))
                .andExpect(jsonPath("$.data.prepared").value(true));
    }

    @Test
    void studentCanDeleteAQuestion() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Defense project");
        String id = create(student, project, "Remove me please?");

        send(HttpMethod.DELETE, url(project) + "/" + id, student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        send(HttpMethod.GET, url(project), student.token(), null)
                .andExpect(jsonPath("$.data.length()").value(0));
        send(HttpMethod.DELETE, url(project) + "/" + id, student.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEFENSE_QUESTION_NOT_FOUND"));
    }

    @Test
    void questionsAreValidated() throws Exception {
        Account student = createStudentWithProfile();
        String project = createProject(student, "Defense project");

        send(HttpMethod.POST, url(project), student.token(), Map.of("question", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'question')]").exists());
        send(HttpMethod.POST, url(project), student.token(), Map.of("question", "Valid?", "category", "FOOD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void anotherStudentCannotReachTheQuestions() throws Exception {
        Account owner = createStudentWithProfile();
        Account other = createStudentWithProfile();
        String ownerProject = createProject(owner, "Owner project");
        String question = create(owner, ownerProject, "Owner private question?");
        String otherProject = createProject(other, "Other project");

        send(HttpMethod.GET, url(ownerProject), other.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        send(HttpMethod.POST, url(ownerProject), other.token(), Map.of("question", "Injected?"))
                .andExpect(status().isNotFound());
        send(HttpMethod.DELETE, url(ownerProject) + "/" + question, other.token(), null)
                .andExpect(status().isNotFound());
        // even through the attacker's own project, the owner's question id is not reachable
        send(HttpMethod.PUT, url(otherProject) + "/" + question, other.token(), Map.of("question", "Hijacked?"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEFENSE_QUESTION_NOT_FOUND"));

        send(HttpMethod.GET, url(ownerProject), owner.token(), null)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].question").value("Owner private question?"));
    }

    @Test
    void unknownProjectAndWrongRoleAreRejected() throws Exception {
        Account student = createStudentWithProfile();
        Account customer = createCustomerWithProfile();

        send(HttpMethod.GET, url(UUID.randomUUID().toString()), student.token(), null).andExpect(status().isNotFound());
        send(HttpMethod.GET, url(UUID.randomUUID().toString()), customer.token(), null).andExpect(status().isForbidden());
        send(HttpMethod.GET, url(UUID.randomUUID().toString()), null, null).andExpect(status().isUnauthorized());
    }
}
