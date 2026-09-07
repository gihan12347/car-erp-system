package com.carsale.erp.readypipeline;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.readypipeline.SaleListing;

public interface SaleListingRepository extends JpaRepository<SaleListing, String> {
}
