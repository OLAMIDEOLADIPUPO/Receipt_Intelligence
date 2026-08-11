package com.olamide.receipthandler.enums;

public enum Category {
    VEHICLE_MAINTENANCE,
    TRANSPORTATION,
    INTERNET_DATA_BUNDLE,
    LAUNDRY_OF_OFFICE_WEAR,
    HEALTH_AND_WELLNESS,
    DIESEL,
    ENTERTAINMENT_MARKETING,
    STAFF_TRAINING,
    OTHER;

    public static Category fromString(String category) {
        if (category == null || category.isBlank()) {
            return Category.OTHER;
        }
        String normalized = category.trim().toUpperCase().replaceAll("[\\s\\-]+", "_");

        try {
            return Category.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}