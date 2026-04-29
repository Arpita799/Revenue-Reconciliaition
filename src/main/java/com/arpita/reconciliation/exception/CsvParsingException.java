package com.arpita.reconciliation.exception;

public class CsvParsingException extends IllegalArgumentException{

    public CsvParsingException(String message){
        super(message);
    }
}
