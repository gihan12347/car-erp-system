package com.carsale.erp.service;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.YardRecord;
import com.carsale.erp.repository.VehicleRepository;
import com.carsale.erp.repository.YardRecordRepository;

@Service
public class YardService {

    private final VehicleRepository vehicleRepository;
    private final YardRecordRepository yardRecordRepository;

    public YardService(VehicleRepository vehicleRepository, YardRecordRepository yardRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.yardRecordRepository = yardRecordRepository;
    }

    public YardRecord findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return yardRecordRepository.findById(chassisNo.trim()).orElse(null);
    }

    public YardRecord prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        YardRecord record = yardRecordRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new YardRecord();
            record.setChassisNo(chassisNo);
            record.setInspectionStatus("PENDING");
        }
        return record;
    }

    public boolean isComplete(String chassisNo) {
        YardRecord record = findByChassisNo(chassisNo);
        return record != null && record.isCompleted();
    }

    @Transactional
    public YardRecord save(YardRecord incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        if (isBlank(incoming.getInspectionStatus())) {
            incoming.setInspectionStatus("PENDING");
        }

        YardRecord existing = yardRecordRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            BeanUtils.copyProperties(incoming, existing);
            existing.setChassisNo(chassisNo);
            return yardRecordRepository.save(existing);
        }
        incoming.setChassisNo(chassisNo);
        return yardRecordRepository.save(incoming);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
