package com.carsale.erp.shared.share;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleShareLinkRepository extends JpaRepository<VehicleShareLink, Long> {

    Optional<VehicleShareLink> findFirstByChassisNoAndActiveTrueOrderByIdDesc(String chassisNo);

    Optional<VehicleShareLink> findByPublicToken(String publicToken);
}
