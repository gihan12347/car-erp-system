package com.carsale.erp.shared.vehicle;

import com.carsale.erp.shared.vehicle.Vehicle;

final class VehicleFieldNormalizer {

    private VehicleFieldNormalizer() {
    }

    static void normalize(Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        vehicle.setStockNo(trimTo(vehicle.getStockNo(), 20));
        vehicle.setMake(trimTo(vehicle.getMake(), 80));
        vehicle.setModel(trimTo(vehicle.getModel(), 80));
        vehicle.setGrade(trimTo(vehicle.getGrade(), 80));
        vehicle.setModelCode(trimTo(vehicle.getModelCode(), 40));
        vehicle.setYear(normalizeFirstRegistration(vehicle.getYear()));
        vehicle.setChassisNo(trimTo(vehicle.getChassisNo(), 40));
        vehicle.setMileage(trimTo(vehicle.getMileage(), 40));
        vehicle.setColor(trimTo(vehicle.getColor(), 40));
        vehicle.setEngineSize(trimTo(vehicle.getEngineSize(), 40));
        vehicle.setTransmission(trimTo(vehicle.getTransmission(), 40));
        vehicle.setFuel(trimTo(vehicle.getFuel(), 40));
        vehicle.setDoors(trimTo(vehicle.getDoors(), 10));
        vehicle.setSeats(trimTo(vehicle.getSeats(), 20));
        vehicle.setAuctionHouse(trimTo(vehicle.getAuctionHouse(), 80));
        vehicle.setLotNo(trimTo(vehicle.getLotNo(), 40));
        vehicle.setAuctionGrade(trimTo(vehicle.getAuctionGrade(), 20));
        vehicle.setInspection(trimTo(vehicle.getInspection(), 40));
        vehicle.setRegistrationMonth(trimTo(vehicle.getRegistrationMonth(), 10));
        vehicle.setExteriorGrade(trimTo(vehicle.getExteriorGrade(), 10));
        vehicle.setInteriorGrade(trimTo(vehicle.getInteriorGrade(), 10));
        vehicle.setBodyStyle(trimTo(vehicle.getBodyStyle(), 40));
        vehicle.setHistory(trimTo(vehicle.getHistory(), 40));
        vehicle.setColorCode(trimTo(vehicle.getColorCode(), 20));
        vehicle.setAcType(trimTo(vehicle.getAcType(), 40));
        vehicle.setLengthCm(trimTo(vehicle.getLengthCm(), 20));
        vehicle.setWidthCm(trimTo(vehicle.getWidthCm(), 20));
        vehicle.setHeightCm(trimTo(vehicle.getHeightCm(), 20));
        vehicle.setNotes(trimTo(vehicle.getNotes(), 2000));
        vehicle.setSheetOriginalName(trimTo(vehicle.getSheetOriginalName(), 255));
        vehicle.setSheetStoredName(trimTo(vehicle.getSheetStoredName(), 120));
        vehicle.setSheetContentType(trimTo(vehicle.getSheetContentType(), 80));
    }

    private static String normalizeFirstRegistration(String value) {
        String trimmed = trimTo(value, 40);
        if (trimmed == null) {
            return null;
        }
        // If OCR pasted a long header line, keep only a plausible registration snippet.
        if (trimmed.length() > 24) {
            java.util.regex.Matcher monthYear = java.util.regex.Pattern
                    .compile("(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}")
                    .matcher(trimmed);
            if (monthYear.find()) {
                return monthYear.group();
            }
            java.util.regex.Matcher yearOnly = java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(trimmed);
            if (yearOnly.find()) {
                return yearOnly.group();
            }
            return trimmed.substring(0, 40);
        }
        return trimmed;
    }

    private static String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
