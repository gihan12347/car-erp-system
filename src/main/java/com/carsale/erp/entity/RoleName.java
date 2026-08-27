package com.carsale.erp.entity;

public enum RoleName {
    ROLE_USER("User"),
    ROLE_ADMIN("Admin"),
    ROLE_SUPER_ADMIN("Super Admin");

    private final String displayName;

    RoleName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
