package com.carsale.erp.shared.document.document;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Service
public class ExportCertificateParser implements DocumentParser {

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private static final Set<String> DATE_FIELDS = new HashSet<>();
    private static final Set<String> MONTH_FIELDS = new HashSet<>();

    static {
        TYPE_TO_FIELD.put("address_of_owner", "ownerAddress");
        TYPE_TO_FIELD.put("address_of_user", "userAddress");
        TYPE_TO_FIELD.put("chassis_no", "chassisVin");
        TYPE_TO_FIELD.put("classified_no", "classificationNo");
        TYPE_TO_FIELD.put("date", "issueDate");
        TYPE_TO_FIELD.put("director_general_land_transport_branch", "directorGeneralLandTransportBranch");
        TYPE_TO_FIELD.put("displacement", "engineCapacity");
        TYPE_TO_FIELD.put("export_scheduled_day", "exportScheduledDate");
        TYPE_TO_FIELD.put("first_registration_date", "firstRegDate");
        TYPE_TO_FIELD.put("form_of_vehicle", "bodyType");
        TYPE_TO_FIELD.put("gross_weight_of_vehicle", "grossWeightKg");
        TYPE_TO_FIELD.put("height", "heightCm");
        TYPE_TO_FIELD.put("length", "lengthCm");
        TYPE_TO_FIELD.put("max_carrying_capacity", "maxCarry");
        TYPE_TO_FIELD.put("motor_vehicle_registration_no", "registrationNo");
        TYPE_TO_FIELD.put("name_of_owner", "ownerName");
        TYPE_TO_FIELD.put("name_of_user", "userName");
        TYPE_TO_FIELD.put("parking_place", "localityOfUse");
        TYPE_TO_FIELD.put("private_or_commercial", "purpose");
        TYPE_TO_FIELD.put("registration_date", "registrationDate");
        TYPE_TO_FIELD.put("seating_capacity", "seatingCapacity");
        TYPE_TO_FIELD.put("specification_no", "specificationNo");
        TYPE_TO_FIELD.put("type_of_fuel", "fuelType");
        TYPE_TO_FIELD.put("type_of_vehicle", "vehicleClassification");
        TYPE_TO_FIELD.put("usage", "useType");
        TYPE_TO_FIELD.put("weight_of_front_front_axel", "frontAxleWeight");
        TYPE_TO_FIELD.put("weight_of_front_rear_axel", "frWeight");
        TYPE_TO_FIELD.put("weight_of_rear_front_axel", "rfWeight");
        TYPE_TO_FIELD.put("weight_of_rear_rear_axel", "rearAxleWeight");
        TYPE_TO_FIELD.put("weight_of_vehicle", "weightKg");
        TYPE_TO_FIELD.put("width", "widthCm");

        DATE_FIELDS.add("issueDate");
        DATE_FIELDS.add("exportScheduledDate");
        DATE_FIELDS.add("registrationDate");
        MONTH_FIELDS.add("firstRegDate");
    }

    private final String processorId;

    public ExportCertificateParser(
            @Value("${app.ocr.documentAi.exportCertificateProcessorId:}") String processorId
    ) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        result.setRawText(text);
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The export certificate was empty.");
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
        List<DocumentAiClient.DocumentAiEntity> entities = documentAi.getEntities();
        if (entities != null) {
            for (DocumentAiClient.DocumentAiEntity entity : entities) {
                if (entity == null) {
                    continue;
                }
                String field = mapType(entity.getType());
                if (field == null || result.getFields().containsKey(field)) {
                    continue;
                }
                result.put(field, cleanDocumentAiValue(field, entity.getValue()));
            }
        }
        CustomsDocumentParserUtils.finish(result, "Document AI (export certificate)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public String getDocumentName() {
        return "export certificate";
    }

    @Override
    public String getOcrLanguage() {
        return "eng";
    }

