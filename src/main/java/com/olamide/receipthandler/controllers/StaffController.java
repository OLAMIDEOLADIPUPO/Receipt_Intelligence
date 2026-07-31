package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.models.Staff;
import com.olamide.receipthandler.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public ResponseEntity<List<Staff>> search(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(staffService.search(query));
    }

    // New: called when the typed name doesn't match an existing staff member.
    @PostMapping
    public ResponseEntity<Staff> create(@RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(staffService.findOrCreate(request.name()));
    }

    public record CreateStaffRequest(String name) {}
}