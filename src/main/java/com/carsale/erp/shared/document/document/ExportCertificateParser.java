package com.carsale.erp.shared.document.document;

import java.util.regex.Matcher;

import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.regex.RegexConstants;

@Service
public class ExportCertificateParser implements DocumentParser {

    @Value("${app.ocr.export.language:jpn}")
    private String language;

    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The export certificate was empty.");
            return result;
        }

        String normalized = CustomsDocumentParserUtils.normalize(text);
        boolean japanese = looksJapanese(normalized);
        result.put("certificateNo", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.CERTIFICATE_NO, "Arrangement", "Export"),
                extractLabeled(normalized, RegexConstants.Export.JP_NUMBER, "整理番号", "輸出")
        ));
        result.put("arrangementNo", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.ARRANGEMENT_NO, "Registration", "Export"),
                extractLabeled(normalized, "整理番号", "車両番号", "輸出"),
                findLongNumber(normalized)
        ));
        result.put("registrationNo", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.REGISTRATION_NO, "Date of Registration", "First"),
                extractLabeled(normalized, RegexConstants.Export.MOTOR_VEHICLE_NUMBER, "Grant Date", "First"),
                extractLabeled(normalized, "車両番号", "交付年月日", "初度")
        ));
        result.put("registrationDate", normalizeDate(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Date of Registration", "First Reg", "Chassis"),
                extractLabeled(normalized, "Grant Date", "First Grant", "Classification"),
                extractLabeled(normalized, "交付年月日", "初度検査年月", "自動車の種別")
        )));
        result.put("firstRegDate", normalizeDate(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.FIRST_REG_DATE, "Chassis", "Trademark"),
                extractLabeled(normalized, "First Grant Date", "Classification", "Use"),
                extractLabeled(normalized, "初度検査年月", "自動車の種別", "用途")
        )));
        result.put("issueDate", normalizeDate(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Issue Date", "Export scheduled", "Notes"),
                extractLabeled(normalized, "交付年月日", "初度検査年月", "自動車の種別")
        )));
        result.put("chassisVin", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.CHASSIS_NO, "Trademark", "Maker"),
                extractLabeled(normalized, "Maker's serial number", "Fixed Number", "乗車"),
                extractLabeled(normalized, "車台番号", "乗車定員", "最大積載量"),
                findChassis(normalized)
        ));
        result.put("make", normalizeMake(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Trademark of the maker", "Model", "Engine"),
                extractLabeled(normalized, "Maker", "Model", "Engine"),
                extractLabeled(normalized, "車名", "型式", "原動機")
        )));
        result.put("model", stripCode(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.MODEL, "Engine Model", "Engine Capacity"),
                extractLabeled(normalized, RegexConstants.Export.MODEL_JP, "原動機の型式", "燃料")
        )));
        result.put("engineModel", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Engine Model", "Name of User", "Classification of Fuel"),
                extractLabeled(normalized, "原動機の型式", "燃料の種別", "総排気量")
        ));
        result.put("vehicleClassification", normalizeClass(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Classification of Vehicle", "Use", "Purpose"),
                extractLabeled(normalized, "自動車の種別", "用途", "自家用")
        )));
        result.put("useType", normalizeUse(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.USE, "Purpose", "Type of Body"),
                extractLabeled(normalized, "用途", "自家用", "車体の形状")
        )));
        result.put("purpose", normalizePurpose(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Purpose", "Type of Body", "Fixed Number"),
                extractLabeled(normalized, "自家用・事業用の別", "車体の形状", "車台番号"),
                extractLabeled(normalized, "自家用・事業用", "車体の形状", "車台番号")
        )));
        result.put("bodyType", normalizeBody(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Type of Body", "Fixed Number", "Maxim"),
                extractLabeled(normalized, "車体の形状", "車台番号", "乗車定員")
        )));
        result.put("seatingCapacity", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, "Fixed Number"),
                extractMeasure(normalized, "乗車定員"),
                extractLabeled(normalized, "乗車定員", "最大積載量", "車両重量")
        ));
        result.put("maxCarry", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, RegexConstants.Export.MAX_CARRY),
                extractMeasure(normalized, "最大積載量"),
                extractLabeled(normalized, "最大積載量", "車両重量", "車両総重量")
        ));
        result.put("weightKg", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, RegexConstants.Export.WEIGHT),
                extractMeasure(normalized, "車両重量")
        ));
        result.put("grossWeightKg", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, RegexConstants.Export.G_WEIGHT),
                extractMeasure(normalized, RegexConstants.Export.GC_WEIGHT),
                extractMeasure(normalized, "車両総重量")
        ));
        result.put("lengthCm", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, "Length"),
                extractMeasure(normalized, "長さ")
        ));
        result.put("widthCm", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, "Width"),
                extractMeasure(normalized, "幅")
        ));
        result.put("heightCm", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, "Height"),
                extractMeasure(normalized, "高さ")
        ));
        result.put("engineCapacity", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Engine Capacity", "Classification of Fuel", "Specification"),
                extractLabeled(normalized, "総排気量又は定格出力", "前前軸重", "型式指定"),
                extractLabeled(normalized, "総排気量", "前前軸重", "型式指定")
        ));
        result.put("fuelType", normalizeFuel(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Classification of Fuel", "Specification", "Length"),
                extractLabeled(normalized, "燃料の種別", "総排気量", "前前軸重")
        )));
        result.put("specificationNo", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.SPECIFICATION_NO, "Classification No", "Length"),
                extractLabeled(normalized, "型式指定番号", "類別区分番号", "使用者")
        ));
        result.put("classificationNo", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, RegexConstants.Export.CLASSIFICATION_NO, "Name of User", "FF"),
                extractLabeled(normalized, "類別区分番号", "使用者", "所有者")
        ));
        result.put("frontAxleWeight", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, RegexConstants.Export.FF_WEIGHT),
                extractMeasure(normalized, RegexConstants.Export.F_WEIGHT),
                extractMeasure(normalized, "前前軸重")
        ));
        result.put("rearAxleWeight", CustomsDocumentParserUtils.firstNonNull(
                extractMeasure(normalized, RegexConstants.Export.RR_WEIGHT),
                extractMeasure(normalized, RegexConstants.Export.R_WEIGHT),
                extractMeasure(normalized, "後後軸重")
        ));
        result.put("frWeight", extractMeasure(normalized, RegexConstants.Export.FR_WEIGHT));
        result.put("rfWeight", extractMeasure(normalized, RegexConstants.Export.RF_WEIGHT));
        result.put("userName", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Name of User", "Address of User", "Name of Owner"),
                extractLabeled(normalized, "使用者の氏名又は名称", "使用者の住所", "所有者")
        ));
        result.put("userAddress", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Address of User", "Name of Owner", "Address of Owner"),
                extractLabeled(normalized, "使用者の住所", "所有者の氏名", "所有者の住所")
        ));
        result.put("ownerName", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Name of Owner", "Address of Owner", "Locality"),
                extractLabeled(normalized, "所有者の氏名又は名称", "所有者の住所", "使用の本拠")
        ));
        result.put("ownerAddress", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Address of Owner", "Locality", "Export scheduled"),
                extractLabeled(normalized, "所有者の住所", "使用の本拠", "輸出予定")
        ));
        result.put("localityOfUse", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Locality of principal abode of use", "Export scheduled", "Issue Date"),
                extractLabeled(normalized, "使用の本拠の位置", "輸出予定日", "備考")
        ));
        result.put("exportScheduledDate", normalizeDate(CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Export scheduled day", "Issue Date", "Notes"),
                extractLabeled(normalized, "輸出予定日", "備考", "証明書有効")
        )));
        result.put("remarks", extractRemarks(normalized));

        boolean any = !result.getFields().isEmpty();
        if (any) {
            result.put("documentType", japanese ? "Japanese" : "English");
        }
        result.setSuccess(any);
        result.setMessage(any
                ? "Filled " + result.getFields().size() + " fields from the "
                        + (japanese ? "Japanese" : "English") + " export certificate. Please review."
                : "The document was read, but fields could not be mapped. Please fill them manually.");
        result.setRawText(text);
        return result;
    }

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        return null;
    }

    @Override
    public String getProcessorId() {
        return "";
    }

    @Override
    public String getDocumentName() {
        return "export certificate";
    }

    @Override
    public String getOcrLanguage() {
        return language == null || language.trim().isEmpty() ? "jpn" : language.trim();
    }

    public boolean looksJapanese(String text) {
        if (text == null) {
            return false;
        }
        return text.contains("輸出")
                || text.contains("車台番号")
                || text.contains("車両番号")
                || text.contains("整理番号")
                || RegexConstants.Text.JAPANESE_SCRIPT_NARROW.matcher(text).find();
    }

    private String extractLabeled(String text, String label, String... stops) {
        Matcher matcher = RegexConstants.Labeled.valueAfterLabel(label).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        matcher = RegexConstants.Labeled.valueOnNextLineAfterLabel(label).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        return null;
    }

    private String extractMeasure(String text, String label) {
        Matcher matcher = RegexConstants.Export.measurementAfterLabel(label).matcher(text);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private String extractRemarks(String text) {
        Matcher matcher = RegexConstants.Export.REMARKS.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = cleanLabeled(matcher.group(1));
        if (value == null || isStampNoise(value)) {
            return null;
        }
        return value;
    }

    private String findChassis(String text) {
        Matcher matcher = RegexConstants.Identifiers.CHASSIS_WIDE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String findLongNumber(String text) {
        Matcher matcher = RegexConstants.Identifiers.LONG_NUMBER.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String normalizeDate(String value) {
        if (value == null) {
            return null;
        }
        Matcher reiwa = RegexConstants.Dates.REIWA_YMD_PATTERN.matcher(value);
        if (reiwa.find()) {
            return formatDate(2018 + Integer.parseInt(reiwa.group(1)), reiwa.group(2), reiwa.group(3));
        }
        Matcher westernJp = RegexConstants.Dates.WESTERN_JP_YMD_PATTERN.matcher(value);
        if (westernJp.find()) {
            return formatDate(Integer.parseInt(westernJp.group(1)), westernJp.group(2), westernJp.group(3));
        }
        return cleanLabeled(value);
    }

    private String formatDate(int year, String month, String day) {
        int mo = Integer.parseInt(month);
        if (day == null) {
            return String.format("%02d/%04d", mo, year);
        }
        return String.format("%02d/%02d/%04d",
                Integer.parseInt(day),
                mo,
                year);
    }

    private String normalizeMake(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = stripCode(value);
        if (cleaned.contains("ホンダ") || cleaned.equalsIgnoreCase("Honda")) {
            return "Honda";
        }
        if (cleaned.contains("トヨタ") || cleaned.equalsIgnoreCase("Toyota")) {
            return "Toyota";
        }
        if (cleaned.contains("日産") || cleaned.contains("ニッサン") || cleaned.equalsIgnoreCase("Nissan")) {
            return "Nissan";
        }
        if (cleaned.contains("マツダ") || cleaned.equalsIgnoreCase("Mazda")) {
            return "Mazda";
        }
        if (cleaned.contains("スズキ") || cleaned.equalsIgnoreCase("Suzuki")) {
            return "Suzuki";
        }
        if (cleaned.contains("ダイハツ") || cleaned.equalsIgnoreCase("Daihatsu")) {
            return "Daihatsu";
        }
        if (cleaned.contains("スバル") || cleaned.equalsIgnoreCase("Subaru")) {
            return "Subaru";
        }
        if (cleaned.contains("三菱") || cleaned.contains("ミツビシ") || cleaned.equalsIgnoreCase("Mitsubishi")) {
            return "Mitsubishi";
        }
        if (cleaned.contains("レクサス") || cleaned.equalsIgnoreCase("Lexus")) {
            return "Lexus";
        }
        return cleaned;
    }

    private String normalizeClass(String value) {
        if (value == null) {
            return null;
        }
        if (value.contains("軽自動車") || value.toLowerCase().contains("light")) {
            return "Light Vehicle";
        }
        return cleanLabeled(value);
    }

    private String normalizeUse(String value) {
        if (value == null) {
            return null;
        }
        if (value.contains("乗用") || value.toLowerCase().contains("passenger")) {
            return "Passenger";
        }
        return cleanLabeled(value);
    }

    private String normalizePurpose(String value) {
        if (value == null) {
            return null;
        }
        if (value.contains("事業用") || value.toLowerCase().contains("commercial")) {
            return "Commercial";
        }
        if (value.contains("自家用") || value.toLowerCase().contains("private")) {
            return "Private";
        }
        return cleanLabeled(value);
    }

    private String normalizeBody(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = stripCode(value);
        if (cleaned.contains("ステーションワゴン") || cleaned.toLowerCase().contains("station wagon")) {
            return "Station Wagon";
        }
        return cleaned;
    }

    private String normalizeFuel(String value) {
        if (value == null) {
            return null;
        }
        if (value.contains("ガソリン") || value.toLowerCase().contains("petrol")
                || value.toLowerCase().contains("gasoline")) {
            return "Petrol";
        }
        if (value.contains("軽油") || value.toLowerCase().contains("diesel")) {
            return "Diesel";
        }
        return cleanLabeled(value);
    }

    private String stripCode(String value) {
        if (value == null) {
            return null;
        }
        return cleanLabeled(value.replaceAll(RegexConstants.Text.BRACKET_CODE, ""));
    }

    private String cleanLabeled(String value, String... stops) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll(RegexConstants.Text.TRAILING_EMDASH, "").trim();
        for (String stop : stops) {
            cleaned = RegexConstants.Labeled.fromStopPatternToEnd(stop).matcher(cleaned).replaceAll("");
        }
        cleaned = cleaned.replaceAll(RegexConstants.Text.WHITESPACE_RUN, " ").trim();
        if (cleaned.isEmpty() || isStampNoise(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll(RegexConstants.Text.WHITESPACE, "").replace("—", "-").replace("–", "-");
        if ("-".equals(cleaned)) {
            return "-";
        }
        return cleaned;
    }

    private boolean isStampNoise(String value) {
        String upper = value.toUpperCase();
        return upper.contains("AMANA")
                || upper.contains("SEYLAN")
                || upper.contains("TRADE SERVICES")
                || upper.contains("DOCUMENT CHECKED")
                || upper.contains("DOCUMENT RECEIVED");
    }
}
