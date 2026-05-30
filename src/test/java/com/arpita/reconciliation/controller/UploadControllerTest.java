package com.arpita.reconciliation.controller;


import com.arpita.reconciliation.dto.UploadResponse;
import com.arpita.reconciliation.service.IngestionService;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UploadController.class)
public class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngestionService ingestionService;

    @Test
    void uploadBilling_validCsv_returns200WithSummary() throws Exception{
        when(ingestionService.processBillingFile(any()))
                .thenReturn(new UploadResponse(3,3,0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "billing.csv",
                "text/csv",
                "accountId,recordDate,billedAmount,invoiceId\nACC001,2026-01-15,150.00,INV-001".getBytes()
        );

        mockMvc.perform(multipart("/upload/billing").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void uploadBilling_emptyFile_return400WithSummary() throws Exception{
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "emptyFile.csv",
                "text/csv",
                new byte[0]
        );

        mockMvc.perform(multipart("/upload/billing").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is missing or empty!"));
    }

    @Test
    void uploadBilling_wrongExtension_returns400() throws Exception {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "data.txt", "text/plain", "some content".getBytes()
        );

        mockMvc.perform(multipart("/upload/billing").file(txtFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only CSV files are allowed!"));
    }
}
