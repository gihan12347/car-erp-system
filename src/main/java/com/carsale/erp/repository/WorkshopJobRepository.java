package com.carsale.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.entity.WorkshopJob;

public interface WorkshopJobRepository extends JpaRepository<WorkshopJob, String> {
}
