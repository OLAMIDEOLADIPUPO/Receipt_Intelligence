package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.dto.ExcelExportResult;
import com.olamide.receipthandler.enums.ProcessingStatus;
import com.olamide.receipthandler.exceptions.ExcelExportException;
import com.olamide.receipthandler.models.Receipt;
import com.olamide.receipthandler.models.ReceiptItem;
import com.olamide.receipthandler.repository.ReceiptItemRepository;
import com.olamide.receipthandler.repository.ReceiptRepository;
import com.olamide.receipthandler.service.ReceiptExcelExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReceiptExcelExportServiceImpl implements ReceiptExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] HEADERS = {
            "Staff Name", "Date", "Vendor", "Category", "Item", "Amount"
    };

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;

    public ReceiptExcelExportServiceImpl(ReceiptRepository receiptRepository,
                                         ReceiptItemRepository receiptItemRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
    }

    @Override
    public ExcelExportResult exportMonth(YearMonth month) {
        // Company-wide export — Accounts' report covers every staff member's
        // completed receipts, including ones uploaded via the public
        // self-upload flow (which attach to the placeholder system user).
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<Receipt> receipts = receiptRepository.findForExport(
                ProcessingStatus.COMPLETED, start, end);

        List<UUID> receiptIds = receipts.stream().map(Receipt::getId).toList();
        Map<UUID, List<ReceiptItem>> itemsByReceipt = receiptIds.isEmpty()
                ? Map.of()
                : receiptItemRepository.findByReceiptIdIn(receiptIds).stream()
                .collect(Collectors.groupingBy(i -> i.getReceipt().getId()));

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = workbook.createSheet(month.toString());
            writeHeader(workbook, sheet);

            int rowIdx = 1;
            for (Receipt receipt : receipts) {
                List<ReceiptItem> items = itemsByReceipt.getOrDefault(receipt.getId(), List.of());
                if (items.isEmpty()) {
                    rowIdx = writeRow(sheet, rowIdx, receipt, null);
                } else {
                    for (ReceiptItem item : items) {
                        rowIdx = writeRow(sheet, rowIdx, receipt, item);
                    }
                }
            }

            for (int col = 0; col < HEADERS.length; col++) {
                sheet.trackColumnForAutoSizing(col);
                sheet.autoSizeColumn(col);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();

            String filename = "receipts-" + month + ".xlsx";
            return new ExcelExportResult(out.toByteArray(), filename);
        } catch (IOException e) {
            throw new ExcelExportException("Failed to generate Excel export: " + e.getMessage());
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private int writeRow(Sheet sheet, int rowIdx, Receipt receipt, ReceiptItem item) {
        Row row = sheet.createRow(rowIdx);

        row.createCell(0).setCellValue(
                receipt.getStaff() != null ? receipt.getStaff().getName() : "Unassigned");
        row.createCell(1).setCellValue(
                receipt.getDate() != null ? receipt.getDate().format(DATE_FMT) : "");
        row.createCell(2).setCellValue(
                receipt.getMerchantName() != null ? receipt.getMerchantName() : "");

        if (item != null) {
            row.createCell(3).setCellValue(item.getCategory() != null ? item.getCategory().name() : "");
            row.createCell(4).setCellValue(item.getName() != null ? item.getName() : "");
            if (item.getAmount() != null) {
                row.createCell(5).setCellValue(item.getAmount().doubleValue());
            }
        } else {
            // Defensive fallback — a COMPLETED receipt should always have at
            // least one item (the async processor guarantees this), but a
            // receipt with zero items still needs a row, not to vanish silently.
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("");
            if (receipt.getTotalAmount() != null) {
                row.createCell(5).setCellValue(receipt.getTotalAmount().doubleValue());
            }
        }

        return rowIdx + 1;
    }
}