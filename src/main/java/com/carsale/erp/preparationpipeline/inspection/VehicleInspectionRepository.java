package com.carsale.erp.preparationpipeline.inspection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, String> {
}
