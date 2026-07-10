package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.configurations.SecurityUtils;
import com.olamide.receipthandler.dto.*;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.exceptions.*;
import com.olamide.receipthandler.models.*;
import com.olamide.receipthandler.repository.*;
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
    private final ReceiptItemRepository receiptItemRepository;
    private final GeminiClient geminiClient;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );
    private static final String DEFAULT_CURRENCY = "NGN";

    public ReceiptServiceImpl(ReceiptRepository receiptRepository,
                              ReceiptItemRepository receiptItemRepository,
                              GeminiClient geminiClient) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.geminiClient = geminiClient;
    }

    @Override
    @Transactional
    public ReceiptResponseDTO processReceipt(MultipartFile file) {
        User currentUser = getCurrentUser();

        validateFile(file);
        byte[] fileBytes = readBytes(file);
        String mimeType = file.getContentType();

        GeminiAnalysisResult result = geminiClient.analyzeReceipt(fileBytes, mimeType);
        GeminiReceiptData receiptData = result.geminiReceiptData();

        if (!Boolean.TRUE.equals(receiptData.isReceipt())) {
            throw new GeminiParseException("The uploaded file does not appear to be a receipt.");
        }


        Receipt receipt = new Receipt(
                currentUser,
                receiptData.merchantName(),
                DEFAULT_CURRENCY,
                receiptData.totalAmount(),
                receiptData.receiptDate(),
                null,
                result.rawResponse()
        );
        Receipt saved = receiptRepository.save(receipt);


        List<ReceiptItem> items = new ArrayList<>();
        if (receiptData.items() != null && !receiptData.items().isEmpty()) {
            items = receiptData.items().stream()
                    .map(item -> {
                        Category category = Category.fromString(item.category());
                        return new ReceiptItem(saved, item.name(), item.amount(), category);
                    })
                    .collect(Collectors.toList());
            receiptItemRepository.saveAll(items);
        }

        return mapToDto(saved, items);
    }

    @Override
    public List<ReceiptResponseDTO> getAllReceipts(Category category) {
        User currentUser = getCurrentUser();

        List<Receipt> receipts = category != null
                ? receiptRepository.findByUserAndItemCategory(currentUser, category)
                : receiptRepository.findByUserOrderByCreatedAtDesc(currentUser);

        // For each receipt, load its items and map to DTO
        return receipts.stream()
                .map(receipt -> {
                    List<ReceiptItem> items = receiptItemRepository
                            .findByReceiptId(receipt.getId());
                    return mapToDto(receipt, items);
                })
                .collect(Collectors.toList());
    }

    @Override
    public SpendingSummary getSpendingSummary(YearMonth yearMonth) {
        User currentUser = getCurrentUser();

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Receipt> receiptsThisMonth = receiptRepository
                .findByUserAndDateBetween(currentUser, start, end);

        // Category totals now come from ReceiptItems, not Receipt directly
        // We load all items for all receipts this month, then group by category
        List<ReceiptItem> allItems = receiptsThisMonth.stream()
                .flatMap(r -> receiptItemRepository.findByReceiptId(r.getId()).stream())
                .toList();

        Map<Category, List<ReceiptItem>> groupedByCategory = allItems.stream()
                .collect(Collectors.groupingBy(ReceiptItem::getCategory));

        List<SpendingSummary.CategoryBreakdown> breakdown = groupedByCategory
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal total = entry.getValue().stream()
                            .map(ReceiptItem::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new SpendingSummary.CategoryBreakdown(
                            entry.getKey(), total, entry.getValue().size());
                })
                .toList();

        BigDecimal totalSpend = receiptsThisMonth.stream()
                .map(Receipt::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String period = yearMonth.getMonth().toString() + " " + yearMonth.getYear();

        // Receipts where Gemini couldn't extract a date — scoped to current user
        List<Receipt> receiptsWithNoDate = receiptRepository
                .findByUserAndDateIsNull(currentUser);
        BigDecimal unknownDateTotal = receiptsWithNoDate.stream()
                .map(Receipt::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int unknownDateCount = receiptsWithNoDate.size();

        return new SpendingSummary(totalSpend, DEFAULT_CURRENCY, period,
                breakdown, unknownDateTotal, unknownDateCount);
    }

    @Override
    public ReceiptResponseDTO getReceiptById(UUID id) {
        User currentUser = SecurityUtils.getAuthenticatedUser();

        Receipt receipt = receiptRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ReceiptNotFoundException("No receipt with this id"));

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(id);
        return mapToDto(receipt, items);
    }

    // mapToDto now takes both the receipt AND its items
    // because ReceiptResponseDTO needs to include the items list
    // and Receipt entity no longer has a category field
    private ReceiptResponseDTO mapToDto(Receipt receipt, List<ReceiptItem> items) {
        List<ReceiptItemDTO> itemDTOs = items.stream()
                .map(item -> new ReceiptItemDTO(
                        item.getId(),
                        item.getName(),
                        item.getAmount(),
                        item.getCategory()
                ))
                .collect(Collectors.toList());

        return new ReceiptResponseDTO(
                receipt.getId(),
                receipt.getMerchantName(),
                receipt.getTotalAmount(),
                receipt.getCurrency(),
                receipt.getDate(),
                receipt.getCreatedAt(),
                itemDTOs
        );
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileException("Could not read the uploaded file: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new InvalidFileException("Please upload a JPEG, PNG, WEBP, or PDF file.");
        }
    }
    private User getCurrentUser(){
        return SecurityUtils.getAuthenticatedUser();
    }
}