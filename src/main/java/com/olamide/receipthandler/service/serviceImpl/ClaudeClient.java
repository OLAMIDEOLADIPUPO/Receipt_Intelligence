package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.dto.ClaudeResponse;
import com.olamide.receipthandler.dto.ExtractedReceiptData;
import com.olamide.receipthandler.dto.ExtractionResult;
import com.olamide.receipthandler.exceptions.ExtractionServiceException;
import com.olamide.receipthandler.service.ExtractionJsonUtils;
import com.olamide.receipthandler.service.ReceiptExtractionPrompt;
import com.olamide.receipthandler.service.ReceiptExtractionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Claude implementation of {@link ReceiptExtractionService}, using Anthropic's
 * Messages API. Selected in production via {@code receipt.extraction.provider=claude}
 * (see {@code ExtractionConfig}).
 *
 * Unlike Gemini, Claude authenticates via a header (not a query param) and
 * distinguishes images from PDFs at the content-block level, so both are
 * handled explicitly here rather than passed through generically.
 */
@Component
public class ClaudeClient implements ReceiptExtractionService {

    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 1000L;
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    @Value("${claude.api.model:claude-sonnet-5}")
    private String model;

    @Value("${claude.api.max-tokens:1024}")
    private int maxTokens;

    public ClaudeClient(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public ExtractionResult extractReceiptData(byte[] fileBytes, String mimeType) {
        String base64File = Base64.getEncoder().encodeToString(fileBytes);
        Map<String, Object> requestBody = buildRequestBody(base64File, mimeType);
        ResponseEntity<ClaudeResponse> response = callClaude(requestBody);
        String rawText = extractText(response);
        ExtractedReceiptData extractedReceiptData = ExtractionJsonUtils.parse(objectMapper, rawText, "Claude");
        return new ExtractionResult(extractedReceiptData, rawText);
    }

    private ResponseEntity<ClaudeResponse> callClaude(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        long backoffMs = INITIAL_BACKOFF_MS;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            try {
                return restTemplate.postForEntity(apiUrl, request, ClaudeResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Claude rate limit exceeded after " + MAX_RETRIES + " retries");
                }
                sleep(backoffMs);
                backoffMs *= 2;

            } catch (HttpServerErrorException e) {
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Claude server error after " + MAX_RETRIES + " retries (HTTP " + e.getStatusCode() + ")");
                }
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (HttpClientErrorException e) {
                // Any other 4xx - bad request, invalid key, unsupported media type, etc. Not transient, don't retry.
                // Never surface e.getMessage()/getResponseBodyAsString(): this message is persisted to
                // Receipt.errorMessage and returned to the client, so it must not carry the provider's raw
                // error body or any request context. Status code alone is enough to diagnose.
                throw new ExtractionServiceException("Claude API rejected the request (HTTP " + e.getStatusCode() + ")");
            } catch (Exception e) {
                // Network-level failure (timeout, connection reset, etc.) - worth one retry.
                // Never include e.getMessage() here: for I/O errors it embeds the full request URI,
                // and this message is persisted and returned to the client.
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Claude API call failed after " + MAX_RETRIES + " retries");
                }
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
        throw new ExtractionServiceException("Claude API call failed after retries");
    }

    private String extractText(ResponseEntity<ClaudeResponse> response) {
        ClaudeResponse body = response.getBody();
        if (body == null) {
            throw new ExtractionServiceException("Claude returned empty response body");
        }
        // A truncated response yields invalid JSON. Catch it here with a clear message
        // instead of letting it fail downstream as a confusing "could not parse" error.
        if ("max_tokens".equals(body.stopReason())) {
            throw new ExtractionServiceException(
                    "Claude response was truncated at the token limit (stop_reason=max_tokens); "
                            + "increase claude.api.max-tokens (currently " + maxTokens + ")");
        }
        try {
            return body.content().stream()
                    .filter(block -> "text".equals(block.type()))
                    .findFirst()
                    .orElseThrow()
                    .text();
        } catch (Exception e) {
            throw new ExtractionServiceException("Unexpected Claude response shape " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(String base64File, String mimeType) {
        Map<String, Object> source = Map.of(
                "type", "base64",
                "media_type", mimeType,
                "data", base64File
        );
        Map<String, Object> filePart = Map.of(
                "type", isPdf(mimeType) ? "document" : "image",
                "source", source
        );

        Map<String, Object> textPart = Map.of(
                "type", "text",
                "text", ReceiptExtractionPrompt.TEXT
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(filePart, textPart)
        );

        return Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(userMessage)
        );
    }

    private boolean isPdf(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExtractionServiceException("Claude API call interrupted during retry backoff");
        }
    }
}
