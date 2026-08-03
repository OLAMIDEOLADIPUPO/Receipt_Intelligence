package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.dto.ExtractedReceiptData;
import com.olamide.receipthandler.dto.ExtractionResult;
import com.olamide.receipthandler.dto.GeminiResponse;
import com.olamide.receipthandler.exceptions.ExtractionParseException;
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

@Component
public class GeminiClient implements ReceiptExtractionService {
    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 1000L;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiClient(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public ExtractionResult extractReceiptData(byte[] fileBytes, String mimeType) {
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        Map<String, Object> requestBody = buildRequestBody(base64Image, mimeType);
        ResponseEntity<GeminiResponse> response = callGemini(requestBody);
        String rawText = extractText(response);
        ExtractedReceiptData extractedReceiptData = ExtractionJsonUtils.parse(objectMapper, rawText, "Gemini");
        return new ExtractionResult(extractedReceiptData, rawText);
    }

    private ResponseEntity<GeminiResponse> callGemini(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Send the key as a header, not a query param, so it can never leak into a
        // request URI that might surface in an exception message or a log line.
        headers.set("x-goog-api-key", apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        long backoffMs = INITIAL_BACKOFF_MS;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            try {
                return restTemplate.postForEntity(apiUrl, request, GeminiResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Gemini rate limit exceeded after " + MAX_RETRIES + " retries");
                }
                sleep(backoffMs);
                backoffMs *= 2;

            } catch (HttpServerErrorException e) {
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Gemini server error after " + MAX_RETRIES + " retries (HTTP " + e.getStatusCode() + ")");
                }
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (HttpClientErrorException e) {
                // Any other 4xx - bad request, invalid key, etc. Not transient, don't retry.
                throw new ExtractionServiceException("Gemini API rejected the request (HTTP " + e.getStatusCode() + ")");
            } catch (Exception e) {
                // Network-level failure (timeout, connection reset, etc.) - worth one retry.
                // Never include e.getMessage() here: for I/O errors it embeds the full
                // request URI, and this message is persisted and returned to the client.
                if (attempt == MAX_RETRIES) {
                    throw new ExtractionServiceException("Gemini API call failed after " + MAX_RETRIES + " retries");
                }
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
        throw new ExtractionServiceException("Gemini API call failed after retries");
    }

    private String extractText(ResponseEntity<GeminiResponse> response) {
        GeminiResponse body = response.getBody();
        if (body == null) {
            throw new ExtractionServiceException("Gemini returned empty response body");
        }
        try {
            return body.candidates().getFirst()
                    .content().parts().getFirst()
                    .text();
        } catch (Exception e) {
            throw new ExtractionServiceException("Unexpected Gemini response shape" + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(String base64Image, String mimeType) {
        Map<String, Object> textPart = Map.of(
                "text", ReceiptExtractionPrompt.TEXT
        );

        Map<String, Object> imagePart = Map.of(
                "inlineData", Map.of(
                        "mimeType", mimeType,
                        "data", base64Image
                )
        );

        Map<String, Object> content = Map.of(
                "parts", List.of(imagePart, textPart)
        );

        return Map.of(
                "contents", List.of(content)
        );
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExtractionParseException("Retry Interrupted");
        }
    }
}