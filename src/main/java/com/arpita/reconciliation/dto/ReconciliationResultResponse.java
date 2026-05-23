package com.arpita.reconciliation.dto;

import com.arpita.reconciliation.entity.ReconciliationResult;
import com.arpita.reconciliation.enums.ReconciliationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReconciliationResultResponse(
        Long id,
        String invoiceId,
        String accountId,
        String transactionId,
        BigDecimal billedAmount,
        BigDecimal paidAmount,
        BigDecimal difference,
        ReconciliationStatus status,
        LocalDate billingDate,
        String notes,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime reconciledAt
) {
    public static ReconciliationResultResponse from(ReconciliationResult entity){
        return new ReconciliationResultResponse(
                entity.getId(),
                entity.getInvoiceId(),
                entity.getAccountId(),
                entity.getTransactionId(),
                entity.getBilledAmount(),
                entity.getPaidAmount(),
                entity.getDifference(),
                entity.getStatus(),
                entity.getBillingDate(),
                entity.getNotes(),
                entity.getReconciledAt()
        );
    }
}
