package com.carsale.erp.preparationpipeline.repository;

import com.carsale.erp.preparationpipeline.model.PreviousOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreviousOwnerRepository extends JpaRepository<PreviousOwner, String> {
}
