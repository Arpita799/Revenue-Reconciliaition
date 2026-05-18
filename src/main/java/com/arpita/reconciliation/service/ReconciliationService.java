package com.arpita.reconciliation.service;

import com.arpita.reconciliation.dto.ReconciliationSummaryResponse;
import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.entity.PaymentRecords;
import com.arpita.reconciliation.entity.ReconciliationResult;
import com.arpita.reconciliation.enums.BillingStatus;
import com.arpita.reconciliation.enums.ReconciliationStatus;
import com.arpita.reconciliation.repository.BillingRecordsRepository;
import com.arpita.reconciliation.repository.PaymentRecordsRepository;
import com.arpita.reconciliation.repository.ReconciliationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReconciliationService {
    private final BillingRecordsRepository billingRecordsRepository;
    private final PaymentRecordsRepository paymentRecordsRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;

    @Value("${reconciliation.match.tolerance:0.01}")
    private BigDecimal matchTolerance;

    @Transactional
    public ReconciliationSummaryResponse runReconciliation(){
        log.info("Reconciliation started");
        List<BillingRecords> pendingBilling = billingRecordsRepository.findByBillingStatusIn(
                List.of(BillingStatus.PENDING,BillingStatus.PARTIAL)
        );
        if(pendingBilling.isEmpty()){
            log.info("No pending records found, reconciliation skipped");
            return emptyResponse();
        }

        List<String> invoiceIds = pendingBilling.stream()
                .map(BillingRecords::getInvoiceId)
                .toList();

        List<PaymentRecords> relevantPayments = paymentRecordsRepository
                .findByBillingRecords_InvoiceIdIn(invoiceIds);

        Map<String,List<PaymentRecords>> paymentsByInvoice = relevantPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getBillingRecords().getInvoiceId()
                ));

        int matched = 0;
        int unpaid = 0;
        int partial = 0;
        int overpaid = 0;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalBilled = BigDecimal.ZERO;

        for(BillingRecords billing:pendingBilling) {
            String invoiceId = billing.getInvoiceId();
            BigDecimal billedAmount = billing.getBilledAmount();
            totalBilled = totalBilled.add(billedAmount);

            List<PaymentRecords> payments = paymentsByInvoice.getOrDefault(invoiceId, List.of());
            BigDecimal paidAmount = payments.stream()
                    .filter(p -> !p.isDuplicate())
                    .map(PaymentRecords::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalPaid = totalPaid.add(paidAmount);

            BigDecimal difference = billedAmount.subtract(paidAmount);

            ReconciliationStatus status;
            BillingStatus billingStatus;
            String notes;

            if (payments.isEmpty()) {
                status = ReconciliationStatus.UNPAID;
                billingStatus = BillingStatus.PENDING;
                notes = "No payment found for this invoice.";
            } else if (difference.compareTo(matchTolerance) == 0) {
                status = ReconciliationStatus.MATCHED;
                billingStatus = BillingStatus.PAID;
                notes = "Fully Paid";
            } else if (difference.compareTo(matchTolerance) < 0) {
                status = ReconciliationStatus.OVERPAID;
                billingStatus = BillingStatus.OVERPAID;
                notes = "Overpaid by " + difference.abs();
            } else {
                status = ReconciliationStatus.PARTIAL;
                billingStatus = BillingStatus.PARTIAL;
                notes = "Underpaid by " + difference;
            }

            switch (status) {
                case MATCHED -> matched++;
                case PARTIAL -> partial++;
                case OVERPAID -> overpaid++;
                case UNPAID -> unpaid++;
            }

            String transactionId = payments.stream()
                    .reduce((first, second) -> second)
                    .map(PaymentRecords::getTransactionId)
                    .orElse(null);

            Optional<ReconciliationResult> existing = reconciliationResultRepository.findByInvoiceId(invoiceId);
            ReconciliationResult result = existing.orElse(new ReconciliationResult());

            result.setAccountId(billing.getAccountId());
            result.setInvoiceId(invoiceId);
            result.setTransactionId(transactionId);
            result.setBilledAmount(billedAmount);
            result.setPaidAmount(paidAmount);
            result.setDifference(difference);
            result.setStatus(status);
            result.setBillingDate(billing.getRecordDate());
            result.setNotes(notes);
            reconciliationResultRepository.save(result);

            billing.setBillingStatus(billingStatus);
            billingRecordsRepository.save(billing);
        }

        BigDecimal totalDifference = totalBilled.subtract(totalPaid);
        log.info("Reconciliation Complete: {} invoices processed",pendingBilling.size());

        return new ReconciliationSummaryResponse(
                pendingBilling.size(),
                matched, partial, overpaid, unpaid,
                totalBilled, totalPaid, totalDifference
        );
    }

    private ReconciliationSummaryResponse emptyResponse(){
        return new ReconciliationSummaryResponse(0,0,0,0,0,
                BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);
    }

    public Page<ReconciliationResult> getResults(int page, int size){
        return reconciliationResultRepository.findAll(
                PageRequest.of(page,size, Sort.by("reconciledAt").descending())
        );
    }
}
