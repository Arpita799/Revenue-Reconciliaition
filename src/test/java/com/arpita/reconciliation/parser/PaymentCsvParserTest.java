package com.arpita.reconciliation.parser;

import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.entity.PaymentRecords;
import com.arpita.reconciliation.exception.CsvParsingException;
import com.arpita.reconciliation.repository.BillingRecordsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentCsvParserTest {

    @Mock
    private BillingRecordsRepository billingRecordsRepository;

    @InjectMocks
    private PaymentCsvParser paymentCsvParser;

    private BillingRecords fakeBillings;

    @BeforeEach
    void setUp(){
        fakeBillings = new BillingRecords();
        fakeBillings.setInvoiceId("INV-001");
        fakeBillings.setAccountId("ACC001");
    }

    @Test
    void parse_validLine_returnsPopulatedRecord(){

        when(billingRecordsRepository.findByInvoiceId("INV-001")).thenReturn(fakeBillings);

        String line = "ACC001,2026-01-20,150.00,TXN-001,INV-001";
        PaymentRecords result = paymentCsvParser.parse(line,"payment.csv");

        assertThat(result.getAccountId()).isEqualTo("ACC001");
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getRecordDate().toString()).isEqualTo("2026-01-20");
        assertThat(result.getPaidAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getBillingRecords().getInvoiceId()).isEqualTo("INV-001");
    }

    @Test
    void parse_negativePayment_throwsCsvParsingException(){

        assertThatThrownBy(() -> paymentCsvParser.parse("ACC001,2026-01-20,-150.00,TXN-001,INV-001","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Negative payment not supported");
    }

    @Test
    void parse_invoiceNotFound_throwsCsvParsingException(){

        when(billingRecordsRepository.findByInvoiceId("INV-GHOST")).thenReturn(null);

        assertThatThrownBy(() -> paymentCsvParser.parse("ACC001,2026-01-20,150.00,TXN-001,INV-GHOST","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Invoice not found");
    }

    @Test
    void parse_invalidAmount_throwsCsvParsingException(){

        assertThatThrownBy(() -> paymentCsvParser.parse("ACC001,2026-01-20,abc,TXN-001,INV-001","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Invalid amount format");
    }

    @Test
    void parse_invalidDate_throwsCsvParsingException(){

        assertThatThrownBy(() -> paymentCsvParser.parse("ACC001,15-01-2026,12.00,TXN-001,INV-001","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Invalid date format");
    }
}
