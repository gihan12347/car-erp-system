package com.carsale.erp.importpipeline.service;

import com.carsale.erp.customspipeline.service.CustomsDocumentService;
import com.carsale.erp.shared.pipeline.PipelineProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.vehicle.VehicleRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ImportProgressService {

    private static final List<String> IMPORT_STAGE_KEYS = Arrays.asList(
            FlowStage.AUCTION.getStageKey(),
            FlowStage.PRESHIP.getStageKey(),
            FlowStage.EQUIPMENT.getStageKey(),
            FlowStage.JEVIC.getStageKey(),
            FlowStage.COI.getStageKey(),
            FlowStage.STANDARDS.getStageKey(),
            FlowStage.EXPORT.getStageKey(),
            FlowStage.GRADE.getStageKey(),
            FlowStage.PHOTOS.getStageKey(),
            FlowStage.WORKSHEET.getStageKey()
    );

    private final VehicleRepository vehicleRepository;
    private final PreShipmentService preShipmentService;
    private final EquipmentInspectionService equipmentInspectionService;
    private final CustomsDocumentService customsDocumentService;
    private final CertificateOfInspectionService certificateOfInspectionService;
    private final StandardsCertificateService standardsCertificateService;
    private final ExportCertificateService exportCertificateService;
    private final GradeSearchService gradeSearchService;
    private final VehiclePhotoService vehiclePhotoService;

    public ImportProgressService(
            VehicleRepository vehicleRepository,
            PreShipmentService preShipmentService,
            EquipmentInspectionService equipmentInspectionService,
            CustomsDocumentService customsDocumentService,
            CertificateOfInspectionService certificateOfInspectionService,
            StandardsCertificateService standardsCertificateService,
            ExportCertificateService exportCertificateService,
            GradeSearchService gradeSearchService,
            VehiclePhotoService vehiclePhotoService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.preShipmentService = preShipmentService;
        this.equipmentInspectionService = equipmentInspectionService;
        this.customsDocumentService = customsDocumentService;
        this.certificateOfInspectionService = certificateOfInspectionService;
        this.standardsCertificateService = standardsCertificateService;
        this.exportCertificateService = exportCertificateService;
        this.gradeSearchService = gradeSearchService;
        this.vehiclePhotoService = vehiclePhotoService;
    }

    public ImportProgress progressFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new ImportProgress(false, false, false, false, false, false, false, false, false, false);
        }
        String chassisNo = vehicle.getChassisNo();
        return new ImportProgress(
                hasAuctionDocument(vehicle),
                preShipmentService.hasPreShipment(chassisNo),
                equipmentInspectionService.hasEquipmentInspection(chassisNo),
                customsDocumentService.hasOdometerCertificate(chassisNo),
                certificateOfInspectionService.hasCertificate(chassisNo),
                standardsCertificateService.hasCertificate(chassisNo),
                exportCertificateService.hasCertificate(chassisNo),
                vehiclePhotoService.hasPhotos(chassisNo),
                gradeSearchService.hasDocument(chassisNo),
                customsDocumentService.hasWorksheet(chassisNo),
                parseSkippedStages(vehicle.getSkippedStages()));
    }

    @Transactional
    public void skipStage(String chassisNo, String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage == null || !IMPORT_STAGE_KEYS.contains(stage.getStageKey())) {
            throw new IllegalArgumentException("That stage cannot be skipped.");
        }
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        if (progressFor(vehicle).isStageFilled(stage.getStageKey())) {
            return;
        }
        Set<String> skipped = parseSkippedStages(vehicle.getSkippedStages());
        skipped.add(stage.getStageKey());
        vehicle.setSkippedStages(joinSkippedStages(skipped));
        vehicleRepository.save(vehicle);
        syncVehicleStage(vehicle.getChassisNo());
    }

    public boolean isStageSatisfied(String chassisNo, String stageKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty() || stageKey == null) {
            return false;
        }
        return progressFor(chassisNo).isStageComplete(stageKey);
    }

    public String skippedToken(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return "|";
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (vehicle == null) {
            return "|";
        }
        StringBuilder token = new StringBuilder("|");
        for (String key : parseSkippedStages(vehicle.getSkippedStages())) {
            token.append(key).append('|');
        }
        return token.toString();
    }

    static Set<String> parseSkippedStages(String stored) {
        Set<String> keys = new LinkedHashSet<String>();
        if (stored == null || stored.trim().isEmpty()) {
            return keys;
        }
        String[] parts = stored.split(",");
        for (String part : parts) {
            FlowStage stage = FlowStage.fromKey(part);
            if (stage != null && IMPORT_STAGE_KEYS.contains(stage.getStageKey())) {
                keys.add(stage.getStageKey());
            }
        }
        return keys;
    }

    private static String joinSkippedStages(Set<String> keys) {
        StringBuilder stored = new StringBuilder();
        for (String key : keys) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    public ImportProgress progressFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new ImportProgress(false, false, false, false, false, false, false, false, false, false);
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return progressFor(vehicle);
    }

    @Transactional
    public void syncVehicleStage(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return;
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (vehicle == null) {
            return;
        }
        ImportProgress status = progressFor(vehicle);
        if (!status.isPipelineCompleted()) {
            return;
        }
        VehicleStage current = vehicle.getStage();
        if (current == null
                || current == VehicleStage.PURCHASED
                || current == VehicleStage.SHIPPING) {
            vehicle.setStage(VehicleStage.CUSTOMS);
            vehicleRepository.save(vehicle);
        }
    }

    public boolean hasAuctionDocument(Vehicle vehicle) {
        return vehicle != null
                && vehicle.getSheetStoredName() != null
                && !vehicle.getSheetStoredName().trim().isEmpty();
    }

    public static final class ImportProgress implements PipelineProgress {
        private final boolean auctionReady;
        private final boolean preshipReady;
        private final boolean equipmentReady;
        private final boolean jevicReady;
        private final boolean coiReady;
        private final boolean standardsReady;
        private final boolean exportReady;
        private final boolean photosReady;
        private final boolean gradeReady;
        private final boolean worksheetReady;
        private final Set<String> skippedStages;

        public ImportProgress(
                boolean auctionReady,
                boolean preshipReady,
                boolean equipmentReady,
                boolean jevicReady,
                boolean coiReady,
                boolean standardsReady,
                boolean exportReady,
                boolean photosReady,
                boolean gradeReady,
                boolean worksheetReady
        ) {
            this(
                    auctionReady,
                    preshipReady,
                    equipmentReady,
                    jevicReady,
                    coiReady,
                    standardsReady,
                    exportReady,
                    photosReady,
                    gradeReady,
                    worksheetReady,
                    Collections.<String>emptySet()
            );
        }

        public ImportProgress(
                boolean auctionReady,
                boolean preshipReady,
                boolean equipmentReady,
                boolean jevicReady,
                boolean coiReady,
                boolean standardsReady,
                boolean exportReady,
                boolean photosReady,
                boolean gradeReady,
                boolean worksheetReady,
                Set<String> skippedStages
        ) {
            this.auctionReady = auctionReady;
            this.preshipReady = preshipReady;
            this.equipmentReady = equipmentReady;
            this.jevicReady = jevicReady;
            this.coiReady = coiReady;
            this.standardsReady = standardsReady;
            this.exportReady = exportReady;
            this.photosReady = photosReady;
            this.gradeReady = gradeReady;
            this.worksheetReady = worksheetReady;
            this.skippedStages = skippedStages == null
                    ? Collections.<String>emptySet()
                    : skippedStages;
        }

        public boolean isAuctionReady() {
            return auctionReady;
        }

        public boolean isPreshipReady() {
            return preshipReady;
        }

        public boolean isEquipmentReady() {
            return equipmentReady;
        }

        public boolean isOdometerReady() {
            return jevicReady;
        }

        public boolean isCoiReady() {
            return coiReady;
        }

        public boolean isStandardsReady() {
            return standardsReady;
        }

        public boolean isExportReady() {
            return exportReady;
        }

        public boolean isPhotosReady() {
            return photosReady;
        }

        public boolean isGradeReady() {
            return gradeReady;
        }

        public boolean isWorksheetReady() {
            return worksheetReady;
        }

        @Override
        public boolean hasAnyCompletedStage() {
            for (String key : IMPORT_STAGE_KEYS) {
                if (isStageComplete(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int completedCount() {
            int count = 0;
            for (String key : IMPORT_STAGE_KEYS) {
                if (isStageComplete(key)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean isPipelineCompleted() {
            for (String key : IMPORT_STAGE_KEYS) {
                if (!isStageComplete(key)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isImportComplete() {
            return isPipelineCompleted();
        }

        public boolean isStageFilled(String stageKey) {
            if (FlowStage.PRESHIP.getStageKey().equals(stageKey)) {
                return preshipReady;
            }
            if (FlowStage.EQUIPMENT.getStageKey().equals(stageKey)) {
                return equipmentReady;
            }
            if (FlowStage.JEVIC.getStageKey().equals(stageKey)) {
                return jevicReady;
            }
            if (FlowStage.COI.getStageKey().equals(stageKey)) {
                return coiReady;
            }
            if (FlowStage.STANDARDS.getStageKey().equals(stageKey)) {
                return standardsReady;
            }
            if (FlowStage.EXPORT.getStageKey().equals(stageKey)) {
                return exportReady;
            }
            if (FlowStage.GRADE.getStageKey().equals(stageKey)) {
                return gradeReady;
            }
            if (FlowStage.PHOTOS.getStageKey().equals(stageKey)) {
                return photosReady;
            }
            if (FlowStage.WORKSHEET.getStageKey().equals(stageKey)) {
                return worksheetReady;
            }
            return auctionReady;
        }

        public boolean isStageSkipped(String stageKey) {
            return stageKey != null && skippedStages.contains(stageKey) && !isStageFilled(stageKey);
        }

        @Override
        public boolean isStageComplete(String stageKey) {
            return isStageFilled(stageKey) || (stageKey != null && skippedStages.contains(stageKey));
        }

        public String firstIncompleteStageKey(List<String> keys) {
            for (String key : keys) {
                if (!isStageComplete(key)) {
                    return key;
                }
            }
            return null;
        }
    }
}
