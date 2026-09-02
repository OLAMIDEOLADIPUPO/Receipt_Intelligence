package com.olamide.receipthandler.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExtractionParseException.class)
    public ResponseEntity<ErrorResponse> handleExtractionParseException(ExtractionParseException ex) {
        ErrorResponse error = new ErrorResponse(
                "COULD_NOT_READ_RECEIPT",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(ExtractionServiceException.class)
    public ResponseEntity<ErrorResponse> handleExtractionServiceDown(ExtractionServiceException ex) {
        ErrorResponse error = new ErrorResponse(
                "RECEIPT_READING_SERVICE_UNAVAILABLE",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileException(InvalidFileException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_FILE_FORMAT",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ReceiptNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReceiptNotFoundException(ReceiptNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                "RECEIPT_NOT_FOUND",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParseException(DateTimeParseException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_DATE_FORMAT",
                "Invalid month format. Please use yyyy-MM, e.g. 2026-06.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse(
                "EMAIL_ALREADY_EXISTS",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
                "UNAUTHORIZED",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_FAILED",
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_TOKEN",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    @ExceptionHandler(ExcelExportException.class)
    public ResponseEntity<ErrorResponse> handleExcelExportException(ExcelExportException ex) {
        ErrorResponse error = new ErrorResponse(
                "EXCEL_EXPORT_FAILED",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    @ExceptionHandler(UnknownEmployeeIdException.class)
    public ResponseEntity<ErrorResponse> handleUnknownEmployeeId(UnknownEmployeeIdException e) {
        ErrorResponse error = new ErrorResponse(
                "UNKNOWN_EMPLOYEE_ID",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AlreadyUploadedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyUploaded(AlreadyUploadedException e) {
        ErrorResponse error = new ErrorResponse(
                "ALREADY_UPLOADED_THIS_MONTH",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UploadWindowClosedException.class)
    public ResponseEntity<ErrorResponse> handleUploadWindowClosed(UploadWindowClosedException e) {
        ErrorResponse error = new ErrorResponse(
                "UPLOAD_WINDOW_CLOSED",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyLoginAttempts(TooManyLoginAttemptsException e) {
        ErrorResponse error = new ErrorResponse(
                "TOO_MANY_LOGIN_ATTEMPTS",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

}