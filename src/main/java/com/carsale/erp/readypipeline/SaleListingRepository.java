package com.carsale.erp.readypipeline;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleListingRepository extends JpaRepository<SaleListing, String> {

    List<SaleListing> findBySaleCodeIgnoreCase(String saleCode);
}
