package com.carsale.erp.preparationpipeline.service;

import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import com.carsale.erp.preparationpipeline.repository.RegistrationCertificateRepository;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationCertificateService {

    private final VehicleRepository vehicleRepository;
    private final RegistrationCertificateRepository certificateRepository;
    private final SheetDocumentStorageService documentStorageService;

    public RegistrationCertificate findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, certificateRepository, null);
    }

    public RegistrationCertificate prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        RegistrationCertificate record = certificateRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new RegistrationCertificate();
            record.setChassisNo(chassisNo);
            record.setMake(vehicle.getMake());
            record.setModel(vehicle.getModel());
            record.setYearOfManufacture(vehicle.getYear());
            record.setColour(vehicle.getColor());
            record.setMileage(vehicle.getMileage());
        }
        return record;
    }

    public void applyToVehicle(RegistrationCertificate certificate, Vehicle vehicle) {
        String chassisNo = certificate.getChassisNo() == null ? "" : certificate.getChassisNo().trim();
        vehicle.setChassisNo(chassisNo);
        setIfPresent(vehicle::setMake, certificate.getMake());
        setIfPresent(vehicle::setModel, certificate.getModel());
        setIfPresent(vehicle::setYear, firstPresent(certificate.getYearOfManufacture()));
        setIfPresent(vehicle::setColor, certificate.getColour());
        setIfPresent(vehicle::setMileage, certificate.getMileage());
    }

    @Transactional
    public RegistrationCertificate save(RegistrationCertificate incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        Vehicle vehicle = vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        RegistrationCertificate existing = certificateRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            keepStoredDocument(incoming, existing);
            if (incoming.getDocumentStoredName() != null
                    && existing.getDocumentStoredName() != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deleteRegistrationCertificateIfExists(existing.getDocumentStoredName());
            }
            copyFields(incoming, existing);
        } else {
            existing = incoming;
            existing.setChassisNo(chassisNo);
        }
        RegistrationCertificate saved = certificateRepository.save(existing);
        applyToVehicle(saved, vehicle);
        vehicleRepository.save(vehicle);
        return saved;
    }

    private void keepStoredDocument(RegistrationCertificate incoming, RegistrationCertificate existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void copyFields(RegistrationCertificate source, RegistrationCertificate target) {
        target.setAbsoluteOwner(source.getAbsoluteOwner());
        target.setClassOfVehicle(source.getClassOfVehicle());
        target.setColour(source.getColour());
        target.setConditionsSpecialNotes(source.getConditionsSpecialNotes());
        target.setCountryOfOrigin(source.getCountryOfOrigin());
        target.setCrNo(source.getCrNo());
        target.setCurrentOwnerAddressIdNo(source.getCurrentOwnerAddressIdNo());
        target.setCylinderCapacity(source.getCylinderCapacity());
        target.setDateOfFirstRegistration(source.getDateOfFirstRegistration());
        target.setEngineNo(source.getEngineNo());
        target.setFuelType(source.getFuelType());
        target.setHeight(source.getHeight());
        target.setLength(source.getLength());
        target.setMake(source.getMake());
        target.setManufacturesDescription(source.getManufacturesDescription());
        target.setModel(source.getModel());
        target.setOverHang(source.getOverHang());
        target.setPreviousOwners(source.getPreviousOwners());
        target.setPrintedDate(source.getPrintedDate());
        target.setProvincialCouncil(source.getProvincialCouncil());
        target.setRegistrationNo(source.getRegistrationNo());
        target.setSeatingCapacity(source.getSeatingCapacity());
        target.setStatusWhenRegistered(source.getStatusWhenRegistered());
        target.setTaxationClass(source.getTaxationClass());
        target.setTaxesPayable(source.getTaxesPayable());
        target.setTypeOfBody(source.getTypeOfBody());
        target.setTyreSizeDual(source.getTyreSizeDual());
        target.setTyreSizeFront(source.getTyreSizeFront());
        target.setTyreSizeRear(source.getTyreSizeRear());
        target.setTyreSizeSingle(source.getTyreSizeSingle());
        target.setWeightGross(source.getWeightGross());
        target.setWeightUnladen(source.getWeightUnladen());
        target.setWheelBase(source.getWheelBase());
        target.setWidth(source.getWidth());
        target.setYearOfManufacture(source.getYearOfManufacture());
        target.setMileage(source.getMileage());
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }

    private static void setIfPresent(java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }

    private static String firstPresent(String value) {
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return null;
    }
}
