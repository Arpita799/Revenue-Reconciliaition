package com.arpita.reconciliation.controller;

import com.arpita.reconciliation.dto.ReconciliationResultResponse;
import com.arpita.reconciliation.dto.ReconciliationSummaryResponse;
import com.arpita.reconciliation.entity.ReconciliationResult;
import com.arpita.reconciliation.enums.ReconciliationStatus;
import com.arpita.reconciliation.service.ReconciliationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
@Slf4j
public class ReconciliationController {
    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    public ResponseEntity<ReconciliationSummaryResponse> runReconciliation(){
        log.info("Manual reconciliation triggered via API");
        ReconciliationSummaryResponse summary = reconciliationService.runReconciliation();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/results")
    public ResponseEntity<Page<ReconciliationResultResponse>> getResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)ReconciliationStatus status,
            @RequestParam(required = false) String accountId
            ){
        return ResponseEntity.ok(reconciliationService.getResults(page,size,status,accountId));
    }

    @GetMapping("/export")
    public void exportCsv(HttpServletResponse response) throws IOException{
        reconciliationService.streamResultsCsv(response);
    }
}
