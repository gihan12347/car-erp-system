package com.carsale.erp.preparationpipeline.inspection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Workshop inspection checklist choices: OK, No, N/A.
 */
public final class InspectionResults {

    public static final String OK = "OK";
    public static final String NO = "NO";
    public static final String NA = "N/A";

    private static final List<String> OPTIONS = Collections.unmodifiableList(
            Arrays.asList(OK, NO, NA));

    private InspectionResults() {
    }

    public static List<String> options() {
        return OPTIONS;
    }

    public static boolean isNo(String value) {
        return NO.equals(canonical(value));
    }

    public static boolean same(String left, String right) {
        String leftValue = canonical(left);
        String rightValue = canonical(right);
        return !leftValue.isEmpty() && leftValue.equals(rightValue);
    }

    public static String display(String value) {
        String canonical = canonical(value);
        if (NO.equals(canonical)) {
            return "No";
        }
        return canonical;
    }

    public static String jobSummary(String itemTitle) {
        String title = itemTitle == null ? "" : itemTitle.trim();
        if (title.isEmpty()) {
            return "Inspection fail";
        }
        return "Inspection fail: " + title;
    }

    public static String canonical(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return "";
        }
        String compact = trimmed.toUpperCase(Locale.ROOT).replace(" ", "");
        if ("OK".equals(compact)) {
            return OK;
        }
        if ("NO".equals(compact) || "N".equals(compact)) {
            return NO;
        }
        if ("NA".equals(compact) || "N/A".equals(compact) || "N.A.".equals(compact)
                || "N.A".equals(compact) || "NOTAPPLICABLE".equals(compact)) {
            return NA;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static String choiceDomId(String prefix, int index, String option) {
        return prefix + index + "_" + cssClass(option);
    }

    public static String cssClass(String option) {
        String canonical = canonical(option);
        if (OK.equals(canonical)) {
            return "OK";
        }
        if (NO.equals(canonical)) {
            return "NO";
        }
        if (NA.equals(canonical)) {
            return "NA";
        }
        String suffix = canonical.replaceAll("[^A-Za-z0-9]+", "");
        return suffix.isEmpty() ? "choice" : suffix;
    }
}
