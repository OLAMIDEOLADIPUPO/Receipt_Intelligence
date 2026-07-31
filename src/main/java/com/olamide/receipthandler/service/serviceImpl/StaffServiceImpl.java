package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.models.Staff;
import com.olamide.receipthandler.repository.StaffRepository;
import com.olamide.receipthandler.service.StaffService;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Override
    public Staff findOrCreate(String name) {
        String trimmed = name.trim();
        return staffRepository.findByActiveTrueAndNameIgnoreCase(trimmed)
                .orElseGet(() -> staffRepository.save(new Staff(trimmed)));
    }

    @Override
    public void deactivate(UUID staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        staff.setActive(false);
        staffRepository.save(staff);
    }
}