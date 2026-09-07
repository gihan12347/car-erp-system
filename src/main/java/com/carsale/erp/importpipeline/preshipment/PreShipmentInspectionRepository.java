package com.carsale.erp.importpipeline.preshipment;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.importpipeline.preshipment.PreShipmentInspection;

public interface PreShipmentInspectionRepository extends JpaRepository<PreShipmentInspection, String> {
}
