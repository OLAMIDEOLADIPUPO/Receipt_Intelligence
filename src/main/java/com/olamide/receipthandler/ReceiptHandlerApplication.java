package com.olamide.receipthandler;

import com.olamide.receipthandler.dto.GeminiAnalysisResult;
import com.olamide.receipthandler.service.GeminiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class ReceiptHandlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceiptHandlerApplication.class, args);
    }

}
