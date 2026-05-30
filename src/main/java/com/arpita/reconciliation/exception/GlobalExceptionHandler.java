package com.arpita.reconciliation.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //    400 — Validation / bad input errors thrown by controller or parsers
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ){
        return buildResponse(HttpStatus.BAD_REQUEST,ex.getMessage(),request.getRequestURI());
    }

    // 413 — Spring's own exception when multipart size is exceeded in config
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ){
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE,"File size exceeds maximum allowed limit",request.getRequestURI());
    }

    // 500 — Catch-all for unexpected exceptions (RuntimeException, etc.) - Generic
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ){
        log.error("Unexpected error at {}: {}",request.getRequestURI(),ex.getMessage(),ex); // The trailing `ex` argument tells Slf4j to print the full stack trace
        return  buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occured. Please try again later",request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ){
        return buildResponse(HttpStatus.CONFLICT,
                "Duplicate record detected - this invoice or transaction has already been ingested.",
                request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path
    ){
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
