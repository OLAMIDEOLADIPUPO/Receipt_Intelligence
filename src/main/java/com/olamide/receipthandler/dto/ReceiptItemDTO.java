package com.olamide.receipthandler.dto;


import com.olamide.receipthandler.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "A single line item extracted from a receipt")
public record ReceiptItemDTO(
        @Schema(description = "Item ID") UUID id,
        @Schema(description = "Item name as printed on the receipt", example = "Milk 1L") String name,
        @Schema(description = "Item amount", example = "1500.00") BigDecimal amount,
        @Schema(description = "Spending category the item was classified into") Category category
) {}