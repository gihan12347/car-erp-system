package com.carsale.erp.preparationpipeline.yard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class YardService {

    private final VehicleRepository vehicleRepository;
    private final YardRecordRepository yardRecordRepository;
    private final YardBayService yardBayService;

    public YardService(
            VehicleRepository vehicleRepository,
            YardRecordRepository yardRecordRepository,
            YardBayService yardBayService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.yardRecordRepository = yardRecordRepository;
        this.yardBayService = yardBayService;
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
        return isAssigned(record);
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

        if (!isBlank(incoming.getBayNo())) {
            YardBay yard = yardBayService.requireAssignable(incoming.getBayNo(), chassisNo);
            incoming.setBayNo(yard.getBayCode());
            incoming.setYardSection(yard.getLocation());
        }

        incoming.setCompleted(isAssigned(incoming));

        YardRecord existing = yardRecordRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            existing.setBayNo(incoming.getBayNo());
            existing.setYardSection(incoming.getYardSection());
            existing.setArrivalDate(incoming.getArrivalDate());
            existing.setCompleted(incoming.isCompleted());
            existing.setChassisNo(chassisNo);
            return yardRecordRepository.save(existing);
        }
        incoming.setChassisNo(chassisNo);
        return yardRecordRepository.save(incoming);
    }

    private static boolean isAssigned(YardRecord record) {
        return record != null
                && !isBlank(record.getBayNo())
                && !isBlank(record.getArrivalDate());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
