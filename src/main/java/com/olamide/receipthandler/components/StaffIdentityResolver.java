package com.olamide.receipthandler.components;


import com.olamide.receipthandler.exceptions.UnknownEmployeeIdException;
import com.olamide.receipthandler.models.Staff;
import com.olamide.receipthandler.models.User;
import com.olamide.receipthandler.repository.UserRepository;
import com.olamide.receipthandler.service.StaffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StaffIdentityResolver {

    private static final Logger log = LoggerFactory.getLogger(StaffIdentityResolver.class);

    // Fixed the placeholder account every self-uploaded Receipt.user attaches to,
    // since that column is NOT NULL and there is no logged-in User in this
    // flow. Seeded once via a manual SQL insert — see seed_placeholder_user.sql.
    private static final String SYSTEM_USER_EMAIL = "staff-self-upload@system.local";

    private final StaffService staffService;
    private final UserRepository userRepository;

    public StaffIdentityResolver(StaffService staffService, UserRepository userRepository) {
        this.staffService = staffService;
        this.userRepository = userRepository;
    }


    public Staff resolveStaff(String firstName, String lastName, String employeeId) {
        Staff staff = staffService.findByEmployeeId(employeeId)
                .orElseThrow(() -> new UnknownEmployeeIdException(
                        "We don't recognize this employee ID. Please check it and try again, "
                                + "or contact Accounts if the problem continues."));

        String submittedName = ((firstName == null ? "" : firstName.trim())
                + " " + (lastName == null ? "" : lastName.trim())).trim();
        if (!submittedName.isBlank() && !namesRoughlyMatch(submittedName, staff.getName())) {
            log.warn("Self-upload name mismatch for employeeId={}: submitted='{}', roster='{}'",
                    employeeId, submittedName, staff.getName());
        }

        return staff;
    }

    // Placeholder system User every self-uploaded Receipt attaches to. See
    // class-level note — this is the seam that gets replaced once real
    // same-session identity (SSO/gateway-injected header) is available; at
    // that point this method (and only this method) changes to resolve a real
    // per-staff User instead of one fixed row.
    public User resolveSystemUser() {
        return userRepository.findByEmail(SYSTEM_USER_EMAIL)
                .orElseThrow(() -> new IllegalStateException(
                        "Placeholder self-upload system user not found. Run seed_placeholder_user.sql "
                                + "against this environment before using the self-upload endpoint."));
    }

    // Deliberately loose: case-insensitive, ignores extra whitespace. Not
    // trying to catch every typo — just distinguishing "close enough" from
    // "completely different person," since this is a soft warning, not a gate.
    private boolean namesRoughlyMatch(String submitted, String rosterName) {
        String a = submitted.replaceAll("\\s+", " ").trim().toLowerCase();
        String b = rosterName == null ? "" : rosterName.replaceAll("\\s+", " ").trim().toLowerCase();
        return a.equals(b);
    }
}