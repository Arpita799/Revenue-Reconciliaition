package com.arpita.reconciliation.service;

import com.arpita.reconciliation.dto.UploadResponse;
import com.arpita.reconciliation.entity.BillingRecords;
import com.arpita.reconciliation.entity.IngestionErrors;
import com.arpita.reconciliation.entity.PaymentRecords;
import com.arpita.reconciliation.enums.FileType;
import com.arpita.reconciliation.parser.BillingCsvParser;
import com.arpita.reconciliation.parser.PaymentCsvParser;
import com.arpita.reconciliation.repository.BillingRecordsRepository;
import com.arpita.reconciliation.repository.IngestionErrorsRepository;
import com.arpita.reconciliation.repository.PaymentRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;


@Service
@RequiredArgsConstructor
public class IngestionService {
    private final BillingRecordsRepository billingRecordsRepository;
    private final PaymentRecordsRepository paymentRecordsRepository;
    private final BillingCsvParser billingParser;
    private final PaymentCsvParser paymentParser;
    private final IngestionErrorsRepository ingestionErrorsRepository;

    public UploadResponse processBillingFile(MultipartFile file){
        return processFile(
                file,
                line->billingParser.parse(line,file.getOriginalFilename()),
                billingRecordsRepository::save,FileType.BILLING);
    }

    public UploadResponse processPaymentFile(MultipartFile file){
        Set<String> existingTransactionIds = new HashSet<>(
                paymentRecordsRepository.findAllTransactionIds()
        );
        return processFile(
                file,
                line->paymentParser.parse(line,file.getOriginalFilename()),
                record -> {
                    if(existingTransactionIds.contains(record.getTransactionId())){
                        record.setDuplicate(true);
                    }
                    paymentRecordsRepository.save(record);
                },FileType.PAYMENT);
    }

    private <T> UploadResponse processFile(
            MultipartFile file,
            Function<String,T> parser,
            Consumer<T> saver,
            FileType fileType){

        int total = 0;
        int success = 0;
        int failed = 0;

        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream())
        )){
            String line;
            int rowNumber = 0;

            while((line = reader.readLine()) != null){
                rowNumber++;

                if(rowNumber == 1)
                    continue;

                total++;

                try{
                    T entity = parser.apply(line);
                    saver.accept(entity);
                    success++;
                }
                catch (Exception e){
                    failed++;
                    logError(line,file.getOriginalFilename(),rowNumber,e.getClass().getSimpleName() + ": " + e.getMessage(),fileType);
                }
            }
        }
        catch (IOException e){
            throw new RuntimeException("File processing failed!",e);
        }

        return new UploadResponse(total,success,failed);
    }

    private void logError(
            String line,
            String sourceFile,
            int rowNumber,
            String errorMessage,
            FileType fileType
    ){
        IngestionErrors error = new IngestionErrors();
        error.setSourceFile(sourceFile);
        error.setRawLine(line);
        error.setRowNumber(rowNumber);
        error.setErrorMessage(errorMessage);
        error.setFileType(fileType);
        ingestionErrorsRepository.save(error);
    }
}
