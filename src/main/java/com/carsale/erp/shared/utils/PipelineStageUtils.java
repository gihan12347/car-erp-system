package com.carsale.erp.shared.utils;

import com.carsale.erp.shared.dto.CustomsProgress;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.repository.PipelineProgress;
import com.carsale.erp.shared.model.PipelineStage;
import org.springframework.ui.Model;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class PipelineStageUtils {

    private static Function<String, String> selectedPipelinesLookup = chassis -> null;

    public static void setSelectedPipelinesLookup(Function<String, String> lookup) {
        selectedPipelinesLookup = lookup == null ? chassis -> null : lookup;
    }

    public static String selectedPipelinesOf(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return selectedPipelinesLookup.apply(chassisNo.trim());
    }

    /**
     * Next pipeline after the current one. A null selection keeps the original order.
     * An explicit selection skips pipelines that were left out.
     */
    public static FlowPipeline nextSelectedPipeline(FlowPipeline current, String selectedPipelines) {
        if (current == null || current == FlowPipeline.PREP) {
            return null;
        }
        boolean legacy = selectedPipelines == null;
        boolean customs = legacy || containsToken(selectedPipelines, "customs");
        boolean prep = legacy || containsToken(selectedPipelines, "prep");
        if (current == FlowPipeline.IMPORT) {
            if (customs) {
                return FlowPipeline.CUSTOMS;
            }
            if (prep) {
                return FlowPipeline.PREP;
            }
            return null;
        }
        if (current == FlowPipeline.CUSTOMS) {
            if (prep) {
                return FlowPipeline.PREP;
            }
            return null;
        }
        return FlowPipeline.findPipelineBySortOrder(current.getSortOrder() + 1);
    }

    public static boolean containsToken(String selected, String token) {
        if (selected == null || token == null || "-".equals(selected)) {
            return false;
        }
        for (String part : selected.split(",")) {
            if (token.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

     public static final List<String> IMPORT_STAGE_KEYS = Arrays.asList(
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

    public static final List<String> CUSTOMS_STAGE_KEYS = Arrays.asList(
            FlowStage.BILL_OF_LADING.getStageKey(),
            FlowStage.DECLARATION.getStageKey(),
            FlowStage.ASSESSMENT.getStageKey(),
            FlowStage.WORKSHEET.getStageKey()
    );

    public static final List<String> PREP_STAGE_KEYS = Arrays.asList(
            FlowStage.INSPECTION.getStageKey(),
            FlowStage.WORKSHOP.getStageKey(),
            FlowStage.YARD.getStageKey()
    );

    public static final List<String> READY_STAGE_KEYS = Arrays.asList(
            FlowStage.DETAILS.getStageKey(),
            FlowStage.LISTING.getStageKey(),
            FlowStage.REGISTRATION.getStageKey()
    );

    public static String redirectAfterStageSave(
            String chassisNo,
            String basePath,
            List<String> keys,
            String currentKey
    ) {
        String encoded = encode(chassisNo);
        return redirectToNext(basePath, keys, currentKey, encoded);
    }

    public static String redirectToNext(
            String basePath,
            List<String> keys,
            String currentKey,
            String encoded
    ) {
        int index = keys.indexOf(currentKey);
        if (index < keys.size() - 1) {
            String nextKey = keys.get(index + 1);
            return viewUrl(viewBase(basePath,encoded), nextKey);
        } else if (index == keys.size() - 1) {
            FlowPipeline currentFlowPipeline = FlowPipeline.findPipelineByBasePath(basePath);
            if (currentFlowPipeline == null) {
                return viewBase(basePath, encoded);
            }
            if (currentFlowPipeline == FlowPipeline.PREP) {
                return "/yards";
            }
            String rawChassis = UriUtils.decode(encoded, StandardCharsets.UTF_8);
            FlowPipeline nextFlowPipeline = nextSelectedPipeline(
                    currentFlowPipeline, selectedPipelinesOf(rawChassis));
            if (nextFlowPipeline == null) {
                return viewBase(basePath, encoded);
            }
            return viewBase(nextFlowPipeline.getCurrentBase(), encoded);
        }
        return viewBase(basePath, encoded);
    }

    public static String viewBase(String basePath, String encoded) {
        return basePath + encoded;
    }

    public static String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    public static String viewUrl(String viewBase, String stageKey) {
        if (stageKey == null || stageKey.trim().isEmpty()) {
            return viewBase;
        }
        return viewBase + "?stage=" + stageKey;
    }

    public static int clampStageIndex(Integer requested, PipelineProgress status, List<String> keys) {
        if (keys == null || keys.isEmpty() || (requested != null && requested < 0)) {
            return 0;
        }
        int max = Math.max(0, keys.size() - 1);
        if (requested == null) {
            return resolveStartStageIndex(status, keys);
        }
        return requested > max ? max : requested;
    }

    public static int resolveStartStageIndex(PipelineProgress status, List<String> keys) {
        if (status.isPipelineCompleted() || keys == null || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (isIncomplete(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
    }

    public static boolean isIncomplete(String stageKey, PipelineProgress status) {
        return !status.isStageComplete(stageKey);
    }

    public static String stageKeyAt(List<PipelineStage> stages, int index) {
        if (stages == null || stages.isEmpty()) {
            return null;
        } else if (index < 0) {
            return stages.get(0).getStageKey();
        } else if (index >= stages.size()) {
            return stages.get(stages.size() - 1).getStageKey();
        } else {
            return stages.get(index).getStageKey();
        }
    }

    public static Integer parseStageIndex(List<String> keys, String requested) {
        if (requested == null || requested.trim().isEmpty()) {
            return null;
        }
        String value = requested.trim();
        if (keys != null) {
            for (int i = 0; i < keys.size(); i++) {
                if (value.equalsIgnoreCase(keys.get(i))) {
                    return i;
                }
            }
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static void addPipelineFlags(Model model, CustomsProgress status) {
        model.addAttribute("blReady", status.isBlReady());
        model.addAttribute("declarationReady", status.isDeclarationReady());
        model.addAttribute("assessmentReady", status.isAssessmentReady());
        model.addAttribute("worksheetReady", status.isWorksheetReady());
    }

    public static void addPipelineFlags(Model model, ImportProgress status) {
        model.addAttribute("auctionDocReady", status.isAuctionReady());
        model.addAttribute("preshipReady", status.isPreshipReady());
        model.addAttribute("equipmentReady", status.isEquipmentReady());
        model.addAttribute("jevicReady", status.isJevicReady());
        model.addAttribute("coiReady", status.isCoiReady());
        model.addAttribute("standardsReady", status.isStandardsReady());
        model.addAttribute("exportReady", status.isExportReady());
        model.addAttribute("gradeReady", status.isGradeReady());
        model.addAttribute("photosReady", status.isPhotosReady());
        model.addAttribute("worksheetReady", status.isWorksheetReady());
    }
}
