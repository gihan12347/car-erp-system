package com.carsale.erp.shared.vehicle;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleExtraCostRepository extends JpaRepository<VehicleExtraCost, Long> {

    List<VehicleExtraCost> findByChassisNoOrderByCostDateAscIdAsc(String chassisNo);
}
