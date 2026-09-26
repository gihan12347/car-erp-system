package com.carsale.erp.readypipeline.service;

import com.carsale.erp.readypipeline.model.VehicleRegistration;
import com.carsale.erp.readypipeline.repository.VehicleRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class VehicleRegistrationService {

    private final VehicleRepository vehicleRepository;
    private final VehicleRegistrationRepository vehicleRegistrationRepository;

    public VehicleRegistrationService(
            VehicleRepository vehicleRepository,
            VehicleRegistrationRepository vehicleRegistrationRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleRegistrationRepository = vehicleRegistrationRepository;
    }

    public VehicleRegistration findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return vehicleRegistrationRepository.findById(chassisNo.trim()).orElse(null);
    }

    public VehicleRegistration prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        VehicleRegistration record = vehicleRegistrationRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new VehicleRegistration();
            record.setChassisNo(chassisNo);
        }
        return record;
    }

    public boolean isComplete(String chassisNo) {
        return isComplete(findByChassisNo(chassisNo));
    }

    @Transactional
    public VehicleRegistration save(VehicleRegistration incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        incoming.setChassisNo(chassisNo);
        incoming.setCompleted(hasEssentialDetails(incoming));

        VehicleRegistration existing = vehicleRegistrationRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            copyFields(incoming, existing);
            return vehicleRegistrationRepository.save(existing);
        }
        return vehicleRegistrationRepository.save(incoming);
    }

    static boolean isComplete(VehicleRegistration record) {
        if (record == null) {
            return false;
        }
        if (record.isCompleted()) {
            return true;
        }
        return hasEssentialDetails(record);
    }

    private static boolean hasEssentialDetails(VehicleRegistration record) {
        return isBlank(record.getRegistrationNo()) && isBlank(record.getRegisteredDate());
    }

    private static void copyFields(VehicleRegistration source, VehicleRegistration target) {
        target.setChassisNo(source.getChassisNo());
        target.setRegistrationNo(source.getRegistrationNo());
        target.setRegisteredOwner(source.getRegisteredOwner());
        target.setRegisteredDate(source.getRegisteredDate());
        target.setRmvOffice(source.getRmvOffice());
        target.setFileNo(source.getFileNo());
        target.setRevenueLicenseNo(source.getRevenueLicenseNo());
        target.setRevenueLicenseExpiry(source.getRevenueLicenseExpiry());
        target.setInsuranceCompany(source.getInsuranceCompany());
        target.setInsurancePolicyNo(source.getInsurancePolicyNo());
        target.setInsuranceExpiry(source.getInsuranceExpiry());
        target.setNotes(source.getNotes());
        target.setCompleted(source.isCompleted());
    }

    private static boolean isBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
