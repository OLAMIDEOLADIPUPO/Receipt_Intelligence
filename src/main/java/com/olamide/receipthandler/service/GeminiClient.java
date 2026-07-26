package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.GeminiAnalysisResult;
import com.olamide.receipthandler.dto.GeminiReceiptData;
import com.olamide.receipthandler.dto.GeminiResponse;
import com.olamide.receipthandler.exceptions.GeminiParseException;
import com.olamide.receipthandler.exceptions.GeminiServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;


import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {
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

    public GeminiAnalysisResult analyzeReceipt(byte[] fileBytes, String mimeType) {
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        Map<String, Object> requestBody = buildRequestBody( base64Image, mimeType);
        ResponseEntity<GeminiResponse>response =callGemini(requestBody);
        String rawText = extractText(response);
        String cleanJson = cleanJson(rawText);
        GeminiReceiptData geminiReceiptData = parseJson(cleanJson);
        return new GeminiAnalysisResult(geminiReceiptData, rawText);



    }

    private ResponseEntity<GeminiResponse> callGemini(Map<String,Object> requestBody){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Object>>request = new HttpEntity<>(requestBody, headers);
        String urlWithKey = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", apiKey)
                .toUriString();
        long backoffMs = INITIAL_BACKOFF_MS;
        for(int attempt = 0; attempt<=MAX_RETRIES; attempt++){

            try{
                return restTemplate.postForEntity(urlWithKey, request, GeminiResponse.class);
            }
            catch(HttpClientErrorException.TooManyRequests e){
                if (attempt == MAX_RETRIES){
                    throw new GeminiServiceException ("Gemini rate limit exceeded after " + MAX_RETRIES + " retries: " + e.getMessage());
                }
                sleep(backoffMs);
                backoffMs *=2;


            }
            catch(HttpServerErrorException e ){
                if (attempt == MAX_RETRIES){
                    throw new GeminiParseException("Gemini server error after " + MAX_RETRIES + " retries: " + e.getMessage());
                }
                sleep(backoffMs);
                backoffMs *=2;
            }
            catch (HttpClientErrorException e) {
                // Any other 4xx - bad request, invalid key, etc. Not transient, don't retry.
                throw new GeminiServiceException("Gemini API rejected the request: " + e.getMessage());
            }
            catch (Exception e) {
                // Network-level failure (timeout, connection reset, etc.) - worth one retry
                if (attempt == MAX_RETRIES) {
                    throw new GeminiServiceException("Gemini API call failed: " + e.getMessage());
                }
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
        throw new GeminiServiceException("Gemini API call failed after retries");
    }

    private String extractText(ResponseEntity<GeminiResponse> response){
        GeminiResponse body = response.getBody();
        if(body == null){
            throw new GeminiServiceException("Gemini returned empty response body");
        }
        try{

           return response.getBody()
                   .candidates().getFirst()
                   .content().parts().getFirst()
                   .text();


        }

        catch(Exception e){
            throw new GeminiServiceException("Unexpected Gemini response shape" + e.getMessage());
        }
    }

    private String cleanJson(String rawText){
        return rawText.replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private GeminiReceiptData parseJson(String rawText){
        try{
            return objectMapper.readValue(rawText, GeminiReceiptData.class);
        }
        catch (Exception e){
            throw new GeminiParseException("Could not parse Gemini response:" + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(String base64Image, String mimeType) {
        Map<String, Object> textPart = Map.of(
                "text", buildPrompt()
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
    private void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiParseException("Retry Interrupted");
        }
    }
    private String buildPrompt() {
        return """
        You are a receipt parser. Extract data from this receipt image and return
        ONLY a valid JSON object. Do not include any explanation, commentary, or
        markdown formatting. Do not wrap the JSON in backticks. Return nothing
        except the raw JSON object itself.

        Use exactly these keys:

        isReceipt: boolean. true if this image or document is a genuine
        purchase receipt, invoice, or proof of payment. false if it is
        anything else — a random document, a screenshot of something
        unrelated, a blank page, a photo of a person, text unrelated to
        a transaction, etc.
        If isReceipt is false, set all other fields to null and items to [].

        merchantName: string. The name of the business on the receipt.
        Use null if it cannot be read.

        totalAmount: number. The final total amount paid, written as a plain
        number with no currency symbol, no commas, and no text.
        Example: 4500.00, not "4,500.00" or "₦4,500".

        receiptDate: string in YYYY-MM-DD format. The date printed on the receipt.
        Use null if no date is visible. Do not guess or invent a date.

        items: array of objects. Each object represents one line item on the receipt.
        Each object must have exactly these keys:
          name: string. The item or service name as printed on the receipt.
          amount: number or null if the individual item price cannot be read.
          category: string. Choose exactly one from:
                    FOOD, TRANSPORT, SHOPPING, UTILITIES, ENTERTAINMENT, OTHER

        If you cannot identify individual line items, return a single item object
        using the merchant name as the name, the totalAmount as the amount,
        and the most appropriate category.

        Return the JSON object now, with no other text before or after it.
        """;

    }

    }

