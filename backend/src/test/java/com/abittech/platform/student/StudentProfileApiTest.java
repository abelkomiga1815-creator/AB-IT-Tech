package com.abittech.platform.student;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class StudentProfileApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/student/profile";

    private Map<String, Object> profile(String university, String number) {
        Map<String, Object> body = new HashMap<>();
        body.put("university", university);
        body.put("programme", "Computer Science");
        body.put("yearOfStudy", 3);
        body.put("phone", "+255 755 000 111");
        if (number != null) {
            body.put("studentNumber", number);
        }
        return body;
    }

    private String uniqueUniversity() {
        return "University " + UUID.randomUUID();
    }

    @Test
    void studentCanCreateProfile() throws Exception {
        Account student = createAccount("STUDENT");

        send(HttpMethod.POST, URL, student.token(), profile(uniqueUniversity(), "2024/CS/001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.programme").value("Computer Science"))
                .andExpect(jsonPath("$.data.yearOfStudy").value(3))
                .andExpect(jsonPath("$.data.studentNumber").value("2024/CS/001"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.fullName").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist());
    }

    @Test
    void studentCanRetrieveProfile() throws Exception {
        Account student = createAccount("STUDENT");
        String university = uniqueUniversity();
        send(HttpMethod.POST, URL, student.token(), profile(university, null)).andExpect(status().isCreated());

        send(HttpMethod.GET, URL, student.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.university").value(university))
                .andExpect(jsonPath("$.data.studentNumber").doesNotExist());
    }

    @Test
    void studentCanUpdateProfile() throws Exception {
        Account student = createAccount("STUDENT");
        send(HttpMethod.POST, URL, student.token(), profile(uniqueUniversity(), null)).andExpect(status().isCreated());

        Map<String, Object> update = profile(uniqueUniversity(), "NEW-77");
        update.put("programme", "  Software Engineering ");
        update.put("yearOfStudy", 4);

        send(HttpMethod.PUT, URL, student.token(), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.programme").value("Software Engineering"))
                .andExpect(jsonPath("$.data.yearOfStudy").value(4))
                .andExpect(jsonPath("$.data.studentNumber").value("NEW-77"));

        send(HttpMethod.GET, URL, student.token(), null)
                .andExpect(jsonPath("$.data.programme").value("Software Engineering"));
    }

    @Test
    void duplicateProfileIsRejected() throws Exception {
        Account student = createAccount("STUDENT");
        send(HttpMethod.POST, URL, student.token(), profile(uniqueUniversity(), null)).andExpect(status().isCreated());

        send(HttpMethod.POST, URL, student.token(), profile(uniqueUniversity(), null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("STUDENT_PROFILE_ALREADY_EXISTS"));
    }

    @Test
    void getAndUpdateBeforeCreationReturnNotFound() throws Exception {
        Account student = createAccount("STUDENT");

        send(HttpMethod.GET, URL, student.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROFILE_NOT_FOUND"));
        send(HttpMethod.PUT, URL, student.token(), profile(uniqueUniversity(), null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROFILE_NOT_FOUND"));
    }

    @Test
    void nonStudentCannotCreateStudentProfile() throws Exception {
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.POST, URL, customer.token(), profile(uniqueUniversity(), null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        send(HttpMethod.POST, URL, null, profile("Some University", null))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        send(HttpMethod.GET, URL, null, null).andExpect(status().isUnauthorized());
        send(HttpMethod.PUT, URL, null, profile("Some University", null)).andExpect(status().isUnauthorized());
    }

    @Test
    void optionalFieldsMayBeOmitted() throws Exception {
        Account student = createAccount("STUDENT");
        Map<String, Object> minimal = Map.of("university", uniqueUniversity(), "programme", "Information Systems");

        send(HttpMethod.POST, URL, student.token(), minimal)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.yearOfStudy").doesNotExist())
                .andExpect(jsonPath("$.data.studentNumber").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    @Test
    void invalidInputIsRejectedWithFieldErrors() throws Exception {
        Account student = createAccount("STUDENT");
        Map<String, Object> body = new HashMap<>();
        body.put("university", "");
        body.put("yearOfStudy", 11);
        body.put("studentNumber", "bad number!");
        body.put("phone", "abc");

        send(HttpMethod.POST, URL, student.token(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'university')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'programme')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'yearOfStudy')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'studentNumber')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'phone')]").exists());
    }

    @Test
    void studentNumberMustBeUniqueWithinAUniversity() throws Exception {
        String university = uniqueUniversity();
        Account first = createAccount("STUDENT");
        Account second = createAccount("STUDENT");
        send(HttpMethod.POST, URL, first.token(), profile(university, "S-500")).andExpect(status().isCreated());

        send(HttpMethod.POST, URL, second.token(), profile(university.toUpperCase(), "s-500"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_NUMBER_ALREADY_EXISTS"));

        // the same number at a different university is fine
        send(HttpMethod.POST, URL, second.token(), profile(uniqueUniversity(), "S-500"))
                .andExpect(status().isCreated());
    }

    @Test
    void updatingToTheOwnStudentNumberIsAllowed() throws Exception {
        Account student = createAccount("STUDENT");
        String university = uniqueUniversity();
        send(HttpMethod.POST, URL, student.token(), profile(university, "S-900")).andExpect(status().isCreated());

        send(HttpMethod.PUT, URL, student.token(), profile(university, "S-900")).andExpect(status().isOk());
    }

    @Test
    void updatingToSomeoneElsesStudentNumberIsRejected() throws Exception {
        String university = uniqueUniversity();
        Account first = createAccount("STUDENT");
        Account second = createAccount("STUDENT");
        send(HttpMethod.POST, URL, first.token(), profile(university, "S-1")).andExpect(status().isCreated());
        send(HttpMethod.POST, URL, second.token(), profile(university, "S-2")).andExpect(status().isCreated());

        send(HttpMethod.PUT, URL, second.token(), profile(university, "S-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_NUMBER_ALREADY_EXISTS"));
    }
}
