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

@Component
public class StuckReceiptReaperJob {

    private static final Logger log = LoggerFactory.getLogger(StuckReceiptReaperJob.class);

    private final ReceiptRepository receiptRepository;

    // A receipt normally reaches a terminal status in seconds. Anything still
    // PROCESSING after this long is treated as abandoned and failed. Configured
    // via app.jobs.stuck-receipt-reaper.stuck-after in application.yaml.
    private final Duration stuckAfter;

    public StuckReceiptReaperJob(ReceiptRepository receiptRepository,
                                 @Value("${app.jobs.stuck-receipt-reaper.stuck-after}") Duration stuckAfter) {
        this.receiptRepository = receiptRepository;
        this.stuckAfter = stuckAfter;
    }

    @Scheduled(fixedRateString = "${app.jobs.stuck-receipt-reaper.rate}")
    @Transactional
    public void reapStuckReceipts() {
        Instant cutoff = Instant.now().minus(stuckAfter);
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

        log.warn("Reaped {} receipt(s) stuck in PROCESSING for over {}", stuck.size(), stuckAfter);
    }
}
