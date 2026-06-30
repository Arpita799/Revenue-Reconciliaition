package com.arpita.reconciliation.controller;

import com.arpita.reconciliation.dto.UploadResponse;
import com.arpita.reconciliation.exception.InvalidFileException;
import com.arpita.reconciliation.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {
    private final IngestionService ingestionService;

    @Value("${upload.max-file-size-bytes}")
    private long MAX_FILE_SIZE;

    @PostMapping("/billing")
    public ResponseEntity<UploadResponse> uploadBilling(@RequestParam("file") MultipartFile file){
        validateFile(file);
        log.info("Billing file upload started: {}",file.getOriginalFilename());
        UploadResponse response = ingestionService.processBillingFile(file);
        log.info("Billing file upload completed: {}",file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/payment")
    public ResponseEntity<UploadResponse> uploadPayment(@RequestParam("file") MultipartFile file){
        validateFile(file);
        log.info("Payment file upload started: {}",file.getOriginalFilename());
        UploadResponse response = ingestionService.processPaymentFile(file);
        log.info("Payment file upload completed: {}",file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    private void validateFile(MultipartFile file){
        if(file == null || file.isEmpty()){
            throw new InvalidFileException("File is missing or empty!");
        }

        if(file.getSize() > MAX_FILE_SIZE){
            throw new InvalidFileException("File size exceeds allowed limit!");
        }

        String filename = file.getOriginalFilename();

        if(!StringUtils.hasText(filename) || !filename.toLowerCase().endsWith(".csv")){
            throw new InvalidFileException("Only CSV files are allowed!");
        }

        String contentType = file.getContentType();
        final Set<String> ALLOWED_MIME_TYPES = Set.of(
                "text/csv",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/octet-stream",
                "text/plain"
        );
        if(contentType == null ||
                !ALLOWED_MIME_TYPES.contains(contentType)){
            throw new InvalidFileException("Unsupported file content type: " + contentType);
        }
    }
}
