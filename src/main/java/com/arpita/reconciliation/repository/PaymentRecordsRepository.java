package com.arpita.reconciliation.repository;

import com.arpita.reconciliation.entity.PaymentRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRecordsRepository extends JpaRepository<PaymentRecords,Long> { }
