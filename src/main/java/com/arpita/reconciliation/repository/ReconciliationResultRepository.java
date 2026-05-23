package com.arpita.reconciliation.repository;

import com.arpita.reconciliation.entity.ReconciliationResult;

import com.arpita.reconciliation.enums.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult,Long> {
    Page<ReconciliationResult> findAll( Pageable pageable);
    Optional<ReconciliationResult> findByInvoiceId(String invoiceId);
    Page<ReconciliationResult> findByStatus(ReconciliationStatus status, Pageable pageable);
    Page<ReconciliationResult> findByAccountId(String accountId, Pageable pageable);
    Page<ReconciliationResult> findByStatusAndAccountId(ReconciliationStatus status,String accountId, Pageable pageable);

}
