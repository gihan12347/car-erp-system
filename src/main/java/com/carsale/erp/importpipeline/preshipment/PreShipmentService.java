package com.carsale.erp.importpipeline.preshipment;

import com.carsale.erp.shared.document.document.PreShipmentParser;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.regex.RegexConstants;

@Service
public class PreShipmentService {

    private final VehicleRepository vehicleRepository;
    private final PreShipmentInspectionRepository preShipmentRepository;
    private final SheetDocumentStorageService documentStorageService;

    public PreShipmentService(
            VehicleRepository vehicleRepository,
            PreShipmentInspectionRepository preShipmentRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.preShipmentRepository = preShipmentRepository;
        this.documentStorageService = documentStorageService;
    }

    public PreShipmentInspection findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        PreShipmentInspection record = preShipmentRepository.findById(chassisNo.trim()).orElse(null);
        normalizeDateFields(record);
        return record;
    }

    public PreShipmentInspection prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }

        PreShipmentInspection record = preShipmentRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new PreShipmentInspection();
            record.setChassisNo(chassisNo);
            prefillFromVehicle(record, vehicle);
        }
        normalizeDateFields(record);
        return record;
    }

    public boolean hasPreShipment(String chassisNo) {
        PreShipmentInspection record = findByChassisNo(chassisNo);
        return record != null
                && record.getDocumentStoredName() != null
                && !record.getDocumentStoredName().trim().isEmpty();
    }

    @Transactional
    public PreShipmentInspection save(PreShipmentInspection incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        normalizeDateFields(incoming);
        PreShipmentInspection existing = preShipmentRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing != null) {
            keepStoredDocument(incoming, existing);
            if (incoming.getDocumentStoredName() != null
                    && existing.getDocumentStoredName() != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deletePreShipmentIfExists(existing.getDocumentStoredName());
            }
            copyFields(incoming, existing);
        } else {
            existing = incoming;
        }
        return preShipmentRepository.save(existing);
    }

    private void keepStoredDocument(PreShipmentInspection incoming, PreShipmentInspection existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void prefillFromVehicle(PreShipmentInspection record, Vehicle vehicle) {
        record.setChassisNo(vehicle.getChassisNo());
        record.setMake(vehicle.getMake());
        record.setModel(vehicle.getModel());
        record.setVehicleType(vehicle.getBodyStyle());
        record.setBodyColour(vehicle.getColor());
        record.setFuelType(vehicle.getFuel());
        record.setFirstRegistration(vehicle.getYear());
        record.setInspectionMileage(vehicle.getMileage());
        record.setEngineCapacity(vehicle.getEngineSize());
        record.setDrivingSystem(vehicle.getDriveSystem());
        record.setPreshipAuctionGrade(vehicle.getAuctionGrade());
        record.setFullModelNo(vehicle.getModelCode());
        if (vehicle.getYear() != null && vehicle.getYear().matches(RegexConstants.Dates.CONTAINS_YEAR)) {
            java.util.regex.Matcher matcher = RegexConstants.Dates.YEAR_CAPTURE_PATTERN.matcher(vehicle.getYear());
            if (matcher.find()) {
                record.setYearOfManufacture(matcher.group(1));
            }
        }
    }

    private void copyFields(PreShipmentInspection source, PreShipmentInspection target) {
        target.setCertificateReference(source.getCertificateReference());
        target.setDocumentTitle(source.getDocumentTitle());
        target.setBvNumber(source.getBvNumber());
        target.setCertificateDate(source.getCertificateDate());
        target.setPageInfo(source.getPageInfo());
        target.setDocumentControlNumber(source.getDocumentControlNumber());
        target.setInspectionOrgName(source.getInspectionOrgName());
        target.setInspectionOrgAddress(source.getInspectionOrgAddress());
        target.setInspectionOrgTel(source.getInspectionOrgTel());
        target.setInspectionOrgFax(source.getInspectionOrgFax());
        target.setInspectionOrgEmail(source.getInspectionOrgEmail());
        target.setPlaceOfInspection(source.getPlaceOfInspection());
        target.setDateOfInspection(source.getDateOfInspection());
        target.setApplicantName(source.getApplicantName());
        target.setApplicantAddress(source.getApplicantAddress());
        target.setApplicantTel(source.getApplicantTel());
        target.setApplicantFax(source.getApplicantFax());
        target.setApplicantEmail(source.getApplicantEmail());
        target.setVehicleType(source.getVehicleType());
        target.setMake(source.getMake());
        target.setModel(source.getModel());
        target.setCommonName(source.getCommonName());
        target.setManufactureGrade(source.getManufactureGrade());
        target.setPreshipAuctionGrade(source.getPreshipAuctionGrade());
        target.setBodyColour(source.getBodyColour());
        target.setFuelType(source.getFuelType());
        target.setFirstRegistration(source.getFirstRegistration());
        target.setInspectionMileage(source.getInspectionMileage());
        target.setEngineCapacity(source.getEngineCapacity());
        target.setEngineModel(source.getEngineModel());
        target.setEngineNo(source.getEngineNo());
        target.setDrivingSystem(source.getDrivingSystem());
        target.setAccidentMarksOnChassis(source.getAccidentMarksOnChassis());
        target.setChassisCondition(source.getChassisCondition());
        target.setFullModelNo(source.getFullModelNo());
        target.setYearOfManufacture(source.getYearOfManufacture());
        target.setTyreSize(source.getTyreSize());
        target.setWheelBase(source.getWheelBase());
        target.setGrossVehicleMass(source.getGrossVehicleMass());
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }

    private void normalizeDateFields(PreShipmentInspection record) {
        if (record == null) {
            return;
        }
        record.setFirstRegistration(PreShipmentParser.normalizeYearMonth(record.getFirstRegistration()));
    }
}
