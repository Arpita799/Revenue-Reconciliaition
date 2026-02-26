package com.arpita.reconciliation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="ingestion_errors")
@Data
@RequiredArgsConstructor
public class IngestionErrors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceFile;

    @Column(nullable = false)
    private Integer rowNumber;

    @Column(nullable = false, length = 2000)
    private String rawLine;

    @Column(nullable = false, length = 1000)
    private String errorMessage;

    private LocalDateTime createdAt;

}
