package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.dto.UploadWindowStatusDTO;
import com.olamide.receipthandler.enums.OverrideMode;
import com.olamide.receipthandler.models.UploadWindowSettings;
import com.olamide.receipthandler.repository.UploadWindowSettingsRepository;
import com.olamide.receipthandler.service.UploadWindowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UploadWindowServiceImpl implements UploadWindowService {

    // The staff self-upload window is open by default from the 10th through
    // the 15th (inclusive) of every month. Accounts can override this via
    // setOverride — see OverrideMode.
    private static final int AUTO_WINDOW_START_DAY = 10;
    private static final int AUTO_WINDOW_END_DAY = 15;

    private final UploadWindowSettingsRepository repository;

    public UploadWindowServiceImpl(UploadWindowSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public UploadWindowStatusDTO getStatus() {
        return toStatusDto(getOrCreateSettings());
    }

    @Override
    @Transactional
    public UploadWindowStatusDTO setOverride(OverrideMode mode) {
        UploadWindowSettings settings = getOrCreateSettings();
        settings.setOverrideMode(mode);
        repository.save(settings);
        return toStatusDto(settings);
    }

    @Override
    public boolean isOpenNow() {
        return isOpen(getOrCreateSettings().getOverrideMode());
    }

    private UploadWindowSettings getOrCreateSettings() {
        return repository.findById(UploadWindowSettings.SINGLETON_ID)
                .orElseGet(() -> repository.save(new UploadWindowSettings(OverrideMode.AUTO)));
    }

    private boolean isOpen(OverrideMode mode) {
        return switch (mode) {
            case FORCE_OPEN -> true;
            case FORCE_CLOSED -> false;
            case AUTO -> isWithinAutoWindow(LocalDate.now());
        };
    }

    private boolean isWithinAutoWindow(LocalDate date) {
        int day = date.getDayOfMonth();
        return day >= AUTO_WINDOW_START_DAY && day <= AUTO_WINDOW_END_DAY;
    }

    private UploadWindowStatusDTO toStatusDto(UploadWindowSettings settings) {
        return new UploadWindowStatusDTO(
                isOpen(settings.getOverrideMode()),
                settings.getOverrideMode(),
                AUTO_WINDOW_START_DAY,
                AUTO_WINDOW_END_DAY
        );
    }
}
