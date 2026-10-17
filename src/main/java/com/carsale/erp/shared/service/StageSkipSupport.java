package com.carsale.erp.shared.service;

import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.preparationpipeline.service.RegisteredProgressService;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("stageSkipSupport")
@RequiredArgsConstructor
public class StageSkipSupport {

    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PreparationProgressService preparationProgressService;
    private final RegisteredProgressService registeredProgressService;
    private final SaleListingService saleListingService;

    public boolean canSkip(String chassisNo, String stageKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty() || stageKey == null || stageKey.trim().isEmpty()) {
            return false;
        }
        String key = stageKey.trim();
        if (isRegistered(key)) {
            return !registeredProgressService.isStageSatisfied(chassisNo, key);
        }
        if (PipelineStageUtils.READY_STAGE_KEYS.contains(key)) {
            return saleListingService.isStageSatisfied(chassisNo, key);
        }
        if (isCustomsOnly(key)) {
            return !customsProgressService.isStageSatisfied(chassisNo, key);
        }
        if (PipelineStageUtils.PREP_STAGE_KEYS.contains(key)) {
            return !preparationProgressService.isStageSatisfied(chassisNo, key);
        }
        if (PipelineStageUtils.IMPORT_STAGE_KEYS.contains(key)) {
            return !importProgressService.isStageSatisfied(chassisNo, key);
        }
        return false;
    }

    private static boolean isRegistered(String key) {
        return SkippedStageService.REG_BOOK.equals(key)
                || SkippedStageService.REG_PHOTOS.equals(key)
                || SkippedStageService.REG_OWNER.equals(key);
    }

    private static boolean isCustomsOnly(String key) {
        return FlowStage.BILL_OF_LADING.getStageKey().equals(key)
                || FlowStage.DECLARATION.getStageKey().equals(key)
                || FlowStage.ASSESSMENT.getStageKey().equals(key);
    }
}
