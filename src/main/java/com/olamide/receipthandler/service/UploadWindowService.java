package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.UploadWindowStatusDTO;
import com.olamide.receipthandler.enums.OverrideMode;

public interface UploadWindowService {

    /** Current status — whether open now, the active override mode, and the default schedule. */
    UploadWindowStatusDTO getStatus();

    /** Accounts sets the override (AUTO / FORCE_OPEN / FORCE_CLOSED). Returns the resulting status. */
    UploadWindowStatusDTO setOverride(OverrideMode mode);

    /** Cheap check used by the self-upload flow to gate submissions. */
    boolean isOpenNow();
}
