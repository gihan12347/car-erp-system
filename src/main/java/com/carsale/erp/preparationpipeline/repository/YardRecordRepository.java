package com.carsale.erp.preparationpipeline.repository;

import java.util.List;

import com.carsale.erp.preparationpipeline.model.YardRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YardRecordRepository extends JpaRepository<YardRecord, String> {

    List<YardRecord> findByBayNoIgnoreCase(String bayNo);

    long countByBayNoIgnoreCase(String bayNo);
}
