package com.olamide.receipthandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeResponse(
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason
) {
    public record ContentBlock(
            String type,
            String text
    ) {}
}
