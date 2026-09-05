package com.campus.campuscertify.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class BoardControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(post("/api/reset"));
    }

    @Test
    @DisplayName("GET /api/activities returns the four fixed rows in order")
    void activitiesEndpoint() throws Exception {
        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("A01"))
                .andExpect(jsonPath("$[0].category").value("LEARN"))
                .andExpect(jsonPath("$[0].points").value(2))
                .andExpect(jsonPath("$[1].id").value("A02"))
                .andExpect(jsonPath("$[1].points").value(3))
                .andExpect(jsonPath("$[3].id").value("A04"));
    }

    @Test
    @DisplayName("GET /api/participants returns the five built-in rows")
    void participantsEndpoint() throws Exception {
        mockMvc.perform(get("/api/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").value("C01"))
                .andExpect(jsonPath("$[0].name").value("Asha"))
                .andExpect(jsonPath("$[0].completedActivityIds.length()").value(3));
    }

    @Test
    @DisplayName("POST /api/evaluate returns the built-in oracle with counts 2 and 3")
    void evaluateBuiltInOracle() throws Exception {
        mockMvc.perform(post("/api/evaluate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors.length()").value(0))
                .andExpect(jsonPath("$.results.length()").value(5))
                .andExpect(jsonPath("$.results[0].participantId").value("C01"))
                .andExpect(jsonPath("$.results[0].totalPoints").value(7))
                .andExpect(jsonPath("$.results[0].eligible").value(true))
                .andExpect(jsonPath("$.results[1].participantId").value("C02"))
                .andExpect(jsonPath("$.results[1].totalPoints").value(6))
                .andExpect(jsonPath("$.results[4].participantId").value("C05"))
                .andExpect(jsonPath("$.results[4].totalPoints").value(4))
                .andExpect(jsonPath("$.results[4].failureReasons[0]").value("MISSING_CATEGORY: BUILD"))
                .andExpect(jsonPath("$.results[4].failureReasons[1]").value("POINTS_BELOW_6"))
                .andExpect(jsonPath("$.summary.eligibleCount").value(2))
                .andExpect(jsonPath("$.summary.ineligibleCount").value(3));
    }

    @Test
    @DisplayName("Duplicate participation returns 200 with errors and no results or summary")
    void evaluateWithDuplicateParticipation() throws Exception {
        mockMvc.perform(put("/api/participants/C01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"C01","name":"Asha","completedActivityIds":["A01","A02","A03","A01"]}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/evaluate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].code").value("DUPLICATE_PARTICIPATION"))
                .andExpect(jsonPath("$.errors[0].participantId").value("C01"))
                .andExpect(jsonPath("$.errors[0].offendingValue").value("A01"))
                .andExpect(jsonPath("$.results.length()").value(0))
                .andExpect(jsonPath("$.summary").doesNotExist());
    }

    @Test
    @DisplayName("PUT keeps the edited participant in its original display position")
    void updateKeepsDisplayOrder() throws Exception {
        mockMvc.perform(put("/api/participants/C01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"C01","name":"Asha","completedActivityIds":[]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("C01"))
                .andExpect(jsonPath("$[0].completedActivityIds.length()").value(0))
                .andExpect(jsonPath("$[4].id").value("C05"));
    }

    @Test
    @DisplayName("Untrimmed input is normalized before it reaches the board")
    void inputIsTrimmed() throws Exception {
        mockMvc.perform(put("/api/participants/C05")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"C05","name":"  Eshan  ","completedActivityIds":["  A01","A03 ","A04"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[4].name").value("Eshan"))
                .andExpect(jsonPath("$[4].completedActivityIds[0]").value("A01"));

        mockMvc.perform(post("/api/evaluate"))
                .andExpect(jsonPath("$.summary.eligibleCount").value(3))
                .andExpect(jsonPath("$.summary.ineligibleCount").value(2));
    }

    @Test
    @DisplayName("POST /api/reset restores the built-in rows and the oracle counts")
    void resetEndpoint() throws Exception {
        mockMvc.perform(put("/api/participants/C01")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":"C01","name":"Asha","completedActivityIds":[]}"""));

        mockMvc.perform(post("/api/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].completedActivityIds.length()").value(3));

        mockMvc.perform(post("/api/evaluate"))
                .andExpect(jsonPath("$.summary.eligibleCount").value(2))
                .andExpect(jsonPath("$.summary.ineligibleCount").value(3));
    }
}
