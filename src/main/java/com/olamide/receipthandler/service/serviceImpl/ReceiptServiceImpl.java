package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.configurations.SecurityUtils;
import com.olamide.receipthandler.dto.*;
import com.olamide.receipthandler.enums.Category;
import com.olamide.receipthandler.exceptions.*;
import com.olamide.receipthandler.models.*;
import com.olamide.receipthandler.repository.*;
import com.olamide.receipthandler.service.ReceiptService;
import com.olamide.receipthandler.service.async.ReceiptAsyncProcessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
    private final ReceiptAsyncProcessor receiptAsyncProcessor;
    private final StaffRepository staffRepository;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );
    private static final String DEFAULT_CURRENCY = "NGN";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    public ReceiptServiceImpl(ReceiptRepository receiptRepository,
                              ReceiptItemRepository receiptItemRepository,
                              ReceiptAsyncProcessor receiptAsyncProcessor,
                              StaffRepository staffRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.receiptAsyncProcessor = receiptAsyncProcessor;
        this.staffRepository = staffRepository;
    }

    @Override
    public ReceiptResponseDTO processReceipt(MultipartFile file) {
        User currentUser = getCurrentUser();

        validateFile(file);
        byte[] fileBytes = readBytes(file);
        String mimeType = file.getContentType();

        Receipt placeholder = new Receipt(currentUser, DEFAULT_CURRENCY);
        Receipt saved = receiptRepository.save(placeholder);
        receiptAsyncProcessor.processReceiptAsync(saved.getId(), fileBytes, mimeType);

        return mapToDto(saved, List.of());
    }

    @Override
    public BatchUploadResponseDTO processBatch(UUID staffId, List<MultipartFile> files) {
        User currentUser = getCurrentUser();

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        if (files == null || files.isEmpty()) {
            throw new InvalidFileException("No files were uploaded.");
        }

        List<ReceiptResponseDTO> accepted = new ArrayList<>();
        List<BatchUploadResponseDTO.BatchFileError> rejected = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                validateFile(file);
                byte[] fileBytes = readBytes(file);
                String mimeType = file.getContentType();

                Receipt placeholder = new Receipt(currentUser, DEFAULT_CURRENCY, staff);
                Receipt saved = receiptRepository.save(placeholder);
                receiptAsyncProcessor.processReceiptAsync(saved.getId(), fileBytes, mimeType);

                accepted.add(mapToDto(saved, List.of()));
            } catch (InvalidFileException e) {
                String name = file != null ? file.getOriginalFilename() : "unknown";
                rejected.add(new BatchUploadResponseDTO.BatchFileError(name, e.getMessage()));
            }
        }

        return new BatchUploadResponseDTO(accepted, rejected);
    }

    @Override
    public PagedResponse<ReceiptResponseDTO> getAllReceipts(YearMonth month, int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = toPageable(page, size);

        Page<Receipt> receiptPage;
        if (month != null) {
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();
            receiptPage = receiptRepository.findByUserAndDateBetween(currentUser, start, end, pageable);
        } else {
            receiptPage = receiptRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
        }

        List<ReceiptResponseDTO> content = mapReceiptsToDto(receiptPage.getContent());
        return PagedResponse.of(receiptPage, content);
    }

    @Override
    public SpendingSummary getSpendingSummary(YearMonth yearMonth) {
        User currentUser = getCurrentUser();

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Receipt> receiptsThisMonth = receiptRepository
                .findByUserAndDateBetween(currentUser, start, end);

        List<UUID> receiptIds = receiptsThisMonth.stream()
                .map(Receipt::getId)
                .toList();

        List<ReceiptItem> allItems = receiptIds.isEmpty()
                ? List.of()
                : receiptItemRepository.findByReceiptIdIn(receiptIds);

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

        List<Receipt> receiptsWithNoDate = receiptRepository.findByUserAndDateIsNull(currentUser);
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
        User currentUser = getCurrentUser();

        Receipt receipt = receiptRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ReceiptNotFoundException("No receipt with this id"));

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(id);
        return mapToDto(receipt, items);
    }

    @Override
    public PagedResponse<ReceiptItemWithContextDTO> getItemsByCategory(Category category, int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = toPageable(page, size);

        Page<ReceiptItem> itemPage = receiptItemRepository.findByUserAndCategory(currentUser, category, pageable);

        List<ReceiptItemWithContextDTO> content = itemPage.getContent().stream()
                .map(item -> new ReceiptItemWithContextDTO(
                        item.getId(),
                        item.getName(),
                        item.getAmount(),
                        item.getCategory(),
                        item.getReceipt().getId(),
                        item.getReceipt().getMerchantName(),
                        item.getReceipt().getDate()
                ))
                .collect(Collectors.toList());

        return PagedResponse.of(itemPage, content);
    }

    private List<ReceiptResponseDTO> mapReceiptsToDto(List<Receipt> receipts) {
        List<UUID> receiptIds = receipts.stream().map(Receipt::getId).toList();

        List<ReceiptItem> allItems = receiptIds.isEmpty()
                ? List.of()
                : receiptItemRepository.findByReceiptIdIn(receiptIds);

        Map<UUID, List<ReceiptItem>> itemsByReceiptId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getReceipt().getId()));

        return receipts.stream()
                .map(receipt -> mapToDto(
                        receipt,
                        itemsByReceiptId.getOrDefault(receipt.getId(), List.of())
                ))
                .collect(Collectors.toList());
    }

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
                itemDTOs,
                receipt.getStatus(),
                receipt.getErrorMessage()
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

    // Turns raw page/size request params into a safe Pageable: negative pages
    // become 0, non-positive sizes fall back to the default, and oversized
    // requests are capped so a caller can't pull the whole table in one page.
    // No Sort is attached — ordering is owned by the repository queries.
    private Pageable toPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }

    private User getCurrentUser() {
        return SecurityUtils.getAuthenticatedUser();
    }
}