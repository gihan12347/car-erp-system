package com.carsale.erp.shared.settings;

import java.util.Locale;

import com.carsale.erp.shared.pipeline.FlowPipeline;

/**
 * Which intake pipelines a workspace offers.
 * Registered keeps local purchase only. Unregistered keeps the import pipelines.
 * Both keeps every option.
 */
public enum BusinessType {
    REGISTERED("registered"),
    UNREGISTERED("unregistered"),
    BOTH("both");

    public static final String REGISTERED_ONLY_MESSAGE =
            "This business uses only the local purchase pipeline.";
    public static final String UNREGISTERED_ONLY_MESSAGE =
            "This business uses the unregistered vehicle pipelines.";

    private final String code;

    BusinessType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isRegistered() {
        return this == REGISTERED;
    }

    public boolean isUnregistered() {
        return this == UNREGISTERED;
    }

    public boolean isBoth() {
        return this == BOTH;
    }

    public boolean isLocalPurchaseEnabled() {
        return this == REGISTERED || this == BOTH;
    }

    public boolean isUnregisteredPipelinesEnabled() {
        return this == UNREGISTERED || this == BOTH;
    }

    public boolean isFlowEnabled(String flowKey) {
        if (flowKey == null || isUnregisteredPipelinesEnabled()) {
            return true;
        }
        return !FlowPipeline.IMPORT.getFlowKey().equalsIgnoreCase(flowKey)
                && !FlowPipeline.CUSTOMS.getFlowKey().equalsIgnoreCase(flowKey);
    }

    public static BusinessType parse(String raw) {
        BusinessType type = fromStored(raw);
        if (type == null) {
            throw new IllegalArgumentException("Choose a business type.");
        }
        return type;
    }

    public static BusinessType fromStored(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        for (BusinessType type : values()) {
            if (type.code.equals(key)) {
                return type;
            }
        }
        return null;
    }
}
