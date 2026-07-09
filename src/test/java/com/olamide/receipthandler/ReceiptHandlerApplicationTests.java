package com.olamide.receipthandler;

import com.olamide.receipthandler.dto.GeminiAnalysisResult;
import com.olamide.receipthandler.service.GeminiClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
class ReceiptHandlerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Nested
    @SpringBootTest
    class GeminiClientTest {

        @Autowired
        private GeminiClient geminiClient;

        @Test
        void testAnalyzeReceipt() throws Exception {
            byte[] imageBytes = Files.readAllBytes(
                    Path.of("C:/Users/USER/Downloads/Gemini_Generated_Image_dr34ppdr34ppdr34.png")
            );
            GeminiAnalysisResult result = geminiClient.analyzeReceipt(imageBytes, "image/jpeg");
            System.out.println(result.geminiReceiptData());
            System.out.println(result.rawResponse());
        }
    }

}
