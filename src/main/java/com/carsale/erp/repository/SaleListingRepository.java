package com.carsale.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.entity.SaleListing;

public interface SaleListingRepository extends JpaRepository<SaleListing, String> {
}
