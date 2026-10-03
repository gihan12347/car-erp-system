package com.carsale.erp.shared.utils;

import java.util.Map;
import java.util.Locale;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.regex.RegexConstants;

public class CustomsDocumentParserUtils {

    public static String firstNonNull(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    public static void finish(AuctionParseResult result, String label) {
        boolean any = !result.getFields().isEmpty();
        result.setSuccess(any);
        result.setMessage(any
                ? "Filled " + result.getFields().size() + " fields from " + label + ". Please review."
                : "The document was read, but fields could not be mapped. Please fill them manually.");
    }

    public static String normalize(String text) {
        return text.replace('\r', '\n')
                .replaceAll(RegexConstants.Text.HORIZONTAL_SPACE, " ")
                .replaceAll(RegexConstants.Text.MANY_NEWLINES, "\n\n");
    }

    public static String suffix(String name) {
        if (name == null || name.lastIndexOf('.') < 0) {
            return ".img";
        }
        return name.substring(name.lastIndexOf('.'));
    }

    public static String toSlug(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return text.trim()
                .toLowerCase()
                .replaceAll("\\s+", "-");
    }

    public static String getNormalizedValue(String compact) {
        switch (compact) {
            case "YES":
            case "Y":
                return "YES";
            case "NO":
            case "N":
                return "NO";
            case "OK":
                return "OK";
            default:
                return "N/A";
        }
    }

    public static String formatThousands(String digits) {
        int length = digits.length();
        if (length < 4) {
            return digits;
        }
        StringBuilder result = new StringBuilder();
        int lead = length % 3;
        if (lead == 0) {
            lead = 3;
        }
        result.append(digits, 0, lead);
        for (int i = lead; i < length; i += 3) {
            result.append(',').append(digits, i, i + 3);
        }
        return result.toString();
    }

    public static String mapType(String type, Map<String, String> typeToFiled) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        String key = typeKey(type);
        String mapped = typeToFiled.get(key);
        if (mapped != null) {
            return mapped;
        }
        int slash = key.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < key.length()) {
            return typeToFiled.get(key.substring(slash + 1));
        }
        return null;
    }

    public static String typeKey(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "";
        }
        return type.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static String formatDate(int year, int month, int day) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day);
    }

    public static String formatMonth(int year, int month) {
        return String.format(Locale.ROOT, "%04d-%02d", year, month);
    }
}
