package com.carsale.erp.shared.settings;

public final class BusinessProfile {

    private final String name;
    private final String logoStoredName;

    public BusinessProfile(String name, String logoStoredName) {
        this.name = name;
        this.logoStoredName = logoStoredName;
    }

    public static BusinessProfile empty() {
        return new BusinessProfile(null, null);
    }

    public String getName() {
        return name;
    }

    public String getLogoStoredName() {
        return logoStoredName;
    }

    public boolean isHasName() {
        return name != null;
    }

    public boolean isHasLogo() {
        return logoStoredName != null;
    }

    public boolean isHasAny() {
        return isHasName() || isHasLogo();
    }
}
