package com.carsale.erp.readypipeline.repository;

import com.carsale.erp.readypipeline.model.VehicleRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRegistrationRepository extends JpaRepository<VehicleRegistration, String> {
}
