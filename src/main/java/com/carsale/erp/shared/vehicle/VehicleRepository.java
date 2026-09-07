package com.carsale.erp.shared.vehicle;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carsale.erp.shared.vehicle.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    List<Vehicle> findAllByOrderByChassisNoAsc();

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
    Optional<Vehicle> findByChassisNo(String chassisNo);
    boolean existsByChassisNo(String chassisNo);
}
