package com.carsale.erp.importpipeline.preshipment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Service
public class PreShipmentParser {

    private static final String[] ATTRIBUTE_ONLY_PREFIXES = new String[] {
            "Marks of accident on chassis (by visual check)",
            "Marks of accident on chassis",
            "Year/month of first registration",
            "Inspection mileage (odometer reading)",
            "Inspection mileage",
            "Chassis No. (original)",
            "Chassis No.",
            "Chassis No",
            "Commonly called (emblem reading)",
            "Commonly called",
            "Manufacture Grade (emblem reading)",
            "Manufacture Grade",
            "Condition of chassis",
            "Engine capacity",
            "Driving system",
            "Auction Grade",
            "Body colour",
            "Body color",
            "Fuel type",
            "Engine No.",
            "Engine No",
            "Type of vehicle",
            "Full Model No.",
            "Full Model No",
            "Year of Manufacture",
            "Make",
            "Model"
    };

    private static final int[] ATTRIBUTE_ONLY_NUMBERS = new int[] {
            15, 15, 9, 10, 10, 12, 12, 12, 4, 4, 5, 5, 16, 11, 14, 6, 7, 7, 8, 13, 13, 1, 0, 0, 0, 2, 3
    };

    public AuctionParseResult parse(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The pre-shipment document was empty.");
            return result;
        }

        String normalized = normalize(text);

        result.put("certificateReference", firstNonNull(
                extractLabelValue(normalized, "Document No"),
                extractReference(normalized)
        ));
        result.put("documentTitle", firstNonNull(
                extractLabelValue(normalized, "Document Title"),
                extractHeading(normalized, "PRE-SHIPMENT INSPECTION CERTIFICATE")
        ));
        result.put("bvNumber", extractBvNumber(normalized));
        result.put("certificateDate", firstNonNull(
                extractHeaderDate(normalized),
                extractCertificateDate(normalized)
        ));
        result.put("pageInfo", extractPageInfo(normalized));

        result.put("inspectionOrgName", extractInspectionOrgName(normalized));
        result.put("inspectionOrgAddress", extractInspectionOrgAddress(normalized));
        putSectionContacts(
                result,
                extractInspectionOrgSection(normalized),
                "inspectionOrgTel",
                "inspectionOrgFax",
                "inspectionOrgEmail"
        );
        result.put("placeOfInspection", extractLabelValue(normalized, "Place of Inspection"));
        result.put("dateOfInspection", extractLabelValue(normalized, "Date of Inspection"));

        result.put("applicantName", extractApplicantName(normalized));
        result.put("applicantAddress", extractApplicantAddress(normalized));
        putSectionContacts(
                result,
                extractApplicantSection(normalized),
                "applicantTel",
                "applicantFax",
                "applicantEmail"
        );

        String vehicleSection = extractVehicleSection(normalized);
        VehicleRowIndex vehicleRows = buildVehicleRowIndex(vehicleSection);

