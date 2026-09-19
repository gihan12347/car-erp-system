package com.carsale.erp.preparationpipeline.yard;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface YardRecordRepository extends JpaRepository<YardRecord, String> {

    List<YardRecord> findByBayNoIgnoreCase(String bayNo);

    long countByBayNoIgnoreCase(String bayNo);
}
