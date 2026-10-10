package com.carsale.erp.shared.pipeline;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.preparationpipeline.service.RegisteredProgressService;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class StageSkipSupport {

    private static final List<String> CUSTOMS_KEYS = Arrays.asList(
            FlowStage.BILL_OF_LADING.getStageKey(),
            FlowStage.DECLARATION.getStageKey(),
            FlowStage.ASSESSMENT.getStageKey()
    );
    private static final List<String> PREP_KEYS = Arrays.asList(
            FlowStage.INSPECTION.getStageKey(),
            FlowStage.WORKSHOP.getStageKey(),
            FlowStage.YARD.getStageKey(),
            FlowStage.SALE.getStageKey()
    );
    private static final List<String> READY_KEYS = Arrays.asList(
            FlowStage.DETAILS.getStageKey(),
            FlowStage.LISTING.getStageKey(),
            FlowStage.REGISTRATION.getStageKey()
    );
    private static final List<String> IMPORT_KEYS = Arrays.asList(
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
    private static final List<String> REGISTERED_KEYS = Arrays.asList(
            SkippedStageService.REG_BOOK,
            SkippedStageService.REG_PHOTOS,
            SkippedStageService.REG_OWNER
    );

    private final VehicleRepository vehicleRepository;
    private final CustomsProgressService customsProgressService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingService saleListingService;
    private final RegisteredProgressService registeredProgressService;
    private final ImportProgressService importProgressService;

    public StageSkipSupport(
            VehicleRepository vehicleRepository,
            CustomsProgressService customsProgressService,
            PreparationProgressService preparationProgressService,
            SaleListingService saleListingService,
            RegisteredProgressService registeredProgressService,
            ImportProgressService importProgressService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.customsProgressService = customsProgressService;
        this.preparationProgressService = preparationProgressService;
        this.saleListingService = saleListingService;
        this.registeredProgressService = registeredProgressService;
        this.importProgressService = importProgressService;
    }

    public boolean canSkip(String chassisNo, String stageKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty() || stageKey == null || stageKey.trim().isEmpty()) {
            return false;
        }
        String chassis = chassisNo.trim();
        String key = stageKey.trim();
        Vehicle vehicle = vehicleRepository.findById(chassis).orElse(null);
        if (vehicle == null) {
            return false;
        }
        if (REGISTERED_KEYS.contains(key)) {
            return vehicle.getIntake() == VehicleIntake.LOCAL_REGISTERED
                    && !registeredProgressService.isStageSatisfied(chassis, key);
        }
        if (CUSTOMS_KEYS.contains(key)) {
            return !customsProgressService.isStageSatisfied(chassis, key);
        }
        if (PREP_KEYS.contains(key)) {
            return !preparationProgressService.isStageSatisfied(chassis, key);
        }
        if (READY_KEYS.contains(key)) {
            return !saleListingService.isStageSatisfied(chassis, key);
        }
        if (IMPORT_KEYS.contains(key)) {
            return !importProgressService.isStageSatisfied(chassis, key);
        }
        return false;
    }
}
