package com.abittech.platform.research;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.ai.AiClient;
import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Research assistance through the real controllers, security and database; only the call to the AI
 * engine is replaced by a mock.
 */
class ResearchApiTest extends IntegrationTestBase {

    private static final String REPORT = """
            {"generatedAt":"2026-10-07T12:00:00Z","project":{"title":"x","hasAbstract":true},
             "retrieved":{"queries":["crop"],"sources":[{"provider":"openalex","status":"ok","papersFound":2,"message":""}],
               "papersFound":2,"relatedWork":[{"title":"Paper one","url":"https://doi.org/10.1/one","similarityScore":0.4}]},
             "analysis":{"keywords":[],"similarity":{"level":"moderate","maxScore":0.4,"disclaimer":"not a plagiarism check"},
               "gapAnalysis":{"suggestions":[]},"relatedTerms":[],"alternativeTitles":[]},
             "methodology":[],"notices":["n"]}""";

    @MockitoBean
    private AiClient aiClient;

    private JsonNode report() throws Exception {
        return objectMapper.readTree(REPORT);
    }

    private String base(String projectId) {
        return "/api/v1/student/projects/" + projectId + "/research";
    }

    // ---------- analyses ----------

    @Test
    void studentCanRunAndReadBackAnAnalysis() throws Exception {
        when(aiClient.analyze(any(), any(), anyInt())).thenReturn(report());
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Crop disease detection");

        String id = read(send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.similarityLevel").value("moderate"))
                .andExpect(jsonPath("$.data.papersFound").value(2))
                .andExpect(jsonPath("$.data.projectTitle").value("Crop disease detection"))
                .andExpect(jsonPath("$.data.report.retrieved.relatedWork[0].title").value("Paper one"))
                .andReturn()).at("/data/id").asText();

        send(HttpMethod.GET, base(projectId) + "/analyses", student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].report").doesNotExist());
        send(HttpMethod.GET, base(projectId) + "/analyses/" + id, student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.report.analysis.similarity.disclaimer").value("not a plagiarism check"))
                .andExpect(jsonPath("$.data.report.notices[0]").value("n"));
    }

    @Test
    void theProjectTitleAndDescriptionAreSentToTheEngine() throws Exception {
        when(aiClient.analyze(any(), any(), anyInt())).thenReturn(report());
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Blockchain voting systems");
        send(HttpMethod.PUT, "/api/v1/student/projects/" + projectId, student.token(),
                Map.of("title", "Blockchain voting systems", "description", "Secure e-voting.", "status", "PLANNING"))
                .andExpect(status().isOk());

        send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null).andExpect(status().isCreated());

        verify(aiClient).analyze(eq("Blockchain voting systems"), eq("Secure e-voting."), anyInt());
    }

    @Test
    void asecondAnalysisRightAfterTheFirstIsRefused() throws Exception {
        when(aiClient.analyze(any(), any(), anyInt())).thenReturn(report());
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Crop disease detection");
        send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null).andExpect(status().isCreated());

