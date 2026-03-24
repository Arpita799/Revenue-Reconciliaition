package com.arpita.reconciliation.entity;


import com.arpita.reconciliation.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
//this table stores computed data so no need for mapping relations; for easier and faster execution
@Entity
@Table(name="reconciliation_result")
@Data
@NoArgsConstructor
public class ReconciliationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String invoiceId;

    private String transactionId;

    @Column(nullable = false)
    private BigDecimal billedAmount;

    @Column(nullable = false)
    private BigDecimal paidAmount;

    @Column(nullable = false)
    private BigDecimal difference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReconciliationStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
