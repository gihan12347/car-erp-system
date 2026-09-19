package com.carsale.erp.readypipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleLocationRepository extends JpaRepository<SaleLocation, Long> {

    List<SaleLocation> findAllByOrderBySortOrderAscIdAsc();

    List<SaleLocation> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsBySaleCodeIgnoreCase(String saleCode);

    Optional<SaleLocation> findBySaleCodeIgnoreCase(String saleCode);
}