        send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ANALYSIS_TOO_FREQUENT"));
    }

    @Test
    void whenTheEngineIsDownNothingIsStoredAndTheStudentGetsAClearError() throws Exception {
        when(aiClient.analyze(any(), any(), anyInt())).thenThrow(new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Crop disease detection");

        send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"));
        send(HttpMethod.GET, base(projectId) + "/analyses", student.token(), null)
                .andExpect(jsonPath("$.data.length()").value(0));

        // a failed attempt must not block a retry
        when(aiClient.analyze(any(), any(), anyInt())).thenReturn(report());
        send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null).andExpect(status().isCreated());
    }

    @Test
    void aBrokenEngineAnswerIsABadGateway() throws Exception {
        when(aiClient.analyze(any(), any(), anyInt())).thenThrow(new ApiException(ErrorCode.AI_SERVICE_ERROR));
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Crop disease detection");

        send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_ERROR"));
    }

    @Test
    void tooShortATitleIsRejectedBeforeCallingTheEngine() throws Exception {
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "ab");

        send(HttpMethod.POST, base(projectId) + "/analyses", student.token(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verify(aiClient, never()).analyze(any(), any(), anyInt());
    }

    @Test
    void otherStudentsCannotRunOrReadAnalyses() throws Exception {
        when(aiClient.analyze(any(), any(), anyInt())).thenReturn(report());
        Account owner = createStudentWithProfile();
        Account other = createStudentWithProfile();
        String projectId = createProject(owner, "Owner project");
        String analysisId = read(send(HttpMethod.POST, base(projectId) + "/analyses", owner.token(), null)
                .andReturn()).at("/data/id").asText();

        send(HttpMethod.POST, base(projectId) + "/analyses", other.token(), null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        send(HttpMethod.GET, base(projectId) + "/analyses", other.token(), null).andExpect(status().isNotFound());
        send(HttpMethod.GET, base(projectId) + "/analyses/" + analysisId, other.token(), null)
                .andExpect(status().isNotFound());
        verify(aiClient).analyze(any(), any(), anyInt());   // only the owner's call reached the engine
    }

    @Test
    void anAnalysisIsNotReachableThroughAnotherProject() throws Exception {
        when(aiClient.analyze(any(), any(), anyInt())).thenReturn(report());
        Account student = createStudentWithProfile();
        String one = createProject(student, "Project one");
        String two = createProject(student, "Project two");
        String analysisId = read(send(HttpMethod.POST, base(one) + "/analyses", student.token(), null)
                .andReturn()).at("/data/id").asText();

        send(HttpMethod.GET, base(two) + "/analyses/" + analysisId, student.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESEARCH_ANALYSIS_NOT_FOUND"));
    }

    @Test
    void rolesAndAuthenticationAreEnforced() throws Exception {
        Account customer = createCustomerWithProfile();
        String anyProject = UUID.randomUUID().toString();

        send(HttpMethod.POST, base(anyProject) + "/analyses", customer.token(), null).andExpect(status().isForbidden());
        send(HttpMethod.POST, base(anyProject) + "/analyses", null, null).andExpect(status().isUnauthorized());
        send(HttpMethod.GET, base(anyProject) + "/sources", null, null).andExpect(status().isUnauthorized());
        verify(aiClient, never()).analyze(any(), any(), anyInt());
    }

    // ---------- saved sources ----------

    private Map<String, Object> source(String doi) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Deep learning for crop disease");
        body.put("authors", "A. Author, B. Writer");
        body.put("year", 2021);
        body.put("venue", "Journal of Crops");
        body.put("doi", doi);
        body.put("url", "https://doi.org/" + doi);
        body.put("provider", "openalex");
        body.put("note", "Read chapter 3");
        return body;
    }

    @Test
    void studentCanSaveListAndDeleteSources() throws Exception {
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Reading list project");

        String id = read(send(HttpMethod.POST, base(projectId) + "/sources", student.token(), source("10.1/ABC"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.doi").value("10.1/abc"))
                .andExpect(jsonPath("$.data.year").value(2021))
                .andReturn()).at("/data/id").asText();

        send(HttpMethod.GET, base(projectId) + "/sources", student.token(), null)
                .andExpect(jsonPath("$.data.length()").value(1));
        send(HttpMethod.DELETE, base(projectId) + "/sources/" + id, student.token(), null)
                .andExpect(status().isOk());
        send(HttpMethod.GET, base(projectId) + "/sources", student.token(), null)
                .andExpect(jsonPath("$.data.length()").value(0));
        send(HttpMethod.DELETE, base(projectId) + "/sources/" + id, student.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESEARCH_SOURCE_NOT_FOUND"));
    }

    @Test
    void theSamePaperCannotBeSavedTwice() throws Exception {
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Reading list project");
        send(HttpMethod.POST, base(projectId) + "/sources", student.token(), source("10.1/dup")).andExpect(status().isCreated());

        send(HttpMethod.POST, base(projectId) + "/sources", student.token(), source("10.1/DUP"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESEARCH_SOURCE_ALREADY_EXISTS"));

        Map<String, Object> noDoi = Map.of("title", "A paper without a DOI");
        send(HttpMethod.POST, base(projectId) + "/sources", student.token(), noDoi).andExpect(status().isCreated());
        send(HttpMethod.POST, base(projectId) + "/sources", student.token(), Map.of("title", "a paper WITHOUT a doi"))
                .andExpect(status().isConflict());
    }

    @Test
    void sourceLinksMustBeHttpAndTheTitleIsRequired() throws Exception {
        Account student = createStudentWithProfile();
        String projectId = createProject(student, "Reading list project");

        for (String badUrl : new String[] {"javascript:alert(1)", "data:text/html,<script>", "ftp://x.org/p", "//evil.example"}) {
            Map<String, Object> body = source("10.1/x");
            body.put("url", badUrl);
            send(HttpMethod.POST, base(projectId) + "/sources", student.token(), body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field == 'url')]").exists());
        }
        send(HttpMethod.POST, base(projectId) + "/sources", student.token(), Map.of("title", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists());
    }

    @Test
    void sourcesAreOnlyVisibleToTheOwner() throws Exception {
        Account owner = createStudentWithProfile();
        Account other = createStudentWithProfile();
        String projectId = createProject(owner, "Owner project");
        String sourceId = read(send(HttpMethod.POST, base(projectId) + "/sources", owner.token(), source("10.1/own"))
                .andReturn()).at("/data/id").asText();

        send(HttpMethod.GET, base(projectId) + "/sources", other.token(), null).andExpect(status().isNotFound());
        send(HttpMethod.POST, base(projectId) + "/sources", other.token(), source("10.1/x")).andExpect(status().isNotFound());
        send(HttpMethod.DELETE, base(projectId) + "/sources/" + sourceId, other.token(), null)
                .andExpect(status().isNotFound());
        assertThat(read(send(HttpMethod.GET, base(projectId) + "/sources", owner.token(), null).andReturn())
                .at("/data").size()).isEqualTo(1);
    }
}
