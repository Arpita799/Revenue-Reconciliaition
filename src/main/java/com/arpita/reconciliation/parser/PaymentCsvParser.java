package com.arpita.reconciliation.parser;

import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.entity.PaymentRecords;
import com.arpita.reconciliation.repository.BillingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentCsvParser {

    private final BillingRecordsRepository billingRecordsRepository;

    public PaymentRecords parse(String line, String sourceFile) {
        if(line == null || line.trim().isEmpty()){
            throw new IllegalArgumentException("Empty line encountered!");
        }

        String[] fields = line.split(",");

        if(fields.length != 5){
            throw new IllegalArgumentException("Invalid column count!");
        }
        String accountId = fields[0].trim();
        String dateStr = fields[1].trim();
        String amountStr = fields[2].trim();
        String transactionId = fields[3].trim();
        String referenceId = fields[4].trim();
        BigDecimal paidAmount = new BigDecimal(amountStr.trim());
        if(accountId.isEmpty()){
            throw new IllegalArgumentException("Account ID is missing!");
        }
        if(transactionId.isEmpty()){
            throw new IllegalArgumentException("Transaction ID is missing!");
        }
        if(referenceId.isEmpty()){
            throw new IllegalArgumentException("Reference ID is missing!");
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative payment not supported yet");
        }
        try{
            LocalDate recordDate = LocalDate.parse(dateStr);
            PaymentRecords record = new PaymentRecords();
            record.setAccountId(accountId);
            record.setRecordDate(recordDate);
            record.setPaidAmount(paidAmount);
            record.setTransactionId(transactionId);
            BillingRecords billing = billingRecordsRepository.findByInvoiceId(referenceId);
            if (billing == null) {
                throw new IllegalArgumentException("Invoice not found: " + referenceId);
            }
            record.setBillingRecords(billing);
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
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
