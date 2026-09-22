package com.carsale.erp.shared.regex;

import java.util.regex.Pattern;

/**
 * Shared regular expressions, grouped by domain.
 */
public final class RegexConstants {

    private RegexConstants() {
    }

    private static Pattern compile(String regex) {
        return Pattern.compile(regex);
    }

    public static Pattern compileIgnoreCase(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private static Pattern compileIgnoreCaseMultiline(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    }

    private static Pattern compileIgnoreCaseDotAll(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    public static final class Text {
        private Text() {
        }

        public static final String WHITESPACE = "\\s+";
        public static final String WHITESPACE_RUN = "\\s{2,}";
        public static final String HORIZONTAL_SPACE = "[ \t]+";
        public static final String NBSP_HORIZONTAL = "[ \\t\\u00A0]+";
        public static final String NEWLINE_WRAP_SPACE = " *\\n *";
        public static final String MANY_NEWLINES = "\n{3,}";
        public static final String SPACES = " +";
        public static final String TABS = "[\\t]+";
        public static final String TAB = "\\t";
        public static final String NEWLINE = "\\n";
        public static final String LF = "\n";
        public static final String TRAILING_HORIZONTAL = "[ \\t]+$";
        public static final String ANY_WHITESPACE = "\\s";
        public static final String NON_DIGIT = "\\D";
        public static final String DIGITS_ONLY = "[^0-9]";
        public static final String ALNUM_ONLY = "[^A-Za-z0-9]+";
        public static final String LOWER_SLUG = "[^a-z0-9]+";
        public static final String SLUG_EDGE_DASH = "^-+|-+$";
        public static final String TRAILING_DASHES = "-+$";
        public static final String SQL_IDENT = "[A-Za-z0-9_]+";
        public static final String LEADING_PUNCT = "^[.:|\\-/]+";
        public static final String TRAILING_PUNCT = "[.:|\\-/]+$";
        public static final String TRAILING_DASH = "[\\-/]+$";
        public static final String TRAILING_EMDASH = "[\\u2013\\u2014_]+$";
        public static final String LEADING_COLON = "^[.:]+";
        public static final String TRAILING_DOTS = "\\.+$";
        public static final String COMPACT_PUNCT = "[\\s.:\\-]+";
        public static final String PIPE = "[|｜]+";
        public static final String PARENTHESES = "\\([^)]*\\)";
        public static final String LEADING_PARENTHESES = "^\\([^)]*\\)\\s*";
        public static final String TRAILING_LINE_JUNK = "[\\]\\|]+$";
        public static final String COMMA_OR_DOT = "[.,]";
        public static final String CURRENCY_CHARS = "[^0-9.,]";
        public static final String BRACKET_CODE = "\\[\\s*\\d+\\s*\\]";
        public static final String EMPTY_MARKERS = "(?i)[-–—\\s/]+";
        public static final String SLASH_SPACES = "\\s+/\\s*";
        public static final String STARTS_WITH_LETTER = "[A-Za-z].*";
        public static final String SPACE_BEFORE_PAREN = "(?<=[A-Za-z0-9])\\(";
        public static final String SPACE_BEFORE_COMMA = "\\s+,";
        public static final String LEADING_TRAILING_COMMAS = "(^,\\s*)+|(\\s*,)+$";
        public static final String LABEL_PUNCT = "[\\s:：・\\-_/]";
        public static final String CHASSIS_CHARS = "[^A-Z0-9-]";
        public static final String LEADING_G = "^[Gg]";
        public static final Pattern JAPANESE_SCRIPT = compile("[\\u3040-\\u30FF\\u4E00-\\u9FFF]");
        public static final Pattern JAPANESE_SCRIPT_NARROW = compile("[\\u3040-\\u30ff\\u4e00-\\u9faf]");
        public static final Pattern TWO_NUMBER_CLUSTERS = Pattern.compile("\\d{3,}.*\\d{3,}", Pattern.DOTALL);
        public static final Pattern NEXT_LINE_VALUE = compile("^\\n\\s*([^\\n]+)");
        public static final String HYBRID_Z = "(?i)HybridZ";
    }

    public static final class Dates {
        private Dates() {
        }

        public static final String MONTH_NAME =
                "(?:JAN(?:UARY)?|FEB(?:RUARY)?|MAR(?:CH)?|APR(?:IL)?|MAY|JUN(?:E)?"
                        + "|JUL(?:Y)?|AUG(?:UST)?|SEP(?:T(?:EMBER)?)?|OCT(?:OBER)?"
                        + "|NOV(?:EMBER)?|DEC(?:EMBER)?)";
        public static final String MONTH_FULL =
                "January|February|March|April|May|June|July|August|September|October|November|December";
        public static final String MONTH_ANY =
                MONTH_FULL + "|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec";
        public static final String DAY_MONTH_YEAR = "(\\d{1,2}\\s+" + MONTH_NAME + "\\.?\\s+\\d{4})";
        public static final String NUMERIC_DATE =
                "(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})";
        public static final String ISO_DATE = "\\d{4}-\\d{2}-\\d{2}";
        public static final String ISO_DATE_TIME = "^(" + ISO_DATE + ")(?:[T\\s].*)?$";
        public static final String CONTAINS_YEAR = ".*\\d{4}.*";
        public static final String YEAR_CAPTURE = "(\\d{4})";
        public static final String YEAR_WORD = "\\b(19|20)\\d{2}\\b";
        public static final String ONE_OR_TWO_DIGITS = "\\d{1,2}";
        public static final String YEAR_MONTH_ISO = "^(\\d{4})-(\\d{1,2})$";
        public static final String MONTH_YEAR_NUMERIC = "^(\\d{1,2})[/.-](\\d{4})$";
        public static final String YEAR_MONTH_NUMERIC = "^(\\d{4})[/.-](\\d{1,2})$";
        public static final String NAMED_MONTH_YEAR =
                "^(" + MONTH_NAME + ")[.\\-/ ]*(\\d{4})$";
        public static final String DAY_MONTH_YEAR_NUMERIC = "^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})$";
        public static final String YEAR_MONTH_DAY_NUMERIC = "^(\\d{4})[/.-](\\d{1,2})[/.-](\\d{1,2})$";
        public static final String MONTH_FULL_YEAR = "(" + MONTH_FULL + ")\\s+\\d{4}";
        public static final String DAY_MON_YEAR = "\\b(\\d{1,2}-[A-Za-z]{3}-\\d{2})\\b";
        public static final String SLASH_DATE = "(\\d{1,2}/\\d{1,2}/\\d{4})";
        public static final String REIWA_YMD = "令和\\s*(\\d+)\\s*年\\s*(\\d{1,2})\\s*月(?:\\s*(\\d{1,2})\\s*日)?";
        public static final String WESTERN_JP_YMD = "(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月(?:\\s*(\\d{1,2})\\s*日)?";
        public static final String ERA_YEAR_MONTH = "(令和|平成|昭和)\\s*(\\d{1,2}|元)\\s*年\\s*(\\d{1,2})\\s*月";
        public static final String JP_YEAR_MONTH = "(\\d{1,2})\\s*年\\s*(\\d{1,2})\\s*月";
        public static final String YEAR_MONTH_LOOSE = "((?:19|20)\\d{2})\\s*[/.\\-]\\s*([1-9]|1[0-2])";
        public static final Pattern ISO_DATE_PATTERN = compile(ISO_DATE);
        public static final Pattern ISO_DATE_TIME_PATTERN = compile(ISO_DATE_TIME);
        public static final Pattern YEAR_MONTH_ISO_PATTERN = compile(YEAR_MONTH_ISO);
        public static final Pattern MONTH_YEAR_NUMERIC_PATTERN = compile(MONTH_YEAR_NUMERIC);
        public static final Pattern YEAR_MONTH_NUMERIC_PATTERN = compile(YEAR_MONTH_NUMERIC);
        public static final Pattern NAMED_MONTH_YEAR_PATTERN = compileIgnoreCase(NAMED_MONTH_YEAR);
        public static final Pattern DAY_MONTH_YEAR_NUMERIC_PATTERN = compile(DAY_MONTH_YEAR_NUMERIC);
        public static final Pattern YEAR_MONTH_DAY_NUMERIC_PATTERN = compile(YEAR_MONTH_DAY_NUMERIC);
        public static final Pattern MONTH_FULL_YEAR_PATTERN = compile(MONTH_FULL_YEAR);
        public static final Pattern YEAR_WORD_PATTERN = compile(YEAR_WORD);
        public static final Pattern YEAR_CAPTURE_PATTERN = compile(YEAR_CAPTURE);
        public static final Pattern DAY_MON_YEAR_PATTERN = compile(DAY_MON_YEAR);
        public static final Pattern REIWA_YMD_PATTERN = compile(REIWA_YMD);
        public static final Pattern WESTERN_JP_YMD_PATTERN = compile(WESTERN_JP_YMD);
        public static final Pattern ERA_YEAR_MONTH_PATTERN = compile(ERA_YEAR_MONTH);
        public static final Pattern JP_YEAR_MONTH_PATTERN = compile(JP_YEAR_MONTH);
        public static final Pattern YEAR_MONTH_LOOSE_PATTERN = compile(YEAR_MONTH_LOOSE);
        public static final Pattern MONTH_NAME_DATE = compileIgnoreCase("\\b" + DAY_MONTH_YEAR + "\\b");
    }

    public static final class Amounts {
        private Amounts() {
        }

        public static final String GROUPED =
                "(\\d{1,3}(?:[,.\\u00A0\\u202F]\\d{3})+|\\d{3,})";
        public static final String GROUPED_SPACED = "(\\d{1,3}(?:[\\s.,\\u00A0\\u202F]\\d{3})+)";
        public static final String GROUPED_COMMA = "(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)";
        public static final String DEC_MONEY = "(\\d{1,3}(?:[.,]\\d{3})+\\.\\d{2}|\\d{4,}\\.\\d{2})";
        public static final String EUROPEAN_MONEY = "\\d{1,3}(?:\\.\\d{3})+\\.\\d{2}";
        public static final String COMPACT_DIGITS = "(\\d{4,})";
        public static final String COMPACT_DIGITS_OPTIONAL_DEC = "([\\d,]+(?:\\.\\d+)?)";
        public static final String TRAILING_ZERO_DECIMALS = ".*\\.0+$";
        public static final String ALL_ZEROS = "0+";
        public static final String THOUSANDS_SPACE = ",\\s+(?=\\d{3}\\b)";
        public static final String CURRENCY_LKR = "(?i)\\b(LKR|SLR|USD|RS\\.?)\\b";
        public static final String CURRENCY_JPY = "(?i)\\bJ?PY\\b";
        public static final String AFTER_EQUALS = "=\\s*" + GROUPED_COMMA;
        public static final Pattern GROUPED_PATTERN = compile(GROUPED);
        public static final Pattern GROUPED_SPACED_PATTERN = compile(GROUPED_SPACED);
        public static final Pattern GROUPED_COMMA_PATTERN = compile(GROUPED_COMMA);
        public static final Pattern COMPACT_DIGITS_PATTERN = compile(COMPACT_DIGITS);
        public static final Pattern AFTER_EQUALS_PATTERN = compile(AFTER_EQUALS);
    }

    public static final class Identifiers {
        private Identifiers() {
        }

        public static final Pattern YARD_CODE = compileIgnoreCase("^YD-(\\d+)$");
        public static final Pattern SALE_CODE = compileIgnoreCase("^SL-(\\d+)$");
        public static final Pattern CHASSIS = compile("\\b([A-Z0-9]{3,10}-[0-9]{5,10})\\b");
        public static final Pattern CHASSIS_WIDE = compile("\\b([A-Z0-9]{2,8}-[A-Z0-9]{5,12})\\b");
        public static final Pattern CHASSIS_TOKEN = compileIgnoreCase("[A-Z]{1,5}\\d{2,3}[A-Z0-9]{0,2}-\\d{5,8}");
        public static final Pattern CHASSIS_CAPTURE = compile("([A-Z]{1,5}\\d{2,3}[A-Z0-9]{0,2}-\\d{5,8})");
        public static final Pattern MODEL_CODE = compileIgnoreCase("\\b(?:[0-9][A-Z]{2}|[A-Z]{3})-[A-Z0-9]{3,10}\\b");
        public static final Pattern MODEL_CODE_CAPTURE = compile("((?:[0-9][A-Z]{2}|[A-Z]{3})-[A-Z0-9]{3,10})");
        public static final Pattern MODEL_CODE_SPACED = compile("((?:[0-9][A-Z]{2}|[A-Z]{3})\\s+[A-Z0-9]{4,10})");
        public static final Pattern MODEL_CODE_OPTIONAL_SEP = compile("((?:[0-9][A-Z]{2}|[A-Z]{3})[-\\s]?[A-Z0-9]{3,10})");
        public static final Pattern HS_CODE = compile("\\b(8703\\.\\d{2}\\.\\d{2})\\b");
        public static final Pattern HS_DOTTED = compile("\\b(\\d{4}\\.\\d{2}\\.\\d{2})\\b");
        public static final Pattern LONG_NUMBER = compile("\\b(\\d{14,20})\\b");
        public static final Pattern PRESERVE_SHEET_TOKEN = compileIgnoreCase(
                "[A-Z]{1,5}\\d{2,3}[A-Z0-9]{0,2}-\\d{5,8}"
                        + "|(?:[0-9][A-Z]{2}|[A-Z]{3})-[A-Z0-9]{3,10}"
                        + "|(?<![A-Z0-9])[A-Z]\\d{2}(?![A-Z0-9])"
                        + "|(?<![A-Z0-9])\\d[A-Z]\\d(?![A-Z0-9])"
        );
        public static final String CHASSIS_SHAPE = "[A-Z0-9]+-\\d{5,8}";
        public static final String COLOR_CODE = "[A-Z]\\d{2}|\\d{3}|\\d[A-Z]\\d";
        public static final Pattern COLOR_CODE_CAPTURE = compile("(?<![A-Z0-9])([A-Z]\\d{2}|\\d{3}|\\d[A-Z]\\d)(?![A-Z0-9])");
        public static final Pattern WHITE_CODE = compile("(?<!\\d)(W\\d{2})(?!\\d)");

        public static Pattern chassisContainingDigits(String digits) {
            return compile("[A-Z0-9]-" + Pattern.quote(digits));
        }

        public static Pattern tokenNotTouchingDigits(String token) {
            return compile("(?<!\\d)" + Pattern.quote(token) + "(?!\\d)");
        }

        public static Pattern translationPlaceholder(int index) {
            return compileIgnoreCase("\\[\\[\\s*T\\s*" + index + "\\s*\\]\\]");
        }
    }

    public static final class Labeled {
        private Labeled() {
        }

        public static final String LINE_VALUE = "\\s*[:\\.]?\\s*([^\\n]+)";
        public static final String LINE_VALUE_OPTIONAL = "\\s*[:\\.]?\\s*([^\\n]*)";
        public static final String NEXT_LINE_VALUE = "\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)";
        public static final String HASH_OR_NO = "(?:\\s*#|\\s*No\\.?)?\\s*[:\\.]?\\s*([^\\n]+)";
        public static final String HASH_OR_NO_NEXT = "(?:\\s*#|\\s*No\\.?)?\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)";
        public static final String STOP_TAIL = ".*$";
        public static final String NOT_LETTER_BEFORE = "(?<![A-Za-z])";
        public static final String NOT_LETTER_AFTER = "(?![A-Za-z])";
        public static final String NON_CAPTURING_OPEN = "(?:";
        public static final String NON_CAPTURING_CLOSE = ")";

        public static String notAdjacentToLetters(String quoted) {
            return NOT_LETTER_BEFORE + quoted + NOT_LETTER_AFTER;
        }

        public static String nonCapturingGroup(String regex) {
            return NON_CAPTURING_OPEN + regex + NON_CAPTURING_CLOSE;
        }

        public static Pattern optionalValueAfterQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + LINE_VALUE_OPTIONAL);
        }

        public static Pattern valueAfterQuotedLabelOrNumber(String label) {
            return compileIgnoreCase(Pattern.quote(label) + HASH_OR_NO);
        }

        public static Pattern valueAfterLabel(String labelRegex) {
            return compileIgnoreCase(labelRegex + LINE_VALUE);
        }

        public static Pattern valueOnNextLineAfterLabel(String labelRegex) {
            return compileIgnoreCase(labelRegex + NEXT_LINE_VALUE);
        }

        public static Pattern valueAfterLabelOrNumber(String labelRegex) {
            return compileIgnoreCase(labelRegex + HASH_OR_NO);
        }

        public static Pattern valueOnNextLineAfterLabelOrNumber(String labelRegex) {
            return compileIgnoreCase(labelRegex + HASH_OR_NO_NEXT);
        }

        public static Pattern quotedTextIgnoreCase(String value) {
            return compileIgnoreCase(Pattern.quote(value));
        }

        public static Pattern ignoreCaseAtWordBoundary(String label) {
            return compile("(?i)(?<![A-Za-z0-9])" + Pattern.quote(label));
        }

        public static Pattern fromQuotedStopToEnd(String stop) {
            return compileIgnoreCase("\\s+" + Pattern.quote(stop) + STOP_TAIL);
        }

        public static Pattern fromStopPatternToEnd(String stop) {
            return compileIgnoreCase("\\s+" + stop + STOP_TAIL);
        }

        public static Pattern stopAfterJevicLabel(String label) {
            return compileIgnoreCase("\\s+" + Pattern.quote(label) + "(?:\\s*[:\\.]?|\\s+No\\.?\\s*[:\\.]?)");
        }

        public static Pattern tabSeparatedValueAfterQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "\\.?\\s*[:\\.]?[\\t\\s]+([^\\n]+)");
        }
    }

    public static final class Assessment {
        private Assessment() {
        }

        public static final String TAX_CODES = "OTC|COM|EXM|CID|SUR|XID|VAT|VEL";
        public static final String TAX_CODE_BOUNDARY = TAX_CODES + "|SEL";
        public static final Pattern ITEM_TAXES_SECTION = compileIgnoreCase(
                "Item\\s+taxes([\\s\\S]*?)(?=Total\\s+assessed|Total\\s+amount\\s+paid|\\z)"
        );
        public static final Pattern GLOBAL_TAXES_SECTION = compileIgnoreCase(
                "Global\\s+taxes([\\s\\S]*?)(?=Item\\s+taxes|Total\\s+assessed|\\z)"
        );
        public static final Pattern CODE_ON_LINE = compileIgnoreCase("\\b(" + TAX_CODES + ")\\b");
        public static final Pattern TOTAL_AMOUNT_PAID = compileIgnoreCase(
                "Total\\s+amount\\s+paid\\s*[:\\.]?\\s*" + Amounts.GROUPED
        );
        public static final Pattern IMPORT_OFFICE = compileIgnoreCase("([A-Za-z][A-Za-z ]+Import Office(?:\\s*-\\s*Sea)?)");
        public static final Pattern NOTICE_REF = compileIgnoreCase("\\b(\\d{4}\\s+[A-Z]{2,}\\d*\\s+[IA]\\s+\\d{3,})\\b");
        public static final Pattern MODEL_IM = compileIgnoreCase("\\b(IM\\s*\\d)\\b");
        public static final Pattern DECLARANT_REFERENCE = compileIgnoreCase("Declarant reference\\s*[:\\.]?\\s*(\\d{4}\\s*#?\\s*\\d+)");
        public static final Pattern HASH_REFERENCE = compile("\\b(\\d{4}\\s*#\\s*\\d+)\\b");
        public static final Pattern PACKAGES = compileIgnoreCase("Packages\\s*[:\\.]?\\s*" + Amounts.COMPACT_DIGITS_OPTIONAL_DEC);
        public static final Pattern CHA_EXP = compileIgnoreCase("CHA\\s*EXP\\s*[:\\.]?\\s*" + Dates.SLASH_DATE);
        public static final Pattern PARTY_ID = compileIgnoreCase("\\bID\\b\\s*[:\\.]?\\s*(\\d[\\d\\-]{6,})");
        public static final Pattern PARTY_NAME = compile("(?m)^\\s*([A-Z][A-Z0-9 .,&'/\\-]{6,})\\s*$");
        public static final Pattern PARTY_ADDRESS = compileIgnoreCase("(?m)^\\s*(NO\\.?\\s*\\d[^\\n]+|\\d+[A-Z0-9 /\\-,]+[A-Z][^\\n]{4,})\\s*$");
        public static final String CUSTOMS_REF_VALUE = "(\\d{1,2}/\\d{1,2}/\\d{4}\\s+[IA]\\s+\\d{3,})";

        public static Pattern snippetAfterTaxCode(String code, String boundary) {
            return compileIgnoreCase("\\b" + Pattern.quote(code) + "\\b([\\s\\S]*?)(?=\\b(?:" + boundary + ")\\b|\\z)");
        }

        public static Pattern amountAfterQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "[^\\d]{0,40}?" + Amounts.GROUPED);
        }

        public static Pattern customsRefAfterQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "\\s*[:\\.]?\\s*" + CUSTOMS_REF_VALUE);
        }

        public static Pattern customsRefNearQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "[^\\n]{0,40}?" + CUSTOMS_REF_VALUE);
        }

        public static Pattern sectionBetweenHeadings(String heading, String stopHeading) {
            return compileIgnoreCase("(?:^|\\n)\\s*" + Pattern.quote(heading)
                    + "(?!\\s+reference)\\b[\\s\\S]*?(?=(?:^|\\n)\\s*" + Pattern.quote(stopHeading) + "\\b|$)");
        }
    }

    public static final class Declaration {
        private Declaration() {
        }

        public static final Pattern INVOICE_SECTION = compileIgnoreCase(
                "(?:\\(\\s*)?TOTAL\\s+INVOICE\\s+AMOUNT(?:\\s*\\))?"
                        + "([\\s\\S]*?)"
                        + "(?=Declaration\\s+Submitted|Authorised\\s+Signatory|Authorized\\s+Signatory"
                        + "|\\b53\\.\\b|I\\s+do\\s+hereby|COLOMBO\\s+MOTOR\\s+TRADING\\s+COMPANY\\s+do\\b"
                        + "|\\bB\\s*/\\s*L\\s*:|\\bExchange\\s+Rate\\b|\\bValue\\s*\\(?\\s*NCY|\\z)"
        );
        public static final Pattern SECTION_FALLBACK = compileIgnoreCase(
                "(?:FOB\\s*/\\s*CIF|F0[0O]\\s*/\\s*CIF)\\s*" + Amounts.DEC_MONEY
                        + "[\\s\\S]{0,400}?\\bTOTAL\\b\\s*" + Amounts.DEC_MONEY
        );
        public static final Pattern LINE_FOB = compileIgnoreCase(
                "(?:FOB\\s*/\\s*CIF|F0[0O]\\s*/\\s*CIF)\\s*[:\\-.]?\\s*" + Amounts.DEC_MONEY + "(?:\\s*J?PY)?"
        );
        public static final Pattern LINE_FREIGHT = compileIgnoreCase("\\bFREIGHT\\b\\s*[:\\-.]?\\s*" + Amounts.DEC_MONEY + "(?:\\s*J?PY)?");
        public static final Pattern LINE_INSURANCE = compileIgnoreCase("\\bINSURANCE\\b\\s*[:\\-.]?\\s*" + Amounts.DEC_MONEY + "(?:\\s*J?PY)?");
        public static final Pattern LINE_OTHER = compileIgnoreCase("\\bOTHER\\b\\s*[:\\-.]?\\s*" + Amounts.DEC_MONEY + "(?:\\s*J?PY)?");
        public static final Pattern LINE_TOTAL = compileIgnoreCase("\\bTOTAL\\b\\s*[:\\-.]?\\s*" + Amounts.DEC_MONEY + "(?:\\s*J?PY)?");
        public static final Pattern LABEL_ONLY = compileIgnoreCase("(?:FOB\\s*/\\s*CIF|F0[0O]\\s*/\\s*CIF|FREIGHT|INSURANCE|OTHER|TOTAL)");
        public static final Pattern ORPHAN_AMOUNT = compileIgnoreCase(Amounts.DEC_MONEY + "\\s*(?:J?PY)?");
        public static final Pattern FREIGHT_BL = compileIgnoreCase("B\\s*/\\s*L\\s*:\\s*FRT\\b[\\s\\S]{0,100}?JPY\\s*" + Amounts.DEC_MONEY);
        public static final Pattern TOTAL_INVOICED = compileIgnoreCase(
                "Currency\\s+And\\s+Total\\s+Amount\\s+Invoiced[\\s\\S]{0,120}?JPY\\s*" + Amounts.DEC_MONEY
        );
        public static final Pattern EXCHANGE_RATE = compileIgnoreCase("(?:23\\.?\\s*)?Exchange\\s+Rate\\s*[:\\-.]?\\s*(\\d+\\.\\d{2,6})");
        public static final Pattern VALUE_NCY = compileIgnoreCase("(?:46\\.?\\s*)?Value\\s*\\(?\\s*NCY\\s*\\)?\\s*[:\\-.]?\\s*" + Amounts.DEC_MONEY);
        public static final Pattern VALUE_NCY_NEAR = compileIgnoreCase("(?:46\\.?\\s*)?Value\\s*\\(?\\s*NCY\\s*\\)?[\\s\\S]{0,80}?" + Amounts.DEC_MONEY);
    }

    public static final class OceanBill {
        private OceanBill() {
        }

        public static final Pattern NO_LABELED = compileIgnoreCase(
                "(?:B\\s*/\\s*L|BL)\\s*(?:No\\.?|Number|#)\\s*[:.\\-]*\\s*([A-Z]{2,6}[A-Z0-9]{5,16})"
        );
        public static final Pattern NO_NYK = compileIgnoreCase("\\b(NYK[A-Z0-9]{6,16})\\b");
        public static final Pattern DATE_AFTER_PLACE_OF_ISSUE = compileIgnoreCase(
                "Place\\s+of\\s+B\\(?s?\\)?\\s*/?\\s*L\\s+Issue[\\s\\S]{0,160}?" + Dates.DAY_MONTH_YEAR
        );
        public static final Pattern DATE_AFTER_PLACE_OF_ISSUE_NUMERIC = compileIgnoreCase(
                "Place\\s+of\\s+B\\(?s?\\)?\\s*/?\\s*L\\s+Issue[\\s\\S]{0,160}?" + Dates.NUMERIC_DATE
        );
        public static final Pattern DATE_AFTER_DATED = compileIgnoreCase("\\bDated\\b\\s*[:.\\-]*\\s*" + Dates.DAY_MONTH_YEAR);
        public static final Pattern DATE_OF_BL_ISSUE = compileIgnoreCase(
                "Date\\s+of\\s+B\\(?s?\\)?\\s*/?\\s*L\\s+Issue\\s*[:.\\-]*\\s*(?:"
                        + Dates.DAY_MONTH_YEAR + "|" + Dates.NUMERIC_DATE + ")"
        );
    }

    public static final class PreShipment {
        private PreShipment() {
        }

        public static final Pattern PAREN_NUMBERED = compileIgnoreCase("^\\(\\s*(\\d{1,2})\\s*\\)\\s*(.+)$");
        public static final Pattern NUMBERED = compileIgnoreCase("^(\\d{1,2})\\s+(.+)$");
        public static final Pattern INLINE_NUMBERED = compileIgnoreCase("\\(\\s*(\\d{1,2})\\s*\\)\\s*([^:\\n]+?)\\s*:\\s*([^\\n]+)");
        public static final String PAREN_NUMBERED_LINE = "(?i)^\\(\\s*\\d{1,2}\\s*\\).*";
        public static final Pattern REFERENCE = compile("\\b(\\d{6}[A-Z])\\b");
        public static final Pattern BV_NUMBER = compileIgnoreCase("\\b(SRL\\d{4,}\\s*-\\s*\\d{2,})\\b");
        public static final Pattern HEADER_DATE = compileIgnoreCaseMultiline("(?:^|\\n)\\s*Date\\s*[:\\.]?\\s*(\\d{1,2}-[A-Za-z]{3}-\\d{2,4})\\b");
        public static final Pattern CERTIFICATE_DATE = compileIgnoreCase(
                "PRE[-\\s]?SHIPMENT INSPECTION CERTIFICATE[^\\n]{0,80}?\\b(\\d{1,2}-[A-Za-z]{3}-\\d{2,4})\\b"
        );
        public static final Pattern PAGE_SLASH = compileIgnoreCase("Page\\s+(\\d+\\s*/\\s*\\d+)");
        public static final Pattern PAGE_OF = compileIgnoreCase("Page\\s+(\\d+\\s+of\\s+\\d+)");
        public static final Pattern VEHICLE_FIELD_LABEL = compileIgnoreCase(
                "^(?:\\d{1,2}(?![0-9])[\\t\\s\\.]+)?(?:Type of vehicle|Make|Model|Commonly called|Manufacture Grade|"
                        + "Auction Grade|Body colou?r|Fuel type|Year/month of first registration|Inspection mileage|"
                        + "Engine capacity|Chassis No|Engine No|Driving system|Marks of accident|Condition of chassis|"
                        + "Full Model No|Year of Manufacture)\\b"
        );
        public static final Pattern ADDRESS_LABEL_ONLY = compileIgnoreCaseMultiline("\\(\\s*b\\s*\\)\\s*Address\\s*[:\\.]?\\s*$");
        public static final Pattern ADDRESS_MARKER = compileIgnoreCaseDotAll("\\(\\s*b\\s*\\)\\s*Address\\s*[:\\.]?\\s*(.*)");
        public static final Pattern LETTERED_TAIL = compileIgnoreCase("\\s*\\([c-z]\\)\\s*.*$");
        public static final Pattern CONTACT_SUFFIX = compileIgnoreCase("\\s+(?:Tel\\.?\\s*No\\.?|Tel|Fax\\.?\\s*No\\.?|Fax|Email|No\\.)\\b.*$");
        public static final Pattern NAME_INLINE = compileIgnoreCaseDotAll(
                "\\(\\s*a\\s*\\)\\s*Name\\b\\s*[:\\.]?\\s*(.+?)(?=\\s*\\([a-z]\\)\\s*\\w|\\s*(?:Tel|Fax|Email|No\\.)\\b|$)"
        );
        public static final Pattern SUBFIELD_NAME_ADDRESS = compileIgnoreCase("^\\([a-z]\\)\\s*(Name|Address)\\b");
        public static final Pattern LETTERED_CONTINUATION = compileIgnoreCase("^\\([c-z]\\)\\b");
        public static final Pattern EMAIL = compileIgnoreCase("Email\\s*[:\\.]?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
        public static final Pattern PHONE_NO = compile("No\\.\\s*\\+?[0-9]");
        public static final String VEHICLE_SECTION_HEAD =
                "(?is)^\\s*(?:3\\.\\s*)?(?:Particulars of Second[- ]Hand Motor Vehicle|PARTICULARS OF SECOND[- ]HAND MOTOR VEHICLE)\\s*";
        public static final String ATTRIBUTE_HEADER =
                "(?im)^\\s*(?:No\\.?\\s*)?(?:Exact\\s+Attribute|Attribute)\\s+Value\\s*\\n?";
        public static final String TEL =
                "(?:Tel\\.?\\s*No\\.?|Tel)\\s*[:\\.]?\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+(?:Fax|Email)\\b|\\s*$)";
        public static final String TEL_NO =
                "(?<![A-Za-z])No\\.\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+Fax\\s+No\\.?\\b|\\s+Fax\\b|\\s+Email\\b|\\s*$)";
        public static final String FAX_NO =
                "Fax\\.?\\s*No\\.?\\s*[:\\.]?\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+Email\\b|\\s*$)";
        public static final String FAX =
                "Fax\\s*[:\\.]?\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+Email\\b|\\s*$)";

        public static Pattern numberedFieldValue(int number, String label) {
            return compileIgnoreCase("(?:^|\\n)\\s*(?:\\(\\s*" + number + "\\s*\\)|"
                    + number + "(?![0-9])(?:\\.|\\s+|\\t+))\\s*"
                    + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*[:\\.]?\\s*([^\\n]+)");
        }

        public static Pattern parenthesizedNumberFieldValue(int number, String label) {
            return compileIgnoreCase("\\(\\s*" + number + "\\s*\\)\\s*"
                    + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*:\\s*([^\\n]+)");
        }

        public static Pattern parenthesizedNumberLabelAlone(int number, String label) {
            return compileIgnoreCaseMultiline("\\(\\s*" + number + "\\s*\\)\\s*"
                    + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*:\\s*$");
        }

        public static Pattern numberedTableCellValue(int number, String label) {
            return compileIgnoreCase("(?:^|\\n)\\s*" + number + "(?![0-9])[\\t\\s]+"
                    + Pattern.quote(label) + "(?:\\([^)]*\\))?\\.?[\\t\\s]+(.+?)\\s*(?:\\n|$)");
        }

        public static Pattern attributeRowValue(String numberPrefix, String label) {
            return compileIgnoreCase("(?:^|\\n)\\s*" + numberPrefix
                    + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\.?\\s+[\\t:]*\\s*(.+?)\\s*(?:\\n|$)");
        }

        public static String optionalLeadingRowNumber(int number) {
            return "(?:" + number + "(?![0-9])[\\t\\s]+)?";
        }

        public static Pattern numberedLabelAlone(int number, String label) {
            return compileIgnoreCaseMultiline("(?:^|\\n)\\s*(?:"
                    + number + "(?![0-9])[\\t\\s\\.]+)?"
                    + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*[:\\.]?\\s*$");
        }

        public static Pattern letterAValueUntilNextMarker(String label) {
            return compileIgnoreCaseDotAll("\\(\\s*a\\s*\\)\\s*" + Pattern.quote(label)
                    + "\\s*[:\\.]?\\s*(.+?)(?=\\s*\\([a-z]\\)\\s*\\w|\\s*(?:Tel|Fax|Email|No\\.)\\b|$)");
        }

        public static Pattern letterAValueOnSameLine(String label) {
            return compileIgnoreCase("\\(\\s*a\\s*\\)\\s*" + Pattern.quote(label) + "\\s*[:\\.]?\\s*([^\\n]+)");
        }

        public static Pattern letterALabelAlone(String label) {
            return compileIgnoreCaseMultiline("\\(\\s*a\\s*\\)\\s*" + Pattern.quote(label) + "\\s*[:\\.]?\\s*$");
        }
    }

    public static final class Javic {
        private Javic() {
        }

        public static final Pattern CERT_NO_LABELED = compileIgnoreCase(
                "Certificate\\s+No\\.?\\s*[:\\.]?\\s*(?:Date\\s+of\\s+Issue\\s*[:\\.]?\\s*)*(LK1-[A-Z0-9]+)"
        );
        public static final Pattern CERT_NO_NEXT = compileIgnoreCase("Certificate\\s+No\\.?\\s*[:\\.]?\\s*\\n\\s*(LK1-[A-Z0-9]+)");
        public static final Pattern CERT_NO = compileIgnoreCase("\\b(LK1-[A-Z0-9]+)\\b");
        public static final Pattern ISSUE_AFTER_CERT = compileIgnoreCase("LK1-[A-Z0-9]+\\s+" + Dates.SLASH_DATE);
        public static final Pattern ISSUE_AFTER_CERT_NEXT = compileIgnoreCase("LK1-[A-Z0-9]+\\s*\\n\\s*" + Dates.SLASH_DATE);
        public static final Pattern LOCATION = compileIgnoreCase(
                "\\bLocation\\s*:\\s*(?:Certificate\\s+No\\.?\\s*:\\s*)*(?:Date\\s+of\\s+Issue\\s*:\\s*)*"
                        + "([A-Za-z][A-Za-z0-9 \\-/]+?)"
                        + "(?=\\s*(?:Certificate\\s+No|Date\\s+of\\s+Issue|Current\\s+Odometer|LK1-|\\n|$))"
        );
        public static final Pattern LOCATION_NEXT = compileIgnoreCase(
                "\\bLocation\\s*:\\s*\\n\\s*([A-Za-z][A-Za-z0-9 \\-/]+)"
                        + "(?=\\s*(?:\\n|Certificate|Date\\s+of\\s+Issue|LK1-))"
        );
        public static final Pattern LOCATION_BEFORE_CERT = compileIgnoreCase("([A-Za-z][A-Za-z0-9 \\-/]{2,}?)\\s+LK1-[A-Z0-9]+");
        public static final Pattern LOCATION_BEFORE_CERT_NEXT = compileIgnoreCase("([A-Za-z][A-Za-z0-9 \\-/]{2,}?)\\s*\\n\\s*LK1-[A-Z0-9]+");
        public static final String ODOMETER = "(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)?)";
        public static final String ODOMETER_WORD = "(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)\\b)";
        public static final String ODOMETER_MATCH = "(?i)\\d+[\\d,]*\\s*(?:km|miles)?";
        public static final Pattern INSPECTED_MILEAGE = compileIgnoreCase(
                "Inspected\\s+Mileage(?:\\s*\\(\\s*Odometer\\s+Reading\\s*\\))?\\s*[:\\.]?\\s*" + ODOMETER
        );
        public static final Pattern INSPECTED_MILEAGE_NEXT = compileIgnoreCase(
                "Inspected\\s+Mileage(?:\\s*\\(\\s*Odometer\\s+Reading\\s*\\))?\\s*[:\\.]?\\s*\\n\\s*" + ODOMETER
        );
        public static final Pattern REMARKS = compileIgnoreCase("Remarks\\s*[:\\.]?\\s*([^\\n]+)");
        public static final Pattern REMARKS_NEXT = compileIgnoreCase("Remarks\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)");
        public static final Pattern CURRENT_ODOMETER_NEAR = compileIgnoreCase(
                "\\bCurrent\\s+Odometer\\s+Reading\\b[\\s\\S]{0,160}?" + ODOMETER_WORD
        );
        public static final Pattern CURRENT_ODOMETER = compileIgnoreCase(
                "\\bCurrent\\s+Odometer\\s+Reading\\b\\s*[:\\.]?\\s*"
                        + "(?:Auction\\s+Reading\\s*/\\s*Date\\s*[:\\.]?\\s*)*" + ODOMETER
        );
        public static final Pattern CURRENT_ODOMETER_NEXT = compileIgnoreCase(
                "\\bCurrent\\s+Odometer\\s+Reading\\b\\s*[:\\.]?\\s*\\n\\s*" + ODOMETER
        );
        public static final Pattern CURRENT_ODOMETER_SKIP_NEXT = compileIgnoreCase(
                "\\bCurrent\\s+Odometer\\s+Reading\\b\\s*[:\\.]?\\s*(?:Auction\\s+Reading\\s*/\\s*Date\\s*[:\\.]?\\s*)*\\n\\s*"
                        + ODOMETER
        );

        public static Pattern slashDateAfterLabel(String labelPattern, String stopLabels) {
            return compileIgnoreCase(labelPattern + "\\s*[:\\.]?\\s*(?:" + stopLabels + "\\s*[:\\.]?\\s*)*" + Dates.SLASH_DATE);
        }

        public static Pattern slashDateOnNextLineAfterLabel(String labelPattern) {
            return compileIgnoreCase(labelPattern + "\\s*[:\\.]?\\s*\\n\\s*" + Dates.SLASH_DATE);
        }

        public static Pattern odometerRowDateValue(String labelPattern) {
            return compileIgnoreCase("(?:^|\\n)\\s*" + labelPattern + "\\s*/\\s*Date\\s*[:\\.]?\\s*([^\\n]{0,60})");
        }

        public static String replaceSpacesWithWhitespace(String label) {
            return label.replace(" ", Text.WHITESPACE);
        }

        public static final String DATE_OF_ISSUE = "Date\\s+of\\s+Issue";
        public static final String INSPECTION_BRANCH = "Inspection\\s+Branch";
        public static final String ENGINE_CAPACITY = "Engine\\s+Capacity";
        public static final String FIRST_REGISTRATION = "Year\\s+of\\s+First\\s+Registration";
        public static final String ENGINE_NUMBER = "Engine\\s+Number";
        public static final String INSPECTION_DATE = "Inspection\\s+Date";
        public static final String DATE_OF_INSPECTION = "Date\\s+of\\s+Inspection";
        public static final String CHASSIS_NUMBER = "Chassis\\s+Number";
        public static final String CHASSIS_VIN = "Chassis\\s*/\\s*VIN";
        public static final String CURRENT_ODOMETER_LABEL = "Current\\s+Odometer";
    }

    public static final class Standards {
        private Standards() {
        }

        public static final String MARK = "[\\u2713\\u2714\\u221A\\u2611xX]|\\[\\s*[xX\\u2713\\u2714]\\s*\\]";
        public static final String VALUE = "([0-9]+(?:\\.[0-9]+)?|N\\s*/\\s*A|N\\.?\\s*A\\.?|NA|-|—|–)";
        public static final Pattern MARK_PATTERN = compile(MARK);
        public static final Pattern REMARKS = compileIgnoreCase("Remarks\\s*[:\\.]?\\s*([^\\n]+)");
        public static final String CO = "\\bCO\\b(?!\\w)";
        public static final String NMHC = "\\bNMHC\\b";
        public static final String NOX = "(?<!\\+)\\bNO\\s*x\\b";
        public static final String PM = "\\bPM\\b";
        public static final String HC_NOX = "\\bHC\\s*\\+\\s*NO\\s*x\\b";
        public static final String HC = "(?<![A-Z])\\bHC\\b(?!\\s*\\+)";
        public static final String THC = "\\bTHC\\b";
        public static final String CH4 = "\\bCH\\s*4\\b";
        public static final String SMOKE = "\\bSmoke\\b";
        public static final String THREE_POINT_BELTS = "Three\\s+point\\s+seat\\s+belts(?:\\s+for\\s+driver\\s+and\\s+front\\s+passengers)?";
        public static final String TWO_POINT_BELTS = "Minimum\\s+two\\s+point\\s+seat\\s+belts(?:\\s+for\\s+other\\s+passengers)?";
        public static final String DRIVER_AIRBAG_MARK =
                "Air\\s*Bags?\\s*[:\\-]?\\s*(?:(?:" + MARK + ")\\s*)?(?:Driver)";
        public static final String DRIVER_AIRBAG = "Driver(?:'s)?\\s+Air\\s*Bag";
        public static final String FRONT_PASSENGER = "Front\\s+Passenger";
        public static final String PASSENGER_AIRBAG = "Passenger(?:'s)?\\s+Air\\s*Bag";
        public static final String ABS = "\\bABS\\b";
        public static final String CHASSIS_NO = "Chassis No\\.?";

        public static Pattern markedSchedule(String schedule) {
            return compileIgnoreCase("(?:" + MARK + "\\s*)?Schedule\\s+" + schedule + "(?:\\s*" + MARK + ")?");
        }

        public static Pattern emissionValueAfterLabel(String label) {
            return compileIgnoreCase(label + "\\s*[:\\-]?\\s*" + VALUE + "(?:\\s*g\\s*/\\s*km)?");
        }

        public static Pattern checkmarkNearLabel(String label) {
            return compileIgnoreCase("(?:" + label + ")[^\\n]{0,48}|(?:" + MARK + ")\\s*(?:" + label + ")");
        }
    }

    public static final class Export {
        private Export() {
        }

        public static final String MEASURE = "([0-9]+(?:\\.[0-9]+)?|-|—|–)";
        public static final Pattern REMARKS = compileIgnoreCase("(?:Remarks|備考)\\s*[:\\.]?\\s*([^\\n]+)");
        public static final String CERTIFICATE_NO = "Certificate\\s+No\\.?";
        public static final String JP_NUMBER = "(?:^|\\n)\\s*番号";
        public static final String ARRANGEMENT_NO = "Arrangement\\s+No\\.?";
        public static final String REGISTRATION_NO = "Registration\\s+No\\.?";
        public static final String MOTOR_VEHICLE_NUMBER = "Motor\\s+vehicle\\s+number";
        public static final String FIRST_REG_DATE = "First\\s+Reg(?:istration)?\\.?\\s+Date";
        public static final String CHASSIS_NO = "Chassis\\s+No\\.?";
        public static final String MODEL = "(?<!Engine\\s)Model";
        public static final String MODEL_JP = "(?<!原動機の)型式(?!指定)";
        public static final String USE = "(?:^|\\n)\\s*Use\\b";
        public static final String MAX_CARRY = "Maxim(?:um)?\\.?\\s*Carry";
        public static final String WEIGHT =
                "(?<!FF\\s)(?<!FR\\s)(?<!RF\\s)(?<!RR\\s)(?<!G/)\\bWeight\\b";
        public static final String G_WEIGHT = "G\\s*/\\s*Weight";
        public static final String GC_WEIGHT = "G\\.?C\\.?\\s*Weight";
        public static final String SPECIFICATION_NO = "Specification\\s+No\\.?";
        public static final String CLASSIFICATION_NO = "Classification\\s+No\\.?";
        public static final String FF_WEIGHT = "FF\\s+Weight";
        public static final String F_WEIGHT = "F\\s+Weight";
        public static final String RR_WEIGHT = "RR\\s+Weight";
        public static final String R_WEIGHT = "R\\s+Weight";
        public static final String FR_WEIGHT = "FR\\s+Weight";
        public static final String RF_WEIGHT = "RF\\s+Weight";

        public static Pattern measurementAfterLabel(String label) {
            return compileIgnoreCase(label + "\\s*[:\\.]?\\s*" + MEASURE + "(?:\\s*(?:kg|cm|L|KW/?L|kW/?L|人|Person))?");
        }
    }

    public static final class Equipment {
        private Equipment() {
        }

        public static final String VALUE_GROUP = "(YES|NO|OK|N\\s*/\\s*A|N\\.?\\s*A\\.?|NA|NIL|NONE|NOT\\s+APPLICABLE"
                + "|SINGLE|DUAL|AUTO|MANUAL|PLASTIC|STEEL|OTHER|HARD|SOFT|PASS|NORMAL|\\d{1,2})";
        public static final Pattern VALUE_AFTER_LABEL = compileIgnoreCase("\\s*[:\\-]?\\s*(" + VALUE_GROUP + ")\\b");
    }

    public static final class Worksheet {
        private Worksheet() {
        }

        public static final Pattern REF = compile("\\b([A-Z]\\d[-\\s]\\d{5,}[-\\s][A-Z]{2})\\b");
        public static final Pattern AGENTS_FOB_CALC = compileIgnoreCase(
                "(?:/\\s*110\\s*[Xx×]\\s*100\\s*=\\s*)" + Amounts.GROUPED_COMMA
        );
        public static final Pattern AGENTS_FOB_LINE = compileIgnoreCase("Agents?\\s+FOB\\s*[:\\.]?\\s*([^\\n]+)");
        public static final Pattern BL_FREIGHT = compileIgnoreCase("B/?L\\s*Freight\\s*Calculation\\s*[:\\.]?\\s*([^\\n]+)");
        public static final Pattern BL_FREIGHT_NEXT = compileIgnoreCase("B/?L\\s*Freight\\s*Calculation\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)");
        public static final Pattern BL_FREIGHT_USD = compileIgnoreCase(
                "(USD\\s+[\\d,.]+\\s*@\\s*[\\d.]+\\s*=\\s*JPY\\s*@\\s*[\\d.]+\\s*=\\s*[\\d,.]+)"
        );
        public static final Pattern AGE_DIFFERENCE = compileIgnoreCase(
                "Age\\s+Difference(?:\\s+for\\s+I\\.?C\\.?L)?\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d+)\\s+(\\d+)\\s+(\\d+)"
        );
        public static final Pattern LC_NO_TOKEN = compileIgnoreCase("([A-Z]{5,}\\d{8,})");
        public static final Pattern LC_NO_WORD = compile("\\b([A-Z]{5,}\\d{8,})\\b");
        public static final Pattern LC_AMOUNT = compileIgnoreCaseDotAll(
                "LC\\s*No\\.?.{0,120}?Amount\\s*[:\\.]?\\s*(?:\\n\\s*)?" + Amounts.GROUPED_COMMA
        );
        public static final Pattern BANK_LINE = compileIgnoreCase("(?:^|\\n)\\s*Bank\\s*[:\\.]?\\s*([^\\n]+)");
        public static final Pattern BANK_NEXT = compileIgnoreCase("(?:^|\\n)\\s*Bank\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)");
        public static final Pattern BANK_NAME = compile("\\b([A-Z][A-Z ]{2,}BANK[A-Z ]*(?:LIMITED|LTD\\.?|PLC)?)\\b");
        public static final Pattern IMPORTER_TRADING = compile("\\b([A-Z][A-Z ]{3,}(?:MOTOR\\s+)?TRADING(?:\\s+COMPANY)?)\\b");
        public static final Pattern IMPORTER_MOTORS = compile("\\b([A-Z][A-Z ]{6,}(?:MOTORS|COMPANY|PVT)[A-Z ]*)\\b");
        public static final Pattern CLEARING = compile("\\b([A-Z][A-Z0-9 &./]{2,}CLEARING[A-Z0-9 &./]*)\\b");
        public static final Pattern UNIT_USED = compileIgnoreCase("(0?1\\s+UNIT\\s+USED\\s+[^\\n]+)");
        public static final Pattern FIFTEEN_PERCENT = compileIgnoreCase("15\\s*%\\s*of\\s+Value[^\\n]{0,40}?" + Amounts.GROUPED_COMMA);
        public static final Pattern FIFTEEN_PERCENT_NEXT = compileIgnoreCase(
                "15\\s*%\\s*of\\s+Value\\s*[:\\.]?\\s*\\n\\s*" + Amounts.GROUPED_COMMA
        );
        public static final Pattern FOB_85 = compileIgnoreCase(
                "FOB\\s+Value\\s*\\(?\\s*85\\s*%?\\s*\\)?[^\\n]{0,30}?" + Amounts.GROUPED_COMMA
        );
        public static final Pattern FOB_85_NEXT = compileIgnoreCase(
                "FOB\\s+Value\\s*\\(?\\s*85\\s*%?\\s*\\)?\\s*[:\\.]?\\s*\\n\\s*(?:JPY\\s*)?" + Amounts.GROUPED_COMMA
        );

        public static Pattern amountNearQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "[^\\n]{0,80}?" + Amounts.GROUPED_COMMA);
        }

        public static Pattern amountOnNextLineAfterQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "\\s*[:\\.]?\\s*\\n\\s*(?:JPY\\s*)?" + Amounts.GROUPED_COMMA);
        }

        public static Pattern yearMonthDayAfterQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d{4})\\s+(\\d{1,2})\\s+(\\d{1,2})");
        }

        public static Pattern slashDateAfterQuotedLabel(String label) {
            return compileIgnoreCase(Pattern.quote(label) + "\\s*[:\\.]?\\s*(?:\\n\\s*)?" + Dates.SLASH_DATE);
        }
    }

    public static final class AuctionSheet {
        private AuctionSheet() {
        }

        public static final class Ocr {
            private Ocr() {
            }

            public static final String KM_THOUSANDS = "(\\d{1,3})[.,\\s](\\d{3})\\s*(km|kn|KM|キロ)";
            public static final String CC_THOUSANDS = "(\\d{1,3})[.,](\\d{3})\\s*(cc|CC|ｃｃ)";
            public static final String SAA_PREFIX = "(?<![A-Z0-9])SAA-";
            public static final String RISE_OCR = "車名[^\\n]{0,24}ライス";
            public static final String KN_SPACED = "(\\d{4,6})\\s*k\\s*n";
            public static final String KN = "(\\d{4,6})\\s*kn";
            public static final String REIWA_ERA = "(?:R|Ｒ)\\s*([1-8])\\s*[/.．\\-年]\\s*([1-9]|1[0-2])\\s*月?";
            public static final String HEISEI_SLASH = "(?:H|Ｈ)\\s*([1-3]?[0-9])\\s*[/.．\\-年]\\s*([1-9]|1[0-2])";
            public static final String HEISEI_SPACE = "(?:H|Ｈ)\\s*([1-3]?[0-9])\\s+([1-9]|1[0-2])";
            public static final String G_AS_REIWA = "初度登録([^\\n]{0,40}?)G\\s*([1-8])";
            public static final String LOT_NO = "出\\s*品\\s*番\\s*号";
            public static final String FIRST_REG = "初\\s*度\\s*登\\s*録";
            public static final String YEAR_MONTH = "年\\s*月";
            public static final String MILEAGE = "走\\s+行";
            public static final String MODEL = "型\\s+式";
            public static final String DISPLACEMENT = "排\\s*気\\s*量";
            public static final String GRADE = "グレ\\s*ード";
            public static final String ROOMY = "ルー\\s*ミー";
            public static final String EVAL_POINT = "評\\s*価\\s*点";
            public static final String EVAL = "評\\s*点";
            public static final String INTERIOR = "内\\s*装";
            public static final String EXTERIOR = "外\\s*装(?!色)";
            public static final String CHASSIS_NO = "車\\s*台\\s*番\\s*号";
            public static final String DOOR_SHAPE = "ド[アア]\\s*形\\s*状";
        }

        public static final class Grade {
            private Grade() {
            }

            public static final Pattern TOGETHER = compile("(?<![A-Za-z0-9])([S6])\\s+([A-E])(?![A-Za-z0-9])");
            public static final Pattern STANDALONE_LINE = compile("(?m)^\\s*[\\[\\(（・*]*\\s*([S6*]|[1-6](?:\\.5)?)\\s*[\\]\\)）点]*\\s*$");
            public static final Pattern LABELED = compileIgnoreCase(
                    "(?:評価|評点|点数|総合|evaluation|rating|score|auction\\s+grade|overall)[^A-Za-z0-9]{0,40}([S6*]|[1-6](?:\\.5)?)"
            );
            public static final Pattern TOKEN_S = compile("(?<![A-Za-z0-9])([S*])(?![A-Za-z0-9])");
            public static final Pattern PANEL_LINE = compile("(?m)^\\s*[\\[\\(（]*\\s*([A-Ea-e])\\s*[\\]\\)）]*\\s*$");
            public static final Pattern PANEL_TOKEN = compile("(?<![A-Za-z0-9])([A-E])(?![A-Za-z0-9])");
            public static final Pattern EVALUATION = compileIgnoreCase(
                    "(?:評価点|評点|総合評価|評価|evaluation|rating|score|auction\\s+grade|overall(?:\\s+grade)?)[^A-Za-z0-9]{0,40}([SRA-E6]|[1-6](?:\\.\\d)?)"
            );
            public static final Pattern EVAL_VALUE = compile("^([SRA-E]|[0-6](?:\\.\\d)?)");
            public static final Pattern G_GRADE = compile("\\bG\\s+([1-6])\\b");
            public static final Pattern PAIR = compile("\\b([A-Ea-e])\\s*[/|lI]\\s*([A-Ea-e])\\b");
            public static final String PANEL_TOKEN_MATCH = "(?i)[A-ES*]|[1-6](?:\\.5)?";
            public static final String GRADE_TOKEN = "(?i)[SRA-E]|[0-6](?:\\.\\d)?";
            public static final String TRANSMISSION = "(?i)CVT|IAT|F\\.AT|FAT|AT|MT";
            public static final Pattern AT = compile("\\bAT\\b");
            public static final Pattern AC_LABELED = compile("(?:エアコン|冷房|AIR\\s*CON(?:DITIONER)?)[^A-Za-z]{0,12}(AAC|AC)");
            public static final Pattern AC = compile("\\bAC\\b");
            public static final String RENTAL = "(?i).*rental.*";
            public static final String PRIVATE = "(?i).*private.*";
            public static final String COMMERCIAL = "(?i).*commercial.*";
            public static final String HISTORY = "(?i)rental|private|commercial";
            public static final String SPEC_TOKEN = "IAT|AAC|AT|MT|CVT|FAT|PS|PW|ABS|ETC|AC|KM|CC";
            public static final String NOT_COLOR = "(?!色)(?!(?:erior)?\\s*colou?r)";

            public static Pattern gradeLetterAfterLabel(String label) {
                return compileIgnoreCase(label + "[^A-Ea-e]{0,40}(?<![A-Z0-9])([A-Ea-e])(?![A-Za-z0-9])");
            }

            public static Pattern gradeLetterOnNextLineAfterLabel(String label) {
                return compileIgnoreCase(label + "[^\\n]{0,12}\\n\\s*([A-Ea-e])(?![A-Za-z0-9])");
            }
        }

        public static final class Identity {
            private Identity() {
            }

            public static final Pattern ENGLISH_REG = compileIgnoreCase(
                    "(?:first\\s+registration|registration(?:\\s+date)?|year(?:\\s+of)?\\s+registration)[^\\n]{0,48}"
                            + "(" + Dates.MONTH_ANY + ")[.]?\\s+((?:19|20)\\d{2})"
            );
            public static final Pattern ENGLISH_MONTH_YEAR = compileIgnoreCase("(" + Dates.MONTH_ANY + ")[.]?\\s+((?:19|20)\\d{2})");
            public static final Pattern FIRST_REG_LABEL = compileIgnoreCase("初度登録(?:年月)?|first\\s+registration|registration\\s+date");
            public static final Pattern MODEL_YEAR_LABEL = compileIgnoreCase("年式|model\\s+year");
            public static final Pattern LOT_ERA = compileIgnoreCase(
                    "(?:(?:出品番号|(?<![A-Za-z])lot(?:\\s*(?:no\\.?|number))?)[^\\d]{0,16})?(\\d{3,6})[^\\n]{0,40}?((?:R|Ｒ|H|Ｈ|G)\\s*\\d{1,2}\\s+\\d{1,2})"
            );
            public static final Pattern REIWA = compile("(?:令和|(?:^|[^A-Z])[RＲ])\\s*(\\d{1,2}|元)\\s*(?:年)?\\s*([1-9]|1[0-2])");
            public static final Pattern HEISEI = compile("(?:平成|(?:^|[^A-Z])[HＨ])\\s*(\\d{1,2})\\s*(?:年)?\\s*([1-9]|1[0-2])");
            public static final Pattern G_AS_R = compile("(?:^|[^A-Z])G\\s*([1-8])\\s+([1-9]|1[0-2])");
            public static final Pattern FIRST_REG_NUMBERS = compile("初度登録(?:年月)?[^\\d]{0,20}(\\d{1,2})[^\\d]{1,8}([1-9]|1[0-2])");
            public static final Pattern CHASSIS_DIGITS = compile("(\\d{6,8})");
            public static final Pattern CHASSIS_LABELED_DIGITS = compileIgnoreCase(
                    "(?:車台番号|chassis(?:\\s+(?:no\\.?|number))?|frame(?:\\s+number)?|vin)[^\\d]{0,24}(\\d{6,8})"
            );
            public static final Pattern LOT_LABELED = compileIgnoreCase(
                    "(?:出品番号|(?<![A-Za-z])lot(?:\\s*(?:no\\.?|number|#))?|exhibition\\s+number|listing\\s+number)[^\\d]{0,20}(\\d{3,6})"
            );
            public static final Pattern LOT_DIGITS = compile("(\\d{3,6})");
            public static final Pattern LOT_LINE = compile("(?m)^\\s*(\\d{4,6})(?!\\d)");
            public static final Pattern LOT_LOOSE = compile("(?<!\\d)(\\d{4,6})(?!\\d)(?!\\s*(km|kn|㎞|KM|cc))");
            public static final Pattern GRADE_NAME = compileIgnoreCase(
                    "(?:グレード|(?<!auction\\s)(?<!overall\\s)(?<!evaluation\\s)(?<!interior\\s)(?<!exterior\\s)grade)[^A-Za-z0-9ァ-ヶ一-龥]{0,12}([^\\n]{1,40})"
            );

            public static Pattern valueAfterQuotedKey(String quoted) {
                return compileIgnoreCase(quoted + "[^\\nA-Za-z0-9ァ-ヶ一-龥]{0,16}([^\\n]{0,60})");
            }
        }

        public static final class Specs {
            private Specs() {
            }

            public static final String KM_UNIT = "km|kn|㎞|KM|キロ";
            public static final Pattern MILEAGE_LABELED_UNIT = compileIgnoreCase(
                    "(?:走行|mileage|odometer|travel(?:led|ed)?(?:\\s+distance)?)[^\\d]{0,20}(\\d{1,3}(?:,\\d{3}|\\.\\d{3})+|\\d{1,6})\\s*("
                            + KM_UNIT + "|kilometers?|kilometres?)"
            );
            public static final Pattern MILEAGE_LABELED = compileIgnoreCase(
                    "(?:走行|mileage|odometer|travel(?:led|ed)?(?:\\s+distance)?)[^\\d]{0,20}(\\d{1,3}(?:,\\d{3}|\\.\\d{3})+|\\d{1,6})"
            );
            public static final Pattern KM_DOTTED = compile("(\\d{1,3}[.,]\\d{3})\\s*(" + KM_UNIT + ")");
            public static final Pattern KM_SPACED = compile("(\\d{1,3})\\s+(\\d{3})\\s*(" + KM_UNIT + ")");
            public static final Pattern KM_PLAIN = compile("(\\d{1,6})\\s*(" + KM_UNIT + ")");
            public static final Pattern CC_LABELED_COMMA = compileIgnoreCase(
                    "(?:排気量|displacement|engine\\s+(?:size|capacity|displacement))[^\\d]{0,16}(\\d{1,3})[,.](\\d{3})"
            );
            public static final Pattern CC_LABELED = compileIgnoreCase(
                    "(?:排気量|displacement|engine\\s+(?:size|capacity|displacement))[^\\d]{0,16}(\\d{3,4})"
            );
            public static final Pattern CC_COMMA = compile("(\\d{1,3})[,.](\\d{3})\\s*(cc|CC|ｃｃ)");
            public static final Pattern CC = compile("(\\d{3,4})\\s*(cc|CC|ｃｃ)");
            public static final String ENGINE_SIZES = "660|1000|1200|1300|1500|1800|2000|2400|2500|3000";
            public static final Pattern DOORS_ANY = compileIgnoreCase("([2-5])\\s*(?:SD|HB|ドア|ハコ|doors?)|(?<![A-Z0-9])([2-5])\\s*W(?![A-Z0-9])|([2-5])\\s*ハコ");
            public static final Pattern DOORS_LABELED = compileIgnoreCase("(?:ドア(?:形状|・形状)?|doors?|door\\s+shape)[^\\d]{0,8}([2-5])");
            public static final Pattern SEATS_LABELED = compileIgnoreCase("(?:乗車定員|seating\\s+capacity|passengers?)[^\\d]{0,12}(\\d)");
            public static final Pattern SEATS_PEOPLE = compileIgnoreCase("(\\d)\\s*(人|名|people|persons?|seats?)");
            public static final Pattern DIMENSIONS = compile("(\\d{3})\\s*[×xX]\\s*(\\d{3})\\s*[×xX]\\s*(\\d{3})");
            public static final Pattern DIMENSIONS_LABELED = compileIgnoreCase(
                    "(?:諸元|spec(?:ification)?s?)[^\\d]{0,24}(\\d{3})[^\\d]{1,12}(\\d{3})[^\\d]{1,12}(\\d{3})"
            );
            public static final Pattern LENGTH = compileIgnoreCase("(?:長さ|length)[^\\d]{0,8}(\\d{3})");
            public static final Pattern WIDTH = compileIgnoreCase("(?:幅|width)[^\\d]{0,8}(\\d{3})");
            public static final Pattern HEIGHT = compileIgnoreCase("(?:高さ|height)[^\\d]{0,8}(\\d{3})");
            public static final Pattern DIMENSIONS_LOOSE = compile("(\\d{3})[^\\d]{1,24}(\\d{3})[^\\d]{1,24}(\\d{3})");
        }

        public static final class Color {
            private Color() {
            }

            public static final Pattern NAME_LABEL = compile(
                    "(?i)(?:外装色|(?<!interior\\s)(?<!inner\\s)(?:exterior\\s+|outer\\s+|body\\s+)?(?<![A-Za-z])colou?r(?![A-Za-z])(?:\\s*(?:name|code|no\\.?)?)?|カラー)"
            );
            public static final Pattern CODE_LABEL = compile(
                    "(?i)(?:外装色|色コード|カラー\\s*(?:NO\\.?|番号)?|(?<!interior\\s)(?<!inner\\s)(?:exterior\\s+|outer\\s+|body\\s+)?(?<![A-Za-z])colou?r(?![A-Za-z])(?:\\s*(?:code|no\\.?))?)"
            );
            public static final String STRIP_ROLE = "(?i)\\b(?:exterior|outer|body|interior)\\s+(?:colou?r\\s+)?";
            public static final String STRIP_WORD = "(?i)\\b(?:colou?r(?:\\s*(?:code|no\\.?))?|code)\\b";
            public static final String STRIP_CODE = "(?i)(?<![A-Z0-9])(?:[A-Z]\\d{2}|\\d{3}|\\d[A-Z]\\d)(?![A-Z0-9])";
            public static final String STRIP_CHASSIS = "[A-Za-z]{1,5}\\d{2,3}[A-Za-z0-9]{0,2}-\\d{5,8}";
            public static final String STRIP_MODEL = "(?i)\\b(?:[0-9][A-Z]{2}|[A-Z]{3})-[A-Z0-9]{3,10}\\b";
            public static final String WHITE = "W\\d{2}|070|040|058|W09|W24|W25";
            public static final String BLACK = "202|209|218|219";
            public static final String SILVER = "1F7|1G3|1G4|1C0";
            public static final String IGNORED = "AAC|IAT|ABS|ETC|USS|KCAA|DAA|DBA|CBA|5BA|5AA|6AA|3BA|PS|PW|AC";
        }
    }
}
