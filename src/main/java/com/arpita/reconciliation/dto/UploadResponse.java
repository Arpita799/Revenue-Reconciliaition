package com.arpita.reconciliation.dto;

public record UploadResponse (
    int totalRows,
    int successCount,
    int failedCount
){}
