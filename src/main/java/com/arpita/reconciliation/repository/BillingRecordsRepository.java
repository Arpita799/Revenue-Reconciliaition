package com.arpita.reconciliation.repository;

import com.arpita.reconciliation.entity.BillingRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingRecordsRepository extends JpaRepository<BillingRecords,Long> { }
