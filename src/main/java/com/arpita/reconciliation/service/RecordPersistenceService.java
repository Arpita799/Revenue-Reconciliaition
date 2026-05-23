package com.arpita.reconciliation.service;

import com.arpita.reconciliation.repository.BillingRecordsRepository;
import com.arpita.reconciliation.repository.PaymentRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

// Each row is saved in its own REQUIRES_NEW transaction via RecordPersistenceService.
// This ensures a duplicate key or constraint violation on one row is isolated —
// the row is logged to ingestion_errors and processing continues.
// Without this, a DataIntegrityViolationException would mark the outer transaction
// as rollback-only, causing logError() to also fail silently.

@Service
@RequiredArgsConstructor
public class RecordPersistenceService {
    private final BillingRecordsRepository billingRecordsRepository;
    private final PaymentRecordsRepository paymentRecordsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> void saveRecord(T entity, Consumer<T> saver){
        saver.accept(entity);
    }
}
