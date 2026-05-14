package com.arpita.reconciliation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
            referencedColumnName = "invoice_id",
            nullable = false
    )
    private BillingRecords billingRecords;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(name = "is_duplicate",nullable = false)
    private boolean duplicate = false;

    @Column(nullable = false)
    private BigDecimal paidAmount;

    @Column(length = 3)
    private String currency = "INR";

    private String sourceFile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
}
