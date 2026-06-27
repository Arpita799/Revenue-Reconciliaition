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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReconciliationServiceTest {

    @Mock
    private BillingRecordsRepository billingRecordsRepository;

    @Mock
    private PaymentRecordsRepository paymentRecordsRepository;

    @Mock
    private ReconciliationResultRepository reconciliationResultRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    // @InjectMocks creates the object without Spring, so @Value is never processed.
    // matchTolerance stays null, which would cause a NullPointerException.
    // ReflectionTestUtils.setField lets us inject the value manually.

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(reconciliationService,"matchTolerance",new BigDecimal("0.01"));
    }

    private BillingRecords buildBillingRecords(String invoiceId,String accountId,String amount){
        BillingRecords billingRecords = new BillingRecords();
        billingRecords.setInvoiceId(invoiceId);
        billingRecords.setAccountId(accountId);
        billingRecords.setBilledAmount(new BigDecimal(amount));
        billingRecords.setRecordDate(LocalDate.of(2026,1,15));
        billingRecords.setBillingStatus(BillingStatus.PENDING);
        return billingRecords;
    }

    private PaymentRecords buildPaymentRecords(String transactionId,String invoiceId, String amount){
        BillingRecords billingRecords = new BillingRecords();
        billingRecords.setInvoiceId(invoiceId);

        PaymentRecords paymentRecords = new PaymentRecords();
        paymentRecords.setTransactionId(transactionId);
        paymentRecords.setPaidAmount(new BigDecimal(amount));
        paymentRecords.setBillingRecords(billingRecords);
        paymentRecords.setDuplicate(false);
        return paymentRecords;
    }

    @Test
    void runReconciliation_noPendingBilling_returnsEmptyResponse(){
        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of());

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        assertThat(response.totalInvoices()).isZero();
        assertThat(response.matched()).isZero();
        assertThat(response.unpaid()).isZero();
        assertThat(response.totalBilled()).isEqualByComparingTo(BigDecimal.ZERO);

        verifyNoInteractions(paymentRecordsRepository);
        verifyNoInteractions(reconciliationResultRepository);
    }

    @Test
    void runReconciliation_exactPayment_producesMatchedStatus(){
        BillingRecords billing = buildBillingRecords("INV-001","ACC-001","100.00");
        PaymentRecords payment = buildPaymentRecords("TXN-001","INV-001","99.995");

        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of(billing));
        when(paymentRecordsRepository.findByBillingRecords_InvoiceIdIn(anyList()))
                .thenReturn(List.of(payment));
        when(reconciliationResultRepository.findByInvoiceId("INV-001"))
                .thenReturn(Optional.empty());

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        assertThat(response.matched()).isEqualTo(1);
        assertThat(response.unpaid()).isZero();
        assertThat(response.partial()).isZero();
        assertThat(response.overpaid()).isZero();
    }

    @Test
    void runReconciliation_partialPayment_producesPartialStatus(){
        BillingRecords billing = buildBillingRecords("INV-001","ACC-001","100.00");
        PaymentRecords payment = buildPaymentRecords("TXN-001","INV-001","90.00");

        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of(billing));
        when(paymentRecordsRepository.findByBillingRecords_InvoiceIdIn(anyList()))
                .thenReturn(List.of(payment));
        when(reconciliationResultRepository.findByInvoiceId("INV-001"))
                .thenReturn(Optional.empty());

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        assertThat(response.matched()).isZero();
        assertThat(response.unpaid()).isZero();
        assertThat(response.partial()).isEqualTo(1);
        assertThat(response.overpaid()).isZero();
    }

    @Test
    void runReconciliation_overPayment_producesOverPaidStatus(){
        BillingRecords billing = buildBillingRecords("INV-001","ACC-001","100.00");
        PaymentRecords payment = buildPaymentRecords("TXN-001","INV-001","190.00");

        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of(billing));
        when(paymentRecordsRepository.findByBillingRecords_InvoiceIdIn(anyList()))
                .thenReturn(List.of(payment));
        when(reconciliationResultRepository.findByInvoiceId("INV-001"))
                .thenReturn(Optional.empty());

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        assertThat(response.matched()).isZero();
        assertThat(response.unpaid()).isZero();
        assertThat(response.partial()).isZero();
        assertThat(response.overpaid()).isEqualTo(1);
    }

    @Test
    void runReconciliation_noPayment_producesUnPaidStatus(){
        BillingRecords billing = buildBillingRecords("INV-001","ACC-001","100.00");

        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of(billing));
        when(paymentRecordsRepository.findByBillingRecords_InvoiceIdIn(anyList()))
                .thenReturn(List.of());
        when(reconciliationResultRepository.findByInvoiceId(any()))
                .thenReturn(Optional.empty());

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        assertThat(response.matched()).isZero();
        assertThat(response.unpaid()).isEqualTo(1);
        assertThat(response.partial()).isZero();
        assertThat(response.overpaid()).isZero();
    }

    @Test
    void runReconciliation_duplicatePayment_doesNotEffectTotal(){
        BillingRecords billing = buildBillingRecords("INV-001","ACC-001","100.00");
        PaymentRecords payment = buildPaymentRecords("TXN-001","INV-001","100.00");
        payment.setDuplicate(false);
        PaymentRecords dupPayment = buildPaymentRecords("TXN-DUP","INV-001","100.00");
        dupPayment.setDuplicate(true);

        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of(billing));
        when(paymentRecordsRepository.findByBillingRecords_InvoiceIdIn(anyList()))
                .thenReturn(List.of(payment,dupPayment));
        when(reconciliationResultRepository.findByInvoiceId(any()))
                .thenReturn(Optional.empty());

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        assertThat(response.matched()).isEqualTo(1);
        assertThat(response.unpaid()).isZero();
        assertThat(response.partial()).isZero();
        assertThat(response.overpaid()).isZero();
    }

    @Test
    void runReconciliation_existingResult_updatesInsteadOfCreatingNewOne(){
        BillingRecords billing = buildBillingRecords("INV-001","ACC-001","100.00");
        PaymentRecords payment1 = buildPaymentRecords("TXN-001","INV-001","50.00");
        PaymentRecords payment2 = buildPaymentRecords("TXN-002","INV-001","50.00");


        ReconciliationResult existingReconciliationResult = new ReconciliationResult();
        existingReconciliationResult.setInvoiceId("INV-001");
        existingReconciliationResult.setStatus(ReconciliationStatus.PARTIAL);
        existingReconciliationResult.setPaidAmount(new BigDecimal("50.00"));
        existingReconciliationResult.setDifference(new BigDecimal("50.00"));

        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of(billing));
        when(paymentRecordsRepository.findByBillingRecords_InvoiceIdIn(anyList()))
                .thenReturn(List.of(payment1,payment2));
        when(reconciliationResultRepository.findByInvoiceId("INV-001"))
                .thenReturn(Optional.of(existingReconciliationResult));

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        verify(reconciliationResultRepository).save(captor.capture());
        ReconciliationResult capturedResult = captor.getValue();

        assertThat(capturedResult.getStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(capturedResult.getInvoiceId()).isEqualTo("INV-001");
        assertThat(capturedResult.getPaidAmount()).isEqualTo(new BigDecimal("100.00"));

        assertThat(response.matched()).isEqualTo(1);
        assertThat(response.unpaid()).isZero();
        assertThat(response.partial()).isZero();
        assertThat(response.overpaid()).isZero();
    }

    @Test
    void runReconciliation_multipleInvoices_summaryTotalsAreCorrect(){
        BillingRecords b1 = buildBillingRecords("INV-001","ACC-001","100.00");
        BillingRecords b2 = buildBillingRecords("INV-002","ACC-002","250.00");

        PaymentRecords p1 = buildPaymentRecords("TXN-001","INV-001","99.99");
        PaymentRecords p2 = buildPaymentRecords("TXN-002","INV-002","150.00");

        when(billingRecordsRepository.findByBillingStatusIn(anyList()))
                .thenReturn(List.of(b1,b2));
        when(paymentRecordsRepository.findByBillingRecords_InvoiceIdIn(anyList()))
                .thenReturn(List.of(p1,p2));
        when(reconciliationResultRepository.findByInvoiceId(anyString()))
                .thenReturn(Optional.empty());

        ReconciliationSummaryResponse response = reconciliationService.runReconciliation();

        assertThat(response.totalInvoices()).isEqualTo(2);
        assertThat(response.matched()).isEqualTo(1);
        assertThat(response.unpaid()).isZero();
        assertThat(response.partial()).isEqualTo(1);
        assertThat(response.overpaid()).isZero();
        assertThat(response.totalBilled()).isEqualTo(new BigDecimal("350.00"));
        assertThat(response.totalPaid()).isEqualTo(new BigDecimal("249.99"));
        assertThat(response.totalDifference()).isEqualTo(new BigDecimal("100.01"));
    }
}
