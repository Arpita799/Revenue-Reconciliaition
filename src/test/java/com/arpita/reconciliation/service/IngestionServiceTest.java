package com.arpita.reconciliation.service;

import com.arpita.reconciliation.dto.UploadResponse;
import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.entity.IngestionErrors;
import com.arpita.reconciliation.entity.PaymentRecords;
import com.arpita.reconciliation.exception.CsvParsingException;
import com.arpita.reconciliation.parser.BillingCsvParser;
import com.arpita.reconciliation.parser.PaymentCsvParser;
import com.arpita.reconciliation.repository.BillingRecordsRepository;
import com.arpita.reconciliation.repository.IngestionErrorsRepository;
import com.arpita.reconciliation.repository.PaymentRecordsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionServiceTest {

    @Mock
    private BillingRecordsRepository billingRecordsRepository;

    @Mock
    private PaymentRecordsRepository paymentRecordsRepository;

    @Mock
    private IngestionErrorsRepository ingestionErrorsRepository;

    @Mock
    private BillingCsvParser billingCsvParser;

    @Mock
    private PaymentCsvParser paymentCsvParser;

    @Mock
    private RecordPersistenceService recordPersistenceService;

    @InjectMocks
    private IngestionService ingestionService;

    private MockMultipartFile makeBillingFile(String content){
        return new MockMultipartFile("file","billing.csv","text/csv",content.getBytes());
    }

    private MockMultipartFile makePaymentFile(String content){
        return new MockMultipartFile("file","payment.csv","text/csv",content.getBytes());
    }


    @Test
    void processBillingFile_allValidRows_returnsCorrectCount(){
        String content = "accountId,recordDate,billedAmount,InvoiceId\n"+
                "ACC001,2026-01-15,150.00,INV-001\n" +
                "ACC002,2026-01-16,200.00,INV-002";

        when(billingCsvParser.parse(anyString(),anyString()))
                .thenReturn(new BillingRecords());

        UploadResponse response = ingestionService.processBillingFile(makeBillingFile(content));

        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(2);
        assertThat(response.failedCount()).isEqualTo(0);
    }

    @Test
    void processBillingFile_oneRowFails_countsFailuresAndContinues(){
        String content = "accountId,recordDate,billedAmount,InvoiceId\n"+
                "BAD_ROW\n" +
                "ACC002,2026-01-16,200.00,INV-002";

        when(billingCsvParser.parse(anyString(),anyString()))
                .thenThrow(new CsvParsingException("Invalid column count!"))
                .thenReturn(new BillingRecords());

        UploadResponse response = ingestionService.processBillingFile(makeBillingFile(content));

        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(1);
    }

    @Test
    void processBillingFile_failedRow_logsErrorToRepository(){
        String content = "accountId,recordDate,billedAmount,InvoiceId\n"+
                "BAD_ROW";

        when(billingCsvParser.parse(anyString(),anyString()))
                .thenThrow(new CsvParsingException("Invalid column count!"))
                .thenReturn(new BillingRecords());

        ingestionService.processBillingFile(makeBillingFile(content));

        ArgumentCaptor<IngestionErrors> captor = ArgumentCaptor.forClass(IngestionErrors.class);
        verify(ingestionErrorsRepository).save(captor.capture());

        IngestionErrors logged = captor.getValue();
        assertThat(logged.getErrorMessage()).contains("Invalid column count!");
        assertThat(logged.getRawLine()).isEqualTo("BAD_ROW");
        assertThat(logged.getRowNumber()).isEqualTo(2);
        assertThat(logged.getSourceFile()).isEqualTo("billing.csv");
    }

    @Test
    void processBillingFile_headerOnly_returnsZeroCount(){
        String content = "accountId,recordDate,billedAmount,InvoiceId";

        UploadResponse response = ingestionService.processBillingFile(makeBillingFile(content));

        assertThat(response.totalRows()).isZero();
        assertThat(response.successCount()).isZero();
        assertThat(response.failedCount()).isZero();
        verifyNoInteractions(billingCsvParser);
    }

    @Test
    void processPaymentFile_duplicateTransactionId_markedAsDuplicate(){
        String content = "accountId,recordDate,paidAmount,transactionId,referenceId\n" +
                "ACC001,2026-01-20,150.00,TXN-001,INV-001";

        PaymentRecords record = new PaymentRecords();
        record.setTransactionId("TXN-001");
        record.setDuplicate(false);

        when(paymentRecordsRepository.findAllTransactionIds())
                .thenReturn(List.of("TXN-001"));
        when(paymentCsvParser.parse(anyString(),anyString()))
                .thenReturn(record);

        doAnswer(invocation -> {
            Consumer<PaymentRecords> consumer = invocation.getArgument(1);
            consumer.accept(record);
            return null;
        }).when(recordPersistenceService).saveRecord(any(),any());

        UploadResponse response = ingestionService.processPaymentFile(makePaymentFile(content));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(0);
        assertThat(record.isDuplicate()).isTrue();
    }

    @Test
    void processBillingFile_allRowsFail_returnsAllAsFailed() {
        String content = "accountId,recordDate,billedAmount,invoiceId\n" +
                "BAD1\n" +
                "BAD2";

        when(billingCsvParser.parse(anyString(), anyString()))
                .thenThrow(new CsvParsingException("Invalid column count!"));

        UploadResponse response = ingestionService.processBillingFile(makeBillingFile(content));

        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.successCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(2);

        // Error should be logged twice
        verify(ingestionErrorsRepository, times(2)).save(any(IngestionErrors.class));
    }
}
