package com.carsale.erp.importpipeline.standards;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Service
public class StandardsCertificateParser {


//    public AuctionParseResult parse(String text) {
//        AuctionParseResult result = new AuctionParseResult();
//        if (text == null || text.trim().isEmpty()) {
//            result.setSuccess(false);
//            result.setMessage("The standards certificate was empty.");
//            return result;
//        }
//
//        String normalized = normalize(text);
//        result.put("scheduleType", extractSchedule(normalized));
//        result.put("emissionCo", extractEmission(normalized, "\\bCO\\b(?!\\w)"));
//        result.put("emissionNmhc", extractEmission(normalized, "\\bNMHC\\b"));
//        result.put("emissionNox", extractEmission(normalized, "(?<!\\+)\\bNO\\s*x\\b"));
//        result.put("emissionPm", extractEmission(normalized, "\\bPM\\b"));
//        result.put("emissionHcNox", extractEmission(normalized, "\\bHC\\s*\\+\\s*NO\\s*x\\b"));
//        result.put("emissionHc", extractEmission(normalized, "(?<![A-Z])\\bHC\\b(?!\\s*\\+)"));
//        result.put("emissionThc", extractEmission(normalized, "\\bTHC\\b"));
//        result.put("emissionCh4", extractEmission(normalized, "\\bCH\\s*4\\b"));
//        result.put("emissionSmoke", extractEmission(normalized, "\\bSmoke\\b"));
//        result.put("threePointSeatBelts", marked(normalized,
//                "Three\\s+point\\s+seat\\s+belts(?:\\s+for\\s+driver\\s+and\\s+front\\s+passengers)?"));
//        result.put("twoPointSeatBelts", marked(normalized,
//                "Minimum\\s+two\\s+point\\s+seat\\s+belts(?:\\s+for\\s+other\\s+passengers)?"));
//        result.put("driverAirbag", firstNonNull(
//                marked(normalized, "Air\\s*Bags?\\s*[:\\-]?\\s*(?:Driver)"),
//                marked(normalized, "Driver(?:'s)?\\s+Air\\s*Bag")
//        ));
//        result.put("passengerAirbag", firstNonNull(
//                marked(normalized, "Front\\s+Passenger"),
//                marked(normalized, "Passenger(?:'s)?\\s+Air\\s*Bag")
//        ));
//        result.put("absFitted", marked(normalized, "\\bABS\\b"));
//        result.put("make", extractLabeled(normalized, "Make", "Model", "Chassis"));
//        result.put("model", extractLabeled(normalized, "Model", "Chassis", "Place of Inspection"));
//        result.put("chassisVin", firstNonNull(
//                extractLabeled(normalized, "Chassis Number", "Place of Inspection", "Date of Inspection"),
//                extractLabeled(normalized, "Chassis No\\.?", "Place of Inspection", "Date of Inspection"),
//                findChassis(normalized)
//        ));
//        result.put("placeOfInspection", extractLabeled(
//                normalized, "Place of Inspection", "Date of Inspection", "Remarks"));
//        result.put("inspectionDate", extractLabeled(
//                normalized, "Date of Inspection", "Remarks", "Make"));
//        result.put("remarks", extractRemarks(normalized));
//
//        boolean any = !result.getFields().isEmpty();
//        result.setSuccess(any);
//        result.setMessage(any
//                ? "Filled " + result.getFields().size() + " fields from the standards certificate. Please review."
//                : "The document was read, but fields could not be mapped. Please fill them manually.");
//        result.setRawText(text);
//        return result;
//    }

}
