package com.arpita.reconciliation.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    private LocalDate recordDate;

    private BigDecimal billedAmount;

    private BigDecimal paidAmount;

    private BigDecimal difference;

    private String status;

    private LocalDateTime createdAt;
}
