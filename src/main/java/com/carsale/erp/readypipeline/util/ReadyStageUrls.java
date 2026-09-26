package com.carsale.erp.readypipeline.util;

import java.util.List;

import com.carsale.erp.readypipeline.service.SaleListingService.SaleProgress;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

public final class ReadyStageUrls {

    public static final String TAB_UNREGISTERED = "unregistered";
    public static final String TAB_REGISTERED = "registered";
    public static final String TAB_SOLD = "sold";
    public static final String PERIOD_ALL = "all";

    public static String redirectAfterListingSave(String saleCode, String chassisNo, boolean sold) {
        if (sold) {
            return saleVehicles(saleCode, TAB_SOLD);
        }
        return saleVehicle(saleCode, chassisNo);
    }

    public static String redirectAfterRegistrationSave(String saleCode, String chassisNo, boolean complete) {
        if (complete) {
            return saleVehicles(saleCode, TAB_REGISTERED);
        }
        return saleVehicle(saleCode, chassisNo);
    }

    public static String saleLocation(String saleCode) {
        if (saleCode == null || saleCode.trim().isEmpty()) {
            return "/ready-for-sale";
        }
        return "/ready-for-sale/" + PipelineStageUtils.encode(saleCode.trim());
    }

    public static String saleVehicles(String saleCode, String tab) {
        return saleLocation(saleCode) + "?tab=" + normalizeTab(tab);
    }

    public static String saleVehicle(String saleCode, String chassisNo) {
        if (saleCode == null || saleCode.trim().isEmpty()) {
            return "/ready-for-sale/" + PipelineStageUtils.encode(chassisNo);
        }
        return saleLocation(saleCode) + "/" + PipelineStageUtils.encode(chassisNo);
    }

    public static NavLinks editLinks(String chassisNo, int stageIndex, SaleProgress status, List<PipelineStage> keys) {
        NavLinks view = viewLinks(chassisNo, stageIndex, status, keys, FlowPipeline.READY);
        String encoded = PipelineStageUtils.encode(chassisNo);
        String prevUrl = null;
        if (stageIndex > 0) {
            String prevKey = PipelineStageService.stageKeyAt(keys, stageIndex - 1);
            if (FlowStage.DETAILS.getStageKey().equals(prevKey) || status.isStageComplete(prevKey)) {
                prevUrl = PipelineStageUtils.viewUrl(FlowPipeline.READY.getCurrentBase() + encoded, prevKey);
            } else {
                prevUrl = PipelineStageService.editUrlFor(encoded, prevKey);
            }
        }
        return new NavLinks(view.getStageIndex(), view.getStageLabel(), prevUrl,
                view.getNextUrl(), view.getNextLabel(), null);
    }

    public static String normalizeTab(String tab) {
        if (TAB_REGISTERED.equalsIgnoreCase(tab)) {
            return TAB_REGISTERED;
        }
        if (TAB_SOLD.equalsIgnoreCase(tab)) {
            return TAB_SOLD;
        }
        return TAB_UNREGISTERED;
    }
}
