package com.carsale.erp.shared.document.document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Service
public class PreShipmentParser implements DocumentParser {

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private static final String[] MULTI_VALUE_FIELDS = {
            "grossVehicleMass", "tyreSize", "wheelBase"
    };

    static {
        TYPE_TO_FIELD.put("applicant_address", "applicantAddress");
        TYPE_TO_FIELD.put("applicant_email", "applicantEmail");
        TYPE_TO_FIELD.put("applicant_fax", "applicantFax");
        TYPE_TO_FIELD.put("applicant_name", "applicantName");
        TYPE_TO_FIELD.put("applicant_tel", "applicantTel");
        TYPE_TO_FIELD.put("inspection_organization_address", "inspectionOrgAddress");
        TYPE_TO_FIELD.put("inspection_organization_email", "inspectionOrgEmail");
        TYPE_TO_FIELD.put("inspection_organization_fax", "inspectionOrgFax");
        TYPE_TO_FIELD.put("inspection_organization_name", "inspectionOrgName");
        TYPE_TO_FIELD.put("inspection_organization_tel", "inspectionOrgTel");
        TYPE_TO_FIELD.put("auction_grade", "preshipAuctionGrade");
        TYPE_TO_FIELD.put("body_colour", "bodyColour");
        TYPE_TO_FIELD.put("body_color", "bodyColour");
        TYPE_TO_FIELD.put("chassis_no", "chassisNo");
        TYPE_TO_FIELD.put("commonly_called", "commonName");
        TYPE_TO_FIELD.put("condition_of_chassis", "chassisCondition");
        TYPE_TO_FIELD.put("driving_system", "drivingSystem");
        TYPE_TO_FIELD.put("engine_capacity", "engineCapacity");
        TYPE_TO_FIELD.put("engine_model", "engineModel");
        TYPE_TO_FIELD.put("engine_no", "engineNo");
        TYPE_TO_FIELD.put("fuel_type", "fuelType");
        TYPE_TO_FIELD.put("gross_vehicle_mass", "grossVehicleMass");
        TYPE_TO_FIELD.put("inspection_mileage", "inspectionMileage");
        TYPE_TO_FIELD.put("make", "make");
        TYPE_TO_FIELD.put("manufacture_grade", "manufactureGrade");
        TYPE_TO_FIELD.put("marks_of_accident_on_chassis", "accidentMarksOnChassis");
        TYPE_TO_FIELD.put("model", "model");
        TYPE_TO_FIELD.put("type_of_vehicle", "vehicleType");
        TYPE_TO_FIELD.put("tyre_size", "tyreSize");
        TYPE_TO_FIELD.put("tire_size", "tyreSize");
        TYPE_TO_FIELD.put("wheel_base", "wheelBase");
        TYPE_TO_FIELD.put("year_month_of_first_registration", "firstRegistration");
    }

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

    private final String processorId;

