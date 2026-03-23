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
@Table(name="payment_records")
@Data
@NoArgsConstructor
public class PaymentRecords {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @ManyToOne
    @JoinColumn(
            name = "reference_id",
            referencedColumnName = "invoice_id"
    )
    private BillingRecords billingRecords;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private BigDecimal paidAmount;

    private String sourceFile;

    private LocalDateTime createdAt;
}
