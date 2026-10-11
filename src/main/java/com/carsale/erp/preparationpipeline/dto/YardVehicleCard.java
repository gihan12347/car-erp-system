package com.carsale.erp.preparationpipeline.dto;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.shared.vehicle.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class YardVehicleCard {
    private final Vehicle vehicle;
    private final VehiclePhoto photo;
}