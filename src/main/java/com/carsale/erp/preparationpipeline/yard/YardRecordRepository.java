package com.carsale.erp.preparationpipeline.yard;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.preparationpipeline.yard.YardRecord;

public interface YardRecordRepository extends JpaRepository<YardRecord, String> {
}
