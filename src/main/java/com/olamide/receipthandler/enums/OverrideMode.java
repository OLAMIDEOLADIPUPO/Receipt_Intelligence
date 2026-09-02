package com.olamide.receipthandler.enums;

/**
 * Manual override for the staff self-upload window, sitting on top of the
 * default automatic 10th–15th-of-the-month schedule (see UploadWindowServiceImpl).
 */
public enum OverrideMode {
    /** Follow the default automatic schedule (10th–15th). */
    AUTO,
    /** Force the window open regardless of date — e.g. extend past the 15th. */
    FORCE_OPEN,
    /** Force the window closed regardless of date — e.g. close early or pause. */
    FORCE_CLOSED
}
