package com.olamide.receipthandler.dto;

import java.util.List;

public record GeminiResponse(
        List<Candidate> candidates
) {
    public record Candidate(
            Content content
    ){
        public record Content(
                List<Parts> parts
        ){
            public record Parts(
                    String text
            ){}
        }
    }
}