    public PreShipmentParser(
            @Value("${app.ocr.documentAi.preShipmentProcessorId:}") String processorId
    ) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The pre-shipment document was empty.");
            return result;
        }

        String normalized = CustomsDocumentParserUtils.normalize(text);

        result.put("certificateReference", CustomsDocumentParserUtils.firstNonNull(
                extractLabelValue(normalized, "Document No"),
                extractReference(normalized)
        ));
        result.put("documentTitle", CustomsDocumentParserUtils.firstNonNull(
                extractLabelValue(normalized, "Document Title"),
                extractHeading(normalized)
        ));
        result.put("bvNumber", extractBvNumber(normalized));
        result.put("certificateDate", CustomsDocumentParserUtils.firstNonNull(
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
        result.put("firstRegistration", normalizeYearMonth(lookupVehicleValue(
                vehicleRows, vehicleSection, normalized, 9, "Year/month of first registration")));
        result.put("inspectionMileage", CustomsDocumentParserUtils.firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 10, "Inspection mileage (odometer reading)"),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 10, "Inspection mileage")
        ));
        result.put("engineCapacity", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 11, "Engine capacity"));
        result.put("chassisNo", CustomsDocumentParserUtils.firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 12, "Chassis No. (original)"),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 12, "Chassis No")
        ));
        result.put("engineNo", CustomsDocumentParserUtils.firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 13, "Engine No."),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 13, "Engine No")
        ));
        result.put("drivingSystem", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 14, "Driving system"));
        result.put("accidentMarksOnChassis", CustomsDocumentParserUtils.firstNonNull(
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 15, "Marks of accident on chassis (by visual check)"),
                lookupVehicleValue(vehicleRows, vehicleSection, normalized, 15, "Marks of accident on chassis")
        ));
        result.put("chassisCondition", lookupVehicleValue(vehicleRows, vehicleSection, normalized, 16, "Condition of chassis"));

        String remarksSection = vehicleSection != null ? vehicleSection : normalized;
        result.put("fullModelNo", CustomsDocumentParserUtils.firstNonNull(
                lookupVehicleValue(vehicleRows, remarksSection, normalized, 0, "Full Model No"),
                extractLabelValue(remarksSection, "Full Model No"),
                extractLabelValue(normalized, "Full Model No")
        ));
        result.put("yearOfManufacture", CustomsDocumentParserUtils.firstNonNull(
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

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        AuctionParseResult result = new AuctionParseResult();
        if (documentAi == null) {
            result.setSuccess(false);
            result.setMessage("Document AI returned no result.");
            return result;
        }
        result.setRawText(documentAi.getText());
        Map<String, List<String>> multiValues = new LinkedHashMap<>();
        List<DocumentAiClient.DocumentAiEntity> entities = documentAi.getEntities();
        if (entities != null) {
            for (DocumentAiClient.DocumentAiEntity entity : entities) {
                if (entity == null) {
                    continue;
                }
                String field = mapType(entity.getType());
                if (field == null) {
                    continue;
                }
                String value = cleanDocumentAiValue(field, entity.getValue());
                if (value == null || value.isEmpty()) {
                    continue;
                }
                if (isMultiValueField(field)) {
                    List<String> values = multiValues.computeIfAbsent(field, k -> new ArrayList<>());
                    if (!values.contains(value)) {
                        values.add(value);
                    }
                    continue;
                }
                if (!result.getFields().containsKey(field)) {
                    result.put(field, value);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : multiValues.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.put(entry.getKey(), joinValues(entry.getValue()));
            }
        }
        fillMissingFromText(result, documentAi.getText());
        CustomsDocumentParserUtils.finish(result, "Document AI (pre-shipment certificate)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public String getDocumentName() {
        return "pre shipment";
    }

    static String mapType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        String key = type.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if ("vehicle_details".equals(key) || "vehicle_detail".equals(key)) {
            return null;
        }
        String mapped = TYPE_TO_FIELD.get(key);
        if (mapped != null) {
            return mapped;
        }
        int slash = key.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < key.length()) {
            return TYPE_TO_FIELD.get(key.substring(slash + 1));
        }
        return null;
    }

    static String cleanDocumentAiValue(String field, String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        value = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "").replaceAll(RegexConstants.Text.TRAILING_PUNCT, "").trim();
        if (value.isEmpty() || "not applicable".equalsIgnoreCase(value)) {
            return value.isEmpty() ? null : value;
        }
        if ("bvNumber".equals(field)) {
            return value.toUpperCase(Locale.ROOT).replaceAll(RegexConstants.Text.WHITESPACE, "");
        }
        if ("firstRegistration".equals(field)) {
            return normalizeYearMonth(value);
        }
        return value;
    }

    public static String normalizeYearMonth(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        if (value.isEmpty() || "not applicable".equalsIgnoreCase(value)) {
            return value.isEmpty() ? null : value;
        }
        Matcher iso = RegexConstants.Dates.YEAR_MONTH_ISO_PATTERN.matcher(value);
        if (iso.matches()) {
            return String.format(Locale.ROOT, "%s-%02d", iso.group(1), Integer.parseInt(iso.group(2)));
        }
        Matcher numeric = RegexConstants.Dates.MONTH_YEAR_NUMERIC_PATTERN.matcher(value);
        if (numeric.matches()) {
            return String.format(Locale.ROOT, "%s-%02d", numeric.group(2), Integer.parseInt(numeric.group(1)));
        }
        Matcher yearFirst = RegexConstants.Dates.YEAR_MONTH_NUMERIC_PATTERN.matcher(value);
        if (yearFirst.matches()) {
            return String.format(Locale.ROOT, "%s-%02d", yearFirst.group(1), Integer.parseInt(yearFirst.group(2)));
        }
        Matcher named = RegexConstants.Dates.NAMED_MONTH_YEAR_PATTERN.matcher(value);
        if (named.matches()) {
            Integer month = monthNumber(named.group(1));
            if (month != null) {
                return String.format(Locale.ROOT, "%s-%02d", named.group(2), month);
            }
        }
        return value;
    }

    private static Integer monthNumber(String monthToken) {
        if (monthToken == null || monthToken.isEmpty()) {
            return null;
        }
        String key = monthToken.toLowerCase(Locale.ROOT);
        if (key.startsWith("jan")) {
            return 1;
        }
        if (key.startsWith("feb")) {
            return 2;
        }
        if (key.startsWith("mar")) {
            return 3;
        }
        if (key.startsWith("apr")) {
            return 4;
        }
        if (key.equals("may")) {
            return 5;
        }
        if (key.startsWith("jun")) {
            return 6;
        }
        if (key.startsWith("jul")) {
            return 7;
        }
        if (key.startsWith("aug")) {
            return 8;
        }
        if (key.startsWith("sep")) {
            return 9;
        }
        if (key.startsWith("oct")) {
            return 10;
        }
        if (key.startsWith("nov")) {
            return 11;
        }
        if (key.startsWith("dec")) {
            return 12;
        }
        return null;
    }

    private static boolean isMultiValueField(String field) {
        for (String multi : MULTI_VALUE_FIELDS) {
            if (multi.equals(field)) {
                return true;
            }
        }
        return false;
    }

    private static String joinValues(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(value.trim());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private void fillMissingFromText(AuctionParseResult result, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        AuctionParseResult fromText = parsePage(text);
        if (fromText == null || fromText.getFields() == null) {
            return;
        }
        for (Map.Entry<String, String> entry : fromText.getFields().entrySet()) {
            if (!result.getFields().containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class VehicleRowIndex {
        private final Map<Integer, String> byNumber = new LinkedHashMap<>();
        private final Map<String, String> byAttribute = new LinkedHashMap<>();
    }

    private VehicleRowIndex buildVehicleRowIndex(String section) {
        VehicleRowIndex index = new VehicleRowIndex();
        if (section == null || section.trim().isEmpty()) {
            return index;
        }

        parseBvParenthesisRows(section, index);

        String[] lines = section.split(RegexConstants.Text.LF);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || isTableHeaderLine(line) || isVehicleSectionBoundary(line)) {
                continue;
            }

            if (line.indexOf('\t') >= 0) {
                String[] columns = line.split(RegexConstants.Text.TAB, -1);
                if (columns.length >= 3) {
                    registerVehicleRow(index, parseRowNumber(columns[0]), columns[1], columns[2]);
                    continue;
                }
                if (columns.length == 2) {
                    registerVehicleRow(index, null, columns[0], columns[1]);
                    continue;
                }
            }

            Matcher parenNumbered = RegexConstants.PreShipment.PAREN_NUMBERED.matcher(line);
            if (parenNumbered.matches()) {
                Integer rowNum = parseRowNumber(parenNumbered.group(1));
                String rest = parenNumbered.group(2).trim();
                if (rest.endsWith(":") && i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    if (!nextLine.isEmpty()
                            && !isVehicleFieldLabelLine(nextLine)
                            && !isVehicleSectionBoundary(nextLine)
                            && !nextLine.matches(RegexConstants.PreShipment.PAREN_NUMBERED_LINE)) {
                        rest = rest + " " + nextLine;
                        i++;
                    }
                }
                if (matchAttributeValue(index, rowNum, rest)) {
                    continue;
                }
            }

            Matcher numbered = RegexConstants.PreShipment.NUMBERED.matcher(line);
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
        Pattern row = RegexConstants.PreShipment.INLINE_NUMBERED;
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
            if (value == null || value.isEmpty()) {
                continue;
            }

            Integer mappedNumber = number;
            if (mappedNumber == null && ATTRIBUTE_ONLY_NUMBERS[i] > 0) {
                mappedNumber = ATTRIBUTE_ONLY_NUMBERS[i];
            }
            registerVehicleRow(index, mappedNumber, prefix, value);
            return true;
        }
        return false;
    }

    private void registerVehicleRow(VehicleRowIndex index, Integer number, String attribute, String value) {
        String cleanedValue = stripLeadingParenthetical(cleanValue(value));
        if (cleanedValue == null || cleanedValue.isEmpty()) {
            return;
        }

        String cleanedAttribute = cleanValue(attribute);
        if (cleanedAttribute != null && !cleanedAttribute.isEmpty() && !isTableHeaderLine(cleanedAttribute)) {
            index.byAttribute.put(normalizeVehicleAttribute(cleanedAttribute), cleanedValue);
        }
        if (number != null && number >= 1 && number <= 16) {
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
        if (trimmed == null || !trimmed.matches(RegexConstants.Dates.ONE_OR_TWO_DIGITS)) {
            return null;
        }
        return Integer.valueOf(trimmed);
    }

    private String normalizeVehicleAttribute(String attribute) {
        if (attribute == null) {
            return "";
        }
        String normalized = attribute.trim().toLowerCase();
        normalized = normalized.replaceAll(RegexConstants.Text.PARENTHESES, " ");
        normalized = normalized.replaceAll(RegexConstants.Text.TRAILING_DOTS, "");
        normalized = normalized.replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
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
                String byNumber = index.byNumber.get(number);
                if (byNumber != null && !byNumber.isEmpty()) {
                    return byNumber;
                }
            }
            for (String label : labels) {
                String byAttribute = index.byAttribute.get(normalizeVehicleAttribute(label));
                if (byAttribute != null && !byAttribute.isEmpty()) {
                    return byAttribute;
                }
            }
        }
        if (number >= 1 && number <= 16) {
            return extractVehicleField(vehicleSection, fullText, number, labels);
        }
        return CustomsDocumentParserUtils.firstNonNull(extractAttributeValueRow(vehicleSection, 0, labels[0]),
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

    private String extractHeading(String text) {
        if (text.toUpperCase().contains("PRE-SHIPMENT INSPECTION CERTIFICATE".toUpperCase())) {
            return "PRE-SHIPMENT INSPECTION CERTIFICATE";
        }
        return null;
    }

    private String extractReference(String text) {
        Matcher matcher = RegexConstants.PreShipment.REFERENCE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractBvNumber(String text) {
        Matcher matcher = RegexConstants.PreShipment.BV_NUMBER.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase().replaceAll(RegexConstants.Text.WHITESPACE, "");
        }
        return null;
    }

    private String extractHeaderDate(String text) {
        Matcher matcher = RegexConstants.PreShipment.HEADER_DATE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractCertificateDate(String text) {
        Matcher matcher = RegexConstants.PreShipment.CERTIFICATE_DATE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher top = RegexConstants.Dates.DAY_MON_YEAR_PATTERN.matcher(text);
        if (top.find()) {
            return top.group(1);
        }
        return null;
    }

    private String extractPageInfo(String text) {
        Matcher slash = RegexConstants.PreShipment.PAGE_SLASH.matcher(text);
        if (slash.find()) {
            return slash.group(1).replaceAll(RegexConstants.Text.WHITESPACE, "");
        }
        Matcher matcher = RegexConstants.PreShipment.PAGE_OF.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractNumbered(String text, int number, String... labels) {
        for (String label : labels) {
            Pattern pattern = RegexConstants.PreShipment.numberedFieldValue(number, label);
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
        Pattern pattern = RegexConstants.PreShipment.parenthesizedNumberFieldValue(number, label);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return stripLeadingParenthetical(cleanValue(matcher.group(1)));
        }
        Pattern splitLabel = RegexConstants.PreShipment.parenthesizedNumberLabelAlone(number, label);
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
        for (String startMarker : startMarkers) {
            int index = indexOfIgnoreCase(text, startMarker);
            if (index >= 0 && (start < 0 || index < start)) {
                start = index;
                markerLength = startMarker.length();
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
        for (String endMarker : endMarkers) {
            int index = indexOfIgnoreCase(section, endMarker);
            if (index > markerLength && index < end) {
                end = index;
            }
        }
        section = section.substring(0, end);
        section = section.replaceFirst(RegexConstants.PreShipment.VEHICLE_SECTION_HEAD, "");
        section = section.replaceFirst(RegexConstants.PreShipment.ATTRIBUTE_HEADER, "");
        return section.trim();
    }

    private String extractVehicleField(String vehicleSection, String fullText, int number, String... labels) {
        for (String label : labels) {
            String value = CustomsDocumentParserUtils.firstNonNull(
                    extractFromScopedText(vehicleSection, number, label),
                    extractFromScopedText(fullText, number, label)
            );
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String extractFromScopedText(String text, int number, String label) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return CustomsDocumentParserUtils.firstNonNull(
                extractBvNumberedField(text, number, label),
                extractNumbered(text, number, label),
                extractVehicleTableRow(text, number, label),
                extractAttributeValueRow(text, number, label),
                extractLabelValueNextLine(text, number, label)
        );
    }

    private String extractVehicleTableRow(String text, int number, String label) {
        Pattern pattern = RegexConstants.PreShipment.numberedTableCellValue(number, label);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return stripLeadingParenthetical(cleanValue(matcher.group(1)));
        }
        return null;
    }

    private String extractAttributeValueRow(String text, int number, String label) {
        String numberPrefix = number >= 1 && number <= 16
                ? RegexConstants.PreShipment.optionalLeadingRowNumber(number)
                : "";
        Pattern inline = RegexConstants.PreShipment.attributeRowValue(numberPrefix, label);
        Matcher matcher = inline.matcher(text);
        if (matcher.find()) {
            return stripLeadingParenthetical(cleanValue(matcher.group(1)));
        }
        return null;
    }

    private String extractLabelValueNextLine(String text, int number, String label) {
        Pattern labelOnly = RegexConstants.PreShipment.numberedLabelAlone(number, label);
        Matcher labelMatcher = labelOnly.matcher(text);
        if (!labelMatcher.find()) {
            return null;
        }

        String tail = text.substring(labelMatcher.end());
        String[] lines = tail.split(RegexConstants.Text.LF);
        for (String s : lines) {
            String line = cleanValue(s);
            if (line.isEmpty()) {
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
        return RegexConstants.PreShipment.VEHICLE_FIELD_LABEL.matcher(line.trim()).find();
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
        Pattern pattern = RegexConstants.Labeled.tabSeparatedValueAfterQuotedLabel(label);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private String extractInspectionOrgName(String text) {
        String section = extractInspectionOrgSection(text);
        String name = extractLetteredField(section, "Name of inspection organisation");
        if (name == null) {
            name = extractLetteredField(section, "Name");
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return extractAfterHeading(text);
    }

    private String extractInspectionOrgAddress(String text) {
        String section = extractInspectionOrgSection(text);
        return extractLetteredBlock(section);
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
        return extractLetteredField(section, "Name");
    }

    private String extractApplicantAddress(String text) {
        String section = extractApplicantSection(text);
        return extractLetteredBlock(section);
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

    private String extractLetteredBlock(String section) {
        if (section == null || section.trim().isEmpty()) {
            return null;
        }

        Pattern labelOnly = RegexConstants.PreShipment.ADDRESS_LABEL_ONLY;
        Matcher labelMatcher = labelOnly.matcher(section);
        if (labelMatcher.find()) {
            String value = collectLinesUntilContact(section.substring(labelMatcher.end()));
            if (value != null) {
                return value;
            }
        }

        Pattern marker = RegexConstants.PreShipment.ADDRESS_MARKER;
        Matcher matcher = marker.matcher(section);
        if (!matcher.find()) {
            return null;
        }

        String tail = matcher.group(1);
        if (tail == null) {
            return null;
        }
        String inline = cleanAddressValue(stripContactSuffix(stripTrailingSubFields(cleanValue(tail.replace('\n', ' ')))));
        if (inline != null && !inline.isEmpty() && !isSubFieldLabel(inline)) {
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
        String[] lines = tail.split(RegexConstants.Text.LF);
        StringBuilder builder = new StringBuilder();
        for (String s : lines) {
            String line = cleanValue(s);
            if (line.isEmpty()) {
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
                if (beforeContact != null && !beforeContact.isEmpty()) {
                    appendLine(builder, beforeContact);
                }
                break;
            }
            String addressLine = cleanAddressValue(line);
            if (addressLine != null && !addressLine.isEmpty()) {
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
        Matcher lettered = RegexConstants.PreShipment.LETTERED_TAIL.matcher(line);
        if (lettered.find()) {
            line = line.substring(0, lettered.start());
        }
        return line.trim();
    }

    private String stripContactSuffix(String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = RegexConstants.PreShipment.CONTACT_SUFFIX.matcher(line);
        if (matcher.find()) {
            line = line.substring(0, matcher.start());
        }
        return cleanValue(line);
    }

    /**
     * Reads BV sub-fields such as "(a) Name:" where the value may be on the same line or the next line.
     */
    private String extractLetteredField(String section, String label) {
        if (section == null || section.trim().isEmpty()) {
            return null;
        }

        Pattern inline = RegexConstants.PreShipment.letterAValueUntilNextMarker(label);
        Matcher inlineMatcher = inline.matcher(section.replace('\n', ' '));
        if (inlineMatcher.find()) {
            String value = cleanValue(inlineMatcher.group(1));
            if (value != null && !value.isEmpty() && !isSubFieldLabel(value)) {
                return value;
            }
        }

        Pattern sameLine = RegexConstants.PreShipment.letterAValueOnSameLine(label);
        Matcher sameLineMatcher = sameLine.matcher(section);
        if (sameLineMatcher.find()) {
            String value = cleanValue(sameLineMatcher.group(1));
            if (value != null && !value.isEmpty() && !isSubFieldLabel(value) && !isContactLine(value)) {
                return value;
            }
        }

        Pattern inlineShort = RegexConstants.PreShipment.NAME_INLINE;
        if ("Name".equalsIgnoreCase(label)) {
            inlineMatcher = inlineShort.matcher(section.replace('\n', ' '));
            if (inlineMatcher.find()) {
                String value = cleanValue(inlineMatcher.group(1));
                if (value != null && !value.isEmpty() && !isSubFieldLabel(value)) {
                    return value;
                }
            }
        }

        Pattern labelOnly = RegexConstants.PreShipment.letterALabelAlone(label);
        Matcher labelMatcher = labelOnly.matcher(section);
        if (labelMatcher.find()) {
            String tail = section.substring(labelMatcher.end());
            String[] lines = tail.split(RegexConstants.Text.LF);
            for (String s : lines) {
                String line = cleanValue(s);
                if (line.isEmpty()) {
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
        if (RegexConstants.PreShipment.SUBFIELD_NAME_ADDRESS.matcher(trimmed).find()) {
            return true;
        }
        return isLetteredContinuationLabel(trimmed);
    }

    private boolean isLetteredContinuationLabel(String line) {
        return RegexConstants.PreShipment.LETTERED_CONTINUATION.matcher(line.trim()).find();
    }

    private boolean isInspectionBoundaryLine(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("place of inspection") || lower.startsWith("date of inspection");
    }

    private String extractAfterHeading(String text) {
        int index = text.toLowerCase().indexOf("Name of Inspection Organisation".toLowerCase());
        if (index < 0) {
            return "BUREAU VERITAS";
        }
        String tail = text.substring(index + "Name of Inspection Organisation".length()).trim();
        String[] lines = tail.split(RegexConstants.Text.LF);
        for (String s : lines) {
            String line = cleanValue(s);
            if (line.isEmpty()) {
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
        return "BUREAU VERITAS";
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
        String value = matchFirst(section, RegexConstants.PreShipment.TEL, RegexConstants.PreShipment.TEL_NO);
        return cleanPhone(value);
    }

    private String extractFaxFromSection(String section) {
        String value = matchFirst(section, RegexConstants.PreShipment.FAX_NO, RegexConstants.PreShipment.FAX);
        return cleanPhone(value);
    }

    private String extractEmailFromSection(String section) {
        Matcher matcher = RegexConstants.PreShipment.EMAIL.matcher(section);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private String matchFirst(String section, String... patterns) {
        for (String pattern : patterns) {
            Matcher matcher = RegexConstants.compileIgnoreCase(pattern).matcher(section);
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
                || RegexConstants.PreShipment.PHONE_NO.matcher(line).find();
    }

    private String cleanPhone(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll(RegexConstants.Text.WHITESPACE_RUN, " ");
        int faxIndex = indexOfIgnoreCase(cleaned, " fax");
        if (faxIndex > 0) {
            cleaned = cleaned.substring(0, faxIndex).trim();
        }
        int emailIndex = indexOfIgnoreCase(cleaned, " email");
        if (emailIndex > 0) {
            cleaned = cleaned.substring(0, emailIndex).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String stripLeadingParenthetical(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll(RegexConstants.Text.LEADING_PARENTHESES, "").trim();
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll(RegexConstants.Text.WHITESPACE_RUN, " ");
        cleaned = cleaned.replaceAll(RegexConstants.Text.LEADING_COLON, "").trim();
        return cleaned;
    }
}
