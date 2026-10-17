package com.carsale.erp.shared.vehicle;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    @Query("SELECT v FROM Vehicle v WHERE "
            + "LOWER(v.chassisNo) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(v.model) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(v.make) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(v.lotNo) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(v.stockNo) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(v.modelCode) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "ORDER BY v.chassisNo")
    List<Vehicle> search(@Param("query") String query);

    boolean existsByStockNo(String stockNo);
    boolean existsByChassisNo(String chassisNo);

    List<Vehicle> findByStageOrderByChassisNoAsc(VehicleStage stage);
}
