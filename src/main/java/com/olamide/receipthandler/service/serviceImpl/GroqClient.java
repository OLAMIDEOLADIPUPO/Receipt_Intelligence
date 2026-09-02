package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.dto.ExtractedReceiptData;
import com.olamide.receipthandler.dto.ExtractionResult;
import com.olamide.receipthandler.dto.GroqResponse;
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
 * Groq implementation of {@link ReceiptExtractionService}, using Groq's
 * OpenAI-compatible chat completions API. Selected via
 * {@code receipt.extraction.provider=groq} (see {@code ExtractionConfig}).
 * Intended as a free-tier stand-in for testing the extraction pipeline
 * when Gemini/Claude aren't available — same shared prompt and JSON
 * parsing as every other provider.
 *
 * Groq's vision models take images only (no PDF document type like
 * Claude's API), so PDF uploads are rejected here rather than sent as
 * unsupported input.
 */
@Component
public class GroqClient implements ReceiptExtractionService {

    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 1000L;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.api.model:qwen/qwen3.6-27b}")
    private String model;

    @Value("${groq.api.max-tokens:1024}")
    private int maxTokens;

    public GroqClient(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public ExtractionResult extractReceiptData(byte[] fileBytes, String mimeType) {
        if (isPdf(mimeType)) {
            throw new ExtractionServiceException(
                    "Groq's vision models don't accept PDF documents. Upload a JPEG, PNG, or WEBP image instead.");
        }
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        Map<String, Object> requestBody = buildRequestBody(base64Image, mimeType);
        ResponseEntity<GroqResponse> response = callGroq(requestBody);
        String rawText = extractText(response);
        ExtractedReceiptData extractedReceiptData = ExtractionJsonUtils.parse(objectMapper, rawText, "Groq");
        return new ExtractionResult(extractedReceiptData, rawText);
    }

    private ResponseEntity<GroqResponse> callGroq(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        long backoffMs = INITIAL_BACKOFF_MS;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            try {
                return restTemplate.postForEntity(apiUrl, request, GroqResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Groq rate limit exceeded after " + MAX_RETRIES + " retries");
                }
                sleep(backoffMs);
                backoffMs *= 2;

            } catch (HttpServerErrorException e) {
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Groq server error after " + MAX_RETRIES + " retries (HTTP " + e.getStatusCode() + ")");
                }
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (HttpClientErrorException e) {

                throw new ExtractionServiceException("Groq API rejected the request (HTTP " + e.getStatusCode() + ")");
            } catch (Exception e) {

                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Groq API call failed after " + MAX_RETRIES + " retries");
                }
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
        throw new ExtractionServiceException("Groq API call failed after retries");
    }

    private String extractText(ResponseEntity<GroqResponse> response) {
        GroqResponse body = response.getBody();
        if (body == null) {
            throw new ExtractionServiceException("Groq returned empty response body");
        }
        try {
            return body.choices().getFirst().message().content();
        } catch (Exception e) {
            throw new ExtractionServiceException("Unexpected Groq response shape " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(String base64Image, String mimeType) {
        Map<String, Object> imageUrl = Map.of(
                "url", "data:" + mimeType + ";base64," + base64Image
        );
        Map<String, Object> imagePart = Map.of(
                "type", "image_url",
                "image_url", imageUrl
        );
        Map<String, Object> textPart = Map.of(
                "type", "text",
                "text", ReceiptExtractionPrompt.TEXT
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(imagePart, textPart)
        );

        return Map.of(
                "model", model,
                "max_completion_tokens", maxTokens,
                "reasoning_effort", "none",
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
            throw new ExtractionServiceException("Groq API call interrupted during retry backoff");
        }
    }
}
