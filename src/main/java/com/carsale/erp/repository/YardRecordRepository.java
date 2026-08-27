package com.carsale.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.entity.YardRecord;

public interface YardRecordRepository extends JpaRepository<YardRecord, String> {
}
