package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.models.Staff;
import com.olamide.receipthandler.repository.StaffRepository;
import com.olamide.receipthandler.service.StaffService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;

    public StaffServiceImpl(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    public List<Staff> search(String query) {
        if (query == null || query.isBlank()) {
            return staffRepository.findByActiveTrue();
        }
        return staffRepository.findByActiveTrueAndNameContainingIgnoreCase(query.trim());
    }

    // Idempotent by employeeId, not name — employeeId is the real roster key
    // (admin-assigned, unique); two staff can share a name but never an ID.
    // If the ID already exists, the existing record wins as-is: we don't
    // overwrite the stored name from a later call, since a mismatched name at
    // this point is more likely a typo in the new call than a correction.
    @Override
    public Staff findOrCreate(String name, String employeeId) {
        String trimmedName = name.trim();
        String trimmedId = employeeId.trim();
        return staffRepository.findByActiveTrueAndEmployeeIdIgnoreCase(trimmedId)
                .orElseGet(() -> staffRepository.save(new Staff(trimmedName, trimmedId)));
    }

    // Used by the public self-upload flow (via StaffIdentityResolver) to
    // validate a submitted employeeId against the roster. Returns empty if
    // unrecognized — the caller decides how to surface that.
    @Override
    public Optional<Staff> findByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return Optional.empty();
        }
        return staffRepository.findByActiveTrueAndEmployeeIdIgnoreCase(employeeId.trim());
    }

    @Override
    public void deactivate(UUID staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        staff.setActive(false);
        staffRepository.save(staff);
    }
}