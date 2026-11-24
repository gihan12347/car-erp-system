package com.carsale.erp.shared.vehicle;

import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsale.erp.customspipeline.repository.CustomsDocumentRepository;
import com.carsale.erp.importpipeline.repository.EquipmentInspectionRepository;
import com.carsale.erp.importpipeline.repository.InspectionCertificateRepository;
import com.carsale.erp.importpipeline.repository.ExportCertificateRepository;
import com.carsale.erp.importpipeline.repository.GradeSearchRepository;
import com.carsale.erp.importpipeline.repository.StandardsCertificateRepository;
import com.carsale.erp.importpipeline.repository.PreShipmentInspectionRepository;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.readypipeline.repository.VehicleRegistrationRepository;
import com.carsale.erp.preparationpipeline.repository.PreviousOwnerRepository;
import com.carsale.erp.preparationpipeline.repository.RegistrationCertificateRepository;
import com.carsale.erp.preparationpipeline.repository.VehicleInspectionRepository;
import com.carsale.erp.preparationpipeline.repository.WorkshopJobRepository;
import com.carsale.erp.preparationpipeline.repository.YardRecordRepository;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final SheetDocumentStorageService documentStorageService;
    private final PreShipmentInspectionRepository preShipmentInspectionRepository;
    private final CustomsDocumentRepository clearanceDocumentRepository;
    private final EquipmentInspectionRepository equipmentInspectionRepository;
    private final InspectionCertificateRepository inspectionCertificateRepository;
    private final StandardsCertificateRepository standardsCertificateRepository;
    private final ExportCertificateRepository exportCertificateRepository;
    private final GradeSearchRepository gradeSearchRepository;
    private final VehiclePhotoService vehiclePhotoService;
    private final WorkshopJobRepository workshopJobRepository;
    private final YardRecordRepository yardRecordRepository;
    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final RegistrationCertificateRepository registrationCertificateRepository;
    private final PreviousOwnerRepository previousOwnerRepository;
    private final SaleListingRepository saleListingRepository;
    private final VehicleRegistrationRepository vehicleRegistrationRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            SheetDocumentStorageService documentStorageService,
            PreShipmentInspectionRepository preShipmentInspectionRepository,
            CustomsDocumentRepository clearanceDocumentRepository,
            EquipmentInspectionRepository equipmentInspectionRepository,
            InspectionCertificateRepository inspectionCertificateRepository,
            StandardsCertificateRepository standardsCertificateRepository,
            ExportCertificateRepository exportCertificateRepository,
            GradeSearchRepository gradeSearchRepository,
            VehiclePhotoService vehiclePhotoService,
            WorkshopJobRepository workshopJobRepository,
            YardRecordRepository yardRecordRepository,
            VehicleInspectionRepository vehicleInspectionRepository,
            RegistrationCertificateRepository registrationCertificateRepository,
            PreviousOwnerRepository previousOwnerRepository,
            SaleListingRepository saleListingRepository,
            VehicleRegistrationRepository vehicleRegistrationRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.documentStorageService = documentStorageService;
        this.preShipmentInspectionRepository = preShipmentInspectionRepository;
        this.clearanceDocumentRepository = clearanceDocumentRepository;
        this.equipmentInspectionRepository = equipmentInspectionRepository;
        this.inspectionCertificateRepository = inspectionCertificateRepository;
        this.standardsCertificateRepository = standardsCertificateRepository;
        this.exportCertificateRepository = exportCertificateRepository;
        this.gradeSearchRepository = gradeSearchRepository;
        this.vehiclePhotoService = vehiclePhotoService;
        this.workshopJobRepository = workshopJobRepository;
        this.yardRecordRepository = yardRecordRepository;
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.registrationCertificateRepository = registrationCertificateRepository;
        this.previousOwnerRepository = previousOwnerRepository;
        this.saleListingRepository = saleListingRepository;
        this.vehicleRegistrationRepository = vehicleRegistrationRepository;
    }

    public List<Vehicle> findAll() {
        return newestFirst(vehicleRepository.findAll());
    }

    public List<Vehicle> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        return newestFirst(vehicleRepository.search(query.trim()));
    }

    static List<Vehicle> newestFirst(List<Vehicle> vehicles) {
        List<Vehicle> sorted = new ArrayList<Vehicle>(vehicles);
        Collections.sort(sorted, newestFirstComparator());
        return sorted;
    }

    public static Comparator<Vehicle> newestFirstComparator() {
        return new Comparator<Vehicle>() {
            @Override
            public int compare(Vehicle left, Vehicle right) {
                return compareNewestFirst(left, right);
            }
        };
    }

    public static int compareNewestFirst(Vehicle left, Vehicle right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int byVehicleNo = Integer.compare(
                parseVehicleNoSuffix(right.getStockNo()),
                parseVehicleNoSuffix(left.getStockNo()));
        if (byVehicleNo != 0) {
            return byVehicleNo;
        }
        String leftChassis = left.getChassisNo() == null ? "" : left.getChassisNo();
        String rightChassis = right.getChassisNo() == null ? "" : right.getChassisNo();
        return rightChassis.compareToIgnoreCase(leftChassis);
    }

    public Vehicle findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, vehicleRepository, null);
    }

    public boolean existsByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return false;
        }
        return vehicleRepository.existsByChassisNo(chassisNo.trim());
    }


    public Vehicle save(Vehicle vehicle) {
        if (vehicle.getChassisNo() == null || vehicle.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        vehicle.setChassisNo(vehicle.getChassisNo().trim());
        if (vehicle.getStage() == null) {
            vehicle.setStage(VehicleStage.PURCHASED);
        }
        VehicleFieldNormalizer.normalize(vehicle);
        Vehicle existing = vehicleRepository.findById(vehicle.getChassisNo()).orElse(null);
        boolean isNew = existing == null;
        if (!isNew) {
            preserveExistingStockNo(vehicle, existing);
            preserveExistingDocument(vehicle, existing);
            if (vehicle.getIntake() == null) {
                vehicle.setIntake(existing.getIntake());
            }
        } else {
            vehicle.setStockNo(nextAvailableVehicleNo());
        }
        return vehicleRepository.save(vehicle);
    }

    private void preserveExistingStockNo(Vehicle incoming, Vehicle existing) {
        if (incoming.getStockNo() == null || incoming.getStockNo().trim().isEmpty()) {
            incoming.setStockNo(existing.getStockNo());
        }
    }

    private void preserveExistingDocument(Vehicle incoming, Vehicle existing) {
        if (incoming.getSheetStoredName() == null || incoming.getSheetStoredName().trim().isEmpty()) {
            incoming.setSheetStoredName(existing.getSheetStoredName());
            incoming.setSheetOriginalName(existing.getSheetOriginalName());
            incoming.setSheetContentType(existing.getSheetContentType());
            return;
        }
        String oldStored = existing.getSheetStoredName();
        String newStored = incoming.getSheetStoredName().trim();
        if (oldStored != null && !oldStored.equals(newStored)) {
            documentStorageService.deleteIfExists(oldStored);
        }
    }

    private String nextAvailableVehicleNo() {
        int maxSuffix = 0;
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            maxSuffix = Math.max(maxSuffix, parseVehicleNoSuffix(vehicle.getStockNo()));
        }
        int candidate = maxSuffix + 1;
        String vehicleNo;
        do {
            vehicleNo = formatVehicleNo(candidate++);
        } while (vehicleNoTaken(vehicleNo));
        return vehicleNo;
    }

    private boolean vehicleNoTaken(String vehicleNo) {
        if (vehicleRepository.existsByStockNo(vehicleNo)) {
            return true;
        }
        int suffix = parseVehicleNoSuffix(vehicleNo);
        if (suffix <= 0) {
            return false;
        }
        return vehicleRepository.existsByStockNo("V" + suffix)
                || vehicleRepository.existsByStockNo(String.format("V%03d", suffix));
    }

    static String formatVehicleNo(int suffix) {
        return String.format("V%04d", suffix);
    }

    public static int parseVehicleNoSuffix(String vehicleNo) {
        if (vehicleNo == null) {
            return 0;
        }
        String trimmed = vehicleNo.trim();
        if (trimmed.length() < 2) {
            return 0;
        }
        char prefix = trimmed.charAt(0);
        if (prefix != 'V' && prefix != 'v') {
            return 0;
        }
        try {
            return Integer.parseInt(trimmed.substring(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @Transactional
    public boolean delete(String chassisNo) {
        return deleteFromFlow(chassisNo, PipelineStageService.FLOW_IMPORT);
    }

    @Transactional
    public boolean deleteFromFlow(String chassisNo, String flowKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return false;
        }
        String id = chassisNo.trim();
        Vehicle vehicle = findByChassisNo(id);
        if (vehicle == null) {
            return false;
        }
        if (PipelineStageService.FLOW_IMPORT.equals(flowKey)) {
            deleteReadyData(id);
            deletePrepData(id);
            deleteCustomsData(id, false);
            deleteImportData(id, vehicle);
            vehicleRepository.delete(vehicle);
            return true;
        }
        if (PipelineStageService.FLOW_CUSTOMS.equals(flowKey)) {
            deleteReadyData(id);
            deletePrepData(id);
            deleteCustomsData(id, true);
            setStage(vehicle, VehicleStage.CUSTOMS);
            return true;
        }
        if (PipelineStageService.FLOW_PREP.equals(flowKey)) {
            deleteReadyData(id);
            deletePrepData(id);
            setStage(vehicle, VehicleStage.WORKSHOP);
            return true;
        }
        if (PipelineStageService.FLOW_READY.equals(flowKey)) {
            deleteReadyData(id);
            setStage(vehicle, VehicleStage.READY);
            return true;
        }
        return false;
    }

    private void deleteReadyData(String chassisNo) {
        saleListingRepository.findById(chassisNo).ifPresent(saleListingRepository::delete);
        vehicleRegistrationRepository.findById(chassisNo).ifPresent(vehicleRegistrationRepository::delete);
    }

    private void deletePrepData(String chassisNo) {
        workshopJobRepository.findById(chassisNo).ifPresent(workshopJobRepository::delete);
        yardRecordRepository.findById(chassisNo).ifPresent(yardRecordRepository::delete);
        vehicleInspectionRepository.findById(chassisNo).ifPresent(vehicleInspectionRepository::delete);
        registrationCertificateRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteRegistrationCertificateIfExists(record.getDocumentStoredName());
            registrationCertificateRepository.delete(record);
        });
        previousOwnerRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteIdCardIfExists(record.getDocumentStoredName());
            previousOwnerRepository.delete(record);
        });
    }

    private void deleteCustomsData(String chassisNo, boolean keepOdometerCertificate) {
        clearanceDocumentRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteClearanceIfExists(record.getPage2StoredName());
            documentStorageService.deleteClearanceIfExists(record.getPage3StoredName());
            documentStorageService.deleteClearanceIfExists(record.getPage4StoredName());
            documentStorageService.deleteClearanceIfExists(record.getPage5StoredName());
            boolean hasOdometerCertificate = record.getPage1StoredName() != null && !record.getPage1StoredName().trim().isEmpty();
            if (keepOdometerCertificate && hasOdometerCertificate) {
                record.setPage2OriginalName(null);
                record.setPage2StoredName(null);
                record.setPage2ContentType(null);
                record.setPage3OriginalName(null);
                record.setPage3StoredName(null);
                record.setPage3ContentType(null);
                record.setPage4OriginalName(null);
                record.setPage4StoredName(null);
                record.setPage4ContentType(null);
                record.setPage5OriginalName(null);
                record.setPage5StoredName(null);
                record.setPage5ContentType(null);
                clearanceDocumentRepository.save(record);
                return;
            }
            documentStorageService.deleteClearanceIfExists(record.getPage1StoredName());
            clearanceDocumentRepository.delete(record);
        });
    }

    private void deleteImportData(String chassisNo, Vehicle vehicle) {
        documentStorageService.deleteIfExists(vehicle.getSheetStoredName());
        preShipmentInspectionRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deletePreShipmentIfExists(record.getDocumentStoredName());
            preShipmentInspectionRepository.delete(record);
        });
        equipmentInspectionRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteEquipmentIfExists(record.getDocumentStoredName());
            equipmentInspectionRepository.delete(record);
        });
        inspectionCertificateRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteCoiIfExists(record.getDocumentStoredName());
            inspectionCertificateRepository.delete(record);
        });
        standardsCertificateRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteStandardsIfExists(record.getDocumentStoredName());
            standardsCertificateRepository.delete(record);
        });
        exportCertificateRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteExportIfExists(record.getDocumentStoredName());
            exportCertificateRepository.delete(record);
        });
        gradeSearchRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteGradeSearchIfExists(record.getDocumentStoredName());
            gradeSearchRepository.delete(record);
        });
        vehiclePhotoService.deleteAll(chassisNo);
    }

    private void setStage(Vehicle vehicle, VehicleStage stage) {
        vehicle.setStage(stage);
        vehicleRepository.save(vehicle);
    }

}
