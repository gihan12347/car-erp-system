package com.carsale.erp.readypipeline;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class SaleListingService {

    private final VehicleRepository vehicleRepository;
    private final SaleListingRepository saleListingRepository;

    public SaleListingService(VehicleRepository vehicleRepository, SaleListingRepository saleListingRepository) {
        this.vehicleRepository = vehicleRepository;
        this.saleListingRepository = saleListingRepository;
    }

    public SaleListing findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return saleListingRepository.findById(chassisNo.trim()).orElse(null);
    }

    public SaleListing prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        SaleListing record = saleListingRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new SaleListing();
            record.setChassisNo(chassisNo);
        }
        return record;
    }

    public boolean isListed(String chassisNo) {
        SaleListing record = findByChassisNo(chassisNo);
        return record != null && record.isListed();
    }

    @Transactional
    public SaleListing save(SaleListing incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        SaleListing existing = saleListingRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            BeanUtils.copyProperties(incoming, existing);
            existing.setChassisNo(chassisNo);
            return saleListingRepository.save(existing);
        }
        incoming.setChassisNo(chassisNo);
        return saleListingRepository.save(incoming);
    }
}
