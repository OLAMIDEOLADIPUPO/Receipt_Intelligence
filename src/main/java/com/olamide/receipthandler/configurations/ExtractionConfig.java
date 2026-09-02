package com.olamide.receipthandler.configurations;

import com.olamide.receipthandler.service.serviceImpl.ClaudeClient;
import com.olamide.receipthandler.service.serviceImpl.GeminiClient;
import com.olamide.receipthandler.service.serviceImpl.GroqClient;
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
            @Value("${groq.api.key:}") String groqApiKey,
            GeminiClient geminiClient,
            ClaudeClient claudeClient,
            GroqClient groqClient) {

        return switch (provider.trim().toLowerCase()) {
            case "gemini" -> geminiClient;
            case "claude" -> {

                if (claudeApiKey == null || claudeApiKey.isBlank()) {
                    throw new IllegalStateException(
                            "receipt.extraction.provider=claude but claude.api.key is not set. "
                                    + "Provide CLAUDE_API_KEY to use the Claude provider.");
                }
                yield claudeClient;
            }
            case "groq" -> {
                if (groqApiKey == null || groqApiKey.isBlank()) {
                    throw new IllegalStateException(
                            "receipt.extraction.provider=groq but groq.api.key is not set. "
                                    + "Provide GROQ_API_KEY to use the Groq provider.");
                }
                yield groqClient;
            }
            default -> throw new IllegalStateException(
                    "Unknown receipt.extraction.provider: '" + provider
                            + "'. Expected 'gemini', 'claude', or 'groq'.");
        };
    }
}