    static String mapType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        String key = type.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
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
        value = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "")
                .replaceAll(RegexConstants.Text.TRAILING_PUNCT, "")
                .trim();
        if (value.isEmpty()) {
            return null;
        }
        if (DATE_FIELDS.contains(field)) {
            return toIsoDate(value);
        }
        if (MONTH_FIELDS.contains(field)) {
            return toIsoMonth(value);
        }
        return value;
    }

    public static String toIsoDate(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        Matcher isoDateTime = RegexConstants.Dates.ISO_DATE_TIME_PATTERN.matcher(value);
        if (isoDateTime.matches()) {
            return isoDateTime.group(1);
        }
        Matcher iso = RegexConstants.Dates.ISO_DATE_PATTERN.matcher(value);
        if (iso.find()) {
            return iso.group();
        }
        Matcher yearMonthDay = RegexConstants.Dates.YEAR_MONTH_DAY_NUMERIC_PATTERN.matcher(value);
        if (yearMonthDay.matches()) {
            return formatDate(
                    Integer.parseInt(yearMonthDay.group(1)),
                    Integer.parseInt(yearMonthDay.group(2)),
                    Integer.parseInt(yearMonthDay.group(3))
            );
        }
        Matcher dayMonthYear = RegexConstants.Dates.DAY_MONTH_YEAR_NUMERIC_PATTERN.matcher(value);
        if (dayMonthYear.matches()) {
            return formatDate(
                    Integer.parseInt(dayMonthYear.group(3)),
                    Integer.parseInt(dayMonthYear.group(2)),
                    Integer.parseInt(dayMonthYear.group(1))
            );
        }
        Matcher yearMonth = RegexConstants.Dates.YEAR_MONTH_ISO_PATTERN.matcher(value);
        if (yearMonth.matches()) {
            return formatDate(
                    Integer.parseInt(yearMonth.group(1)),
                    Integer.parseInt(yearMonth.group(2)),
                    1
            );
        }
        return value;
    }

    public static String toIsoMonth(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        Matcher isoDateTime = RegexConstants.Dates.ISO_DATE_TIME_PATTERN.matcher(value);
        if (isoDateTime.matches()) {
            return isoDateTime.group(1).substring(0, 7);
        }
        Matcher iso = RegexConstants.Dates.ISO_DATE_PATTERN.matcher(value);
        if (iso.find()) {
            return iso.group().substring(0, 7);
        }
        Matcher yearMonthIso = RegexConstants.Dates.YEAR_MONTH_ISO_PATTERN.matcher(value);
        if (yearMonthIso.matches()) {
            return formatMonth(
                    Integer.parseInt(yearMonthIso.group(1)),
                    Integer.parseInt(yearMonthIso.group(2))
            );
        }
        Matcher yearMonth = RegexConstants.Dates.YEAR_MONTH_NUMERIC_PATTERN.matcher(value);
        if (yearMonth.matches()) {
            return formatMonth(
                    Integer.parseInt(yearMonth.group(1)),
                    Integer.parseInt(yearMonth.group(2))
            );
        }
        Matcher monthYear = RegexConstants.Dates.MONTH_YEAR_NUMERIC_PATTERN.matcher(value);
        if (monthYear.matches()) {
            return formatMonth(
                    Integer.parseInt(monthYear.group(2)),
                    Integer.parseInt(monthYear.group(1))
            );
        }
        Matcher yearMonthDay = RegexConstants.Dates.YEAR_MONTH_DAY_NUMERIC_PATTERN.matcher(value);
        if (yearMonthDay.matches()) {
            return formatMonth(
                    Integer.parseInt(yearMonthDay.group(1)),
                    Integer.parseInt(yearMonthDay.group(2))
            );
        }
        Matcher dayMonthYear = RegexConstants.Dates.DAY_MONTH_YEAR_NUMERIC_PATTERN.matcher(value);
        if (dayMonthYear.matches()) {
            return formatMonth(
                    Integer.parseInt(dayMonthYear.group(3)),
                    Integer.parseInt(dayMonthYear.group(2))
            );
        }
        return PreShipmentParser.normalizeYearMonth(value);
    }

    private static String formatDate(int year, int month, int day) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day);
    }

    private static String formatMonth(int year, int month) {
        return String.format(Locale.ROOT, "%04d-%02d", year, month);
    }
}
