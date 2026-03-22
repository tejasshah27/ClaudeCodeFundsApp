package com.fundsdashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession loginAs(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    @Test
    void editorGetFunds_returns5FieldsPerFund() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(get("/api/funds").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(25))
            .andExpect(jsonPath("$[0].fundName").exists())
            .andExpect(jsonPath("$[0].assetClass").doesNotExist());
    }

    @Test
    void readonlyGetFunds_returns5FieldsPerFund() throws Exception {
        MockHttpSession session = loginAs("readonly");
        mockMvc.perform(get("/api/funds").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(25))
            .andExpect(jsonPath("$[0].assetClass").doesNotExist());
    }

    @Test
    void approverGetFunds_returnsAllFieldsPerFund() throws Exception {
        MockHttpSession session = loginAs("approver");
        mockMvc.perform(get("/api/funds").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(25))
            .andExpect(jsonPath("$[0].assetClass").exists())
            .andExpect(jsonPath("$[0].bloombergId").exists());
    }

    @Test
    void getFundById_alwaysReturnsFullDetail() throws Exception {
        MockHttpSession session = loginAs("readonly");
        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("FUND-001"))
            .andExpect(jsonPath("$.assetClass").exists());
    }

    @Test
    void editorCanSaveFund() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(put("/api/funds/FUND-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SAVE\",\"fundName\":\"Updated Fund\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.fundName").value("Updated Fund"));
    }

    @Test
    void editorCanSubmitFund_changesStatusToSubmitted() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(put("/api/funds/FUND-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SUBMIT\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void approverCanApproveFund() throws Exception {
        MockHttpSession session = loginAs("approver");
        mockMvc.perform(post("/api/funds/FUND-001/approve").session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approverCanRejectFund() throws Exception {
        MockHttpSession session = loginAs("approver");
        mockMvc.perform(post("/api/funds/FUND-001/reject").session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void readonlyCannotSaveFund_returns403() throws Exception {
        MockHttpSession session = loginAs("readonly");
        mockMvc.perform(put("/api/funds/FUND-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SAVE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void editorCannotApproveFund_returns403() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(post("/api/funds/FUND-001/approve").session(session))
            .andExpect(status().isForbidden());
    }
}
