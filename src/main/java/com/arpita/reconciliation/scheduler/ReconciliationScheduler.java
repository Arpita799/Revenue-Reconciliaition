package com.arpita.reconciliation.scheduler;

import com.arpita.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReconciliationScheduler {
    private final ReconciliationService reconciliationService;

    //If there is no value for reconciliation.schedule.cron in application.properties
    //then use default - 0 0 2 * * * ; 2AM every night.
    @Scheduled(cron = "${reconciliation.schedule.cron:0 0 2 * * *}")
    public void runAtMidnight(){
        log.info("Scheduled reconciliation triggered");
        reconciliationService.runReconciliation();
    }
}
