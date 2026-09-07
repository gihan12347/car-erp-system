package com.carsale.erp.preparationpipeline.workshop;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.preparationpipeline.workshop.WorkshopJob;

public interface WorkshopJobRepository extends JpaRepository<WorkshopJob, String> {
}
