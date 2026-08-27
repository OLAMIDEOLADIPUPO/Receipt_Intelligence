package com.olamide.receipthandler.repository;

import com.olamide.receipthandler.models.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    List<Staff> findByActiveTrueAndNameContainingIgnoreCase(String name);
    List<Staff> findByActiveTrue();
    Optional<Staff> findByActiveTrueAndNameIgnoreCase(String name);


    Optional<Staff> findByActiveTrueAndEmployeeIdIgnoreCase(String employeeId);
}