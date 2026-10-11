package com.carsale.erp.shared.document.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.dto.SaleProgress;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.ocr.DocumentAiConstants;
import com.carsale.erp.shared.ocr.RegistrationCertificateDocumentAi;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.shared.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RegistrationCertificateParser implements DocumentParser {

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<String, String>();
    private static final Pattern[][] LINE_LABELS = {
            line("crNo", "CR\\s*No\\.?"),
            line("registrationNo", "Registration\\s*No\\.?"),
            line("chassisNo", "Chassis\\s*No\\.?"),
            line("engineNo", "Engine\\s*No\\.?"),
            line("classOfVehicle", "Class\\s+of\\s+Vehicle"),
            line("taxationClass", "Taxation\\s+Class"),
            line("statusWhenRegistered", "Status\\s+when\\s+Registered"),
            line("fuelType", "Fuel\\s*Type"),
            line("cylinderCapacity", "Cylinder\\s*Capacity(?:\\s*\\(cc\\))?"),
            line("countryOfOrigin", "Country\\s+of\\s+Origin"),
            line("yearOfManufacture", "Year\\s+of\\s+Manufacture"),
            line("dateOfFirstRegistration", "Date\\s+of\\s+First\\s+Registration"),
            line("typeOfBody", "Type\\s+of\\s+Body"),
            line("seatingCapacity", "Seating\\s+Capacity"),
            line("provincialCouncil", "Provincial\\s+Council"),
            line("printedDate", "Printed\\s+Date"),
            line("wheelBase", "Wheel\\s*Base"),
            line("manufacturesDescription", "Manufactures?\\s+Description"),
            line("conditionsSpecialNotes", "Conditions?(?:\\s*/\\s*Special\\s+Notes|\\s+Special\\s+Notes)?"),
            line("previousOwners", "Previous\\s+Owners"),
            line("taxesPayable", "Taxes\\s+Payable"),
            line("absoluteOwner", "Absolute\\s+Owner"),
            line("currentOwnerAddressIdNo", "Current\\s+Owner(?:\\s*/\\s*Address\\s*/\\s*I\\.?D\\.?\\s*No\\.?)?"),
            line("colour", "Colour"),
            line("make", "(?<![A-Za-z])Make"),
            line("model", "(?<![A-Za-z])Model")
    };
    private static final Pattern CHASSIS = Pattern.compile("\\b([A-Z0-9]{2,8}-[A-Z0-9]{4,})\\b");
    private static final Pattern REGISTRATION = Pattern.compile("\\b([A-Z]{1,3}\\s+[A-Z]{1,4}-\\d{3,6})\\b");

    static {
        for (String name : RegistrationCertificateDocumentAi.fieldNames()) {
            TYPE_TO_FIELD.put(name, camel(name));
        }
        TYPE_TO_FIELD.put("chassis_number", "chassisNo");
        TYPE_TO_FIELD.put("chassis", "chassisNo");
        TYPE_TO_FIELD.put("color", "colour");
        TYPE_TO_FIELD.put("engine_number", "engineNo");
        TYPE_TO_FIELD.put("registration_number", "registrationNo");
    }

    private final String processorId;

    public RegistrationCertificateParser(
            @Value("${app.ocr.documentAi.registrationCertificateProcessorId:8b79a197a34f84f7}") String processorId
    ) {
        this.processorId = processorId == null || processorId.trim().isEmpty()
                ? "8b79a197a34f84f7"
                : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        result.setRawText(text);
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The registration certificate was empty.");
            return result;
        }
        String normalized = text.replace('\r', '\n');
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            for (Pattern[] labeled : LINE_LABELS) {
                Matcher matcher = labeled[0].matcher(trimmed);
                if (!matcher.find()) {
                    continue;
                }
                String field = labeled[1].pattern();
                if (result.getFields().containsKey(field)) {
                    continue;
                }
                String value = clean(matcher.group(1));
                if (value != null) {
                    result.put(field, value);
                }
            }
        }
        if (!result.getFields().containsKey("chassisNo")) {
            Matcher chassis = CHASSIS.matcher(normalized.toUpperCase(Locale.ROOT));
            if (chassis.find()) {
                result.put("chassisNo", chassis.group(1));
            }
        }
        if (!result.getFields().containsKey("registrationNo")) {
            Matcher registration = REGISTRATION.matcher(normalized.toUpperCase(Locale.ROOT));
            if (registration.find()) {
                result.put("registrationNo", registration.group(1).replaceAll("\\s+", " "));
            }
        }
        Utils.finish(result, "the registration certificate");
        return result;
    }

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        if (documentAi == null) {
            AuctionParseResult empty = new AuctionParseResult();
            empty.setSuccess(false);
            empty.setMessage("Document AI returned no result.");
            return empty;
        }
        AuctionParseResult result = parsePage(documentAi.getText());
        List<SaleProgress.DocumentAiEntity> entities = documentAi.getEntities();
        if (entities != null) {
            for (SaleProgress.DocumentAiEntity entity : entities) {
                if (entity == null) {
                    continue;
                }
                String field = Utils.mapType(entity.getType(), TYPE_TO_FIELD);
                if (field == null) {
                    continue;
                }
                String value = clean(entity.getValue());
                if (value != null) {
                    result.put(field, value);
                }
            }
        }
        Utils.finish(result, "Document AI (registration certificate)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return processorId;
    }

    @Override
    public String getDocumentName() {
        return DocumentAiConstants.Documents.REGISTRATION_CERTIFICATE;
    }

    @Override
    public String getOcrLanguage() {
        return "eng";
    }

    static String camel(String snake) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < snake.length(); i++) {
            char c = snake.charAt(i);
            if (c == '_') {
                upper = true;
                continue;
            }
            out.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return out.toString();
    }

    private static Pattern[] line(String field, String label) {
        return new Pattern[] {
                Pattern.compile("(?i)" + label + "\\s*[:.]?\\s+([^\\n]{1,180})"),
                Pattern.compile(field)
        };
    }

    static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        value = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "")
                .replaceAll(RegexConstants.Text.TRAILING_PUNCT, "")
                .trim();
        if (value.isEmpty() || value.length() > 500) {
            return null;
        }
        return value;
    }
}
