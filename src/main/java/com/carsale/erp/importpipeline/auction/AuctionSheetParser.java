package com.carsale.erp.importpipeline.auction;

import com.carsale.erp.shared.ocr.JapaneseTextTranslator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Component
public class AuctionSheetParser {

    private static final Map<String, String> MODEL_MAKES = new LinkedHashMap<>();
    private static final Map<String, String[]> CODE_MODELS = new LinkedHashMap<>();
    private static final Map<String, String[]> MODEL_SPECS = new LinkedHashMap<>();

    private final JapaneseTextTranslator translator;

    public AuctionSheetParser(JapaneseTextTranslator translator) {
        this.translator = translator;
    }

    static {
        putCode("NHP10", "Aqua", "Toyota");
        putCode("NHP11", "Aqua", "Toyota");
        putCode("NKE165", "Corolla Axio", "Toyota");
        putCode("NZE161", "Corolla Axio", "Toyota");
        putCode("NHW20", "Prius", "Toyota");
        putCode("M900A", "Roomy", "Toyota");
        putCode("M910A", "Roomy", "Toyota");
        putCode("MH95S", "Wagon R", "Suzuki");
        putCode("MH55S", "Wagon R", "Suzuki");
        putCode("MH35S", "Wagon R", "Suzuki");
        putCode("ZVW30", "Prius", "Toyota");
        putCode("ZVW50", "Prius", "Toyota");
        putCode("ZVW51", "Prius", "Toyota");
        putCode("RU1", "Vezel", "Honda");
        putCode("RU3", "Vezel", "Honda");
        putCode("A202A", "Raize", "Toyota");
        putCode("A210A", "Raize", "Toyota");
        putCode("A200A", "Raize", "Toyota");
        putCode("A201A", "Raize", "Toyota");
        putCode("A202S", "Rocky", "Daihatsu");
        putCode("A200S", "Rocky", "Daihatsu");
        putSpec("A202A", "5AA-A202A", "Raize", "Toyota", "Hybrid Z", "Hybrid", "1200", "5-door", "5", "CVT");
        putSpec("A210A", "5BA-A210A", "Raize", "Toyota", null, "Gasoline", "1000", "5-door", "5", "CVT");
        putSpec("A200A", "5BA-A200A", "Raize", "Toyota", null, "Gasoline", "1000", "5-door", "5", "CVT");
        putSpec("A201A", "5BA-A201A", "Raize", "Toyota", null, "Gasoline", "1000", "5-door", "5", "CVT");
        putSpec("A202S", "5AA-A202S", "Rocky", "Daihatsu", "Hybrid Z", "Hybrid", "1200", "5-door", "5", "CVT");
        putSpec("A200S", "5BA-A200S", "Rocky", "Daihatsu", null, "Gasoline", "1000", "5-door", "5", "CVT");
    }

    private static void putCode(String code, String model, String make) {
        CODE_MODELS.put(code, new String[] {model, make});
        MODEL_MAKES.put(model, make);
    }

    private static void putSpec(String chassisPrefix, String modelCode, String model, String make,
            String grade, String fuel, String engineCc, String body, String seats, String transmission) {
        MODEL_SPECS.put(chassisPrefix, new String[] {
                modelCode, model, make, grade, fuel, engineCc, body, seats, transmission
        });
    }

    public AuctionParseResult parse(String rawText) {
        AuctionParseResult result = new AuctionParseResult();
        if (rawText == null || rawText.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("No text was read from the auction sheet.");
            return result;
        }
        String original = repairOcrNoise(normalizeLabels(rawText.replace('\r', '\n')));
        String text = translator.translateDocument(original);
        result.setRawText(text);
        fillIdentity(result, text);
        result.put("grade", extractGrade(text));
        result.put("modelCode", firstNonNull(extractModelCode(text), extractModelCode(original)));
        result.put("chassisNo", firstNonNull(
                extractChassis(text, result.getFields().get("modelCode")),
                extractChassis(original, result.getFields().get("modelCode"))));
        fillFromModelCode(result, text + "\n" + original);
        fillVehicleName(result);
        result.put("year", firstNonNull(extractFirstRegistration(text), extractFirstRegistration(original)));
        result.put("mileage", formatKm(firstNonNull(extractMileage(text), extractMileage(original))));
        result.put("colorCode", firstNonNull(extractColorCode(text), extractColorCode(original)));
        result.put("color", firstNonNull(colorFromCode(result.getFields().get("colorCode")), extractColor(text)));
        result.put("engineSize", formatCc(firstNonNull(extractEngine(text), extractEngine(original))));
        result.put("transmission", firstNonNull(extractTransmission(text), extractTransmission(original)));
        result.put("fuel", firstNonNull(extractFuel(text), extractFuel(original)));
        result.put("acType", firstNonNull(extractAc(text), extractAc(original)));
        result.put("bodyStyle", firstNonNull(extractBodyStyle(text), extractBodyStyle(original)));
        result.put("seats", formatSeats(firstNonNull(extractSeats(text), extractSeats(original))));
        result.put("lotNo", firstNonNull(
                extractLot(text, result.getFields().get("mileage")),
                extractLot(original, result.getFields().get("mileage"))));
        result.put("auctionGrade", firstNonNull(extractAuctionGrade(text), extractAuctionGrade(original)));
        result.put("exteriorGrade", firstNonNull(
                extractPanelGrade(text, "Exterior", "外装", "外装評"),
                extractPanelGrade(original, "Exterior", "外装", "外装評")));
        result.put("interiorGrade", firstNonNull(
                extractPanelGrade(text, "Interior", "内装", "内"),
                extractPanelGrade(original, "Interior", "内装", "内")));
        fillPanelGradesFromNoise(result, text);
        fillPanelGradesFromNoise(result, original);
        fillAuctionSheetGrades(result, text);
        fillAuctionSheetGrades(result, original);
        result.put("history", firstNonNull(extractHistory(text), extractHistory(original)));
        extractDimensions(result, text);
        if (isBlank(result.getFields().get("lengthCm"))) {
            extractDimensions(result, original);
        }
        fillKnownSpecs(result, text + "\n" + original);
        englishize(result);
        fillVehicleName(result);
        keepFormFields(result);

        boolean any = !result.getFields().isEmpty();
        result.setSuccess(any);
        result.setMessage(any
                ? "Filled " + result.getFields().size() + " fields from the auction sheet. Please check and edit."
                : "The sheet was read, but fields could not be mapped. Please fill them manually.");
        return result;
    }

    private String repairOcrNoise(String text) {
        String repaired = foldFullWidthDigits(text);
        repaired = repaired.replace('ｋ', 'k').replace('ｍ', 'm').replace("㎞", "km");
        repaired = repaired.replaceAll("(\\d{1,3})[.,\\s](\\d{3})\\s*(km|kn|KM|キロ)", "$1,$2 km");
        repaired = repaired.replaceAll("(\\d{1,3})[.,](\\d{3})\\s*(cc|CC|ｃｃ)", "$1$2 cc");
        repaired = repaired.replace("ハイプリッド", "ハイブリッド");
        repaired = repaired.replace("ハイプリット", "ハイブリッド");
        repaired = repaired.replaceAll("(?<![A-Z0-9])SAA-", "5AA-");
        repaired = repaired.replaceAll("車名[^\\n]{0,24}ライス", "車名 ライズ");
        repaired = repaired.replaceAll("(\\d{4,6})\\s*k\\s*n", "$1 km");
        repaired = repaired.replaceAll("(\\d{4,6})\\s*kn", "$1 km");
        repaired = repaired.replaceAll("(?:R|Ｒ)\\s*([1-8])\\s*[/.．\\-年]\\s*([1-9]|1[0-2])\\s*月?", "R$1 $2");
        repaired = repaired.replaceAll("(?:H|Ｈ)\\s*([1-3]?[0-9])\\s*[/.．\\-年]\\s*([1-9]|1[0-2])", "H$1 $2");
        repaired = repaired.replaceAll("(?:H|Ｈ)\\s*([1-3]?[0-9])\\s+([1-9]|1[0-2])", "H$1 $2");
        repaired = repaired.replaceAll("初度登録([^\\n]{0,40}?)G\\s*([1-8])", "初度登録$1R$2");
        return repaired;
    }

