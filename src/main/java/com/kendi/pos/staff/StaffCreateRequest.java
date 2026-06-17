package com.kendi.pos.staff;

public class StaffCreateRequest {
    private String name;
    private String pin;
    private String role;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}