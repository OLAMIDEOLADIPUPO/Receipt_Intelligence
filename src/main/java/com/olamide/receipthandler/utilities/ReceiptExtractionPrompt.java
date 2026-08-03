package com.olamide.receipthandler.service;

/**
 * Single source of truth for the receipt-parsing instructions sent to
 * whichever AI provider is active. Every {@code ReceiptExtractionService}
 * implementation should use this exact prompt — the 8 fixed categories
 * here must stay in sync with {@code com.olamide.receipthandler.enums.Category}.
 * If you add/rename a category, update both places.
 */
public final class ReceiptExtractionPrompt {

    private ReceiptExtractionPrompt() {}

    public static final String TEXT = """
    You are a receipt parser. Extract data from this receipt image or document and return
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
      category: string. Choose exactly one of the following tokens, spelled
                exactly as shown below (all caps, underscores, no spaces,
                no slashes, no hyphens):
                VEHICLE_MAINTENANCE, TRANSPORTATION, INTERNET_DATA_BUNDLE,
                LAUNDRY_OF_OFFICE_WEAR, HEALTH_AND_WELLNESS, DIESEL,
                ENTERTAINMENT_MARKETING, STAFF_TRAINING, OTHER
                Do not invent new categories and do not reformat these tokens
                (e.g. do not write "Vehicle Maintenance" or "vehicle-maintenance").
                If nothing fits, use OTHER.

    If you cannot identify individual line items, return a single item object
    using the merchant name as the name, the totalAmount as the amount,
    and the most appropriate category.

    Return the JSON object now, with no other text before or after it.
    """;
}
