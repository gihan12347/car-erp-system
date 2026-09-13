package com.carsale.erp.customspipeline.document;

import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Sri Lanka Customs Assessment Notice (ASYCUDA).
 * OCR often places the tax value on the line above the tax code, and inserts
 * spaces after thousands separators (e.g. "805, 692", "3, 346,743").
 */
@Component
public class AssessmentNotice implements DocumentParser {

    private static final String AMOUNT =
            "(\\d{1,3}(?:[,.\\u00A0\\u202F]\\d{3})+|\\d{3,})";

    private static final Pattern ITEM_TAXES_SECTION = Pattern.compile(
            "Item\\s+taxes([\\s\\S]*?)(?=Total\\s+assessed|Total\\s+amount\\s+paid|\\z)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GLOBAL_TAXES_SECTION = Pattern.compile(
            "Global\\s+taxes([\\s\\S]*?)(?=Item\\s+taxes|Total\\s+assessed|\\z)",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[] ITEM_CODES = {"CID", "SUR", "XID", "VAT", "VEL"};
    private static final String[] GLOBAL_CODES = {"OTC", "COM", "EXM"};

    private static final Pattern CODE_ON_LINE = Pattern.compile(
            "\\b(OTC|COM|EXM|CID|SUR|XID|VAT|VEL)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AMOUNT_ON_LINE = Pattern.compile(AMOUNT);

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("assessment notice was empty.");
            return result;
        }
        // "805, 692" / "1,992, 000" / "3, 346,743" → normal thousands grouping
        String normalized = fixBrokenCommas(CustomsDocumentParserUtils.normalize(text));

        result.put("assessmentOffice", extractAssessmentOffice(normalized));
        result.put("assessmentNoticeRef", extractAssessmentNoticeRef(normalized));
        result.put("assessmentModel", extractAssessmentModel(normalized));
        result.put("assessmentCustomsReference", extractLabeledDateRef(normalized, "Customs reference"));
        result.put("assessmentDeclarantReference", extractDeclarantReference(normalized));
        result.put("assessmentReference", extractLabeledDateRef(normalized, "Assessment reference"));
        result.put("assessmentPackages", extractAssessmentPackages(normalized));

        String[] declarant = extractAssessmentParty(normalized, "Declarant", "Consignee");
        result.put("assessmentDeclarantId", declarant[0]);
        result.put("assessmentDeclarantName", declarant[1]);
        result.put("assessmentDeclarantAddress", declarant[2]);
        result.put("assessmentDeclarantChaExp", extractChaExpiry(normalized));

        String[] consignee = extractAssessmentParty(normalized, "Consignee", "Global taxes");
        result.put("assessmentConsigneeId", consignee[0]);
        result.put("assessmentConsigneeName", consignee[1]);
        result.put("assessmentConsigneeAddress", consignee[2]);

        Map<String, String> globalTaxes = extractTaxTable(normalized, GLOBAL_TAXES_SECTION, GLOBAL_CODES);
        result.put("assessmentTaxOtc", globalTaxes.get("OTC"));
        result.put("assessmentTaxCom", globalTaxes.get("COM"));
        result.put("assessmentTaxExm", globalTaxes.get("EXM"));

        Map<String, String> itemTaxes = extractTaxTable(normalized, ITEM_TAXES_SECTION, ITEM_CODES);
        result.put("assessmentTaxCid", itemTaxes.get("CID"));
        result.put("assessmentTaxSur", itemTaxes.get("SUR"));
        result.put("assessmentTaxXid", itemTaxes.get("XID"));
        result.put("assessmentTaxVat", itemTaxes.get("VAT"));
        result.put("assessmentTaxVel", itemTaxes.get("VEL"));

        result.put("assessmentTotalAssessed", extractLabeledAmount(normalized,
                "Total assessed amount for the declaration", "Total assessed amount"));
        result.put("assessmentTotalPaid", extractLabeledAmount(normalized,
                "Total amount paid", "Total   amount paid"));

        CustomsDocumentParserUtils.finish(result, "Assessment notice");
        return result;
    }

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        return null;
    }

    private static String fixBrokenCommas(String text) {
        if (text == null) {
            return null;
        }
        // Collapse spaces after thousands separators produced by OCR
        return text.replaceAll(",\\s+(?=\\d{3}\\b)", ",");
    }

    private static Map<String, String> extractTaxTable(String text, Pattern sectionPattern, String[] codes) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String code : codes) {
            values.put(code, null);
        }

        String section = firstGroup(sectionPattern, text);
        String search = section != null ? section : text;

        // Primary: line-oriented parse (handles amount-above-code OCR)
        Map<String, String> fromLines = parseTaxLines(search, codes);
        for (String code : codes) {
            values.put(code, fromLines.get(code));
        }

        // Fallback: amount immediately after each code until the next code
        for (String code : codes) {
            if (values.get(code) == null) {
                values.put(code, extractCodeAmount(search, code, codes));
            }
        }

        return values;
    }

    /**
     * OCR layout seen on Assessment Notices:
     * <pre>
     * 354,867
     * CID  Customs Import Duty
     * 177,434
     * SUR  Surcharge   1,992,000   ← value before code is SUR; trailing amount is next (XID)
     * XID  Excise ...
     * VAT  Value Added Tax 805,692
     * </pre>
     */
    private static Map<String, String> parseTaxLines(String section, String[] allowedCodes) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String code : allowedCodes) {
            values.put(code, null);
        }

        String pendingAmount = null;
        String[] lines = section.split("\\n");
        for (String s : lines) {
            String line = s.trim();
            if (line.isEmpty()) {
                continue;
            }
            // Strip trailing junk like "300]"
            line = line.replaceAll("[\\]\\|]+$", "").trim();

            List<String> codesOnLine = codesOnLine(line, allowedCodes);
            List<String> amountsOnLine = amountsOnLine(line);

            if (codesOnLine.isEmpty()) {
                if (!amountsOnLine.isEmpty()) {
                    pendingAmount = amountsOnLine.get(amountsOnLine.size() - 1);
                }
                continue;
            }

            String code = codesOnLine.get(0).toUpperCase(Locale.ROOT);
            if (!values.containsKey(code) || values.get(code) != null) {
                // Unknown or already filled — keep pending for a later code if needed
                if (!amountsOnLine.isEmpty()) {
                    pendingAmount = amountsOnLine.get(amountsOnLine.size() - 1);
                }
                continue;
            }

            if (pendingAmount != null) {
                values.put(code, pendingAmount);
                pendingAmount = null;
                // Trailing amount on this line belongs to the next tax code
                if (!amountsOnLine.isEmpty()) {
                    pendingAmount = amountsOnLine.get(amountsOnLine.size() - 1);
                }
            } else if (!amountsOnLine.isEmpty()) {
                values.put(code, amountsOnLine.get(amountsOnLine.size() - 1));
            }
        }

        return values;
    }

    private static List<String> codesOnLine(String line, String[] allowedCodes) {
        List<String> found = new ArrayList<>();
        Matcher matcher = CODE_ON_LINE.matcher(line);
        while (matcher.find()) {
            String code = matcher.group(1).toUpperCase(Locale.ROOT);
            for (String allowed : allowedCodes) {
                if (allowed.equals(code)) {
                    found.add(code);
                    break;
                }
            }
        }
        return found;
    }

    private static List<String> amountsOnLine(String line) {
        List<String> found = new ArrayList<>();
        Matcher matcher = AMOUNT_ON_LINE.matcher(line);
        while (matcher.find()) {
            String amount = normalizeAmount(matcher.group(1));
            if (amount != null) {
                found.add(amount);
            }
        }
        return found;
    }

    private static String extractCodeAmount(String text, String code, String[] allCodes) {
        String boundary = joinCodes(allCodes);
        Pattern pattern = Pattern.compile(
                "\\b" + Pattern.quote(code) + "\\b([\\s\\S]*?)(?=\\b(?:" + boundary + ")\\b|\\z)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return lastAmount(matcher.group(1));
    }

    private static String joinCodes(String[] codes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                out.append('|');
            }
            out.append(Pattern.quote(codes[i]));
        }
        return out.toString();
    }

    private static String lastAmount(String snippet) {
        if (snippet == null || snippet.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = Pattern.compile(AMOUNT).matcher(snippet);
        String last = null;
        while (matcher.find()) {
            last = normalizeAmount(matcher.group(1));
        }
        return last;
    }

    private static String extractLabeledAmount(String text, String... labels) {
        for (String label : labels) {
            Pattern pattern = Pattern.compile(
                    Pattern.quote(label) + "[^\\d]{0,40}?" + AMOUNT,
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return normalizeAmount(matcher.group(1));
            }
        }
        // OCR sometimes splits "Total amount paid"
        Matcher paid = Pattern.compile(
                "Total\\s+amount\\s+paid\\s*[:\\.]?\\s*" + AMOUNT,
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (paid.find()) {
            return normalizeAmount(paid.group(1));
        }
        return null;
    }

    private static String normalizeAmount(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() <= 2) {
            return null;
        }
        return formatThousands(digits);
    }

    private static String formatThousands(String digits) {
        int length = digits.length();
        if (length < 4) {
            return digits;
        }
        StringBuilder result = new StringBuilder();
        int lead = length % 3;
        if (lead == 0) {
            lead = 3;
        }
        result.append(digits.substring(0, lead));
        for (int i = lead; i < length; i += 3) {
            result.append(',').append(digits.substring(i, i + 3));
        }
        return result.toString();
    }

    private static String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractAssessmentOffice(String text) {
        Matcher matcher = Pattern.compile(
                "([A-Za-z][A-Za-z ]+Import Office(?:\\s*-\\s*Sea)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return CustomsDocumentParserUtils.extractLabel(text, "Customs Office", "Office");
    }

    private static String extractAssessmentNoticeRef(String text) {
        Matcher matcher = Pattern.compile(
                "\\b(\\d{4}\\s+[A-Z]{2,}\\d*\\s+[IA]\\s+\\d{3,})\\b",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractAssessmentModel(String text) {
        Matcher matcher = Pattern.compile("\\b(IM\\s*\\d)\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        }
        return CustomsDocumentParserUtils.extractLabel(text, "Model");
    }

    private static String extractLabeledDateRef(String text, String label) {
        Matcher matcher = Pattern.compile(
                Pattern.quote(label) + "\\s*[:\\.]?\\s*(\\d{1,2}/\\d{1,2}/\\d{4}\\s+[IA]\\s+\\d{3,})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        matcher = Pattern.compile(
                Pattern.quote(label) + "[^\\n]{0,40}?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+[IA]\\s+\\d{3,})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractDeclarantReference(String text) {
        Matcher matcher = Pattern.compile(
                "Declarant reference\\s*[:\\.]?\\s*(\\d{4}\\s*#?\\s*\\d+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1).replaceAll("\\s+", " "));
        }
        matcher = Pattern.compile("\\b(\\d{4}\\s*#\\s*\\d+)\\b").matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1).replaceAll("\\s+", " "));
        }
        return null;
    }

    private static String extractAssessmentPackages(String text) {
        Matcher matcher = Pattern.compile(
                "Packages\\s*[:\\.]?\\s*([\\d,]+(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractChaExpiry(String text) {
        Matcher matcher = Pattern.compile(
                "CHA\\s*EXP\\s*[:\\.]?\\s*(\\d{1,2}/\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String[] extractAssessmentParty(String text, String heading, String stopHeading) {
        String[] empty = new String[] { null, null, null };
        Pattern blockPattern = Pattern.compile(
                "(?:^|\\n)\\s*" + Pattern.quote(heading)
                        + "(?!\\s+reference)\\b[\\s\\S]*?(?=(?:^|\\n)\\s*" + Pattern.quote(stopHeading) + "\\b|$)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher blockMatcher = blockPattern.matcher(text);
        if (!blockMatcher.find()) {
            return empty;
        }
        String block = blockMatcher.group();
        String id = null;
        Matcher idMatcher = Pattern.compile(
                "\\bID\\b\\s*[:\\.]?\\s*(\\d[\\d\\-]{6,})",
                Pattern.CASE_INSENSITIVE
        ).matcher(block);
        if (idMatcher.find()) {
            id = idMatcher.group(1);
        }
        String name = CustomsDocumentParserUtils.extractLabel(block, "Name");
        if (name != null && (name.equalsIgnoreCase(heading) || name.toUpperCase(Locale.ROOT).startsWith("ID"))) {
            name = null;
        }
        if (name == null) {
            Matcher nameMatcher = Pattern.compile("(?m)^\\s*([A-Z][A-Z0-9 .,&'/\\-]{6,})\\s*$").matcher(block);
            while (nameMatcher.find()) {
                String candidate = CustomsDocumentParserUtils.clean(nameMatcher.group(1));
                if (candidate == null) {
                    continue;
                }
                String upper = candidate.toUpperCase(Locale.ROOT);
                if (upper.equals(heading.toUpperCase(Locale.ROOT))
                        || upper.startsWith("ID")
                        || upper.startsWith("CHA")
                        || upper.contains("TAX")
                        || upper.contains("REFERENCE")) {
                    continue;
                }
                name = candidate;
                break;
            }
        }
        String address = CustomsDocumentParserUtils.extractLabel(block, "Address");
        if (address == null) {
            Matcher addressMatcher = Pattern.compile(
                    "(?m)^\\s*(NO\\.?\\s*\\d[^\\n]+|\\d+[A-Z0-9 /\\-,]+[A-Z][^\\n]{4,})\\s*$",
                    Pattern.CASE_INSENSITIVE
            ).matcher(block);
            if (addressMatcher.find()) {
                address = CustomsDocumentParserUtils.clean(addressMatcher.group(1));
            }
        }
        return new String[] { id, name, address };
    }
}