    private String foldFullWidthDigits(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '０' && ch <= '９') {
                builder.append((char) ('0' + (ch - '０')));
            } else if (ch == '／') {
                builder.append('/');
            } else if (ch == 'Ｚ' || ch == 'ｚ') {
                builder.append('Z');
            } else if (ch == 'Ｓ' || ch == 'ｓ') {
                builder.append('S');
            } else if (ch == 'Ａ' || ch == 'ａ') {
                builder.append('A');
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String normalizeLabels(String text) {
        String normalized = text;
        normalized = normalized.replaceAll("出\\s*品\\s*番\\s*号", "出品番号");
        normalized = normalized.replaceAll("初\\s*度\\s*登\\s*録", "初度登録");
        normalized = normalized.replaceAll("年\\s*月", "年月");
        normalized = normalized.replaceAll("走\\s+行", "走行");
        normalized = normalized.replaceAll("型\\s+式", "型式");
        normalized = normalized.replaceAll("排\\s*気\\s*量", "排気量");
        normalized = normalized.replaceAll("グレ\\s*ード", "グレード");
        normalized = normalized.replaceAll("ルー\\s*ミー", "ルーミー");
        normalized = normalized.replaceAll("評\\s*価\\s*点", "評価点");
        normalized = normalized.replaceAll("評\\s*点", "評点");
        normalized = normalized.replaceAll("内\\s*装", "内装");
        normalized = normalized.replaceAll("外\\s*装(?!色)", "外装");
        normalized = normalized.replaceAll("車\\s*台\\s*番\\s*号", "車台番号");
        normalized = normalized.replaceAll("ド[アア]\\s*形\\s*状", "ドア形状");
        return normalized;
    }

    private void englishize(AuctionParseResult result) {
        Map<String, String> fields = result.getFields();
        Map<String, String> updated = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String english = AuctionSheetEnglish.convert(entry.getValue(), null);
            if (english != null && !english.isEmpty()) {
                updated.put(entry.getKey(), english);
            } else if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                updated.put(entry.getKey(), entry.getValue().trim());
            }
        }
        fields.clear();
        fields.putAll(updated);
    }

    private void fillIdentity(AuctionParseResult result, String text) {
        String nameLine = extract(text, new String[] {"車名", "Vehicle name", "Car name", "Model name"});
        applyIdentity(result, nameLine);
        if (result.getFields().get("model") == null) {
            applyIdentity(result, extract(text, new String[] {"メーカー", "Maker", "Manufacturer"}));
        }
        if (result.getFields().get("model") == null) {
            applyIdentity(result, text);
        }
        if (result.getFields().get("make") == null) {
            result.put("make", inferMake(text, result.getFields().get("model")));
        }
    }

    private void applyIdentity(AuctionParseResult result, String haystack) {
        if (haystack == null || haystack.trim().isEmpty()) {
            return;
        }
        String matched = matchKnownModel(haystack);
        if (matched != null) {
            result.put("model", matched);
            result.put("make", inferMake(haystack, matched));
            return;
        }
        if (haystack.length() > 40 || haystack.contains("\n") || looksLikeSpecDump(haystack)) {
            return;
        }
        String translated = translateMaker(haystack).replaceAll("\\s+", " ").trim();
        if (translated.isEmpty() || isFieldLabel(translated) || isSpecToken(translated)) {
            return;
        }
        if (result.getFields().get("model") == null) {
            result.put("model", firstModelToken(translated));
            result.put("make", inferMake(haystack, translated));
        }
    }

    private String matchKnownModel(String haystack) {
        String upper = haystack.toUpperCase();
        for (String english : MODEL_MAKES.keySet()) {
            if (english != null && upper.contains(english.toUpperCase())) {
                return english;
            }
        }
        return null;
    }

    private String inferMake(String haystack, String model) {
        if (model != null && MODEL_MAKES.containsKey(model)) {
            return MODEL_MAKES.get(model);
        }
        String source = haystack == null ? "" : haystack;
        String translated = translateMaker(source);
        if (translated.contains("Toyota") || source.toUpperCase().contains("TOYOTA")) {
            return "Toyota";
        }
        if (translated.contains("Suzuki") || source.toUpperCase().contains("SUZUKI") || source.contains("スズキ")) {
            return "Suzuki";
        }
        if (translated.contains("Honda") || source.contains("ホンダ")) {
            return "Honda";
        }
        if (translated.contains("Nissan") || source.contains("日産")) {
            return "Nissan";
        }
        if (translated.contains("Mazda") || source.contains("マツダ")) {
            return "Mazda";
        }
        if (translated.contains("Daihatsu") || source.contains("ダイハツ")) {
            return "Daihatsu";
        }
        return MODEL_MAKES.get(translated);
    }

    private String firstModelToken(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = cleanValue(value);
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private void fillFromModelCode(AuctionParseResult result, String text) {
        String haystack = codeHaystack(text + " " + nullToEmpty(result.getFields().get("modelCode"))
                + " " + nullToEmpty(result.getFields().get("chassisNo")));
        for (Map.Entry<String, String[]> entry : CODE_MODELS.entrySet()) {
            if (haystack.contains(entry.getKey())) {
                if (result.getFields().get("model") == null) {
                    result.put("model", entry.getValue()[0]);
                }
                if (result.getFields().get("make") == null) {
                    result.put("make", entry.getValue()[1]);
                }
                break;
            }
        }
    }

    private void fillKnownSpecs(AuctionParseResult result, String text) {
        String key = findSpecKey(result, text);
        if (key == null) {
            return;
        }
        String[] spec = MODEL_SPECS.get(key);
        result.put("modelCode", spec[0]);
        String model = result.getFields().get("model");
        if (spec[1] != null && (isBlank(model) || !model.contains(spec[1]))) {
            result.put("model", spec[1]);
        }
        putIfBlank(result, "make", spec[2]);
        String grade = result.getFields().get("grade");
        if (spec[3] != null && (isBlank(grade) || AuctionSheetEnglish.containsJapanese(grade))) {
            result.put("grade", spec[3]);
        }
        String fuel = result.getFields().get("fuel");
        if (isBlank(fuel) || ("Hybrid".equals(spec[4]) && "Gasoline".equals(fuel))) {
            result.put("fuel", spec[4]);
        }
        putIfBlank(result, "engineSize", formatCc(spec[5]));
        String body = result.getFields().get("bodyStyle");
        if (isBlank(body) || (spec[6] != null && !spec[6].equals(body))) {
            result.put("bodyStyle", spec[6]);
        }
        putIfBlank(result, "seats", formatSeats(spec[7]));
        putIfBlank(result, "transmission", spec[8]);
    }

    private String findSpecKey(AuctionParseResult result, String text) {
        String haystack = codeHaystack(
                nullToEmpty(result.getFields().get("modelCode")) + " "
                        + nullToEmpty(result.getFields().get("chassisNo")) + " "
                        + text
        );
        for (String key : MODEL_SPECS.keySet()) {
            if (haystack.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private void putIfBlank(AuctionParseResult result, String key, String value) {
        if (value != null && isBlank(result.getFields().get(key))) {
            result.put(key, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean looksLikeSpecDump(String value) {
        String upper = value.toUpperCase();
        return upper.contains("WD") || upper.contains("CC") || upper.contains("-A")
                || Pattern.compile("\\d{3,}.*\\d{3,}", Pattern.DOTALL).matcher(value).find();
    }

    private void fillAuctionSheetGrades(AuctionParseResult result, String text) {
        if (isBlank(result.getFields().get("auctionGrade"))) {
            String overall = findStandaloneAuctionGrade(text);
            if (overall != null) {
                result.put("auctionGrade", overall);
            }
        }
        List<String> panelLetters = findStandalonePanelGrades(text, result.getFields().get("auctionGrade"));
        if (isBlank(result.getFields().get("exteriorGrade")) && isBlank(result.getFields().get("interiorGrade"))
                && panelLetters.size() >= 2) {
            result.put("exteriorGrade", panelLetters.get(0));
            result.put("interiorGrade", panelLetters.get(1));
        }
        if (isBlank(result.getFields().get("interiorGrade"))) {
            putFirstUnusedGrade(result, "interiorGrade", panelLetters);
        }
        if (isBlank(result.getFields().get("exteriorGrade")) && panelLetters.size() >= 2) {
            putFirstUnusedGrade(result, "exteriorGrade", panelLetters);
        }
        Matcher together = Pattern.compile("(?<![A-Za-z0-9])([S6])\\s+([A-E])(?![A-Za-z0-9])").matcher(text.toUpperCase());
        if (together.find()) {
            if (isBlank(result.getFields().get("auctionGrade"))) {
                result.put("auctionGrade", together.group(1));
            }
            if (isBlank(result.getFields().get("interiorGrade"))) {
                result.put("interiorGrade", together.group(2));
            }
        }
    }

    private String findStandaloneAuctionGrade(String text) {
        Matcher line = Pattern.compile("(?m)^\\s*[\\[\\(（・*]*\\s*([S6*]|[1-6](?:\\.5)?)\\s*[\\]\\)）点]*\\s*$")
                .matcher(text);
        if (line.find()) {
            return line.group(1);
        }
        Matcher labeled = Pattern.compile(
                "(?i)(?:評価|評点|点数|総合|evaluation|rating|score|auction\\s+grade|overall)[^A-Za-z0-9]{0,40}([S6*]|[1-6](?:\\.5)?)")
                .matcher(text);
        if (labeled.find()) {
            return labeled.group(1);
        }
        Matcher token = Pattern.compile("(?<![A-Za-z0-9])([S*])(?![A-Za-z0-9])").matcher(text);
        if (token.find()) {
            return token.group(1);
        }
        return null;
    }

    private List<String> findStandalonePanelGrades(String text, String auctionGrade) {
        List<String> grades = new ArrayList<>();
        Matcher line = Pattern.compile("(?m)^\\s*[\\[\\(（]*\\s*([A-Ea-e])\\s*[\\]\\)）]*\\s*$").matcher(text);
        while (line.find()) {
            String grade = line.group(1).toUpperCase();
            if (!grade.equals(auctionGrade) && !grades.contains(grade)) {
                grades.add(grade);
            }
        }
        Matcher token = Pattern.compile("(?<![A-Za-z0-9])([A-E])(?![A-Za-z0-9])").matcher(text.toUpperCase());
        while (token.find()) {
            if (!isGradeLetterContext(text, token.start())) {
                continue;
            }
            String grade = token.group(1);
            if (!grade.equals(auctionGrade) && !grades.contains(grade)) {
                grades.add(grade);
            }
        }
        return grades;
    }

    private void putFirstUnusedGrade(AuctionParseResult result, String key, List<String> panelLetters) {
        for (String grade : panelLetters) {
            if (!grade.equals(result.getFields().get("auctionGrade"))
                    && !grade.equals(result.getFields().get("exteriorGrade"))
                    && !grade.equals(result.getFields().get("interiorGrade"))) {
                result.put(key, grade);
                return;
            }
        }
    }

    private boolean isGradeLetterContext(String text, int start) {
        int from = Math.max(0, start - 8);
        int to = Math.min(text.length(), start + 8);
        String window = text.substring(from, to).toUpperCase();
        if (window.contains("車歴") || window.contains("履歴") || window.contains("HISTORY")
                || window.contains("B )") || window.contains("B)")) {
            return false;
        }
        if (window.contains("AAC") || window.contains("IAT") || window.contains("ABS")
                || window.contains("5AA") || window.contains("5BA") || window.contains("6AA")) {
            return false;
        }
        if (start > 0 && isCodeChar(text.charAt(start - 1))) {
            return false;
        }
        return start + 1 >= text.length() || !isCodeChar(text.charAt(start + 1));
    }

    private boolean isCodeChar(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }

    private void fillVehicleName(AuctionParseResult result) {
        String make = result.getFields().get("make");
        String model = result.getFields().get("model");
        if (make == null || model == null) {
            return;
        }
        if (!model.toLowerCase().startsWith(make.toLowerCase())) {
            result.getFields().put("model", make + " " + model);
        }
    }

    private void keepFormFields(AuctionParseResult result) {
        String[] keys = new String[] {
                "lotNo", "year", "make", "model", "bodyStyle", "grade",
                "auctionGrade", "exteriorGrade", "interiorGrade",
                "mileage", "history", "engineSize", "fuel", "modelCode", "chassisNo",
                "seats", "color", "colorCode", "lengthCm", "widthCm", "heightCm",
                "transmission", "acType"
        };
        Map<String, String> kept = new LinkedHashMap<>();
        for (String key : keys) {
            String value = result.getFields().get(key);
            if (value != null && !value.trim().isEmpty()) {
                kept.put(key, value.trim());
            }
        }
        result.getFields().clear();
        result.getFields().putAll(kept);
    }

    private String formatKm(String mileage) {
        if (mileage == null) {
            return null;
        }
        String digits = mileage.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return mileage;
        }
        return withComma(digits) + " km";
    }

    private String formatCc(String engine) {
        if (engine == null) {
            return null;
        }
        String digits = engine.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return engine;
        }
        return withComma(digits) + " cc";
    }

    private String formatSeats(String seats) {
        if (seats == null) {
            return null;
        }
        String digits = seats.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return seats;
        }
        return digits + " people";
    }

    private String withComma(String digits) {
        if (digits.length() <= 3) {
            return digits;
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                builder.append(',');
            }
            builder.append(digits.charAt(i));
            count++;
        }
        return builder.reverse().toString();
    }

    private String extractFirstRegistration(String text) {
        int[] labeled = extractEraDateNearLabel(text);
        if (labeled != null) {
            return formatFirstRegistration(labeled[0], labeled[1]);
        }
        String english = extractEnglishRegistrationDate(text);
        if (english != null) {
            return english;
        }
        int[] besideLot = extractEraDateBesideLot(text);
        if (besideLot != null) {
            return formatFirstRegistration(besideLot[0], besideLot[1]);
        }
        String header = text.substring(0, Math.min(text.length(), 1200));
        int[] headerDate = extractEraDate(header, true);
        if (headerDate != null) {
            return formatFirstRegistration(headerDate[0], headerDate[1]);
        }
        return extractEnglishRegistrationDate(header);
    }

    private String extractEnglishRegistrationDate(String text) {
        String months = "January|February|March|April|May|June|July|August|September|October|November|December"
                + "|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec";
        Matcher labeled = Pattern.compile(
                "(?i)(?:first\\s+registration|registration(?:\\s+date)?|year(?:\\s+of)?\\s+registration)[^\\n]{0,48}"
                        + "(" + months + ")[.]?\\s+((?:19|20)\\d{2})")
                .matcher(text);
        if (labeled.find()) {
            return formatEnglishMonthYear(labeled.group(1), labeled.group(2));
        }
        Matcher monthYear = Pattern.compile("(?i)(" + months + ")[.]?\\s+((?:19|20)\\d{2})").matcher(text);
        if (monthYear.find()) {
            return formatEnglishMonthYear(monthYear.group(1), monthYear.group(2));
        }
        Matcher yearMonth = Pattern.compile("((?:19|20)\\d{2})\\s*[/.\\-]\\s*([1-9]|1[0-2])").matcher(text);
        if (yearMonth.find()) {
            return formatFirstRegistration(Integer.parseInt(yearMonth.group(1)), Integer.parseInt(yearMonth.group(2)));
        }
        return null;
    }

    private String formatEnglishMonthYear(String monthToken, String year) {
        String[] names = new String[] {
                "jan", "feb", "mar", "apr", "may", "jun",
                "jul", "aug", "sep", "oct", "nov", "dec"
        };
        String key = monthToken.toLowerCase().replace(".", "");
        if (key.startsWith("sept")) {
            key = "sep";
        }
        for (int i = 0; i < names.length; i++) {
            if (key.startsWith(names[i])) {
                return formatFirstRegistration(Integer.parseInt(year), i + 1);
            }
        }
        return monthToken + " " + year;
    }

    private String formatFirstRegistration(int year, int month) {
        if (month >= 1 && month <= 12) {
            return monthName(month) + " " + year;
        }
        return String.valueOf(year);
    }

    private int[] extractEraDateNearLabel(String text) {
        Matcher label = Pattern.compile("(?i)初度登録(?:年月)?|first\\s+registration|registration\\s+date").matcher(text);
        while (label.find()) {
            int from = label.start();
            int to = Math.min(text.length(), label.end() + 80);
            int[] parsed = extractEraDate(text.substring(from, to), false);
            if (parsed != null) {
                return parsed;
            }
        }
        Matcher yearMonth = Pattern.compile("(?i)年式|model\\s+year").matcher(text);
        if (yearMonth.find()) {
            int to = Math.min(text.length(), yearMonth.end() + 40);
            return extractEraDate(text.substring(yearMonth.start(), to), false);
        }
        return null;
    }

    private int[] extractEraDateBesideLot(String text) {
        Matcher lot = Pattern.compile(
                "(?i)(?:(?:出品番号|(?<![A-Za-z])lot(?:\\s*(?:no\\.?|number))?)[^\\d]{0,16})?(\\d{3,6})[^\\n]{0,40}?((?:R|Ｒ|H|Ｈ|G)\\s*\\d{1,2}\\s+\\d{1,2})")
                .matcher(text);
        if (lot.find()) {
            return extractEraDate(lot.group(2).replaceFirst("^[Gg]", "R"), false);
        }
        return null;
    }

    private int[] extractEraDate(String window, boolean headerOnly) {
        Matcher reiwa = Pattern.compile("(?:令和|(?:^|[^A-Z])[RＲ])\\s*(\\d{1,2}|元)\\s*(?:年)?\\s*([1-9]|1[0-2])").matcher(window);
        if (reiwa.find()) {
            int month = Integer.parseInt(reiwa.group(2));
            if (month >= 1 && month <= 12) {
                return new int[] {toWesternYear("令和", reiwa.group(1)), month};
            }
        }
        Matcher heisei = Pattern.compile("(?:平成|(?:^|[^A-Z])[HＨ])\\s*(\\d{1,2})\\s*(?:年)?\\s*([1-9]|1[0-2])").matcher(window);
        if (heisei.find()) {
            int eraYear = Integer.parseInt(heisei.group(1));
            int month = Integer.parseInt(heisei.group(2));
            if (eraYear >= 1 && eraYear <= 31 && month >= 1 && month <= 12) {
                return new int[] {toWesternYear("平成", heisei.group(1)), month};
            }
        }
        Matcher jpYearMonth = Pattern.compile("(\\d{1,2})\\s*年\\s*(\\d{1,2})\\s*月").matcher(window);
        if (jpYearMonth.find()) {
            int eraYear = Integer.parseInt(jpYearMonth.group(1));
            int month = Integer.parseInt(jpYearMonth.group(2));
            if (month >= 1 && month <= 12) {
                if (eraYear >= 9 && eraYear <= 31) {
                    return new int[] {1988 + eraYear, month};
                }
                if (eraYear >= 1 && eraYear <= 8) {
                    return new int[] {2018 + eraYear, month};
                }
            }
        }
        if (headerOnly) {
            Matcher gAsR = Pattern.compile("(?:^|[^A-Z])G\\s*([1-8])\\s+([1-9]|1[0-2])").matcher(window.toUpperCase());
            if (gAsR.find()) {
                int month = Integer.parseInt(gAsR.group(2));
                return new int[] {2018 + Integer.parseInt(gAsR.group(1)), month};
            }
        }
        Matcher two = Pattern.compile("初度登録(?:年月)?[^\\d]{0,20}(\\d{1,2})[^\\d]{1,8}([1-9]|1[0-2])").matcher(window);
        if (two.find()) {
            int eraYear = Integer.parseInt(two.group(1));
            int month = Integer.parseInt(two.group(2));
            if (month >= 1 && month <= 12) {
                if (eraYear >= 9 && eraYear <= 31) {
                    return new int[] {1988 + eraYear, month};
                }
                if (eraYear >= 1 && eraYear <= 8) {
                    return new int[] {2018 + eraYear, month};
                }
            }
        }
        return null;
    }

    private String monthName(int month) {
        String[] names = new String[] {
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };
        if (month >= 1 && month <= 12) {
            return names[month];
        }
        return String.valueOf(month);
    }

    private String extract(String text, String[] keys) {
        for (String key : keys) {
            String quoted = Pattern.quote(key);
            if (key.matches("[A-Za-z].*")) {
                quoted = "(?<![A-Za-z])" + quoted + "(?![A-Za-z])";
            }
            Pattern pattern = Pattern.compile("(?i)" + quoted + "[^\\nA-Za-z0-9ァ-ヶ一-龥]{0,16}([^\\n]{0,60})");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String sameLine = cleanValue(matcher.group(1));
                if (hasContent(sameLine) && !isFieldLabel(sameLine)) {
                    return sameLine;
                }
                String next = nextLine(text, matcher.end());
                if (hasContent(next) && !isFieldLabel(next)) {
                    return cleanValue(next);
                }
            }
        }
        return null;
    }

    private String nextLine(String text, int from) {
        int nl = text.indexOf('\n', from);
        if (nl < 0) {
            return null;
        }
        int end = text.indexOf('\n', nl + 1);
        if (end < 0) {
            end = text.length();
        }
        return text.substring(nl + 1, end).trim();
    }

    private String extractChassis(String text, String modelCode) {
        String labeled = extract(text, new String[] {"車台番号", "Chassis number", "Chassis no", "Frame number", "VIN"});
        String digits = null;
        if (labeled != null) {
            String cleaned = codeHaystack(labeled).replaceAll("[^A-Z0-9-]", "");
            Matcher full = Pattern.compile("([A-Z]{1,5}\\d{2,3}[A-Z0-9]{0,2}-\\d{5,8})").matcher(cleaned);
            if (full.find()) {
                return full.group(1);
            }
            if (cleaned.matches("[A-Z0-9]+-\\d{5,8}")) {
                return cleaned;
            }
            Matcher labeledDigits = Pattern.compile("(\\d{6,8})").matcher(cleaned);
            if (labeledDigits.find()) {
                digits = labeledDigits.group(1);
            }
        }
        if (digits == null) {
            Matcher suffix = Pattern.compile(
                    "(?i)(?:車台番号|chassis(?:\\s+(?:no\\.?|number))?|frame(?:\\s+number)?|vin)[^\\d]{0,24}(\\d{6,8})")
                    .matcher(text);
            if (suffix.find()) {
                digits = suffix.group(1);
            }
        }
        if (digits != null) {
            String prefix = chassisPrefix(modelCode);
            return prefix == null ? digits : prefix + "-" + digits;
        }
        Matcher matcher = Pattern.compile("([A-Z]{1,5}\\d{2,3}[A-Z0-9]{0,2}-\\d{5,8})").matcher(codeHaystack(text));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String chassisPrefix(String modelCode) {
        if (modelCode == null || modelCode.trim().isEmpty()) {
            return null;
        }
        int dash = modelCode.lastIndexOf('-');
        if (dash >= 0 && dash < modelCode.length() - 1) {
            return modelCode.substring(dash + 1);
        }
        return modelCode;
    }

    private String extractModelCode(String text) {
        String haystack = codeHaystack(text);
        String labeled = extract(text, new String[] {"型式", "Model code", "Model type"});
        if (labeled != null) {
            Matcher labeledCode = Pattern.compile("((?:[0-9][A-Z]{2}|[A-Z]{3})[-\\s]?[A-Z0-9]{3,10})")
                    .matcher(codeHaystack(labeled));
            if (labeledCode.find()) {
                return normalizeKnownModelCode(labeledCode.group(1).replace(' ', '-'));
            }
            String cleaned = codeHaystack(labeled).replaceAll("[^A-Z0-9-]", "");
            if (cleaned.length() >= 4) {
                return cleaned;
            }
        }
        Matcher matcher = Pattern.compile("((?:[0-9][A-Z]{2}|[A-Z]{3})-[A-Z0-9]{3,10})").matcher(haystack);
        if (matcher.find()) {
            return normalizeKnownModelCode(matcher.group(1));
        }
        Matcher spaced = Pattern.compile("((?:[0-9][A-Z]{2}|[A-Z]{3})\\s+[A-Z0-9]{4,10})").matcher(haystack);
        if (spaced.find()) {
            return normalizeKnownModelCode(spaced.group(1).replace(' ', '-'));
        }
        return null;
    }

    private String normalizeKnownModelCode(String code) {
        if (code == null) {
            return null;
        }
        String upper = codeHaystack(code);
        for (Map.Entry<String, String[]> entry : MODEL_SPECS.entrySet()) {
            if (upper.endsWith(entry.getKey()) || upper.contains("-" + entry.getKey())) {
                return entry.getValue()[0];
            }
        }
        return code;
    }

    private String codeHaystack(String text) {
        return text.toUpperCase().replace('Ｏ', '0').replace('O', '0');
    }

    private String extractMileage(String text) {
        Matcher withUnit = Pattern.compile(
                "(?i)(?:走行|mileage|odometer|travel(?:led|ed)?(?:\\s+distance)?)[^\\d]{0,20}(\\d{1,3}(?:,\\d{3}|\\.\\d{3})+|\\d{1,6})\\s*(km|kn|㎞|KM|キロ|kilometers?|kilometres?)")
                .matcher(text);
        if (withUnit.find()) {
            return withUnit.group(1).replace(",", "").replace(".", "") + " km";
        }
        Matcher labeled = Pattern.compile(
                "(?i)(?:走行|mileage|odometer|travel(?:led|ed)?(?:\\s+distance)?)[^\\d]{0,20}(\\d{1,3}(?:,\\d{3}|\\.\\d{3})+|\\d{1,6})")
                .matcher(text);
        if (labeled.find()) {
            String digits = labeled.group(1).replace(",", "").replace(".", "");
            if (!digits.isEmpty()) {
                return digits + " km";
            }
        }
        Matcher dotted = Pattern.compile("(\\d{1,3}[.,]\\d{3})\\s*(km|kn|㎞|KM|キロ)").matcher(text);
        if (dotted.find()) {
            return dotted.group(1).replace(",", "").replace(".", "") + " km";
        }
        Matcher spaced = Pattern.compile("(\\d{1,3})\\s+(\\d{3})\\s*(km|kn|㎞|KM|キロ)").matcher(text);
        if (spaced.find()) {
            return spaced.group(1) + spaced.group(2) + " km";
        }
        Matcher matcher = Pattern.compile("(\\d{1,6})\\s*(km|kn|㎞|KM|キロ)").matcher(text);
        if (matcher.find()) {
            String digits = matcher.group(1);
            int from = Math.max(0, matcher.start() - 8);
            String before = text.substring(from, matcher.start()).toLowerCase();
            if (!before.contains("cc")) {
                return digits + " km";
            }
        }
        return null;
    }

    private String extractEngine(String text) {
        Matcher labeledComma = Pattern.compile(
                "(?i)(?:排気量|displacement|engine\\s+(?:size|capacity|displacement))[^\\d]{0,16}(\\d{1,3})[,.](\\d{3})")
                .matcher(text);
        if (labeledComma.find()) {
            return labeledComma.group(1) + labeledComma.group(2) + " cc";
        }
        Matcher labeled = Pattern.compile(
                "(?i)(?:排気量|displacement|engine\\s+(?:size|capacity|displacement))[^\\d]{0,16}(\\d{3,4})")
                .matcher(text);
        if (labeled.find()) {
            return labeled.group(1) + " cc";
        }
        Matcher ccComma = Pattern.compile("(\\d{1,3})[,.](\\d{3})\\s*(cc|CC|ｃｃ)").matcher(text);
        if (ccComma.find()) {
            return ccComma.group(1) + ccComma.group(2) + " cc";
        }
        Matcher cc = Pattern.compile("(\\d{3,4})\\s*(cc|CC|ｃｃ)").matcher(text);
        if (cc.find()) {
            return cc.group(1) + " cc";
        }
        return null;
    }

    private int toWesternYear(String era, String number) {
        int n = "元".equals(number) ? 1 : Integer.parseInt(number);
        if ("令和".equals(era)) {
            return 2018 + n;
        }
        if ("平成".equals(era)) {
            return 1988 + n;
        }
        return 1925 + n;
    }

    private String extractLot(String text, String mileage) {
        String mileageDigits = mileage == null ? "" : mileage.replaceAll("[^0-9]", "");
        Matcher labeled = Pattern.compile(
                "(?i)(?:出品番号|(?<![A-Za-z])lot(?:\\s*(?:no\\.?|number|#))?|exhibition\\s+number|listing\\s+number)[^\\d]{0,20}(\\d{3,6})")
                .matcher(text);
        if (labeled.find()) {
            String lot = normalizeLot(labeled.group(1), mileageDigits);
            if (lot != null && !isUnlikelyLot(lot, mileageDigits, text)) {
                return lot;
            }
        }
        String extracted = extract(text, new String[] {
                "出品番号", "出品No", "出品NO", "ロット", "Lot number", "Lot no", "Exhibition number", "Listing number"
        });
        if (extracted != null) {
            Matcher digits = Pattern.compile("(\\d{3,6})").matcher(extracted);
            if (digits.find()) {
                String lot = normalizeLot(digits.group(1), mileageDigits);
                if (lot != null && !isUnlikelyLot(lot, mileageDigits, text)) {
                    return lot;
                }
            }
        }
        String head = text.substring(0, Math.min(text.length(), 900));
        Matcher rows = Pattern.compile("(?m)^\\s*(\\d{4,6})(?!\\d)").matcher(head);
        String best = null;
        int bestCount = 0;
        while (rows.find()) {
            String candidate = normalizeLot(rows.group(1), mileageDigits);
            if (candidate == null || isUnlikelyLot(candidate, mileageDigits, text)) {
                continue;
            }
            int count = countToken(head, candidate);
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        if (best != null) {
            return best;
        }
        Matcher any = Pattern.compile("(?<!\\d)(\\d{4,6})(?!\\d)(?!\\s*(km|kn|㎞|KM|cc))").matcher(head);
        while (any.find()) {
            String candidate = normalizeLot(any.group(1), mileageDigits);
            if (candidate != null && !isUnlikelyLot(candidate, mileageDigits, text)) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeLot(String digits, String mileageDigits) {
        if (digits == null || digits.length() < 3) {
            return null;
        }
        if (digits.equals(mileageDigits)) {
            return null;
        }
        return digits;
    }

    private boolean isUnlikelyLot(String digits, String mileageDigits, String text) {
        if (digits.equals(mileageDigits)) {
            return true;
        }
        if (digits.matches("660|1000|1200|1300|1500|1800|2000|2400|2500|3000")) {
            return true;
        }
        if (text.contains(digits + " km") || text.contains(digits + "km")) {
            return true;
        }
        return Pattern.compile("[A-Z0-9]-" + Pattern.quote(digits)).matcher(text.toUpperCase()).find();
    }

    private int countToken(String text, String token) {
        Matcher matcher = Pattern.compile("(?<!\\d)" + Pattern.quote(token) + "(?!\\d)").matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String extractAuctionGrade(String text) {
        Matcher matcher = Pattern.compile(
                "(?i)(?:評価点|評点|総合評価|評価|evaluation|rating|score|auction\\s+grade|overall(?:\\s+grade)?)[^A-Za-z0-9]{0,40}([SRA-E6]|[1-6](?:\\.\\d)?)")
                .matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        String labeled = extract(text, new String[] {
                "評価点", "評点", "点数", "総合評価", "評価", "Evaluation", "Rating", "Auction grade", "Overall grade"
        });
        if (labeled != null) {
            Matcher value = Pattern.compile("^([SRA-E]|[0-6](?:\\.\\d)?)").matcher(labeled.trim());
            if (value.find()) {
                return value.group(1);
            }
        }
        Matcher besideGrade = Pattern.compile("\\bG\\s+([1-6])\\b").matcher(text);
        if (besideGrade.find()) {
            return besideGrade.group(1);
        }
        return null;
    }

    private String extractPanelGrade(String text, String... labels) {
        StringBuilder alt = new StringBuilder();
        for (String item : labels) {
            if (alt.length() > 0) {
                alt.append('|');
            }
            alt.append(Pattern.quote(item));
        }
        String label = "(?:" + alt + ")(?!色)(?!(?:erior)?\\s*colou?r)";
        Matcher matcher = Pattern.compile("(?i)" + label + "[^A-Ea-e]{0,40}(?<![A-Z0-9])([A-Ea-e])(?![A-Za-z0-9])")
                .matcher(text);
        if (matcher.find() && isGradeLetterContext(text, matcher.start(1))) {
            return matcher.group(1).toUpperCase();
        }
        Matcher nextLine = Pattern.compile("(?i)" + label + "[^\\n]{0,12}\\n\\s*([A-Ea-e])(?![A-Za-z0-9])").matcher(text);
        if (nextLine.find() && isGradeLetterContext(text, nextLine.start(1))) {
            return nextLine.group(1).toUpperCase();
        }
        return null;
    }

    private String extractGrade(String text) {
        String raw = extract(text, new String[] {"グレード", "Vehicle grade", "Trim"});
        if (isVehicleGrade(raw)) {
            return Objects.requireNonNull(raw).replaceAll("\\s+", " ").trim();
        }
        Matcher labeled = Pattern.compile(
                "(?i)(?:グレード|(?<!auction\\s)(?<!overall\\s)(?<!evaluation\\s)(?<!interior\\s)(?<!exterior\\s)grade)[^A-Za-z0-9ァ-ヶ一-龥]{0,12}([^\\n]{1,40})")
                .matcher(text);
        if (labeled.find()) {
            String grade = cleanValue(labeled.group(1));
            if (isVehicleGrade(grade) && !isFieldLabel(grade)) {
                return grade.replaceAll("\\s+", " ").trim();
            }
        }
        return null;
    }

    private boolean isVehicleGrade(String value) {
        if (!hasContent(value)) {
            return false;
        }
        return !value.trim().matches("(?i)[SRA-E]|[0-6](?:\\.\\d)?");
    }

    private String extractBodyStyle(String text) {
        String labeled = extract(text, new String[] {"ドア形状", "ドア・形状", "Door shape", "Doors"});
        if (labeled != null && labeled.toUpperCase().contains("WD") && !labeled.contains("ドア")
                && !labeled.toUpperCase().contains("SD") && !labeled.toUpperCase().contains("HB")) {
            labeled = null;
        }
        String fromLabel = bodyStyleFromToken(labeled);
        if (fromLabel != null) {
            return fromLabel;
        }
        return bodyStyleFromToken(text);
    }

    private String bodyStyleFromToken(String text) {
        if (text == null) {
            return null;
        }
        String upper = text.toUpperCase();
        if (text.length() <= 40 && upper.contains("WD") && !text.contains("ドア")
                && !upper.contains("SD") && !upper.contains("HB") && !text.contains("ハコ")) {
            return null;
        }
        Matcher any = Pattern.compile(
                "(?i)([2-5])\\s*(?:SD|HB|ドア|ハコ|doors?)|(?<![A-Z0-9])([2-5])\\s*W(?![A-Z0-9])|([2-5])\\s*ハコ")
                .matcher(text);
        if (any.find()) {
            String n = any.group(1) != null ? any.group(1) : (any.group(2) != null ? any.group(2) : any.group(3));
            return n + "-door";
        }
        Matcher doorsAfterLabel = Pattern.compile("(?i)(?:ドア(?:形状|・形状)?|doors?|door\\s+shape)[^\\d]{0,8}([2-5])")
                .matcher(text);
        if (doorsAfterLabel.find()) {
            return doorsAfterLabel.group(1) + "-door";
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.length() == 1 && digits.charAt(0) >= '2' && digits.charAt(0) <= '5') {
            return digits + "-door";
        }
        return null;
    }

    private String extractSeats(String text) {
        Matcher matcher = Pattern.compile("(?i)(?:乗車定員|seating\\s+capacity|passengers?)[^\\d]{0,12}(\\d)").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher people = Pattern.compile("(?i)(\\d)\\s*(人|名|people|persons?|seats?)").matcher(text);
        if (people.find()) {
            return people.group(1);
        }
        return digitsOnly(extract(text, new String[] {"乗車定員", "定員", "Seating capacity", "Passengers"}));
    }

    private String extractFuel(String text) {
        if (text.contains("ハイブリッド") || text.contains("ハイプリッド")
                || text.toUpperCase().contains("HYBRID")) {
            return "Hybrid";
        }
        String labeled = translateFuel(extract(text, new String[] {"燃料", "Fuel"}));
        if (labeled != null && (labeled.contains("Gasoline") || labeled.contains("Petrol") || labeled.contains("Diesel")
                || labeled.contains("Hybrid") || labeled.contains("Electric"))) {
            if (labeled.contains("Gasoline") || labeled.contains("Petrol")) {
                return "Gasoline";
            }
            if (labeled.contains("Diesel")) {
                return "Diesel";
            }
            if (labeled.contains("Electric")) {
                return "Electric";
            }
            return "Hybrid";
        }
        if (text.contains("ガソリン") || text.contains("ガソ") || text.contains("がソ") || text.contains("ヵソ")
                || text.contains("カソ") || text.toUpperCase().contains("GASOLINE")
                || text.toUpperCase().contains("PETROL")) {
            return "Gasoline";
        }
        if (text.contains("軽油") || text.contains("ディーゼル") || text.toUpperCase().contains("DIESEL")) {
            return "Diesel";
        }
        if (text.contains("電気")) {
            return "Electric";
        }
        return labeled;
    }

    private String extractTransmission(String text) {
        String fromLabel = translateTransmission(extract(text, new String[] {
                "シフト", "ミッション", "Shift", "Transmission", "Gear"
        }));
        if (fromLabel != null && fromLabel.matches("(?i)CVT|IAT|F\\.AT|FAT|AT|MT")) {
            return fromLabel;
        }
        String upper = text.toUpperCase();
        if (upper.contains("CVT") || text.contains("無段")) {
            return "CVT";
        }
        if (upper.contains("IAT")) {
            return "IAT";
        }
        if (upper.contains("F.AT") || upper.contains("FAT")) {
            return "F.AT";
        }
        if (Pattern.compile("\\bAT\\b").matcher(upper).find() || text.contains("オート") || text.contains("自動")) {
            return "AT";
        }
        if (upper.contains("MT") || text.contains("マニュアル") || text.contains("手動")) {
            return "MT";
        }
        return fromLabel;
    }

    private String extractAc(String text) {
        Matcher labeled = Pattern.compile("(?:エアコン|冷房|AIR\\s*CON(?:DITIONER)?)[^A-Za-z]{0,12}(AAC|AC)")
                .matcher(text.toUpperCase());
        if (labeled.find()) {
            return labeled.group(1);
        }
        String upper = text.toUpperCase();
        if (upper.contains("AAC")) {
            return "AAC";
        }
        Matcher ac = Pattern.compile("\\bAC\\b").matcher(upper);
        if (ac.find()) {
            return "AC";
        }
        return null;
    }

    private String extractHistory(String text) {
        String labeled = extract(text, new String[] {"車歴", "History", "Vehicle history"});
        String source = labeled != null ? labeled : text;
        if (source.contains("レンタ") || (labeled != null && labeled.matches("(?i).*rental.*"))) {
            return "Rental";
        }
        if (source.contains("自家用") || (labeled != null && (labeled.contains("自家") || labeled.matches("(?i).*private.*")))) {
            return "Private";
        }
        if (source.contains("事業") || (labeled != null && labeled.matches("(?i).*commercial.*"))) {
            return "Commercial";
        }
        if (labeled != null && labeled.matches("(?i)rental|private|commercial")) {
            return labeled;
        }
        return null;
    }

    private void extractDimensions(AuctionParseResult result, String text) {
        Matcher times = Pattern.compile("(\\d{3})\\s*[×xX]\\s*(\\d{3})\\s*[×xX]\\s*(\\d{3})").matcher(text);
        if (times.find()) {
            putDimension(result, times.group(1), times.group(2), times.group(3));
            return;
        }
        Matcher labeled = Pattern.compile("(?i)(?:諸元|spec(?:ification)?s?)[^\\d]{0,24}(\\d{3})[^\\d]{1,12}(\\d{3})[^\\d]{1,12}(\\d{3})")
                .matcher(text);
        if (labeled.find()) {
            putDimension(result, labeled.group(1), labeled.group(2), labeled.group(3));
            return;
        }
        Matcher length = Pattern.compile("(?i)(?:長さ|length)[^\\d]{0,8}(\\d{3})").matcher(text);
        Matcher width = Pattern.compile("(?i)(?:幅|width)[^\\d]{0,8}(\\d{3})").matcher(text);
        Matcher height = Pattern.compile("(?i)(?:高さ|height)[^\\d]{0,8}(\\d{3})").matcher(text);
        if (length.find() && width.find() && height.find()) {
            putDimension(result, length.group(1), width.group(1), height.group(1));
            return;
        }
        Matcher loose = Pattern.compile("(\\d{3})[^\\d]{1,24}(\\d{3})[^\\d]{1,24}(\\d{3})").matcher(text);
        while (loose.find()) {
            if (putDimension(result, loose.group(1), loose.group(2), loose.group(3))) {
                return;
            }
        }
    }

    private boolean putDimension(AuctionParseResult result, String length, String width, String height) {
        int l = Integer.parseInt(length);
        int w = Integer.parseInt(width);
        int h = Integer.parseInt(height);
        if (l >= 250 && l <= 600 && w >= 120 && w <= 220 && h >= 120 && h <= 220) {
            result.put("lengthCm", l + " cm");
            result.put("widthCm", w + " cm");
            result.put("heightCm", h + " cm");
            return true;
        }
        return false;
    }

    private String extractColor(String text) {
        Matcher labeled = Pattern.compile(
                "(?i)(?:外装色|(?<!interior\\s)(?<!inner\\s)(?:exterior\\s+|outer\\s+|body\\s+)?(?<![A-Za-z])colou?r(?![A-Za-z])(?:\\s*(?:name|code|no\\.?)?)?|カラー)")
                .matcher(text);
        while (labeled.find()) {
            String candidate = usableColorName(sameLineAfter(text, labeled.end()));
            if (candidate == null) {
                String next = nextLine(text, labeled.end());
                if (!looksLikeChassis(next) && !looksLikeModelCode(next)) {
                    candidate = usableColorName(next);
                }
            }
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String usableColorName(String value) {
        if (!hasContent(value) || isFieldLabel(value) || isSpecToken(value) || isPanelGradeToken(value)) {
            return null;
        }
        if (isPaintColorCode(value)) {
            return null;
        }
        String translated = translateColor(value);
        if (!hasContent(translated) || isPaintColorCode(translated) || isPanelGradeToken(translated)) {
            return null;
        }
        String cleaned = getString(translated);
        if (!hasContent(cleaned) || isPaintColorCode(cleaned) || isPanelGradeToken(cleaned)
                || looksLikeChassis(cleaned) || looksLikeModelCode(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private static String getString(String translated) {
        String cleaned = translated.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("(?i)\\b(?:exterior|outer|body|interior)\\s+(?:colou?r\\s+)?", "");
        cleaned = cleaned.replaceAll("(?i)\\b(?:colou?r(?:\\s*(?:code|no\\.?))?|code)\\b", "").trim();
        cleaned = cleaned.replaceAll("(?i)(?<![A-Z0-9])(?:[A-Z]\\d{2}|\\d{3}|\\d[A-Z]\\d)(?![A-Z0-9])", "").trim();
        cleaned = cleaned.replaceAll("[A-Za-z]{1,5}\\d{2,3}[A-Za-z0-9]{0,2}-\\d{5,8}", "").trim();
        cleaned = cleaned.replaceAll("(?i)\\b(?:[0-9][A-Z]{2}|[A-Z]{3})-[A-Z0-9]{3,10}\\b", "").trim();
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    private String extractColorCode(String text) {
        Matcher labeled = Pattern.compile(
                "(?i)(?:外装色|色コード|カラー\\s*(?:NO\\.?|番号)?|(?<!interior\\s)(?<!inner\\s)(?:exterior\\s+|outer\\s+|body\\s+)?(?<![A-Za-z])colou?r(?![A-Za-z])(?:\\s*(?:code|no\\.?))?)")
                .matcher(text);
        while (labeled.find()) {
            String code = firstPaintColorCode(sameLineAfter(text, labeled.end()), text);
            if (code != null) {
                return code;
            }
            String next = nextLine(text, labeled.end());
            if (looksLikeChassis(next) || looksLikeModelCode(next)) {
                continue;
            }
            code = firstPaintColorCode(next, text);
            if (code != null) {
                return code;
            }
        }
        return firstPaintColorCode(looseWhiteColorWindow(text), text);
    }

    private String sameLineAfter(String text, int from) {
        if (from >= text.length()) {
            return "";
        }
        int end = text.indexOf('\n', from);
        if (end < 0) {
            end = text.length();
        }
        return cleanValue(text.substring(from, end));
    }

    private String looseWhiteColorWindow(String text) {
        Matcher loose = Pattern.compile("(?<!\\d)(W\\d{2})(?!\\d)").matcher(text.toUpperCase());
        if (loose.find() && !text.toUpperCase().contains(loose.group(1) + "-")) {
            return loose.group(1);
        }
        return null;
    }

    private String firstPaintColorCode(String value, String wholeText) {
        if (value == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?<![A-Z0-9])([A-Z]\\d{2}|\\d{3}|\\d[A-Z]\\d)(?![A-Z0-9])")
                .matcher(value.toUpperCase());
        String upper = wholeText == null ? value.toUpperCase() : wholeText.toUpperCase();
        while (matcher.find()) {
            String code = matcher.group(1);
            if (isIgnoredColorCode(code) || upper.contains(code + "-") || isPanelGradeToken(code)) {
                continue;
            }
            return code;
        }
        return null;
    }

    private boolean isPaintColorCode(String value) {
        return value != null && value.trim().toUpperCase().matches("[A-Z]\\d{2}|\\d{3}|\\d[A-Z]\\d");
    }

    private boolean isPanelGradeToken(String value) {
        return value != null && value.trim().matches("(?i)[A-ES*]|[1-6](?:\\.5)?");
    }

    private boolean looksLikeChassis(String value) {
        return value != null && Pattern.compile("[A-Z]{1,5}\\d{2,3}[A-Z0-9]{0,2}-\\d{5,8}", Pattern.CASE_INSENSITIVE)
                .matcher(value).find();
    }

    private boolean looksLikeModelCode(String value) {
        return value != null && Pattern.compile("(?i)\\b(?:[0-9][A-Z]{2}|[A-Z]{3})-[A-Z0-9]{3,10}\\b").matcher(value).find();
    }

    private String colorFromCode(String code) {
        if (code == null) {
            return null;
        }
        String key = code.toUpperCase();
        if (key.matches("W\\d{2}|070|040|058|W09|W24|W25")) {
            return "White";
        }
        if (key.matches("202|209|218|219")) {
            return "Black";
        }
        if (key.matches("1F7|1G3|1G4|1C0")) {
            return "Silver";
        }
        return null;
    }

    private boolean isIgnoredColorCode(String code) {
        return code.matches("AAC|IAT|ABS|ETC|USS|KCAA|DAA|DBA|CBA|5BA|5AA|6AA|3BA|PS|PW|AC");
    }

    private void fillPanelGradesFromNoise(AuctionParseResult result, String text) {
        if (result.getFields().get("exteriorGrade") != null && result.getFields().get("interiorGrade") != null) {
            return;
        }
        Matcher pair = Pattern.compile("\\b([A-Ea-e])\\s*[/|lI]\\s*([A-Ea-e])\\b").matcher(text);
        if (pair.find()) {
            if (result.getFields().get("exteriorGrade") == null) {
                result.put("exteriorGrade", pair.group(1).toUpperCase());
            }
            if (result.getFields().get("interiorGrade") == null) {
                result.put("interiorGrade", pair.group(2).toUpperCase());
            }
        }
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[|｜]+", " ").trim();
        String[] stop = new String[] {"車台番号", "車名", "型式", "年式", "走行", "カラー", "外装色", "内装色",
                "排気量", "シフト", "燃料", "評価点", "出品番号", "ドア形状", "乗車定員", "グレード", "車検",
                "車歴", "エアコン", "諸元", "セールスポイント", "純正装備", "リサイクル",
                "Chassis number", "Vehicle name", "Car name", "Model code", "First registration",
                "Mileage", "Exterior color", "Interior color", "Displacement", "Engine size",
                "Transmission", "Fuel", "Evaluation", "Lot number", "Door shape",
                "Seating capacity", "Grade", "History", "Air conditioner", "Specifications"};
        String lower = cleaned.toLowerCase();
        for (String s : stop) {
            int idx = lower.indexOf(s.toLowerCase());
            if (idx > 0) {
                cleaned = cleaned.substring(0, idx);
                lower = cleaned.toLowerCase();
            }
        }
        return cleaned.trim();
    }

    private boolean isFieldLabel(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        String[] labels = new String[] {"車台番号", "車名", "型式", "年式", "走行", "カラー", "外装色", "内装色",
                "排気量", "シフト", "燃料", "評価点", "出品番号", "ドア形状", "乗車定員", "グレード", "車検",
                "車歴", "エアコン", "諸元", "セールスポイント", "純正装備", "リサイクル", "登録番号",
                "最大積載", "輸入車", "保証書", "検査員", "注意事項",
                "Chassis number", "Vehicle name", "Car name", "Model name", "Model code",
                "First registration", "Mileage", "Exterior color", "Interior color",
                "Displacement", "Engine size", "Transmission", "Fuel", "Evaluation",
                "Lot number", "Door shape", "Seating capacity", "Grade", "History",
                "Air conditioner", "Specifications"};
        String lower = trimmed.toLowerCase();
        for (String label : labels) {
            String key = label.toLowerCase();
            if (lower.equals(key) || lower.startsWith(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpecToken(String value) {
        if (value == null) {
            return false;
        }
        String token = value.trim().toUpperCase().split("\\s+")[0].replace(".", "");
        return token.matches("IAT|AAC|AT|MT|CVT|FAT|PS|PW|ABS|ETC|AC|KM|CC");
    }

    private boolean hasContent(String value) {
        return value != null && !value.replaceAll("[\\s:：・\\-_/]", "").isEmpty();
    }

    private String digitsOnly(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private String translateMaker(String value) {
        return translator.translateJaToEn(value);
    }

    private String translateColor(String value) {
        return translator.translateJaToEn(value);
    }

    private String translateTransmission(String value) {
        String source = value == null ? "" : value.toUpperCase();
        if (source.contains("CVT") || (value != null && value.contains("無段"))) {
            return "CVT";
        }
        if (source.contains("IAT")) {
            return "IAT";
        }
        if (source.contains("F.AT") || source.contains("FAT")) {
            return "F.AT";
        }
        if (source.contains("AT") || (value != null && (value.contains("オート") || value.contains("自動")))) {
            return "AT";
        }
        if (source.contains("MT") || (value != null && (value.contains("マニュアル") || value.contains("手動")))) {
            return "MT";
        }
        return value;
    }

    private String translateFuel(String value) {
        return translator.translateJaToEn(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String firstNonNull(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }
}
