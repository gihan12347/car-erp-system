package com.carsale.erp.preparationpipeline.inspection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.preparationpipeline.inspection.VehicleInspection;

public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, String> {
}
