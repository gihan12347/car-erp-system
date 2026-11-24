package com.carsale.erp.shared.pipeline;

import java.util.List;

import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.preparationpipeline.service.RegisteredProgressService;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.shared.dto.SaleProgress;
import com.carsale.erp.shared.model.PipelineStage;
import com.carsale.erp.shared.repository.PipelineProgress;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.service.SkippedStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleStage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PipelineResumeService {

    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PreparationProgressService preparationProgressService;
    private final RegisteredProgressService registeredProgressService;
    private final SaleListingService saleListingService;
    private final PipelineStageService pipelineStageService;

    public Resume resumeFor(Vehicle vehicle) {
        if (vehicle == null || vehicle.getChassisNo() == null || vehicle.getChassisNo().trim().isEmpty()) {
            return null;
        }
        if (vehicle.getStage() == VehicleStage.SOLD) {
            return null;
        }
        SaleProgress sale = saleListingService.progressFor(vehicle.getChassisNo());
        if (sale != null && sale.isSold()) {
            return null;
        }

        String chassisNo = vehicle.getChassisNo().trim();
        String encoded = PipelineStageUtils.encode(chassisNo);

        if (vehicle.getIntake() == VehicleIntake.LOCAL_REGISTERED) {
            Resume registered = registeredResume(chassisNo, encoded);
            if (registered != null) {
                return registered;
            }
            Resume prep = resumeInFlow(encoded, preparationProgressService.progressFor(vehicle),
                    PipelineStageService.FLOW_PREP);
            if (prep != null) {
                return prep;
            }
            return saleOrAssign(vehicle, encoded, sale);
        }

        Resume importResume = resumeInFlow(encoded, importProgressService.progressFor(vehicle),
                PipelineStageService.FLOW_IMPORT);
        if (importResume != null) {
            return importResume;
        }

        String selected = PipelineStageUtils.selectedPipelinesOf(chassisNo);
        FlowPipeline current = FlowPipeline.IMPORT;
        while (true) {
            FlowPipeline next = PipelineStageUtils.nextSelectedPipeline(current, selected);
            if (next == null) {
                break;
            }
            Resume resume = resumeInFlow(encoded, progressFor(vehicle, next), flowKey(next));
            if (resume != null) {
                return resume;
            }
            current = next;
        }
        return saleOrAssign(vehicle, encoded, sale);
    }

    private Resume saleOrAssign(Vehicle vehicle, String encoded, SaleProgress sale) {
        boolean onSale = saleListingService.isAssignedToSale(vehicle.getChassisNo())
                || vehicle.getStage() == VehicleStage.RESERVED;
        if (onSale) {
            Resume saleResume = resumeInFlow(encoded, sale, PipelineStageService.FLOW_READY);
            if (saleResume != null) {
                return saleResume;
            }
            return null;
        }
        return new Resume("/yards", "Assign to sale");
    }

    private Resume registeredResume(String chassisNo, String encoded) {
        if (!registeredProgressService.isBookReady(chassisNo)) {
            return new Resume("/workshop-yard/" + encoded + "/certificate", "Vehicle book");
        }
        if (!registeredProgressService.isPhotosReady(chassisNo)) {
            return new Resume("/registered/" + encoded + "/photos", "Vehicle images");
        }
        if (!registeredProgressService.isOwnerReady(chassisNo)) {
            return new Resume("/registered/" + encoded + "/owner", "Previous owner");
        }
        return null;
    }

    private Resume resumeInFlow(String encoded, PipelineProgress progress, String flowKey) {
        if (progress == null || progress.isPipelineCompleted()) {
            return null;
        }
        List<String> keys = pipelineStageService.keys(flowKey);
        String stageKey = firstIncomplete(progress, keys);
        if (stageKey == null) {
            return null;
        }
        String href = PipelineStageService.editUrlFor(encoded, stageKey);
        if (href == null) {
            return null;
        }
        return new Resume(href, stageTitle(flowKey, stageKey));
    }

    private static String firstIncomplete(PipelineProgress progress, List<String> keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            if (PipelineStageUtils.isIncomplete(key, progress)) {
                return key;
            }
        }
        return null;
    }

    private String stageTitle(String flowKey, String stageKey) {
        for (PipelineStage stage : pipelineStageService.list(flowKey)) {
            if (stageKey.equals(stage.getStageKey()) && stage.getTitle() != null && !stage.getTitle().trim().isEmpty()) {
                return stage.getTitle();
            }
        }
        String fallback = PipelineStageService.titleFor(stageKey);
        return fallback == null ? stageKey : fallback;
    }

    private PipelineProgress progressFor(Vehicle vehicle, FlowPipeline pipeline) {
        if (pipeline == FlowPipeline.CUSTOMS) {
            return customsProgressService.progressFor(vehicle);
        }
        if (pipeline == FlowPipeline.PREP) {
            return preparationProgressService.progressFor(vehicle);
        }
        return importProgressService.progressFor(vehicle);
    }

    private static String flowKey(FlowPipeline pipeline) {
        if (pipeline == FlowPipeline.CUSTOMS) {
            return PipelineStageService.FLOW_CUSTOMS;
        }
        if (pipeline == FlowPipeline.PREP) {
            return PipelineStageService.FLOW_PREP;
        }
        return PipelineStageService.FLOW_IMPORT;
    }

    @Getter
    public static final class Resume {
        private final String href;
        private final String label;

        public Resume(String href, String label) {
            this.href = href;
            this.label = label;
        }
    }
}
