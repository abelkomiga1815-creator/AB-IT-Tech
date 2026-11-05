package com.abittech.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.abittech.platform.service.entity.ServiceOffering;
import com.abittech.platform.service.repository.ServiceOfferingRepository;
import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

class ServiceCatalogApiTest extends IntegrationTestBase {

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Test
    void catalogueIsPublicAndContainsExactlyTheSevenAgreedServicesInOrder() throws Exception {
        MvcResult result = send(HttpMethod.GET, "/api/v1/services", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(7))
                .andReturn();

        var names = read(result).at("/data").findValuesAsText("name");
        assertThat(names).containsExactly(
                "Software Development", "Web Development", "Mobile App Development", "Cybersecurity",
                "Data Analysis", "UI/UX & Graphic Design", "IT Support & Maintenance");
        assertThat(String.join(" ", names).toLowerCase()).doesNotContain("cloud").doesNotContain("consultancy");
    }

    @Test
    void serviceCanBeFetchedBySlug() throws Exception {
        send(HttpMethod.GET, "/api/v1/services/cybersecurity", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("cybersecurity"))
                .andExpect(jsonPath("$.data.name").value("Cybersecurity"))
                .andExpect(jsonPath("$.data.summary").isNotEmpty());
    }

    @Test
    void unknownServiceReturnsNotFound() throws Exception {
        send(HttpMethod.GET, "/api/v1/services/does-not-exist", null, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void inactiveServicesAreHidden() throws Exception {
        String slug = "retired-" + UUID.randomUUID();
        ServiceOffering retired = new ServiceOffering(slug, "Retired " + UUID.randomUUID(), "Old", "Old service", 99);
        retired.setActive(false);
        serviceRepository.saveAndFlush(retired);

        send(HttpMethod.GET, "/api/v1/services/" + slug, null, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
        MvcResult list = send(HttpMethod.GET, "/api/v1/services", null, null).andReturn();
        assertThat(read(list).at("/data").findValuesAsText("slug")).doesNotContain(slug);
    }

    @Test
    void catalogueIsReadOnlyForClients() throws Exception {
        send(HttpMethod.POST, "/api/v1/services", null, java.util.Map.of("name", "X"))
                .andExpect(status().isUnauthorized());
    }
}
