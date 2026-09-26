package com.carsale.erp.preparationpipeline.repository;

import com.carsale.erp.preparationpipeline.model.WorkshopJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopJobRepository extends JpaRepository<WorkshopJob, String> {
}
