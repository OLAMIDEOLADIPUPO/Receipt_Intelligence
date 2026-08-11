package com.olamide.receipthandler.models;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;


    @Column(nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false)
    private boolean active = true;

    protected Staff() {}


    public Staff(String name) {
        this.name = name;
    }

    public Staff(String name, String employeeId) {
        this.name = name;
        this.employeeId = employeeId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}