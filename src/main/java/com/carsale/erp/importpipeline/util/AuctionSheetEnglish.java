package com.carsale.erp.importpipeline.util;

import com.carsale.erp.shared.ocr.JapaneseTextTranslator;
import com.carsale.erp.shared.regex.RegexConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AuctionSheetEnglish {

    private static final String[] MONTHS = new String[] {
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    public static boolean containsJapanese(String value) {
        return value != null && RegexConstants.Text.JAPANESE_SCRIPT.matcher(value).find();
    }

    public static String translateLine(String line, JapaneseTextTranslator translator) {
        return translateSheet(line, translator);
    }

    public static String translateSheet(String text, JapaneseTextTranslator translator) {
        if (translator == null || !containsJapanese(text)) {
            return text;
        }
        List<String> tokens = new ArrayList<>();
        String masked = maskSheetTokens(text, tokens);
        String translated = translator.translateJaToEn(masked);
        if (translated == null || translated.trim().isEmpty()) {
            return text;
        }
        return restoreSheetTokens(translated, tokens);
    }

    public static String maskSheetTokens(String line, List<String> tokens) {
        Matcher matcher = RegexConstants.Identifiers.PRESERVE_SHEET_TOKEN.matcher(line);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            tokens.add(matcher.group());
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("[[T" + (tokens.size() - 1) + "]]"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String restoreSheetTokens(String translated, List<String> tokens) {
        if (translated == null) {
            return null;
        }
        String result = translated;
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Pattern marker = RegexConstants.Identifiers.translationPlaceholder(i);
            Matcher found = marker.matcher(result);
            if (found.find()) {
                result = found.replaceAll(Matcher.quoteReplacement(tokens.get(i)));
            } else {
                missing.add(tokens.get(i));
            }
        }
        if (!missing.isEmpty()) {
            result = result.replaceAll(RegexConstants.Text.TRAILING_HORIZONTAL, "") + " " + String.join(" ", missing);
        }
        return result.replaceAll(RegexConstants.Text.NBSP_HORIZONTAL, " ")
                .replaceAll(RegexConstants.Text.NEWLINE_WRAP_SPACE, "\n")
                .trim();
    }

    public static String convert(String value, JapaneseTextTranslator translator) {
        if (value == null) {
            return null;
        }
        String result = convertDates(value);
        result = result.replace('（', '(').replace('）', ')').replace('、', ',');
        if (containsJapanese(result) && translator != null) {
            String translated = translator.translateJaToEn(result);
            if (translated != null && !translated.trim().isEmpty()) {
                result = translated;
            }
        }
        result = result.replaceAll(RegexConstants.Text.JAPANESE_SCRIPT.pattern(), " ");
        result = result.replaceAll(RegexConstants.Text.PIPE, " ");
        result = result.replaceAll(RegexConstants.Text.SPACE_BEFORE_PAREN, " (");
        result = result.replaceAll(RegexConstants.Text.SPACE_BEFORE_COMMA, ",");
        result = result.replaceAll(RegexConstants.Text.HYBRID_Z, "Hybrid Z");
        result = result.replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
        result = result.replaceAll(RegexConstants.Text.LEADING_TRAILING_COMMAS, "");
        return result.isEmpty() ? null : result;
    }

    private static String convertDates(String value) {
        Matcher era = RegexConstants.Dates.ERA_YEAR_MONTH_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (era.find()) {
            int year = toWesternYear(era.group(1), era.group(2));
            int month = Integer.parseInt(era.group(3));
            String english = monthName(month) + " " + year;
            era.appendReplacement(buffer, Matcher.quoteReplacement(english));
        }
        era.appendTail(buffer);
        return buffer.toString();
    }

    private static int toWesternYear(String era, String number) {
        int n = "元".equals(number) ? 1 : Integer.parseInt(number);
        if ("令和".equals(era)) {
            return 2018 + n;
        }
        if ("平成".equals(era)) {
            return 1988 + n;
        }
        return 1925 + n;
    }

    private static String monthName(int month) {
        if (month >= 1 && month <= 12) {
            return MONTHS[month];
        }
        return String.valueOf(month);
    }

    private AuctionSheetEnglish() {
    }
}
