package com.carsale.erp.preparationpipeline.util;

import java.util.Locale;
import java.util.Set;

import com.carsale.erp.preparationpipeline.model.YardRecord;

public final class YardAllocation {

    public static final String UNASSIGNED_KEY = "UNASSIGNED";

    private YardAllocation() {
    }

    public static String laneKey(YardRecord record, Set<String> knownYardCodes) {
        if (record == null) {
            return UNASSIGNED_KEY;
        }
        String bay = normalize(record.getBayNo());
        if (bay.isEmpty()) {
            return UNASSIGNED_KEY;
        }
        if (knownYardCodes == null || !knownYardCodes.contains(bay)) {
            return UNASSIGNED_KEY;
        }
        return bay;
    }

    public static boolean isUnassigned(YardRecord record, Set<String> knownYardCodes) {
        return UNASSIGNED_KEY.equals(laneKey(record, knownYardCodes));
    }

    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static void requireSpace(int remaining, int needed) {
        if (needed <= 0) {
            return;
        }
        if (remaining < needed) {
            throw new IllegalArgumentException(
                    "Yard has " + remaining + (remaining == 1 ? " space" : " spaces")
                            + " left. You selected " + needed + ".");
        }
    }
}
