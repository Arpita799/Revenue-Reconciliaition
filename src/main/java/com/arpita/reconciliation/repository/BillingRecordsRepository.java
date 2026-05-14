package com.arpita.reconciliation.repository;

import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.enums.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRecordsRepository extends JpaRepository<BillingRecords,Long> {

    BillingRecords findByInvoiceId(String id);
    List<BillingRecords> findByBillingStatusIn(List<BillingStatus> statuses);
}
