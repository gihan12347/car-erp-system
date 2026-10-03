package com.carsale.erp.shared.document.document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.util.AuctionParseResult;

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

    private final String processorId;

    public PreShipmentParser(
            @Value("${app.ocr.documentAi.preShipmentProcessorId:}") String processorId
    ) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        result.setRawText(text);
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The pre-shipment document was empty.");
            return result;
        }
        result.setSuccess(false);
        result.setMessage("The document was read, but fields could not be mapped. Please fill them manually.");
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
}
