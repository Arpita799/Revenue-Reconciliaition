package com.arpita.reconciliation.repository;

import com.arpita.reconciliation.entity.PaymentRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRecordsRepository extends JpaRepository<PaymentRecords,Long> {
    List<PaymentRecords> findByBillingRecords_InvoiceIdIn(List<String> invoiceIds);
    boolean existsByTransactionId(String transactionId);
}
