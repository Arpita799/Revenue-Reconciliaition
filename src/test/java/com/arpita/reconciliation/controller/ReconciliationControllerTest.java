package com.arpita.reconciliation.controller;

import com.arpita.reconciliation.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.mockito.ArgumentMatchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReconciliationController.class)
public class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @Test
    void getResults_invalidStatusEnum_returns400notInternalServerError() throws Exception{
        mockMvc.perform(get("/reconciliation/results").param("status","invalid_status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("invalid_status")));
    }
}
