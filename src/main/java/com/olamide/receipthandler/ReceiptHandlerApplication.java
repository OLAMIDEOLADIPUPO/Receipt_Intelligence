package com.olamide.receipthandler;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class ReceiptHandlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceiptHandlerApplication.class, args);
    }

}
