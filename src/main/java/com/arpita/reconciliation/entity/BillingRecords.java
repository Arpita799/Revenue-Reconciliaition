package com.arpita.reconciliation.entity;

import com.arpita.reconciliation.enums.BillingStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name="invoice_id", nullable = false, unique = true)
    private String invoiceId;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private BigDecimal billedAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BillingStatus billingStatus = BillingStatus.PENDING;

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
