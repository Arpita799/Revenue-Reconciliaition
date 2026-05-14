package com.arpita.reconciliation.dto;

import java.math.BigDecimal;
import java.math.BigInteger;

public record ReconciliationSummaryResponse(
        int totalInvoices,
        int matched,
        int partial,
        int overpaid,
        int unpaid,
        BigDecimal totalBilled,
        BigDecimal totalPaid,
        BigDecimal totalDifference
) {
}
