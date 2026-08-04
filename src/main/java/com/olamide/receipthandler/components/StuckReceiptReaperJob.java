package com.olamide.receipthandler.components;

import com.olamide.receipthandler.enums.ProcessingStatus;
import com.olamide.receipthandler.models.Receipt;
import com.olamide.receipthandler.repository.ReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Recovers receipts that get stuck in PROCESSING — e.g. the app crashed
 * mid-extraction, or the async worker died before writing a terminal status.
 * Without this, such a row sits in PROCESSING forever and the uploader has no
 * way to know it will never finish.
 *
 * The file bytes aren't persisted (they only live in memory for the duration of
 * the async call), so a genuine retry isn't possible from here — the honest
 * recovery is to mark the row FAILED with a clear message so the row unsticks
 * and the user knows to re-upload.
 */
@Component
public class StuckReceiptReaperJob {

    private static final Logger log = LoggerFactory.getLogger(StuckReceiptReaperJob.class);

    private final ReceiptRepository receiptRepository;

    // A receipt normally reaches a terminal status in seconds. Anything still
    // PROCESSING after this long is considered abandoned. Configurable so ops
    // can tune it without a recompile; default 10 minutes.
    private final long stuckAfterMinutes;

    public StuckReceiptReaperJob(ReceiptRepository receiptRepository,
                                 @Value("${receipt.processing.stuck-after-minutes:10}") long stuckAfterMinutes) {
        this.receiptRepository = receiptRepository;
        this.stuckAfterMinutes = stuckAfterMinutes;
    }

    @Scheduled(fixedRateString = "${receipt.processing.reaper-rate-ms:120000}") // every 2 min
    @Transactional
    public void reapStuckReceipts() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(stuckAfterMinutes));
        List<Receipt> stuck = receiptRepository.findStuckInStatus(ProcessingStatus.PROCESSING, cutoff);

        if (stuck.isEmpty()) {
            return;
        }

        for (Receipt receipt : stuck) {
            receipt.setStatus(ProcessingStatus.FAILED);
            receipt.setErrorMessage(
                    "Processing was interrupted and did not finish. Please upload this receipt again.");
        }
        receiptRepository.saveAll(stuck);

        log.warn("Reaped {} receipt(s) stuck in PROCESSING for over {} minutes",
                stuck.size(), stuckAfterMinutes);
    }
}
