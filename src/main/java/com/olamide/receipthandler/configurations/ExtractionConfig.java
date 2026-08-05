package com.olamide.receipthandler.configurations;

import com.olamide.receipthandler.service.serviceImpl.ClaudeClient;
import com.olamide.receipthandler.service.serviceImpl.GeminiClient;
import com.olamide.receipthandler.service.ReceiptExtractionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class ExtractionConfig {

    @Bean
    @Primary
    public ReceiptExtractionService receiptExtractionService(
            @Value("${receipt.extraction.provider:gemini}") String provider,
            @Value("${claude.api.key:}") String claudeApiKey,
            GeminiClient geminiClient,
            ClaudeClient claudeClient) {

        return switch (provider.trim().toLowerCase()) {
            case "gemini" -> geminiClient;
            case "claude" -> {
                // claude.api.key defaults to empty, so a missing key would otherwise
                // slip through and only surface as an HTTP 401 on the first upload.
                // Fail fast at startup instead.
                if (claudeApiKey == null || claudeApiKey.isBlank()) {
                    throw new IllegalStateException(
                            "receipt.extraction.provider=claude but claude.api.key is not set. "
                                    + "Provide CLAUDE_API_KEY to use the Claude provider.");
                }
                yield claudeClient;
            }
            default -> throw new IllegalStateException(
                    "Unknown receipt.extraction.provider: '" + provider
                            + "'. Expected 'gemini' or 'claude'.");
        };
    }
}
