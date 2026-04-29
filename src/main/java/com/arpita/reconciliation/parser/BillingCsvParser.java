package com.arpita.reconciliation.parser;

import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.exception.CsvParsingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class BillingCsvParser {
    public BillingRecords parse(String line,String sourceFile) {
        if(line == null || line.trim().isEmpty()){
            throw new CsvParsingException("Empty line encountered!");
        }

        String[] fields = line.split(",");

        if(fields.length != 4){
            throw new CsvParsingException("Invalid column count!");
        }
        String accountId = fields[0].trim();
        String dateStr = fields[1].trim();
        String amountStr = fields[2].trim();
        String invoiceId = fields[3].trim();
        if(accountId.isEmpty()){
            throw new CsvParsingException("Account ID is missing!");
        }
        if(invoiceId.isEmpty()){
            throw new CsvParsingException("Invoice ID is missing!");
        }
         try{
             BigDecimal billedAmount = new BigDecimal(amountStr.trim());
             if (billedAmount.compareTo(BigDecimal.ZERO) < 0) {
                 throw new CsvParsingException("Negative billing amount not allowed");
             }
             LocalDate recordDate = LocalDate.parse(dateStr);
             BillingRecords record = new BillingRecords();
             record.setAccountId(accountId);
             record.setInvoiceId(invoiceId);
             record.setRecordDate(recordDate);
             record.setBilledAmount(billedAmount);
             record.setSourceFile(sourceFile);
             record.setCreatedAt(LocalDateTime.now());

             return record;
         }
         catch (NumberFormatException e){
             throw new CsvParsingException("Invalid amount format:" + amountStr);
         }
         catch (DateTimeException e){
             throw new CsvParsingException("Invalid date format:" + dateStr);
         }
    }
}
