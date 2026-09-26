package com.carsale.erp.importpipeline.repository;

import java.util.List;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;


public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, Long> {

    List<VehiclePhoto> findByChassisNoOrderBySortOrderAscIdAsc(String chassisNo);

    long countByChassisNo(String chassisNo);

    void deleteByChassisNo(String chassisNo);
}
