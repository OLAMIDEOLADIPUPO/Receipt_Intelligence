package com.olamide.receipthandler.service;

import com.olamide.receipthandler.models.Staff;

import java.util.List;
import java.util.UUID;

public interface StaffService {
    List<Staff> search(String query);
    Staff findOrCreate(String name);
    void deactivate(UUID staffId);
}