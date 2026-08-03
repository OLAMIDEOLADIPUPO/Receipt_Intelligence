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
            GeminiClient geminiClient,
            ClaudeClient claudeClient) {

        return switch (provider.trim().toLowerCase()) {
            case "gemini" -> geminiClient;
            case "claude" -> claudeClient;
            default -> throw new IllegalStateException(
                    "Unknown receipt.extraction.provider: '" + provider
                            + "'. Expected 'gemini' or 'claude'.");
        };
    }
}
