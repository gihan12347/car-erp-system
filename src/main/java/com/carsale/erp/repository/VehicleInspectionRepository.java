package com.carsale.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.entity.VehicleInspection;

public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, String> {
}
