package com.kendi.pos.staff;

/**
 * DTO qe permban krejt fushat e nevojshme per offline login cache.
 * KUJDES: Perdoret vetem per Tauri app offline authentication.
 * Ne nje sistem ma te sigurte, ky endpoint duhet me qene protected me admin token.
 */
public class StaffCacheDto {
    private String id;
    private String name;
    private String role;
    private String pinHash;
    private boolean active;
    private long createdAt;

    public static StaffCacheDto from(Staff s) {
        StaffCacheDto dto = new StaffCacheDto();
        dto.id = s.getId();
        dto.name = s.getName();
        dto.role = s.getRole();
        dto.pinHash = s.getPinHash();
        dto.active = s.isActive();
        dto.createdAt = s.getCreatedAt();
        return dto;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getPinHash() { return pinHash; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
}