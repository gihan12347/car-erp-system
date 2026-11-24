package com.carsale.erp.preparationpipeline.dto;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.preparationpipeline.util.Yard;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleStage;
import lombok.Getter;

@Getter
public final class YardVehicleCard {
    private final Vehicle vehicle;
    private final VehiclePhoto photo;
    private final String yardCode;
    private final String yardTitle;
    private final String href;
    private final String arrivalDate;

    public YardVehicleCard(Vehicle vehicle, VehiclePhoto photo) {
        this(vehicle, photo, null, null, null, null);
    }

    public YardVehicleCard(
            Vehicle vehicle,
            VehiclePhoto photo,
            String yardCode,
            String yardTitle,
            String href
    ) {
        this(vehicle, photo, yardCode, yardTitle, href, null);
    }

    public YardVehicleCard(
            Vehicle vehicle,
            VehiclePhoto photo,
            String yardCode,
            String yardTitle,
            String href,
            String arrivalDate
    ) {
        this.vehicle = vehicle;
        this.photo = photo;
        this.yardCode = yardCode;
        this.yardTitle = yardTitle;
        this.href = href;
        this.arrivalDate = arrivalDate;
    }

    public String getMakeModel() {
        return Yard.makeModel(vehicle);
    }

    public String getStatusLabel() {
        VehicleStage stage = vehicle == null ? null : vehicle.getStage();
        if (stage == VehicleStage.SOLD || stage == VehicleStage.RESERVED || stage == VehicleStage.READY) {
            return stage.getDisplayName();
        }
        if (yardCode != null && !yardCode.trim().isEmpty()) {
            return "In yard";
        }
        if (stage == null) {
            return "Unassigned";
        }
        return stage.getDisplayName();
    }

    public String getStatusTone() {
        String label = getStatusLabel();
        if ("Sale".equals(label) || "In yard".equals(label)) {
            return "sea";
        }
        if ("Sold".equals(label) || "Reserved".equals(label)) {
            return "rust";
        }
        if ("Workshop".equals(label) || "Customs".equals(label) || "Shipping".equals(label)) {
            return "gold";
        }
        return "leaf";
    }

    public String getSearchText() {
        if (vehicle == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        append(text, vehicle.getChassisNo());
        append(text, vehicle.getStockNo());
        append(text, vehicle.getMake());
        append(text, vehicle.getModel());
        append(text, vehicle.getYear());
        append(text, vehicle.getColor());
        append(text, yardCode);
        append(text, yardTitle);
        append(text, getStatusLabel());
        return text.toString().trim().toLowerCase();
    }

    private static void append(StringBuilder text, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (text.length() > 0) {
            text.append(' ');
        }
        text.append(value.trim());
    }
}
