package com.aegispay.app.platform.tenancy;

import com.aegispay.app.AegisPayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared-schema discriminator: tenant B must not see tenant A's punches.
 */
@SpringBootTest(classes = AegisPayApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TenantIsolationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("aegispay")
            .withUsername("aegispay")
            .withPassword("aegispay");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("aegispay.seed", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    @Test
    void tenantBDoesNotSeeTenantAPunches() throws Exception {
        String tokenA = signup("North Clinic", "a@example.com");
        String tokenB = signup("South Clinic", "b@example.com");

        String locationJson = """
                {"name":"Downtown","line1":"1 Main","city":"Los Angeles","region":"CA","postalCode":"90001","timeZone":"America/Los_Angeles","jurisdictions":["US-FLSA","US-CA"]}
                """;
        String locationId = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(post("/api/v1/locations")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(locationJson))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );

        mvc.perform(post("/api/v1/people")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalEmployeeCode":"1001","legalName":"Pat Lee","email":"pat@example.com","exemptionStatus":"NON_EXEMPT"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/punches/import?locationId=" + locationId + "&fileName=a.csv")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("employee_code,timestamp,type,location\n1001,2024-06-03 08:00,IN,downtown\n1001,2024-06-03 16:00,OUT,downtown\n"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/punches").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").exists());

        mvc.perform(get("/api/v1/punches").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    private String signup(String clinic, String email) throws Exception {
        String body = """
                {"legalName":"%s","vertical":"DENTAL","displayName":"Owner","email":"%s","password":"Password!23"}
                """.formatted(clinic, email);
        String json = mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.accessToken");
    }
}
