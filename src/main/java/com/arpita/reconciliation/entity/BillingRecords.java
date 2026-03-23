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
@Table(name="billing_records")
@Data
@NoArgsConstructor
public class BillingRecords {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false, unique = true)
    private String invoiceId;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private BigDecimal billedAmount;

    private String sourceFile;

    private LocalDateTime createdAt;

}
