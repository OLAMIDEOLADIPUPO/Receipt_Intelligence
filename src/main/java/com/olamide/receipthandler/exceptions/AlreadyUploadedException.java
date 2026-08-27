package com.olamide.receipthandler.exceptions;

/**
 * Thrown when a staff member tries to submit receipts for a month
 * they have already submitted for.
 */
public class AlreadyUploadedException extends RuntimeException {
    public AlreadyUploadedException(String message) {
        super(message);
    }
}
