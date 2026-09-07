package com.carsale.erp.importpipeline.exportcert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Service
public class ExportCertificateParser {

    private static final String MEASURE = "([0-9]+(?:\\.[0-9]+)?|-|—|–)";

    public AuctionParseResult parse(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The export certificate was empty.");
            return result;
        }

        String normalized = normalize(text);
        boolean japanese = looksJapanese(normalized);
        result.put("certificateNo", firstNonNull(
                extractLabeled(normalized, "Certificate\\s+No\\.?", "Arrangement", "Export"),
                extractLabeled(normalized, "(?:^|\\n)\\s*番号", "整理番号", "輸出")
        ));
        result.put("arrangementNo", firstNonNull(
                extractLabeled(normalized, "Arrangement\\s+No\\.?", "Registration", "Export"),
                extractLabeled(normalized, "整理番号", "車両番号", "輸出"),
                findLongNumber(normalized)
        ));
        result.put("registrationNo", firstNonNull(
                extractLabeled(normalized, "Registration\\s+No\\.?", "Date of Registration", "First"),
                extractLabeled(normalized, "Motor\\s+vehicle\\s+number", "Grant Date", "First"),
                extractLabeled(normalized, "車両番号", "交付年月日", "初度")
        ));
        result.put("registrationDate", normalizeDate(firstNonNull(
                extractLabeled(normalized, "Date of Registration", "First Reg", "Chassis"),
                extractLabeled(normalized, "Grant Date", "First Grant", "Classification"),
                extractLabeled(normalized, "交付年月日", "初度検査年月", "自動車の種別")
        )));
        result.put("firstRegDate", normalizeDate(firstNonNull(
                extractLabeled(normalized, "First\\s+Reg(?:istration)?\\.?\\s+Date", "Chassis", "Trademark"),
                extractLabeled(normalized, "First Grant Date", "Classification", "Use"),
                extractLabeled(normalized, "初度検査年月", "自動車の種別", "用途")
        )));
        result.put("issueDate", normalizeDate(firstNonNull(
                extractLabeled(normalized, "Issue Date", "Export scheduled", "Notes"),
                extractLabeled(normalized, "交付年月日", "初度検査年月", "自動車の種別")
        )));
        result.put("chassisVin", firstNonNull(
                extractLabeled(normalized, "Chassis\\s+No\\.?", "Trademark", "Maker"),
                extractLabeled(normalized, "Maker's serial number", "Fixed Number", "乗車"),
                extractLabeled(normalized, "車台番号", "乗車定員", "最大積載量"),
                findChassis(normalized)
        ));
        result.put("make", normalizeMake(firstNonNull(
                extractLabeled(normalized, "Trademark of the maker", "Model", "Engine"),
                extractLabeled(normalized, "Maker", "Model", "Engine"),
                extractLabeled(normalized, "車名", "型式", "原動機")
        )));
        result.put("model", stripCode(firstNonNull(
                extractLabeled(normalized, "(?<!Engine\\s)Model", "Engine Model", "Engine Capacity"),
                extractLabeled(normalized, "(?<!原動機の)型式(?!指定)", "原動機の型式", "燃料")
        )));
        result.put("engineModel", firstNonNull(
                extractLabeled(normalized, "Engine Model", "Name of User", "Classification of Fuel"),
                extractLabeled(normalized, "原動機の型式", "燃料の種別", "総排気量")
        ));
        result.put("vehicleClassification", normalizeClass(firstNonNull(
                extractLabeled(normalized, "Classification of Vehicle", "Use", "Purpose"),
                extractLabeled(normalized, "自動車の種別", "用途", "自家用")
        )));
        result.put("useType", normalizeUse(firstNonNull(
                extractLabeled(normalized, "Use", "Purpose", "Type of Body"),
                extractLabeled(normalized, "用途", "自家用", "車体の形状")
        )));
        result.put("purpose", normalizePurpose(firstNonNull(
                extractLabeled(normalized, "Purpose", "Type of Body", "Fixed Number"),
                extractLabeled(normalized, "自家用・事業用の別", "車体の形状", "車台番号"),
                extractLabeled(normalized, "自家用・事業用", "車体の形状", "車台番号")
        )));
        result.put("bodyType", normalizeBody(firstNonNull(
                extractLabeled(normalized, "Type of Body", "Fixed Number", "Maxim"),
                extractLabeled(normalized, "車体の形状", "車台番号", "乗車定員")
        )));
        result.put("seatingCapacity", firstNonNull(
                extractMeasure(normalized, "Fixed Number"),
                extractMeasure(normalized, "乗車定員"),
                extractLabeled(normalized, "乗車定員", "最大積載量", "車両重量")
        ));
        result.put("maxCarry", firstNonNull(
                extractMeasure(normalized, "Maxim(?:um)?\\.?\\s*Carry"),
                extractMeasure(normalized, "最大積載量"),
                extractLabeled(normalized, "最大積載量", "車両重量", "車両総重量")
        ));
        result.put("weightKg", firstNonNull(
                extractMeasure(normalized, "(?<!FF\\s)(?<!FR\\s)(?<!RF\\s)(?<!RR\\s)(?<!G/)\\bWeight\\b"),
                extractMeasure(normalized, "車両重量")
        ));
        result.put("grossWeightKg", firstNonNull(
                extractMeasure(normalized, "G\\s*/\\s*Weight"),
                extractMeasure(normalized, "G\\.?C\\.?\\s*Weight"),
                extractMeasure(normalized, "車両総重量")
        ));
        result.put("lengthCm", firstNonNull(
                extractMeasure(normalized, "Length"),
                extractMeasure(normalized, "長さ")
        ));
        result.put("widthCm", firstNonNull(
                extractMeasure(normalized, "Width"),
                extractMeasure(normalized, "幅")
        ));
        result.put("heightCm", firstNonNull(
                extractMeasure(normalized, "Height"),
                extractMeasure(normalized, "高さ")
        ));
        result.put("engineCapacity", firstNonNull(
                extractLabeled(normalized, "Engine Capacity", "Classification of Fuel", "Specification"),
                extractLabeled(normalized, "総排気量又は定格出力", "前前軸重", "型式指定"),
                extractLabeled(normalized, "総排気量", "前前軸重", "型式指定")
        ));
        result.put("fuelType", normalizeFuel(firstNonNull(
                extractLabeled(normalized, "Classification of Fuel", "Specification", "Length"),
                extractLabeled(normalized, "燃料の種別", "総排気量", "前前軸重")
        )));
        result.put("specificationNo", firstNonNull(
                extractLabeled(normalized, "Specification\\s+No\\.?", "Classification No", "Length"),
                extractLabeled(normalized, "型式指定番号", "類別区分番号", "使用者")
        ));
        result.put("classificationNo", firstNonNull(
                extractLabeled(normalized, "Classification\\s+No\\.?", "Name of User", "FF"),
                extractLabeled(normalized, "類別区分番号", "使用者", "所有者")
        ));
        result.put("frontAxleWeight", firstNonNull(
                extractMeasure(normalized, "FF\\s+Weight"),
                extractMeasure(normalized, "F\\s+Weight"),
                extractMeasure(normalized, "前前軸重")
        ));
        result.put("rearAxleWeight", firstNonNull(
                extractMeasure(normalized, "RR\\s+Weight"),
                extractMeasure(normalized, "R\\s+Weight"),
                extractMeasure(normalized, "後後軸重")
        ));
        result.put("frWeight", extractMeasure(normalized, "FR\\s+Weight"));
        result.put("rfWeight", extractMeasure(normalized, "RF\\s+Weight"));
        result.put("userName", firstNonNull(
                extractLabeled(normalized, "Name of User", "Address of User", "Name of Owner"),
                extractLabeled(normalized, "使用者の氏名又は名称", "使用者の住所", "所有者")
        ));
        result.put("userAddress", firstNonNull(
                extractLabeled(normalized, "Address of User", "Name of Owner", "Address of Owner"),
                extractLabeled(normalized, "使用者の住所", "所有者の氏名", "所有者の住所")
        ));
        result.put("ownerName", firstNonNull(
                extractLabeled(normalized, "Name of Owner", "Address of Owner", "Locality"),
                extractLabeled(normalized, "所有者の氏名又は名称", "所有者の住所", "使用の本拠")
        ));
        result.put("ownerAddress", firstNonNull(
                extractLabeled(normalized, "Address of Owner", "Locality", "Export scheduled"),
                extractLabeled(normalized, "所有者の住所", "使用の本拠", "輸出予定")
        ));
        result.put("localityOfUse", firstNonNull(
                extractLabeled(normalized, "Locality of principal abode of use", "Export scheduled", "Issue Date"),
                extractLabeled(normalized, "使用の本拠の位置", "輸出予定日", "備考")
        ));
        result.put("exportScheduledDate", normalizeDate(firstNonNull(
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

    boolean looksJapanese(String text) {
        if (text == null) {
            return false;
        }
        return text.contains("輸出")
                || text.contains("車台番号")
                || text.contains("車両番号")
                || text.contains("整理番号")
                || Pattern.compile("[\\u3040-\\u30ff\\u4e00-\\u9faf]").matcher(text).find();
    }

    private String extractLabeled(String text, String label, String... stops) {
        Matcher matcher = Pattern.compile(
                label + "\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        matcher = Pattern.compile(
                label + "\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        return null;
    }

    private String extractMeasure(String text, String label) {
        Matcher matcher = Pattern.compile(
                label + "\\s*[:\\.]?\\s*" + MEASURE + "(?:\\s*(?:kg|cm|L|KW/?L|kW/?L|人|Person))?",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private String extractRemarks(String text) {
        Matcher matcher = Pattern.compile(
                "(?:Remarks|備考)\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
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
        Matcher matcher = Pattern.compile("\\b([A-Z0-9]{2,8}-[A-Z0-9]{5,12})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String findLongNumber(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d{14,20})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String normalizeDate(String value) {
        if (value == null) {
            return null;
        }
        Matcher reiwa = Pattern.compile("令和\\s*(\\d+)\\s*年\\s*(\\d{1,2})\\s*月(?:\\s*(\\d{1,2})\\s*日)?").matcher(value);
        if (reiwa.find()) {
            return formatDate(2018 + Integer.parseInt(reiwa.group(1)), reiwa.group(2), reiwa.group(3));
        }
        Matcher westernJp = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月(?:\\s*(\\d{1,2})\\s*日)?").matcher(value);
        if (westernJp.find()) {
            return formatDate(Integer.parseInt(westernJp.group(1)), westernJp.group(2), westernJp.group(3));
        }
        return cleanLabeled(value);
    }

    private String formatDate(int year, String month, String day) {
        int mo = Integer.parseInt(month);
        if (day == null) {
            return String.format("%02d/%04d", Integer.valueOf(mo), Integer.valueOf(year));
        }
        return String.format("%02d/%02d/%04d",
                Integer.valueOf(Integer.parseInt(day)),
                Integer.valueOf(mo),
                Integer.valueOf(year));
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
        return cleanLabeled(value.replaceAll("\\[\\s*\\d+\\s*\\]", ""));
    }

    private String cleanLabeled(String value, String... stops) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\u2013\\u2014_]+$", "").trim();
        for (int i = 0; i < stops.length; i++) {
            cleaned = cleaned.replaceAll("(?i)\\s+" + stops[i] + ".*$", "");
        }
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        if (cleaned.isEmpty() || isStampNoise(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", "").replace("—", "-").replace("–", "-");
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

    private String normalize(String text) {
        return text.replace('\r', '\n').replaceAll("[ \t]+", " ").replaceAll("\n{3,}", "\n\n");
    }
}
