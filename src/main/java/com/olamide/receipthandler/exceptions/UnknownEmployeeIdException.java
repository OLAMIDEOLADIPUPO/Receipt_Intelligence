package com.olamide.receipthandler.exceptions;

public class UnknownEmployeeIdException extends RuntimeException {
    public UnknownEmployeeIdException(String message) {
        super(message);
    }
}
