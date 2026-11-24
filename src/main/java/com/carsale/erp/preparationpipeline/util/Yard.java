package com.carsale.erp.preparationpipeline.util;

import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.shared.vehicle.Vehicle;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Yard {

    public static String yardTitle(YardBay yard) {
        if (yard.getYardName() != null && !yard.getYardName().trim().isEmpty()) {
            return yard.getYardName();
        }
        return yard.getBayCode();
    }

    public static String vehicleLabel(Vehicle vehicle) {
        if (vehicle.getModel() != null && !vehicle.getModel().trim().isEmpty()) {
            return vehicle.getModel();
        }
        return vehicle.getChassisNo();
    }

    public static String makeModel(Vehicle vehicle) {
        if (vehicle == null) {
            return "Vehicle";
        }
        String make = vehicle.getMake() == null ? "" : vehicle.getMake().trim();
        String model = vehicle.getModel() == null ? "" : vehicle.getModel().trim();
        if (!make.isEmpty() && !model.isEmpty()) {
            if (model.toLowerCase(Locale.ROOT).contains(make.toLowerCase(Locale.ROOT))) {
                return model;
            }
            return make + " " + model;
        }
        if (!model.isEmpty()) {
            return model;
        }
        if (!make.isEmpty()) {
            return make;
        }
        return vehicleLabel(vehicle);
    }

    public static boolean matchesQuery(Vehicle vehicle, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return contains(vehicle.getChassisNo(), needle)
                || contains(vehicle.getModel(), needle)
                || contains(vehicle.getMake(), needle)
                || contains(vehicle.getLotNo(), needle)
                || contains(vehicle.getStockNo(), needle)
                || contains(vehicle.getColor(), needle)
                || contains(vehicle.getYear(), needle);
    }

    public static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    public static boolean isAssigned(YardRecord record) {
        return record != null
                && !isBlank(record.getBayNo())
                && !isBlank(record.getArrivalDate());
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
