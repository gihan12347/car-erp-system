package com.carsale.erp.preparationpipeline.service;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.repository.YardRecordRepository;
import com.carsale.erp.preparationpipeline.util.Yard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YardService {

    private final VehicleRepository vehicleRepository;
    private final YardRecordRepository yardRecordRepository;
    private final YardBayService yardBayService;
    private final VehiclePhotoService vehiclePhotoService;

    public YardRecord findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, yardRecordRepository, null);
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
        return Yard.isAssigned(record);
    }

    @Transactional
    public YardRecord save(YardRecord incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        if (Yard.isBlank(incoming.getInspectionStatus())) {
            incoming.setInspectionStatus("PENDING");
        }

        if (!Yard.isBlank(incoming.getBayNo())) {
            YardBay yard = yardBayService.requireAssignable(incoming.getBayNo(), chassisNo);
            incoming.setBayNo(yard.getBayCode());
            incoming.setYardSection(yard.getLocation());
        }

        incoming.setCompleted(Yard.isAssigned(incoming));

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

    public VehiclePhoto coverPhoto(String chassisNo) {
        List<VehiclePhoto> photos = vehiclePhotoService.list(chassisNo);
        return photos.isEmpty() ? null : photos.get(0);
    }
}
