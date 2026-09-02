package com.olamide.receipthandler.service;

import com.olamide.receipthandler.models.Staff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffService {
    List<Staff> search(String query);


    Staff findOrCreate(String name, String employeeId);

    Optional<Staff> findByEmployeeId(String employeeId);

    void deactivate(UUID staffId);
}