package com.olamide.receipthandler.enums;


public enum Category {
    FOOD,
    TRANSPORT,
    SHOPPING,
    UTILITIES,
    ENTERTAINMENT,
    OTHER;

    public static Category fromString(String category) {
        if (category == null||category.isBlank()) {
            return Category.OTHER;
        }
        try {
            return Category.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
