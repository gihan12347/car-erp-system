package com.carsale.erp.shared.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;
import com.carsale.erp.importpipeline.auction.AuctionParseResult;
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

    public static String trimBeforeToken(String value, String token) {
        if (value == null) {
            return null;
        }
        int index = value.toUpperCase(Locale.ROOT).indexOf(token.toUpperCase(Locale.ROOT));
        if (index > 0) {
            return value.substring(0, index).trim();
        }
        return value.trim();
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

    public static String extractLabel(String text, String... labels) {
        for (String label : labels) {
            Pattern pattern = RegexConstants.Labeled.valueAfterQuotedLabelOrNumber(label);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return clean(matcher.group(1));
            }
        }
        return null;
    }

    public static String findChassis(String text) {
        Matcher matcher = RegexConstants.Identifiers.CHASSIS.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String extractHsCode(String text) {
        Matcher matcher = RegexConstants.Identifiers.HS_CODE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String extractTaxAmount(String text, String... taxCodes) {
        for (String taxCode : taxCodes) {
            String snippet = taxCodeSnippet(text, taxCode);
            if (snippet == null) {
                continue;
            }
            String amount = amountFromSnippet(snippet);
            if (amount != null) {
                return amount;
            }
        }
        return null;
    }

    private static String taxCodeSnippet(String text, String taxCode) {
        Matcher matcher = RegexConstants.Assessment.snippetAfterTaxCode(taxCode, RegexConstants.Assessment.TAX_CODE_BOUNDARY)
                .matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static String amountFromSnippet(String snippet) {
        if (snippet == null || snippet.trim().isEmpty()) {
            return null;
        }

        // Prefer the rightmost full amount (tax value column is on the right).
        Matcher grouped = RegexConstants.Amounts.GROUPED_SPACED_PATTERN.matcher(snippet);
        String lastGrouped = null;
        while (grouped.find()) {
            lastGrouped = grouped.group(1);
        }
        if (lastGrouped != null) {
            return formatThousands(lastGrouped.replaceAll(RegexConstants.Text.NON_DIGIT, ""));
        }

        Matcher compact = RegexConstants.Amounts.COMPACT_DIGITS_PATTERN.matcher(snippet);
        String lastCompact = null;
        while (compact.find()) {
            lastCompact = compact.group(1);
        }
        if (lastCompact != null) {
            return formatThousands(lastCompact);
        }

        return null;
    }

    private static String formatThousands(String digits) {
        if (digits == null || digits.isEmpty()) {
            return null;
        }
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

    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll(RegexConstants.Text.WHITESPACE_RUN, " ");
        cleaned = cleaned.replaceAll(RegexConstants.Text.LEADING_PUNCT, "").replaceAll(RegexConstants.Text.TRAILING_DASH, "").trim();
        if (cleaned.isEmpty() || isEmptyToken(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private static boolean isEmptyToken(String value) {
        String token = value.trim().toLowerCase(Locale.ROOT);
        return "-".equals(value.trim())
                || "—".equals(value.trim())
                || "--".equals(token)
                || "n/a".equals(token)
                || "na".equals(token)
                || "nil".equals(token)
                || "none".equals(token)
                || ".".equals(token);
    }
}