        result.put("vehicleType", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 1, "Type of vehicle"));
        result.put("make", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 2, "Make"));
        result.put("model", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 3, "Model"));
        result.put("commonName", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 4, "Commonly called"));
        result.put("manufactureGrade", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 5, "Manufacture Grade"));
        result.put("preshipAuctionGrade", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 6, "Auction Grade"));
        result.put("bodyColour", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 7, "Body colour", "Body color"));
        result.put("fuelType", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 8, "Fuel type"));
        result.put("firstRegistration", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 9, "Year/month of first registration"));
        result.put("inspectionMileage", firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 10, "Inspection mileage (odometer reading)"),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 10, "Inspection mileage")
        ));
        result.put("engineCapacity", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 11, "Engine capacity"));
        result.put("chassisNo", firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 12, "Chassis No. (original)"),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 12, "Chassis No")
        ));
        result.put("engineNo", firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 13, "Engine No."),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 13, "Engine No")
        ));
        result.put("drivingSystem", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 14, "Driving system"));
        result.put("accidentMarksOnChassis", firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 15, "Marks of accident on chassis (by visual check)"),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 15, "Marks of accident on chassis")
        ));
        result.put("chassisCondition", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 16, "Condition of chassis"));

        String remarksSection = vehicleSection != null ? vehicleSection : normalized;
        result.put("fullModelNo", firstNonNull(
                lookupVehicleValue(vehicleRows, remarksSection, normalized, 0, "Full Model No"),
                extractLabelValue(remarksSection, "Full Model No"),
                extractLabelValue(normalized, "Full Model No")
        ));
        result.put("yearOfManufacture", firstNonNull(
                lookupVehicleValue(vehicleRows, remarksSection, normalized, 0, "Year of Manufacture"),
                extractLabelValue(remarksSection, "Year of Manufacture"),
                extractLabelValue(normalized, "Year of Manufacture")
        ));

        boolean any = !result.getFields().isEmpty();
        result.setSuccess(any);
        result.setMessage(any
                ? "Filled " + result.getFields().size() + " fields from the pre-shipment certificate. Please review."
                : "The document was read, but fields could not be mapped. Please fill them manually.");
        return result;
    }

    private String normalize(String text) {
        return text.replace('\r', '\n')
                .replaceAll("[ ]+", " ")
                .replaceAll("\n{3,}", "\n\n");
    }

    private static final class VehicleRowIndex {
        private final Map<Integer, String> byNumber = new LinkedHashMap<Integer, String>();
        private final Map<String, String> byAttribute = new LinkedHashMap<String, String>();
    }

    private VehicleRowIndex buildVehicleRowIndex(String section) {
        VehicleRowIndex index = new VehicleRowIndex();
        if (section == null || section.trim().isEmpty()) {
            return index;
        }

        parseBvParenthesisRows(section, index);

        String[] lines = section.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || isTableHeaderLine(line) || isVehicleSectionBoundary(line)) {
                continue;
            }

            if (line.indexOf('\t') >= 0) {
                String[] columns = line.split("\\t", -1);
                if (columns.length >= 3) {
                    registerVehicleRow(index, parseRowNumber(columns[0]), columns[1], columns[2]);
                    continue;
                }
                if (columns.length == 2) {
                    registerVehicleRow(index, null, columns[0], columns[1]);
                    continue;
                }
            }

            Matcher parenNumbered = Pattern.compile(
                    "^\\(\\s*(\\d{1,2})\\s*\\)\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE
            ).matcher(line);
            if (parenNumbered.matches()) {
                Integer rowNum = parseRowNumber(parenNumbered.group(1));
                String rest = parenNumbered.group(2).trim();
                if (rest.endsWith(":") && i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    if (nextLine.length() > 0
                            && !isVehicleFieldLabelLine(nextLine)
                            && !isVehicleSectionBoundary(nextLine)
                            && !nextLine.matches("(?i)^\\(\\s*\\d{1,2}\\s*\\).*")) {
                        rest = rest + " " + nextLine;
                        i++;
                    }
                }
                if (matchAttributeValue(index, rowNum, rest)) {
                    continue;
                }
            }

            Matcher numbered = Pattern.compile(
                    "^(\\d{1,2})\\s+(.+)$",
                    Pattern.CASE_INSENSITIVE
            ).matcher(line);
            if (numbered.matches()) {
                if (matchAttributeValue(index, parseRowNumber(numbered.group(1)), numbered.group(2).trim())) {
                    continue;
                }
            }

            parseAttributeOnlyLine(index, line);
        }
        return index;
    }

    private void parseBvParenthesisRows(String section, VehicleRowIndex index) {
        Pattern row = Pattern.compile(
                "\\(\\s*(\\d{1,2})\\s*\\)\\s*([^:\\n]+?)\\s*:\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = row.matcher(section);
        while (matcher.find()) {
            registerVehicleRow(
                    index,
                    parseRowNumber(matcher.group(1)),
                    matcher.group(2),
                    matcher.group(3)
            );
        }
    }

    private boolean matchAttributeValue(VehicleRowIndex index, Integer number, String rest) {
        for (int i = 0; i < ATTRIBUTE_ONLY_PREFIXES.length; i++) {
            String prefix = ATTRIBUTE_ONLY_PREFIXES[i];
            if (rest.length() <= prefix.length()) {
                continue;
            }
            if (!rest.regionMatches(true, 0, prefix, 0, prefix.length())) {
                continue;
            }
            char next = rest.charAt(prefix.length());
            if (Character.isLetterOrDigit(next)) {
                continue;
            }

            String value = stripLeadingParenthetical(cleanValue(rest.substring(prefix.length())));
            if (value == null || value.length() == 0) {
                continue;
            }

            Integer mappedNumber = number;
            if (mappedNumber == null && ATTRIBUTE_ONLY_NUMBERS[i] > 0) {
                mappedNumber = Integer.valueOf(ATTRIBUTE_ONLY_NUMBERS[i]);
            }
            registerVehicleRow(index, mappedNumber, prefix, value);
            return true;
        }
        return false;
    }

    private void registerVehicleRow(VehicleRowIndex index, Integer number, String attribute, String value) {
        String cleanedValue = stripLeadingParenthetical(cleanValue(value));
        if (cleanedValue == null || cleanedValue.length() == 0) {
            return;
        }

        String cleanedAttribute = cleanValue(attribute);
        if (cleanedAttribute != null && cleanedAttribute.length() > 0 && !isTableHeaderLine(cleanedAttribute)) {
            index.byAttribute.put(normalizeVehicleAttribute(cleanedAttribute), cleanedValue);
        }
        if (number != null && number.intValue() >= 1 && number.intValue() <= 16) {
            index.byNumber.put(number, cleanedValue);
        }
    }

    private void parseAttributeOnlyLine(VehicleRowIndex index, String line) {
        matchAttributeValue(index, null, line);
    }

    private Integer parseRowNumber(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = cleanValue(value);
        if (trimmed == null || !trimmed.matches("\\d{1,2}")) {
            return null;
        }
        return Integer.valueOf(trimmed);
    }

    private String normalizeVehicleAttribute(String attribute) {
        if (attribute == null) {
            return "";
        }
        String normalized = attribute.trim().toLowerCase();
        normalized = normalized.replaceAll("\\([^)]*\\)", " ");
        normalized = normalized.replaceAll("\\.+$", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private String lookupVehicleValue(
            VehicleRowIndex index,
            String vehicleSection,
            String fullText,
            int number,
            String... labels
    ) {
        if (index != null) {
            if (number >= 1 && number <= 16) {
                String byNumber = index.byNumber.get(Integer.valueOf(number));
                if (byNumber != null && byNumber.length() > 0) {
                    return byNumber;
                }
            }
            for (int i = 0; i < labels.length; i++) {
                String byAttribute = index.byAttribute.get(normalizeVehicleAttribute(labels[i]));
                if (byAttribute != null && byAttribute.length() > 0) {
                    return byAttribute;
                }
            }
        }
        if (number >= 1 && number <= 16) {
            return extractVehicleField(vehicleSection, fullText, number, labels);
        }
        return firstNonNull(extractAttributeValueRow(vehicleSection, 0, labels[0]),
                extractAttributeValueRow(fullText, 0, labels[0]));
    }

    private boolean isTableHeaderLine(String line) {
        if (line == null) {
            return false;
        }
        String lower = line.trim().toLowerCase();
        return "no.".equals(lower)
                || "value".equals(lower)
                || "attribute".equals(lower)
                || "exact attribute".equals(lower)
                || lower.startsWith("exact attribute")
                || lower.startsWith("attribute value");
    }

    private String firstNonNull(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && values[i].trim().length() > 0) {
                return values[i];
            }
        }
        return null;
    }

    private String extractHeading(String text, String heading) {
        if (text.toUpperCase().contains(heading.toUpperCase())) {
            return heading;
        }
        return null;
    }

    private String extractReference(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d{6}[A-Z])\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractBvNumber(String text) {
        Matcher matcher = Pattern.compile("\\b(SRL\\d{4,}\\s*-\\s*\\d{2,})\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase().replaceAll("\\s+", "");
        }
        return null;
    }

    private String extractHeaderDate(String text) {
        Matcher matcher = Pattern.compile(
                "(?:^|\\n)\\s*Date\\s*[:\\.]?\\s*(\\d{1,2}-[A-Za-z]{3}-\\d{2,4})\\b",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractCertificateDate(String text) {
        Matcher matcher = Pattern.compile(
                "PRE[-\\s]?SHIPMENT INSPECTION CERTIFICATE[^\\n]{0,80}?\\b(\\d{1,2}-[A-Za-z]{3}-\\d{2,4})\\b",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher top = Pattern.compile("\\b(\\d{1,2}-[A-Za-z]{3}-\\d{2})\\b").matcher(text);
        if (top.find()) {
            return top.group(1);
        }
        return null;
    }

    private String extractPageInfo(String text) {
        Matcher slash = Pattern.compile("Page\\s+(\\d+\\s*/\\s*\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (slash.find()) {
            return slash.group(1).replaceAll("\\s+", "");
        }
        Matcher matcher = Pattern.compile("Page\\s+(\\d+\\s+of\\s+\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractNumbered(String text, int number, String... labels) {
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            Pattern pattern = Pattern.compile(
                    "(?:^|\\n)\\s*(?:\\(\\s*" + number + "\\s*\\)|"
                            + number + "(?![0-9])(?:\\.|\\s+|\\t+))\\s*"
                            + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*[:\\.]?\\s*([^\\n]+)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return stripLeadingParenthetical(cleanValue(matcher.group(1)));
            }
        }
        return null;
    }

    private String extractBvNumberedField(String text, int number, String label) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile(
                "\\(\\s*" + number + "\\s*\\)\\s*"
                        + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*:\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return stripLeadingParenthetical(cleanValue(matcher.group(1)));
        }
        Pattern splitLabel = Pattern.compile(
                "\\(\\s*" + number + "\\s*\\)\\s*"
                        + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*:\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher labelMatcher = splitLabel.matcher(text);
        if (labelMatcher.find()) {
            return extractLabelValueNextLine(text.substring(labelMatcher.start()), number, label);
        }
        return null;
    }

    private String extractVehicleSection(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String[] startMarkers = new String[] {
                "3. PARTICULARS OF SECOND-HAND MOTOR VEHICLE",
                "3. Particulars of Second-Hand Motor Vehicle",
                "Particulars of Second-Hand Motor Vehicle",
                "Particulars of Second Hand Motor Vehicle",
                "PARTICULARS OF SECOND-HAND MOTOR VEHICLE"
        };

        int start = -1;
        int markerLength = 0;
        for (int i = 0; i < startMarkers.length; i++) {
            int index = indexOfIgnoreCase(text, startMarkers[i]);
            if (index >= 0 && (start < 0 || index < start)) {
                start = index;
                markerLength = startMarkers[i].length();
            }
        }
        if (start < 0) {
            return null;
        }

        String section = text.substring(start);
        int end = section.length();
        String[] endMarkers = new String[] {
                "Vehicle Remarks",
                "4. Vehicle Remarks",
                "Inspection result",
                "Certificate valid",
                "End of certificate",
                "This certificate"
        };
        for (int i = 0; i < endMarkers.length; i++) {
            int index = indexOfIgnoreCase(section, endMarkers[i]);
            if (index > markerLength && index < end) {
                end = index;
            }
        }
        section = section.substring(0, end);
        section = section.replaceFirst(
                "(?is)^\\s*(?:3\\.\\s*)?(?:Particulars of Second[- ]Hand Motor Vehicle|PARTICULARS OF SECOND[- ]HAND MOTOR VEHICLE)\\s*",
                ""
        );
        section = section.replaceFirst(
                "(?im)^\\s*(?:No\\.?\\s*)?(?:Exact\\s+Attribute|Attribute)\\s+Value\\s*\\n?",
                ""
        );
        return section.trim();
    }

    private String extractVehicleField(String vehicleSection, String fullText, int number, String... labels) {
        for (int i = 0; i < labels.length; i++) {
            String value = firstNonNull(
                    extractFromScopedText(vehicleSection, number, labels[i]),
                    extractFromScopedText(fullText, number, labels[i])
            );
            if (value != null && value.length() > 0) {
                return value;
            }
        }
        return null;
    }

    private String extractFromScopedText(String text, int number, String label) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return firstNonNull(
                extractBvNumberedField(text, number, label),
                extractNumbered(text, number, label),
                extractVehicleTableRow(text, number, label),
                extractAttributeValueRow(text, number, label),
                extractLabelValueNextLine(text, number, label)
        );
    }

    private String extractVehicleTableRow(String text, int number, String label) {
        Pattern pattern = Pattern.compile(
                "(?:^|\\n)\\s*" + number + "(?![0-9])[\\t\\s]+"
                        + Pattern.quote(label) + "(?:\\([^)]*\\))?\\.?[\\t\\s]+(.+?)\\s*(?:\\n|$)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return stripLeadingParenthetical(cleanValue(matcher.group(1)));
        }
        return null;
    }

    private String extractAttributeValueRow(String text, int number, String label) {
        String numberPrefix = number >= 1 && number <= 16
                ? "(?:" + number + "(?![0-9])[\\t\\s]+)?"
                : "";
        Pattern inline = Pattern.compile(
                "(?:^|\\n)\\s*" + numberPrefix
                        + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\.?\\s+[\\t:]*\\s*(.+?)\\s*(?:\\n|$)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = inline.matcher(text);
        if (matcher.find()) {
            return stripLeadingParenthetical(cleanValue(matcher.group(1)));
        }
        return null;
    }

    private String extractLabelValueNextLine(String text, int number, String label) {
        Pattern labelOnly = Pattern.compile(
                "(?:^|\\n)\\s*(?:"
                        + number + "(?![0-9])[\\t\\s\\.]+)?"
                        + Pattern.quote(label) + "\\s*(?:\\([^)]*\\))?\\s*[:\\.]?\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher labelMatcher = labelOnly.matcher(text);
        if (!labelMatcher.find()) {
            return null;
        }

        String tail = text.substring(labelMatcher.end());
        String[] lines = tail.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = cleanValue(lines[i]);
            if (line.length() == 0) {
                continue;
            }
            if (isVehicleFieldLabelLine(line) || isVehicleSectionBoundary(line)) {
                break;
            }
            return stripLeadingParenthetical(line);
        }
        return null;
    }

    private boolean isVehicleFieldLabelLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        return Pattern.compile(
                "^(?:\\d{1,2}(?![0-9])[\\t\\s\\.]+)?(?:Type of vehicle|Make|Model|Commonly called|Manufacture Grade|Auction Grade|Body colou?r|Fuel type|Year/month of first registration|Inspection mileage|Engine capacity|Chassis No|Engine No|Driving system|Marks of accident|Condition of chassis|Full Model No|Year of Manufacture)\\b",
                Pattern.CASE_INSENSITIVE
        ).matcher(line.trim()).find();
    }

    private boolean isVehicleSectionBoundary(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("vehicle remarks")
                || lower.startsWith("particulars of")
                || lower.startsWith("inspection result")
                || lower.equals("no.")
                || lower.startsWith("exact attribute")
                || lower.startsWith("attribute value");
    }

    private String extractLabelValue(String text, String label) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(label) + "\\.?\\s*[:\\.]?[\\t\\s]+([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private String extractInspectionOrgName(String text) {
        String section = extractInspectionOrgSection(text);
        String name = extractLetteredField(section, "a", "Name of inspection organisation");
        if (name == null) {
            name = extractLetteredField(section, "a", "Name");
        }
        if (name != null && name.length() > 0) {
            return name;
        }
        return extractAfterHeading(text, "Name of Inspection Organisation", "BUREAU VERITAS");
    }

    private String extractInspectionOrgAddress(String text) {
        String section = extractInspectionOrgSection(text);
        return extractLetteredBlock(section, "b", "Address");
    }

    private String extractInspectionOrgSection(String text) {
        String section = extractSection(text, "Name of Inspection Organisation", "Particulars of Applicant");
        if (section == null) {
            section = extractSection(text, "Name of inspection organisation", "Particulars of Applicant");
        }
        if (section == null) {
            section = extractSection(text, "Inspection Organisation", "Particulars of Applicant");
        }
        if (section == null) {
            section = extractSection(text, "Name of Inspection Organisation", "Place of Inspection");
        }
        return section;
    }

    private String extractApplicantName(String text) {
        String section = extractApplicantSection(text);
        return extractLetteredField(section, "a", "Name");
    }

    private String extractApplicantAddress(String text) {
        String section = extractApplicantSection(text);
        return extractLetteredBlock(section, "b", "Address");
    }

    private String extractApplicantSection(String text) {
        String section = extractSection(text, "Particulars of Applicant", "Particulars of Second-Hand Motor Vehicle");
        if (section == null) {
            section = extractSection(text, "Particulars of Applicant", "Particulars of Second Hand Motor Vehicle");
        }
        if (section == null) {
            section = extractSection(text, "Particulars of Applicant", "3. Particulars of Second");
        }
        return section;
    }

    private String extractLetteredBlock(String section, String letter, String label) {
        if (section == null || section.trim().isEmpty()) {
            return null;
        }

        Pattern labelOnly = Pattern.compile(
                "\\(\\s*" + letter + "\\s*\\)\\s*" + label + "\\s*[:\\.]?\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher labelMatcher = labelOnly.matcher(section);
        if (labelMatcher.find()) {
            String value = collectLinesUntilContact(section.substring(labelMatcher.end()));
            if (value != null) {
                return value;
            }
        }

        Pattern marker = Pattern.compile(
                "\\(\\s*" + letter + "\\s*\\)\\s*" + label + "\\s*[:\\.]?\\s*(.*)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = marker.matcher(section);
        if (!matcher.find()) {
            return null;
        }

        String tail = matcher.group(1);
        if (tail == null) {
            return null;
        }
        String inline = cleanAddressValue(stripContactSuffix(stripTrailingSubFields(cleanValue(tail.replace('\n', ' ')))));
        if (inline != null && inline.length() > 0 && !isSubFieldLabel(inline)) {
            return inline;
        }
        return collectLinesUntilContact(tail);
    }

    private String cleanAddressValue(String value) {
        if (value == null) {
            return null;
        }
        value = stripTrailingSubFields(value);
        value = stripContactSuffix(value);
        return cleanValue(value);
    }

    private String collectLinesUntilContact(String tail) {
        if (tail == null) {
            return null;
        }
        String[] lines = tail.split("\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = cleanValue(lines[i]);
            if (line.length() == 0) {
                continue;
            }
            if (isSubFieldLabel(line) || isLetteredContinuationLabel(line)) {
                break;
            }
            if (isInspectionBoundaryLine(line)) {
                break;
            }
            if (isContactLine(line)) {
                String beforeContact = cleanAddressValue(line);
                if (beforeContact != null && beforeContact.length() > 0) {
                    appendLine(builder, beforeContact);
                }
                break;
            }
            String addressLine = cleanAddressValue(line);
            if (addressLine != null && addressLine.length() > 0) {
                appendLine(builder, addressLine);
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private void appendLine(StringBuilder builder, String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line.trim());
    }

    private String stripTrailingSubFields(String line) {
        if (line == null) {
            return null;
        }
        Matcher lettered = Pattern.compile("\\s*\\([c-z]\\)\\s*.*$", Pattern.CASE_INSENSITIVE).matcher(line);
        if (lettered.find()) {
            line = line.substring(0, lettered.start());
        }
        return line == null ? null : line.trim();
    }

    private String stripContactSuffix(String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(
                "\\s+(?:Tel\\.?\\s*No\\.?|Tel|Fax\\.?\\s*No\\.?|Fax|Email|No\\.)\\b.*$",
                Pattern.CASE_INSENSITIVE
        ).matcher(line);
        if (matcher.find()) {
            line = line.substring(0, matcher.start());
        }
        return cleanValue(line);
    }

    /**
     * Reads BV sub-fields such as "(a) Name:" where the value may be on the same line or the next line.
     */
    private String extractLetteredField(String section, String letter, String label) {
        if (section == null || section.trim().isEmpty()) {
            return null;
        }

        Pattern inline = Pattern.compile(
                "\\(\\s*" + letter + "\\s*\\)\\s*" + Pattern.quote(label) + "\\s*[:\\.]?\\s*(.+?)(?=\\s*\\([a-z]\\)\\s*\\w|\\s*(?:Tel|Fax|Email|No\\.)\\b|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher inlineMatcher = inline.matcher(section.replace('\n', ' '));
        if (inlineMatcher.find()) {
            String value = cleanValue(inlineMatcher.group(1));
            if (value != null && value.length() > 0 && !isSubFieldLabel(value)) {
                return value;
            }
        }

        Pattern sameLine = Pattern.compile(
                "\\(\\s*" + letter + "\\s*\\)\\s*" + Pattern.quote(label) + "\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher sameLineMatcher = sameLine.matcher(section);
        if (sameLineMatcher.find()) {
            String value = cleanValue(sameLineMatcher.group(1));
            if (value != null && value.length() > 0 && !isSubFieldLabel(value) && !isContactLine(value)) {
                return value;
            }
        }

        Pattern inlineShort = Pattern.compile(
                "\\(\\s*" + letter + "\\s*\\)\\s*Name\\b\\s*[:\\.]?\\s*(.+?)(?=\\s*\\([a-z]\\)\\s*\\w|\\s*(?:Tel|Fax|Email|No\\.)\\b|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        if ("Name".equalsIgnoreCase(label)) {
            inlineMatcher = inlineShort.matcher(section.replace('\n', ' '));
            if (inlineMatcher.find()) {
                String value = cleanValue(inlineMatcher.group(1));
                if (value != null && value.length() > 0 && !isSubFieldLabel(value)) {
                    return value;
                }
            }
        }

        Pattern labelOnly = Pattern.compile(
                "\\(\\s*" + letter + "\\s*\\)\\s*" + Pattern.quote(label) + "\\s*[:\\.]?\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher labelMatcher = labelOnly.matcher(section);
        if (labelMatcher.find()) {
            String tail = section.substring(labelMatcher.end());
            String[] lines = tail.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = cleanValue(lines[i]);
                if (line.length() == 0) {
                    continue;
                }
                if (isSubFieldLabel(line) || isContactLine(line)) {
                    break;
                }
                return line;
            }
        }

        return null;
    }

    private boolean isSubFieldLabel(String line) {
        String trimmed = line.trim();
        if (Pattern.compile("^\\([a-z]\\)\\s*(Name|Address)\\b", Pattern.CASE_INSENSITIVE).matcher(trimmed).find()) {
            return true;
        }
        return isLetteredContinuationLabel(trimmed);
    }

    private boolean isLetteredContinuationLabel(String line) {
        return Pattern.compile("^\\([c-z]\\)\\b", Pattern.CASE_INSENSITIVE).matcher(line.trim()).find();
    }

    private boolean isInspectionBoundaryLine(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("place of inspection") || lower.startsWith("date of inspection");
    }

    private String extractAfterHeading(String text, String heading, String fallback) {
        int index = text.toLowerCase().indexOf(heading.toLowerCase());
        if (index < 0) {
            return fallback;
        }
        String tail = text.substring(index + heading.length()).trim();
        String[] lines = tail.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = cleanValue(lines[i]);
            if (line.length() == 0) {
                continue;
            }
            if (line.toLowerCase().startsWith("particulars of")) {
                break;
            }
            if (line.startsWith("Tel") || line.startsWith("Place of") || isContactLine(line)) {
                break;
            }
            return line;
        }
        return fallback;
    }

    private String extractBlockAfterLabel(String text, String heading, String... stopMarkers) {
        int index = text.toLowerCase().indexOf(heading.toLowerCase());
        if (index < 0) {
            return null;
        }
        String tail = text.substring(index + heading.length());
        int stop = findEarliestMarker(tail, stopMarkers);
        if (stop > 0) {
            tail = tail.substring(0, stop);
        }
        String[] lines = tail.split("\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = cleanValue(lines[i]);
            if (line.length() == 0) {
                continue;
            }
            if (line.toLowerCase().startsWith("particulars of")) {
                break;
            }
            if (isContactLine(line)) {
                break;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private int findEarliestMarker(String text, String... markers) {
        int earliest = -1;
        for (int i = 0; i < markers.length; i++) {
            int index = indexOfIgnoreCase(text, markers[i]);
            if (index >= 0 && (earliest < 0 || index < earliest)) {
                earliest = index;
            }
        }
        return earliest;
    }

    private int indexOfIgnoreCase(String text, String marker) {
        return text.toLowerCase().indexOf(marker.toLowerCase());
    }

    private String extractSection(String text, String startHeading, String endHeading) {
        int start = text.toLowerCase().indexOf(startHeading.toLowerCase());
        if (start < 0) {
            return null;
        }
        String tail = text.substring(start);
        int end = tail.toLowerCase().indexOf(endHeading.toLowerCase());
        if (end > 0) {
            tail = tail.substring(0, end);
        }
        return tail;
    }

    private void putSectionContacts(
            AuctionParseResult result,
            String section,
            String telKey,
            String faxKey,
            String emailKey
    ) {
        if (section == null || section.trim().isEmpty()) {
            return;
        }
        result.put(telKey, extractTelFromSection(section));
        result.put(faxKey, extractFaxFromSection(section));
        result.put(emailKey, extractEmailFromSection(section));
    }

    private String extractTelFromSection(String section) {
        String value = matchFirst(section,
                "(?:Tel\\.?\\s*No\\.?|Tel)\\s*[:\\.]?\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+(?:Fax|Email)\\b|\\s*$)",
                "(?<![A-Za-z])No\\.\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+Fax\\s+No\\.?\\b|\\s+Fax\\b|\\s+Email\\b|\\s*$)"
        );
        return cleanPhone(value);
    }

    private String extractFaxFromSection(String section) {
        String value = matchFirst(section,
                "Fax\\.?\\s*No\\.?\\s*[:\\.]?\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+Email\\b|\\s*$)",
                "Fax\\s*[:\\.]?\\s*(\\+?[0-9][0-9\\s\\-]{6,}?)(?=\\s+Email\\b|\\s*$)"
        );
        return cleanPhone(value);
    }

    private String extractEmailFromSection(String section) {
        Matcher matcher = Pattern.compile(
                "Email\\s*[:\\.]?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})",
                Pattern.CASE_INSENSITIVE
        ).matcher(section);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private String matchFirst(String section, String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = Pattern.compile(patterns[i], Pattern.CASE_INSENSITIVE).matcher(section);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private boolean isContactLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("tel")
                || lower.contains("fax")
                || lower.contains("email")
                || lower.contains("@")
                || Pattern.compile("No\\.\\s*\\+?[0-9]").matcher(line).find();
    }

    private String cleanPhone(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s{2,}", " ");
        int faxIndex = indexOfIgnoreCase(cleaned, " fax");
        if (faxIndex > 0) {
            cleaned = cleaned.substring(0, faxIndex).trim();
        }
        int emailIndex = indexOfIgnoreCase(cleaned, " email");
        if (emailIndex > 0) {
            cleaned = cleaned.substring(0, emailIndex).trim();
        }
        return cleaned.length() == 0 ? null : cleaned;
    }

    private String stripLeadingParenthetical(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("^\\([^)]*\\)\\s*", "").trim();
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s{2,}", " ");
        cleaned = cleaned.replaceAll("^[.:]+", "").trim();
        if (cleaned.length() == 0 || "not applicable".equalsIgnoreCase(cleaned)) {
            return cleaned;
        }
        return cleaned;
    }
}
