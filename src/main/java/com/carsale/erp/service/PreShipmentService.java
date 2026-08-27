package com.carsale.erp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.PreShipmentInspection;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.repository.PreShipmentInspectionRepository;
import com.carsale.erp.repository.VehicleRepository;

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

    public List<Vehicle> listAuctionVehicles(String query) {
        if (query == null || query.trim().isEmpty()) {
            return vehicleRepository.findAllByOrderByChassisNoAsc();
        }
        return vehicleRepository.search(query.trim());
    }

    public PreShipmentInspection findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return preShipmentRepository.findById(chassisNo.trim()).orElse(null);
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

        Vehicle vehicle = vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + incoming.getChassisNo()));

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

        PreShipmentInspection saved = preShipmentRepository.save(existing);

        return saved;
    }

    private void keepStoredDocument(PreShipmentInspection incoming, PreShipmentInspection existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    public List<VehicleRow> listWithStatus(String query) {
        List<Vehicle> vehicles = listAuctionVehicles(query);
        List<VehicleRow> rows = new ArrayList<VehicleRow>();
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            rows.add(new VehicleRow(vehicle, hasPreShipment(vehicle.getChassisNo())));
        }
        return rows;
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
        if (vehicle.getYear() != null && vehicle.getYear().matches(".*\\d{4}.*")) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{4})").matcher(vehicle.getYear());
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
        target.setEngineNo(source.getEngineNo());
        target.setDrivingSystem(source.getDrivingSystem());
        target.setAccidentMarksOnChassis(source.getAccidentMarksOnChassis());
        target.setChassisCondition(source.getChassisCondition());
        target.setFullModelNo(source.getFullModelNo());
        target.setYearOfManufacture(source.getYearOfManufacture());
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }

    public static class VehicleRow {
        private final Vehicle vehicle;
        private final boolean preShipmentReady;

        public VehicleRow(Vehicle vehicle, boolean preShipmentReady) {
            this.vehicle = vehicle;
            this.preShipmentReady = preShipmentReady;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public boolean isPreShipmentReady() {
            return preShipmentReady;
        }
    }
}
