package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.ExcelExportResult;

import java.time.YearMonth;

public interface ReceiptExcelExportService {
    ExcelExportResult exportMonth(YearMonth month);
}