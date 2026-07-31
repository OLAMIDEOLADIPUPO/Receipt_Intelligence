package com.olamide.receipthandler.dto;

public record ExcelExportResult(byte[] content, String filename) {}