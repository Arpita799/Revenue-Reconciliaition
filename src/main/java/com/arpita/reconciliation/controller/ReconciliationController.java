package com.arpita.reconciliation.controller;

import com.arpita.reconciliation.dto.ReconciliationSummaryResponse;
import com.arpita.reconciliation.entity.ReconciliationResult;
import com.arpita.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<ReconciliationResult>> getResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(reconciliationService.getResults(page,size));
    }

}
