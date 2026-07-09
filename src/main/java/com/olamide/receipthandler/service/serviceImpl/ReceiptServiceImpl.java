package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.dto.GeminiAnalysisResult;
import com.olamide.receipthandler.dto.GeminiReceiptData;
import com.olamide.receipthandler.dto.ReceiptResponseDTO;
import com.olamide.receipthandler.dto.SpendingSummary;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.exceptions.GeminiParseException;
import com.olamide.receipthandler.exceptions.InvalidFileException;
import com.olamide.receipthandler.exceptions.ReceiptNotFoundException;
import com.olamide.receipthandler.models.Receipt;
import com.olamide.receipthandler.repository.ReceiptRepository;
import com.olamide.receipthandler.service.GeminiClient;
import com.olamide.receipthandler.service.ReceiptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final GeminiClient geminiClient;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp","application/pdf"
    );
    private static final String DEFAULT_CURRENCY = "NGN";
    public ReceiptServiceImpl(ReceiptRepository receiptRepository, GeminiClient geminiClient) {
        this.receiptRepository = receiptRepository;
        this.geminiClient = geminiClient;
    }

    @Override
    @Transactional
    public ReceiptResponseDTO processReceipt(MultipartFile file) {
        validateFile(file);
        byte[] fileBytes = readBytes(file);
        String mimeType  = file.getContentType();
        GeminiAnalysisResult result =  geminiClient.analyzeReceipt(fileBytes,mimeType);
        GeminiReceiptData receiptData = result.geminiReceiptData();
        if (!Boolean.TRUE.equals(receiptData.isReceipt())) {
            throw new GeminiParseException("The uploaded file does not appear to be a receipt.");
        }
        Category category = Category.fromString(receiptData.category());

        Receipt newReceipt = new Receipt(
                receiptData.merchantName(),
                DEFAULT_CURRENCY,
                receiptData.totalAmount(),
                category,
                receiptData.receiptDate(),
                null,
                result.rawResponse()
        );
        Receipt saved = receiptRepository.save(newReceipt);
        return mapToDto(saved);

    }

    @Override
    public List<ReceiptResponseDTO> getAllReceipts(Category category) {
        List<Receipt> receipts = category != null
                ? receiptRepository.findByCategoryOrderByCreatedAtDesc(category)
                : receiptRepository.findAllByOrderByCreatedAtDesc();
        return receipts.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public SpendingSummary getSpendingSummary(YearMonth yearMonth) {

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Receipt> receiptsThisMonth = receiptRepository.findByDateBetween(start,end);
        Map<Category, List<Receipt>>groupedByCategory  = receiptsThisMonth.stream()
                .collect(Collectors.groupingBy(Receipt::getCategory));

        List<SpendingSummary.CategoryBreakdown> breakdown = groupedByCategory.entrySet()
                .stream().map(
                        entry -> {
                            Category category = entry.getKey();
                            List<Receipt> categoryReceipts = entry.getValue();
                            BigDecimal totalAmount = categoryReceipts.stream()
                                    .map(Receipt::getTotalAmount).filter(Objects::nonNull)
                                     .reduce(BigDecimal.ZERO, BigDecimal::add);
                            int count = categoryReceipts.size();
                            return new SpendingSummary.CategoryBreakdown(category,totalAmount,count);
                        }
                ).toList();
        BigDecimal totalSpend = receiptsThisMonth.stream()
                .map(Receipt::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String period = yearMonth.getMonth().toString() + " " + yearMonth.getYear();

        List<Receipt>receiptsWithNoDate = receiptRepository.findByDateIsNull();
        BigDecimal unknownDateTotal = receiptsWithNoDate.stream().map(Receipt::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        int unknownDateCount = receiptsWithNoDate.size();



        return new SpendingSummary(totalSpend,DEFAULT_CURRENCY,period,breakdown,unknownDateTotal,unknownDateCount);


    }

    @Override
    public ReceiptResponseDTO getReceiptById(UUID id) {
        return receiptRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ReceiptNotFoundException("No receipt with this id"));
    }

    private ReceiptResponseDTO mapToDto(Receipt receipt) {
        return new ReceiptResponseDTO(
                receipt.getId(),
                receipt.getMerchantName(),
                receipt.getTotalAmount(),
                receipt.getCurrency(),
                receipt.getCategory(),
                receipt.getDate(),
                receipt.getCreatedAt()
        );
    }
    private byte[] readBytes(MultipartFile file) {
        try{
            return file.getBytes();
        }
        catch (IOException e) {
        throw new InvalidFileException("Could not read the uploaded file: " + e.getMessage());
    }

    }
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new InvalidFileException("Please upload a JPEG, PNG, or WEBP image of a receipt.");
        }
    }




}
