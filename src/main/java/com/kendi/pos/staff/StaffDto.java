package com.kendi.pos.staff;

public class StaffDto {
    private String id;
    private String name;
    private String role;
    private boolean active;
    private long createdAt;

    public static StaffDto from(Staff s) {
        StaffDto dto = new StaffDto();
        dto.id = s.getId();
        dto.name = s.getName();
        dto.role = s.getRole();
        dto.active = s.isActive();
        dto.createdAt = s.getCreatedAt();
        return dto;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
}