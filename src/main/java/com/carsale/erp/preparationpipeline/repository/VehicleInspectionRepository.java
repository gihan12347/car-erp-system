package com.carsale.erp.preparationpipeline.repository;

import com.carsale.erp.preparationpipeline.model.VehicleInspection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, String> {
}
