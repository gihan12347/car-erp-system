package com.carsale.erp.preparationpipeline.yard;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface YardBayRepository extends JpaRepository<YardBay, Long> {

    List<YardBay> findAllByOrderBySortOrderAscIdAsc();

    List<YardBay> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByBayCodeIgnoreCase(String bayCode);

    Optional<YardBay> findByBayCodeIgnoreCase(String bayCode);
}
