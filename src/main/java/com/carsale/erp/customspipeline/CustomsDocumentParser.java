package com.carsale.erp.customspipeline;

import com.carsale.erp.shared.vehicle.Vehicle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Service
public class CustomsDocumentParser {

    public AuctionParseResult parsePage1(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Page 1 (JEVIC certificate) was empty.");
            return result;
        }
        String normalized = normalize(text);

        result.put("jevicCertificateNo", extractJevicCertificateNo(normalized));
        result.put("jevicIssueDate", firstNonNull(
                extractJevicDate(normalized, "Date\\s+of\\s+Issue", "Inspection\\s+Branch", "CERTIFICATE"),
                extractJevicIssueDate(normalized)
        ));
        result.put("jevicLocation", firstNonNull(
                extractJevicScalar(normalized, "Inspection\\s+Branch", "Inspected Motor", "Make"),
                extractJevicLocation(normalized)
        ));
        result.put("jevicMake", extractJevicScalar(normalized, "Make", "Model", "Date of Inspection"));
        result.put("jevicModel", extractJevicScalar(
                normalized, "Model", "Engine Capacity", "Date of Inspection", "Location"));
        result.put("jevicEngineCapacity", extractJevicScalar(
                normalized, "Engine\\s+Capacity", "Year of First", "Chassis Number", "Chassis"));
        result.put("jevicFirstRegistration", extractJevicScalar(
                normalized, "Year\\s+of\\s+First\\s+Registration", "Chassis Number", "Chassis", "Engine Number"));
        result.put("jevicChassisVin", extractJevicChassis(normalized));
        result.put("jevicEngineNo", extractJevicScalar(
                normalized, "Engine\\s+Number", "Inspected Mileage", "Odometer", "Inspection Date"));
        result.put("jevicCurrentOdometer", firstNonNull(
                extractInspectedMileage(normalized),
                extractJevicOdometer(normalized)
        ));
        result.put("jevicInspectionDate", firstNonNull(
                extractJevicDate(normalized, "Inspection\\s+Date", "Remarks"),
                extractJevicDate(normalized, "Date\\s+of\\s+Inspection", "Location", "Certificate")
        ));
        result.put("jevicRemarks", extractJevicRemarks(normalized));
        result.put("jevicAuctionReadingDate", extractJevicReadingRow(normalized, "Auction Reading", "Dealer Reading"));
        result.put("jevicDealerReadingDate", extractJevicReadingRow(normalized, "Dealer Reading", "De-Registration Reading", "De Registration Reading"));
        result.put("jevicDeregistrationReadingDate", extractJevicReadingRow(
                normalized, "De-Registration Reading", "De Registration Reading", "Authorized"));

