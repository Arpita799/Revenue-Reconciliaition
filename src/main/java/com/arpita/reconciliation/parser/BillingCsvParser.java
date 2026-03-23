package com.arpita.reconciliation.parser;

import com.arpita.reconciliation.entity.BillingRecords;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class BillingCsvParser {
    public BillingRecords parse(String line,String sourceFile) {
        if(line == null || line.trim().isEmpty()){
            throw new IllegalArgumentException("Empty line encountered!");
        }

        String[] fields = line.split(",");

        if(fields.length < 4){
            throw new IllegalArgumentException("Missing required fields!");
        }
        String accountId = fields[0].trim();
        String dateStr = fields[1].trim();
        String amountStr = fields[2].trim();
        String invoiceId = fields[3].trim();

        if(accountId.isEmpty()){
            throw new IllegalArgumentException("Account ID is missing!");
        }
        if(invoiceId.isEmpty()){
            throw new IllegalArgumentException("Invoice ID is missing!");
        }
         try{
             LocalDate recordDate = LocalDate.parse(dateStr);
             BigDecimal billedAmount = new BigDecimal(amountStr);
             BillingRecords record = new BillingRecords();
             record.setAccountId(accountId);
             record.setInvoiceId(invoiceId);
             record.setRecordDate(recordDate);
             record.setBilledAmount(billedAmount);
             record.setSourceFile(sourceFile);
             record.setCreatedAt(LocalDateTime.now());

             return record;
         }
         catch (DateTimeException e){
             throw new IllegalArgumentException("Invalid date format:" + dateStr,e);
         }
         catch (NumberFormatException e){
             throw new IllegalArgumentException("Invalid amount format:" + amountStr,e);
         }
    }
}
