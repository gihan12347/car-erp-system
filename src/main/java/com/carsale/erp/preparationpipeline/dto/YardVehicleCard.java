package com.carsale.erp.preparationpipeline.dto;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.shared.vehicle.Vehicle;

public final class YardVehicleCard {
    private final Vehicle vehicle;
    private final VehiclePhoto photo;

    public YardVehicleCard(Vehicle vehicle, VehiclePhoto photo) {
        this.vehicle = vehicle;
        this.photo = photo;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public VehiclePhoto getPhoto() {
        return photo;
    }
}