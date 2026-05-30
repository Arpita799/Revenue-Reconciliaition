package com.arpita.reconciliation.parser;

import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.exception.CsvParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class BillingCsvParserTest {
    private BillingCsvParser parser;

    @BeforeEach
    void setUp(){
        parser = new BillingCsvParser();
    }

    @Test
    void parse_validLine_returnsPopulatedRecord(){
        String line = "ACC001,2026-01-15,150.00,INV-001";
        String sourceFile = "billing_valid.csv";

        BillingRecords record = parser.parse(line,sourceFile);

        assertThat(record.getAccountId()).isEqualTo("ACC001");
        assertThat(record.getRecordDate().toString()).isEqualTo("2026-01-15");
        assertThat(record.getBilledAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(record.getInvoiceId()).isEqualTo("INV-001");
        assertThat(record.getSourceFile()).isEqualTo("billing_valid.csv");
    }

    @Test
    void parse_nullLine_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse(null,"test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Empty line");
    }


    @Test
    void parse_invalidColumnCount_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse("ACC001,2026-01-15,150.00","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Invalid column");
    }

    @Test
    void parse_emptyLine_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse(" ","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Empty line");
    }

    @Test
    void parse_missingAccountId_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse(",2026-01-15,150.00,INV-001","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Account ID is missing");
    }

    @Test
    void parse_missingInvoiceId_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse("ACC001,2026-01-15,150.00,","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Invoice ID is missing");
    }

    @Test
    void parse_invalidAmount_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse("ACC001,2026-01-15,abc,INV-001","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Invalid amount format");
    }

    @Test
    void parse_negativeAmount_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse("ACC001,2026-01-15,-12.00,INV-001","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Negative billing amount");
    }

    @Test
    void parse_invalidDate_throwsCsvParsingException(){

        assertThatThrownBy(() -> parser.parse("ACC001,15-01-2026,12.00,INV-001","test.csv"))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Invalid date format");
    }

}
