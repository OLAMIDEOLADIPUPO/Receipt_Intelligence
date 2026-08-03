package com.olamide.receipthandler.dto;

import java.util.List;

public record ClaudeResponse(
        List<ContentBlock> content,
        String stopReason
) {
    public record ContentBlock(
            String type,
            String text
    ) {}
}