        finish(result, "JEVIC certificate of inspection (page 1)");
        return result;
    }

    private String extractJevicChassis(String text) {
        String value = extractJevicScalar(text, "Chassis\\s+Number", "Engine Number", "Inspected Mileage");
        if (value == null) {
            value = extractJevicScalar(text, "Chassis\\s*/\\s*VIN", "Make", "Model");
        }
        if (value == null) {
            value = extractJevicScalar(text, "Chassis/VIN", "Make", "Model");
        }
        if (value == null) {
            value = findChassis(text);
        }
        return value;
    }

    private String extractInspectedMileage(String text) {
        Matcher matcher = Pattern.compile(
                "Inspected\\s+Mileage(?:\\s*\\(\\s*Odometer\\s+Reading\\s*\\))?\\s*[:\\.]?\\s*"
                        + "(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(matcher.group(1));
            if (value != null && !isJevicFieldLabel(value)) {
                return value;
            }
        }
        matcher = Pattern.compile(
                "Inspected\\s+Mileage(?:\\s*\\(\\s*Odometer\\s+Reading\\s*\\))?\\s*[:\\.]?\\s*\\n\\s*"
                        + "(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }
        return null;
    }

    private String extractJevicRemarks(String text) {
        Matcher matcher = Pattern.compile(
                "Remarks\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(trimBeforeNextJevicLabel(matcher.group(1)));
        }
        matcher = Pattern.compile(
                "Remarks\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(trimBeforeNextJevicLabel(matcher.group(1)));
        }
        return null;
    }

    private String firstNonNull(String... values) {
        if (values == null) {
            return null;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].trim().isEmpty()) {
                return values[i];
            }
        }
        return null;
    }

    private String extractJevicLocation(String text) {
        Matcher matcher = Pattern.compile(
                "\\bLocation\\s*:\\s*(?:Certificate\\s+No\\.?\\s*:\\s*)*(?:Date\\s+of\\s+Issue\\s*:\\s*)*"
                        + "([A-Za-z][A-Za-z0-9 \\-/]+?)"
                        + "(?=\\s*(?:Certificate\\s+No|Date\\s+of\\s+Issue|Current\\s+Odometer|LK1-|\\n|$))",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(trimBeforeToken(matcher.group(1), "LK1-"));
            if (value != null) {
                return value;
            }
        }

        matcher = Pattern.compile(
                "\\bLocation\\s*:\\s*\\n\\s*([A-Za-z][A-Za-z0-9 \\-/]+)"
                        + "(?=\\s*(?:\\n|Certificate|Date\\s+of\\s+Issue|LK1-))",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(trimBeforeToken(matcher.group(1), "LK1-"));
            if (value != null) {
                return value;
            }
        }

        return extractJevicLocationNearCertificate(text);
    }

    private String extractJevicLocationNearCertificate(String text) {
        Matcher matcher = Pattern.compile(
                "([A-Za-z][A-Za-z0-9 \\-/]{2,}?)\\s+LK1-[A-Z0-9]+",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }

        matcher = Pattern.compile(
                "([A-Za-z][A-Za-z0-9 \\-/]{2,}?)\\s*\\n\\s*LK1-[A-Z0-9]+",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }
        return null;
    }

    private String extractJevicIssueDate(String text) {
        String direct = extractJevicDate(text, "Date\\s+of\\s+Issue", "Current\\s+Odometer", "Auction");
        if (direct != null) {
            return direct;
        }

        Matcher matcher = Pattern.compile(
                "LK1-[A-Z0-9]+\\s+(\\d{1,2}/\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = Pattern.compile(
                "LK1-[A-Z0-9]+\\s*\\n\\s*(\\d{1,2}/\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String trimBeforeToken(String value, String token) {
        if (value == null) {
            return null;
        }
        int index = value.toUpperCase(Locale.ROOT).indexOf(token.toUpperCase(Locale.ROOT));
        if (index > 0) {
            return value.substring(0, index).trim();
        }
        return value.trim();
    }

    private String extractJevicCertificateNo(String text) {
        Matcher matcher = Pattern.compile(
                "Certificate\\s+No\\.?\\s*[:\\.]?\\s*(?:Date\\s+of\\s+Issue\\s*[:\\.]?\\s*)*(LK1-[A-Z0-9]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }

        matcher = Pattern.compile(
                "Certificate\\s+No\\.?\\s*[:\\.]?\\s*\\n\\s*(LK1-[A-Z0-9]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return findJevicCertNo(text);
    }

    private String extractJevicDate(String text, String labelPattern, String... afterStops) {
        Matcher matcher = Pattern.compile(
                labelPattern + "\\s*[:\\.]?\\s*(?:"
                        + joinStopLabels(afterStops)
                        + "\\s*[:\\.]?\\s*)*(\\d{1,2}/\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = Pattern.compile(
                labelPattern + "\\s*[:\\.]?\\s*\\n\\s*(\\d{1,2}/\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractJevicOdometer(String text) {
        Matcher matcher = Pattern.compile(
                "\\bCurrent\\s+Odometer\\s+Reading\\b[\\s\\S]{0,160}?(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)\\b)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(matcher.group(1));
            if (value != null) {
                return value;
            }
        }

        matcher = Pattern.compile(
                "\\bCurrent\\s+Odometer\\s+Reading\\b\\s*[:\\.]?\\s*"
                        + "(?:Auction\\s+Reading\\s*/\\s*Date\\s*[:\\.]?\\s*)*"
                        + "(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(matcher.group(1));
            if (value != null && !isJevicFieldLabel(value)) {
                return value;
            }
        }

        matcher = Pattern.compile(
                "\\bCurrent\\s+Odometer\\s+Reading\\b\\s*[:\\.]?\\s*\\n\\s*(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }

        matcher = Pattern.compile(
                "\\bCurrent\\s+Odometer\\s+Reading\\b\\s*[:\\.]?\\s*(?:Auction\\s+Reading\\s*/\\s*Date\\s*[:\\.]?\\s*)*\\n\\s*(\\d+[\\d,]*\\s*(?:km|KM|Km|miles|Miles)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }
        return null;
    }

    private String extractJevicScalar(String text, String labelPattern, String... stopLabels) {
        String stops = joinStopLabels(stopLabels);
        Matcher matcher = Pattern.compile(
                labelPattern + "(?:\\s*#|\\s*No\\.?)?\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String inline = trimBeforeNextJevicLabel(matcher.group(1), stopLabels);
            String cleaned = cleanJevicValue(inline);
            if (cleaned != null) {
                return cleaned;
            }
        }

        matcher = Pattern.compile(
                labelPattern + "(?:\\s*#|\\s*No\\.?)?\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String nextLine = trimBeforeNextJevicLabel(matcher.group(1), stopLabels);
            return cleanJevicValue(nextLine);
        }
        return null;
    }

    private String extractJevicReadingRow(String text, String rowLabel, String... followingRowLabels) {
        String labelPattern = rowLabel.replace(" ", "\\s+");
        Matcher matcher = Pattern.compile(
                "(?:^|\\n)\\s*" + labelPattern + "\\s*/\\s*Date\\s*[:\\.]?\\s*([^\\n]{0,60})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (!matcher.find()) {
            return null;
        }

        String raw = matcher.group(1).trim();
        for (int i = 0; i < followingRowLabels.length; i++) {
            String following = followingRowLabels[i];
            if (raw.toLowerCase(Locale.ROOT).contains(following.toLowerCase(Locale.ROOT))) {
                return null;
            }
        }
        if (isJevicFieldLabel(raw) || isOdometerReadingValue(raw)) {
            return null;
        }
        return cleanReadingValue(raw);
    }

    private boolean isOdometerReadingValue(String value) {
        return value != null && value.trim().matches("(?i)\\d+[\\d,]*\\s*(?:km|miles)?");
    }

    private String trimBeforeNextJevicLabel(String value, String... stopLabels) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        String[] defaults = new String[] {
                "Make", "Model", "Engine Capacity", "Year of First Registration",
                "Chassis Number", "Engine Number", "Inspected Mileage", "Inspection Date",
                "Date of Inspection", "Inspection Branch", "Location", "Certificate No",
                "Date of Issue", "Current Odometer", "Remarks", "Auction Reading", "Dealer Reading",
                "De-Registration Reading", "De Registration Reading", "Authorized"
        };

        int cut = trimmed.length();
        for (int i = 0; i < defaults.length; i++) {
            Pattern pattern = Pattern.compile(
                    "\\s+" + Pattern.quote(defaults[i]) + "(?:\\s*[:\\.]?|\\s+No\\.?\\s*[:\\.]?)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find() && matcher.start() < cut) {
                cut = matcher.start();
            }
        }
        for (int i = 0; i < stopLabels.length; i++) {
            Pattern pattern = Pattern.compile(
                    "\\s+" + Pattern.quote(stopLabels[i]) + "(?:\\s*[:\\.]?|\\s+No\\.?\\s*[:\\.]?)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find() && matcher.start() < cut) {
                cut = matcher.start();
            }
        }
        return trimmed.substring(0, cut).trim();
    }

    private String joinStopLabels(String... labels) {
        if (labels == null || labels.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                builder.append("|");
            }
            builder.append("(?:").append(labels[i]).append(")");
        }
        return builder.toString();
    }

    private String cleanJevicValue(String value) {
        String cleaned = clean(value);
        if (cleaned == null || isJevicFieldLabel(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String cleanReadingValue(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.replaceAll("\\s+/\\s*", " / ").trim();
        if (normalized.matches("(?i)[-–—\\s/]+")) {
            return null;
        }
        if (isJevicFieldLabel(normalized)) {
            return null;
        }
        return clean(normalized);
    }

    private boolean isJevicFieldLabel(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.endsWith(":") && trimmed.length() < 60) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.contains("reading / date")
                || lower.contains("reading/ date")
                || lower.contains("reading /date")
                || lower.startsWith("certificate no")
                || lower.startsWith("date of issue")
                || lower.startsWith("date of inspection")
                || lower.startsWith("current odometer reading")
                || lower.startsWith("inspected mileage")
                || lower.startsWith("inspection branch")
                || lower.startsWith("engine capacity")
                || lower.startsWith("year of first registration")
                || lower.startsWith("chassis number")
                || lower.startsWith("engine number")
                || lower.startsWith("inspection date")
                || lower.equals("remarks")
                || lower.startsWith("dealer reading")
                || lower.startsWith("auction reading")
                || lower.startsWith("de-registration reading")
                || lower.startsWith("de registration reading")
                || lower.equals("location");
    }

    public AuctionParseResult parsePage2(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Page 2 (customs declaration) was empty.");
            return result;
        }
        String normalized = normalize(text);

        result.put("customsReference", extractCustomsReference(normalized));
        result.put("declarationType", extractLabel(normalized, "Declaration"));
        result.put("totalPackages", extractLabel(normalized, "Total Packages", "Total packages"));
        result.put("exporterName", extractPartyName(normalized, "Exporter", "Supplier"));
        result.put("consigneeName", extractPartyName(normalized, "Consignee"));
        result.put("consigneeTin", extractTin(normalized, "Consignee"));
        result.put("declarantName", extractPartyName(normalized, "Declarant", "Representative"));
        result.put("declarantTin", extractTin(normalized, "Declarant"));
        result.put("countryExport", extractLabel(normalized, "Country of Export"));
        result.put("countryDestination", extractLabel(normalized, "Country of Destination"));
        result.put("countryOrigin", extractLabel(normalized, "Country of Origin"));
        result.put("vesselFlight", extractLabel(normalized, "Vessel", "Flight"));
        result.put("deliveryTerms", extractLabel(normalized, "Delivery Terms"));
        result.put("voyageNoDate", extractLabel(normalized, "Voyage No", "Voyage"));
        result.put("placeLoadingDischarging", extractLabel(normalized, "Place of Loading", "Loading / Discharging"));
        result.put("currencyInvoiced", extractCurrencyAmount(normalized, "Currency")[0]);
        result.put("totalAmountInvoiced", extractCurrencyAmount(normalized, "Currency")[1]);
        result.put("exchangeRate", extractLabel(normalized, "Exchange Rate"));
        result.put("bankName", extractLabel(normalized, "Bank Name"));
        result.put("bankCode", extractLabel(normalized, "Bank Code"));
        result.put("bankBranch", extractLabel(normalized, "Branch"));
        result.put("bankReference", extractLabel(normalized, "Ref No", "Reference"));
        result.put("locationOfGoods", extractLabel(normalized, "Location of Goods"));
        result.put("hsCode", extractHsCode(normalized));
        result.put("grossMassKg", extractLabel(normalized, "Gross Mass"));
        result.put("netMassKg", extractLabel(normalized, "Net Mass"));
        result.put("blAwbNo", extractBlAwbNo(normalized));
        result.put("goodsDescription", extractGoodsDescription(normalized));
        result.put("modelSpec", extractModelSpec(normalized));
        result.put("yearOfManufacture", extractInlineLabel(normalized, "Y/M", "Year of Manufacture"));
        result.put("dateOfRegistration", extractInlineLabel(normalized, "D/R", "Date of Registration"));
        result.put("clearanceChassisNo", extractInlineLabel(normalized, "CH.NO", "CH NO", "Chassis No"));
        result.put("clearanceEngineNo", extractInlineLabel(normalized, "ENG.NO", "ENG NO", "Engine No"));
        result.put("itemPrice", extractLabel(normalized, "Item Price"));
        result.put("valueNcy", extractLabel(normalized, "Value NCY", "Value"));
        result.put("engineCapacityCc", extractEngineCc(normalized));
        result.put("taxCid", extractTaxAmount(normalized, "CID"));
        result.put("taxSur", extractTaxAmount(normalized, "SUR"));
        result.put("taxVat", extractTaxAmount(normalized, "VAT"));
        result.put("taxXid", extractTaxAmount(normalized, "XID"));
        result.put("taxVel", extractTaxAmount(normalized, "VEL"));
        result.put("taxOther", extractTaxAmount(normalized, "COM", "EXM", "SEL"));
        result.put("totalTaxAmount", extractTotalTax(normalized));
        result.put("invoiceFob", extractInvoiceLine(normalized, "FOB"));
        result.put("invoiceFreight", extractInvoiceLine(normalized, "FREIGHT"));
        result.put("invoiceInsurance", extractInvoiceLine(normalized, "INSURANCE"));
        result.put("invoiceTotal", extractInvoiceLine(normalized, "TOTAL"));
        result.put("declarantDate", extractDeclarationDate(normalized));

        if (result.getFields().get("clearanceChassisNo") == null) {
            result.put("clearanceChassisNo", findChassis(normalized));
        }

        finish(result, "Customs declaration (page 2)");
        return result;
    }

    public AuctionParseResult parsePage3(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Page 3 (assessment notice) was empty.");
            return result;
        }
        String normalized = normalize(text);

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

        result.put("assessmentTaxOtc", extractTaxAmount(normalized, "OTC"));
        result.put("assessmentTaxCom", extractTaxAmount(normalized, "COM"));
        result.put("assessmentTaxExm", extractTaxAmount(normalized, "EXM"));
        result.put("assessmentTaxCid", extractTaxAmount(normalized, "CID"));
        result.put("assessmentTaxSur", extractTaxAmount(normalized, "SUR"));
        result.put("assessmentTaxXid", extractTaxAmount(normalized, "XID"));
        result.put("assessmentTaxVat", extractTaxAmount(normalized, "VAT"));
        result.put("assessmentTaxVel", extractTaxAmount(normalized, "VEL"));
        result.put("assessmentTotalAssessed", extractLabeledAmount(normalized,
                "Total assessed amount for the declaration", "Total assessed amount"));
        result.put("assessmentTotalPaid", extractLabeledAmount(normalized, "Total amount paid"));

        finish(result, "Assessment notice (page 3)");
        return result;
    }

    public AuctionParseResult parsePage4(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Working sheet was empty.");
            return result;
        }
        String normalized = normalize(text);

        result.put("worksheetRef", extractWorksheetRef(normalized));
        result.put("worksheetHsCode", firstNonNull(
                extractAfterLabel(normalized, "H.S. CODE", "H.S.CODE", "HS CODE", "HS. CODE"),
                extractWorksheetHsCode(normalized)
        ));
        result.put("worksheetVehicleType", firstNonNull(
                extractAfterLabel(normalized, "TYPE OF VEHICLE", "Type of Vehicle"),
                extractUnitUsedVehicle(normalized)
        ));
        result.put("worksheetReferenceNo", extractAfterLabel(normalized, "Reference No", "Reference No."));
        result.put("worksheetVesselName", extractAfterLabel(normalized,
                "NAME OF VESSEL", "Name of Vessel"));
        result.put("worksheetChassisNo", firstNonNull(
                extractAfterLabel(normalized, "CHASSIS NOS", "CHASSIS NO", "Chassis Nos", "Chassis No"),
                findChassis(normalized)
        ));
        result.put("worksheetAgentsFob", extractAgentsFob(normalized));
        result.put("worksheetInvoicedFob", extractAmountAfter(normalized, "Invoiced FOB", "Invoice FOB"));
        result.put("worksheetAgentsFreight", extractAmountAfter(normalized, "Agents Freight", "Agent Freight"));
        result.put("worksheetInvoicedFreight", extractAmountAfter(normalized, "Invoiced Freight", "Invoice Freight"));
        result.put("worksheetAgentsInsurance", extractAmountAfter(normalized, "Agents Insurance", "Agent Insurance"));
        result.put("worksheetInvoicedInsurance", extractAmountAfter(normalized, "Invoiced Insurance", "Invoice Insurance"));
        result.put("worksheetOptionsValue", extractAmountAfter(normalized, "Total Value of Options"));
        result.put("worksheetBlFreightCalc", extractBlFreightCalc(normalized));
        result.put("worksheetBlFreightAmount", firstNonNull(
                extractLastAmount(extractBlFreightCalc(normalized)),
                extractAmountAfter(normalized, "B/L Freight Calculation", "BL Freight Calculation")
        ));
        result.put("worksheetBlDate", extractYmdAfter(normalized, "Date of B/L", "Date of BL"));
        result.put("worksheetManufactureDate", extractYmdAfter(normalized,
                "Date of Manufacture", "Date of Manufact"));
        result.put("worksheetAgeDifference", extractAgeDifference(normalized));
        result.put("worksheetFirstRegistrationDate", extractYmdAfter(normalized,
                "Date of 1st Registration", "Date of First Registration"));
        result.put("worksheetWebsiteValue", extractAmountAfter(normalized, "Website Value"));
        result.put("worksheetLocalTaxes", extractAmountAfter(normalized, "Less Local Taxes", "Local Taxes"));
        result.put("worksheetFifteenPercent", firstNonNull(
                extractAmountAfter(normalized, "15% of Value", "Less 15% of Value"),
                extractFifteenPercent(normalized)
        ));
        result.put("worksheetFobValue85", extractFobValue85(normalized));
        result.put("worksheetLcNo", extractLcNo(normalized));
        result.put("worksheetLcAmount", extractLcAmount(normalized));
        result.put("worksheetLcBank", extractLcBank(normalized));
        result.put("worksheetLcImporter", extractWorksheetImporter(normalized));
        result.put("worksheetLcIssueDate", firstNonNull(
                extractSlashDateAfter(normalized, "Date of Issue"),
                extractYmdAfter(normalized, "Date of Issue")
        ));
        result.put("worksheetLcExpiryDate", extractSlashDateAfter(normalized, "Date of Expiry"));
        result.put("worksheetLcAmendmentDate", extractSlashDateAfter(normalized, "Date of Amendment"));
        result.put("worksheetClearingAgent", extractClearingAgent(normalized));
        result.put("worksheetFiscalFob", extractAmountAfter(normalized, "FOB for Fiscal Levies"));
        result.put("worksheetFiscalFreight", extractAmountAfter(normalized,
                "Freight Charges for Fiscal Levies"));
        result.put("worksheetFiscalInsurance", extractAmountAfter(normalized,
                "Insurance Charges for Fiscal Levies"));
        result.put("worksheetFiscalOptions", extractAmountAfter(normalized,
                "Value of Options for Fiscal Levies"));
        result.put("worksheetFiscalTotal", extractAmountAfter(normalized,
                "Total Value for Fiscal Levies"));

        finish(result, "Working sheet for motor vehicles");
        return result;
    }

    private String extractWorksheetRef(String text) {
        Matcher matcher = Pattern.compile("\\b([A-Z]\\d[-\\s]\\d{5,}[-\\s][A-Z]{2})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", "-");
        }
        return extractAfterLabel(text, "Ref", "Reference ID");
    }

    private String extractWorksheetHsCode(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d{4}\\.\\d{2}\\.\\d{2})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractHsCode(text);
    }

    private String extractAgentsFob(String text) {
        Matcher calc = Pattern.compile(
                "(?:/\\s*110\\s*[Xx×]\\s*100\\s*=\\s*)(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (calc.find()) {
            return calc.group(1);
        }
        Matcher line = Pattern.compile(
                "Agents?\\s+FOB\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (line.find()) {
            String lastEquals = extractLastAmountAfterEquals(line.group(1));
            if (lastEquals != null) {
                return lastEquals;
            }
        }
        return extractAmountAfter(text, "Agents FOB", "Agent FOB");
    }

    private String extractBlFreightCalc(String text) {
        Matcher matcher = Pattern.compile(
                "B/?L\\s*Freight\\s*Calculation\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = clean(matcher.group(1));
            if (value != null) {
                return value;
            }
        }
        matcher = Pattern.compile(
                "B/?L\\s*Freight\\s*Calculation\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        matcher = Pattern.compile(
                "(USD\\s+[\\d,.]+\\s*@\\s*[\\d.]+\\s*=\\s*JPY\\s*@\\s*[\\d.]+\\s*=\\s*[\\d,.]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String extractAgeDifference(String text) {
        Matcher matcher = Pattern.compile(
                "Age\\s+Difference(?:\\s+for\\s+I\\.?C\\.?L)?\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d+)\\s+(\\d+)\\s+(\\d+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " years " + matcher.group(2) + " months " + matcher.group(3) + " days";
        }
        return extractAfterLabel(text, "Age Difference for I.C.L", "Age Difference");
    }

    private String extractLcNo(String text) {
        String labeled = extractAfterLabel(text, "LC No", "L/C No", "LC No.");
        if (labeled != null) {
            Matcher token = Pattern.compile("([A-Z]{5,}\\d{8,})", Pattern.CASE_INSENSITIVE).matcher(labeled);
            if (token.find()) {
                return token.group(1).toUpperCase(Locale.ROOT);
            }
        }
        Matcher matcher = Pattern.compile("\\b([A-Z]{5,}\\d{8,})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return labeled;
    }

    private String extractLcAmount(String text) {
        Matcher matcher = Pattern.compile(
                "LC\\s*No\\.?.{0,120}?Amount\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractAmountAfter(text, "Amount");
    }

    private String extractLcBank(String text) {
        Matcher matcher = Pattern.compile(
                "(?:^|\\n)\\s*Bank\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanWorksheetValue(matcher.group(1));
            if (isUsableWorksheetValue(value) && value.toUpperCase(Locale.ROOT).contains("BANK")) {
                return value;
            }
        }
        matcher = Pattern.compile(
                "(?:^|\\n)\\s*Bank\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanWorksheetValue(matcher.group(1));
            if (isUsableWorksheetValue(value) && value.toUpperCase(Locale.ROOT).contains("BANK")) {
                return value;
            }
        }
        matcher = Pattern.compile("\\b([A-Z][A-Z ]{2,}BANK[A-Z ]*(?:LIMITED|LTD\\.?|PLC)?)\\b").matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String extractWorksheetImporter(String text) {
        Matcher matcher = Pattern.compile(
                "\\b([A-Z][A-Z ]{3,}(?:MOTOR\\s+)?TRADING(?:\\s+COMPANY)?)\\b"
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        matcher = Pattern.compile(
                "\\b([A-Z][A-Z ]{6,}(?:MOTORS|COMPANY|PVT)[A-Z ]*)\\b"
        ).matcher(text);
        while (matcher.find()) {
            String value = clean(matcher.group(1));
            if (value == null) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            if (upper.contains("BANK")
                    || upper.contains("CLEARING")
                    || upper.contains("FORWARDING")
                    || upper.contains("WORKING SHEET")
                    || upper.contains("MOTOR VEHICLES")) {
                continue;
            }
            return value;
        }
        return extractAfterLabel(text, "Importer", "Company Name");
    }

    private String extractClearingAgent(String text) {
        String labeled = extractAfterLabel(text, "Clearing Agent Mentioned", "Clearing Agent");
        if (labeled != null && labeled.toUpperCase(Locale.ROOT).contains("CLEARING")) {
            return labeled;
        }
        Matcher matcher = Pattern.compile(
                "\\b([A-Z][A-Z0-9 &./]{2,}CLEARING[A-Z0-9 &./]*)\\b"
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return labeled;
    }

    private String extractUnitUsedVehicle(String text) {
        Matcher matcher = Pattern.compile(
                "(0?1\\s+UNIT\\s+USED\\s+[^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanWorksheetValue(matcher.group(1));
        }
        return null;
    }

    private String extractFifteenPercent(String text) {
        Matcher matcher = Pattern.compile(
                "15\\s*%\\s*of\\s+Value[^\\n]{0,40}?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = Pattern.compile(
                "15\\s*%\\s*of\\s+Value\\s*[:\\.]?\\s*\\n\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractFobValue85(String text) {
        Matcher matcher = Pattern.compile(
                "FOB\\s+Value\\s*\\(?\\s*85\\s*%?\\s*\\)?[^\\n]{0,30}?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = Pattern.compile(
                "FOB\\s+Value\\s*\\(?\\s*85\\s*%?\\s*\\)?\\s*[:\\.]?\\s*\\n\\s*(?:JPY\\s*)?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractAmountAfter(text, "FOB Value (85%)", "FOB Value 85%");
    }

    private String extractAfterLabel(String text, String... labels) {
        for (int i = 0; i < labels.length; i++) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(labels[i]) + "\\s*[:\\.]?\\s*([^\\n]*)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                String value = cleanWorksheetValue(matcher.group(1));
                if (isUsableWorksheetValue(value)) {
                    return value;
                }
                Matcher next = Pattern.compile("^\\n\\s*([^\\n]+)").matcher(text.substring(matcher.end()));
                if (next.find()) {
                    value = cleanWorksheetValue(next.group(1));
                    if (isUsableWorksheetValue(value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private String extractAmountAfter(String text, String... labels) {
        for (int i = 0; i < labels.length; i++) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(labels[i]) + "[^\\n]{0,80}?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            matcher = Pattern.compile(
                    Pattern.quote(labels[i]) + "\\s*[:\\.]?\\s*\\n\\s*(?:JPY\\s*)?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private String extractYmdAfter(String text, String... labels) {
        for (int i = 0; i < labels.length; i++) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(labels[i]) + "\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d{4})\\s+(\\d{1,2})\\s+(\\d{1,2})",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return pad2(matcher.group(3)) + "/" + pad2(matcher.group(2)) + "/" + matcher.group(1);
            }
        }
        return extractSlashDateAfter(text, labels);
    }

    private String extractSlashDateAfter(String text, String... labels) {
        for (int i = 0; i < labels.length; i++) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(labels[i]) + "\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d{1,2}/\\d{1,2}/\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private String extractLastAmountAfterEquals(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("=\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)").matcher(text);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    private boolean isUsableWorksheetValue(String value) {
        return value != null && !isWorksheetFieldLabel(value);
    }

    private boolean isWorksheetFieldLabel(String value) {
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("TYPE OF VEHICLE")
                || upper.startsWith("NAME OF VESSEL")
                || upper.startsWith("CHASSIS")
                || upper.startsWith("AGENTS")
                || upper.startsWith("INVOICED")
                || upper.startsWith("DATE OF")
                || upper.startsWith("LC NO")
                || upper.startsWith("L/C")
                || upper.startsWith("H.S")
                || upper.startsWith("HS CODE")
                || upper.startsWith("REFERENCE")
                || upper.startsWith("WEBSITE VALUE")
                || upper.startsWith("FOB VALUE")
                || upper.startsWith("FOB FOR")
                || upper.equals("BANK")
                || upper.equals("AMOUNT")
                || upper.startsWith("CLEARING AGENT")
                || upper.startsWith("TOTAL VALUE")
                || upper.startsWith("VALUE OF OPTIONS")
                || upper.startsWith("FREIGHT CHARGES")
                || upper.startsWith("INSURANCE CHARGES")
                || upper.startsWith("LESS LOCAL")
                || upper.startsWith("AGE DIFFERENCE")
                || upper.startsWith("WORKING SHEET")
                || upper.equals("YEAR")
                || upper.equals("MONTH")
                || upper.equals("DATE")
                || upper.startsWith("DETAILS OF");
    }

    private String extractLastAmount(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)").matcher(text);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    private String cleanWorksheetValue(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        if (cleaned.isEmpty() || "-".equals(cleaned) || "—".equals(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String pad2(String value) {
        if (value == null) {
            return "";
        }
        return value.length() == 1 ? "0" + value : value;
    }

    private String extractAssessmentOffice(String text) {
        Matcher matcher = Pattern.compile(
                "([A-Za-z][A-Za-z ]+Import Office(?:\\s*-\\s*Sea)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return extractLabel(text, "Customs Office", "Office");
    }

    private String extractAssessmentNoticeRef(String text) {
        Matcher matcher = Pattern.compile(
                "\\b(\\d{4}\\s+[A-Z]{2,}\\d*\\s+[IA]\\s+\\d{3,})\\b",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String extractAssessmentModel(String text) {
        Matcher matcher = Pattern.compile("\\b(IM\\s*\\d)\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        }
        return extractLabel(text, "Model");
    }

    private String extractLabeledDateRef(String text, String label) {
        Matcher matcher = Pattern.compile(
                Pattern.quote(label) + "\\s*[:\\.]?\\s*(\\d{1,2}/\\d{1,2}/\\d{4}\\s+[IA]\\s+\\d{3,})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        matcher = Pattern.compile(
                Pattern.quote(label) + "[^\\n]{0,40}?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+[IA]\\s+\\d{3,})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String extractDeclarantReference(String text) {
        Matcher matcher = Pattern.compile(
                "Declarant reference\\s*[:\\.]?\\s*(\\d{4}\\s*#?\\s*\\d+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1).replaceAll("\\s+", " "));
        }
        matcher = Pattern.compile("\\b(\\d{4}\\s*#\\s*\\d+)\\b").matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1).replaceAll("\\s+", " "));
        }
        return null;
    }

    private String extractAssessmentPackages(String text) {
        Matcher matcher = Pattern.compile(
                "Packages\\s*[:\\.]?\\s*([\\d,]+(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractChaExpiry(String text) {
        Matcher matcher = Pattern.compile(
                "CHA\\s*EXP\\s*[:\\.]?\\s*(\\d{1,2}/\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String[] extractAssessmentParty(String text, String heading, String stopHeading) {
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
        String name = extractLabel(block, "Name");
        if (name != null && (name.equalsIgnoreCase(heading) || name.toUpperCase(Locale.ROOT).startsWith("ID"))) {
            name = null;
        }
        if (name == null) {
            Matcher nameMatcher = Pattern.compile("(?m)^\\s*([A-Z][A-Z0-9 .,&'/\\-]{6,})\\s*$").matcher(block);
            while (nameMatcher.find()) {
                String candidate = clean(nameMatcher.group(1));
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
        String address = extractLabel(block, "Address");
        if (address == null) {
            Matcher addressMatcher = Pattern.compile(
                    "(?m)^\\s*(NO\\.?\\s*\\d[^\\n]+|\\d+[A-Z0-9 /\\-,]+[A-Z][^\\n]{4,})\\s*$",
                    Pattern.CASE_INSENSITIVE
            ).matcher(block);
            if (addressMatcher.find()) {
                address = clean(addressMatcher.group(1));
            }
        }
        return new String[] { id, name, address };
    }

    private String extractLabeledAmount(String text, String... labels) {
        for (int i = 0; i < labels.length; i++) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(labels[i]) + "\\s*[:\\.]?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private void finish(AuctionParseResult result, String label) {
        boolean any = !result.getFields().isEmpty();
        result.setSuccess(any);
        result.setMessage(any
                ? "Filled " + result.getFields().size() + " fields from " + label + ". Please review."
                : "The document was read, but fields could not be mapped. Please fill them manually.");
    }

    private String normalize(String text) {
        return text.replace('\r', '\n').replaceAll("[ \t]+", " ").replaceAll("\n{3,}", "\n\n");
    }

    private String extractBlAwbNo(String text) {
        Matcher matcher = Pattern.compile(
                "(?:BL\\s*/\\s*AWB|B\\s*/\\s*L|AWB)(?:\\s*(?:No\\.?|#))?\\s*[:\\.]?\\s*([A-Z0-9][A-Z0-9\\-/]{3,39})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        String fallback = extractLabel(text, "BL / AWB", "BL/", "AWB");
        if (fallback == null) {
            return null;
        }
        String firstToken = fallback.split("\\s+")[0];
        return clean(firstToken);
    }

    private String extractLabel(String text, String... labels) {
        for (String label : labels) {
            Pattern pattern = Pattern.compile(
                    Pattern.quote(label) + "(?:\\s*#|\\s*No\\.?)?\\s*[:\\.]?\\s*([^\\n]+)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return clean(matcher.group(1));
            }
        }
        return null;
    }

    private String extractInlineLabel(String text, String... labels) {
        for (String label : labels) {
            Pattern pattern = Pattern.compile(
                    label + "\\s*[:\\.]?\\s*([^\\n\\s/]+(?:[/\\-][^\\n\\s/]+)*)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return clean(matcher.group(1));
            }
        }
        return null;
    }

    private String extractTableCell(String text, String... rowLabels) {
        for (String rowLabel : rowLabels) {
            Pattern pattern = Pattern.compile(
                    Pattern.quote(rowLabel) + "(?:\\s*/\\s*Date)?\\s*[:\\.]?\\s*([^\\n|]+)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String value = clean(matcher.group(1));
                if (value != null && !value.equals("-") && !value.equalsIgnoreCase("n/a")) {
                    return value;
                }
            }
        }
        return null;
    }

    private String extractReadingWithDate(String text, String... rowLabels) {
        for (String rowLabel : rowLabels) {
            Pattern pattern = Pattern.compile(
                    Pattern.quote(rowLabel) + "\\s*/\\s*Date\\s*[:\\.]?\\s*([^\\n|]+)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return clean(matcher.group(1));
            }
        }
        return null;
    }

    private String extractReadingDate(String text, String rowLabel) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(rowLabel) + "\\s*/\\s*Date\\s*[:\\.]?\\s*([^\\n|]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String extractBlockAfter(String text, String heading, String stopWord) {
        int index = text.toUpperCase().indexOf(heading.toUpperCase());
        if (index < 0) {
            return null;
        }
        String tail = text.substring(index + heading.length());
        int stop = tail.toUpperCase().indexOf(stopWord.toUpperCase());
        if (stop > 0) {
            tail = tail.substring(0, stop);
        }
        return clean(tail.replace('\n', ' '));
    }

    private String extractContact(String text, String type) {
        Matcher matcher = Pattern.compile(type + "\\s*[:\\.]?\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String extractWebsite(String text) {
        Matcher matcher = Pattern.compile("(www\\.[\\w.\\-]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractLabel(text, "Website");
    }

    private String findChassis(String text) {
        Matcher matcher = Pattern.compile("\\b([A-Z0-9]{3,10}-[0-9]{5,10})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String findJevicCertNo(String text) {
        Matcher matcher = Pattern.compile("\\b(LK1-[A-Z0-9]+)\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    private String extractCustomsReference(String text) {
        Matcher matcher = Pattern.compile(
                "Customs Reference(?:\\s+Number)?\\s*[:\\.]?\\s*(\\d{4,8})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher manifest = Pattern.compile("Manifest[^\\d]*(\\d{4,8})").matcher(text);
        if (manifest.find()) {
            return manifest.group(1);
        }
        return null;
    }

    private String extractPartyName(String text, String... keywords) {
        for (int i = 0; i < keywords.length; i++) {
            Pattern pattern = Pattern.compile(
                    keywords[i] + "[^\\n]{0,40}?\\n\\s*([A-Z][A-Z0-9 .,&()\\-/]+(?:LTD|CO\\.|COMPANY|PVT)?[^\\n]*)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return clean(matcher.group(1));
            }
        }
        return null;
    }

    private String extractTin(String text, String partyKeyword) {
        Pattern pattern = Pattern.compile(
                partyKeyword + "[\\s\\S]{0,300}?TIN\\s*[:\\.]?\\s*(\\d{10,14})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher any = Pattern.compile("TIN\\s*[:\\.]?\\s*(\\d{10,14})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (any.find()) {
            return any.group(1);
        }
        return null;
    }

    private String[] extractCurrencyAmount(String text, String label) {
        Matcher matcher = Pattern.compile(
                label + "[^\\n]{0,40}?(JPY|USD|LKR)\\s*([\\d,]+(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return new String[] { matcher.group(1).toUpperCase(), matcher.group(2) };
        }
        return new String[] { null, null };
    }

    private String extractHsCode(String text) {
        Matcher matcher = Pattern.compile("\\b(8703\\.\\d{2}\\.\\d{2})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractGoodsDescription(String text) {
        Matcher matcher = Pattern.compile(
                "USED\\s+[A-Z]+\\s+[A-Z0-9 ]+(?:CAR|WAGON|VEHICLE)[^\\n]*",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group());
        }
        return extractLabel(text, "Description of goods", "Goods Description");
    }

    private String extractModelSpec(String text) {
        Matcher matcher = Pattern.compile(
                "\\b(\\d[A-Z]{2,3}-[A-Z0-9]+[^\\n]{0,80}(?:CC|cc)[^\\n]*)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String extractEngineCc(String text) {
        Matcher matcher = Pattern.compile("(\\d{3,4})\\s*CC", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " CC";
        }
        return null;
    }

    private static final String TAX_CODE_BOUNDARY = "OTC|COM|EXM|CID|SUR|XID|VAT|VEL|SEL";

    private String extractTaxAmount(String text, String... taxCodes) {
        for (int i = 0; i < taxCodes.length; i++) {
            String snippet = taxCodeSnippet(text, taxCodes[i]);
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

    private String taxCodeSnippet(String text, String taxCode) {
        Matcher matcher = Pattern.compile(
                "\\b" + Pattern.quote(taxCode) + "\\b([\\s\\S]*?)(?=\\b(?:" + TAX_CODE_BOUNDARY + ")\\b|\\z)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private String amountFromSnippet(String snippet) {
        if (snippet == null || snippet.trim().isEmpty()) {
            return null;
        }

        Matcher grouped = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})+)").matcher(snippet);
        if (grouped.find()) {
            return formatThousands(grouped.group(1).replaceAll("\\D", ""));
        }

        Matcher spaced = Pattern.compile("(\\d{1,3}(?:\\s+\\d{3})+)").matcher(snippet);
        if (spaced.find()) {
            return formatThousands(spaced.group(1).replaceAll("\\D", ""));
        }

        Matcher compact = Pattern.compile("(\\d{4,})").matcher(snippet);
        if (compact.find()) {
            return formatThousands(compact.group(1));
        }

        Matcher small = Pattern.compile("(\\d{1,3})(?!\\d)").matcher(snippet);
        String lastSmall = null;
        while (small.find()) {
            lastSmall = small.group(1);
        }
        return lastSmall;
    }

    private String formatThousands(String digits) {
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
        result.append(digits.substring(0, lead));
        for (int i = lead; i < length; i += 3) {
            result.append(',').append(digits.substring(i, i + 3));
        }
        return result.toString();
    }

    private String extractTotalTax(String text) {
        Matcher matcher = Pattern.compile(
                "Total Tax(?:\\s+Amount)?\\s*[:\\.]?\\s*(\\d{1,3}(?:,\\d{3})*)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractInvoiceLine(String text, String keyword) {
        Matcher matcher = Pattern.compile(
                "\\b" + keyword + "\\b\\s*[:\\.]?\\s*([\\d,]+(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractDeclarationDate(String text) {
        Matcher matcher = Pattern.compile(
                "(?:Declarant|Authorised|Authorized)[\\s\\S]{0,120}?(\\d{1,2}/\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s{2,}", " ");
        cleaned = cleaned.replaceAll("^[.:|\\-/]+", "").replaceAll("[\\-/]+$", "").trim();
        if (cleaned.isEmpty() || isEmptyToken(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private boolean isEmptyToken(String value) {
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
