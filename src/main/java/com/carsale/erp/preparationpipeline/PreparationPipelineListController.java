package com.carsale.erp.preparationpipeline;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.shared.pipeline.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;
import com.carsale.erp.preparationpipeline.inspection.VehicleInspectionService;
import com.carsale.erp.shared.vehicle.VehicleService;
import com.carsale.erp.preparationpipeline.workshop.WorkshopService;
import com.carsale.erp.preparationpipeline.yard.YardService;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

@Controller
public class PreparationPipelineListController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final WorkshopService workshopService;
    private final YardService yardService;
    private final VehicleInspectionService vehicleInspectionService;
    private final PipelineStageService pipelineStageService;

    public PreparationPipelineListController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            WorkshopService workshopService,
            YardService yardService,
            VehicleInspectionService vehicleInspectionService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.workshopService = workshopService;
        this.yardService = yardService;
        this.vehicleInspectionService = vehicleInspectionService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/workshop-yard")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = preparationProgressService.listEligible(query);
        List<PreparationListRow> rows = new ArrayList<PreparationListRow>();
        for (Vehicle vehicle : vehicles) {
            rows.add(new PreparationListRow(vehicle, preparationProgressService.progressFor(vehicle)));
        }
        model.addAttribute("pageTitle", "Preparation pipeline");
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("rows", rows);
        model.addAttribute("pipelineStages", pipelineStageService.list(PipelineStageService.FLOW_PREP));
        model.addAttribute("pipelineKeys", pipelineStageService.keys(PipelineStageService.FLOW_PREP));
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "preparation-pipeline/list";
    }

    @PostMapping("/workshop-yard/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (vehicleService.deleteFromFlow(chassisNo, PipelineStageService.FLOW_PREP)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Preparation data deleted. Ready-for-sale data was also removed.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete preparation data.");
        }
        return "redirect:/workshop-yard";
    }

    @GetMapping("/workshop-yard/{chassisNo}")
    public String view(
            @PathVariable String chassisNo,
            @RequestParam(value = "stage", required = false) String requestedStage,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/workshop-yard";
        }
        if (!preparationProgressService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    preparationProgressService.ineligibleNotice(vehicle));
            return "redirect:" + preparationProgressService.redirectWhenNotEligible(vehicle);
        }

        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        java.util.List<String> prepKeys = pipelineStageService.keys(PipelineStageService.FLOW_PREP);
        int stageIndex = PreparationStageUrls.clampStageIndex(
                PipelineStageService.parseStageIndex(prepKeys, requestedStage),
                status,
                prepKeys
        );
        String stageKey = PipelineStageService.keyAt(prepKeys, stageIndex);
        if (stageKey == null) {
            return "redirect:/workshop-yard";
        }

        if (FlowStage.INSPECTION.getStageKey().equals(stageKey)
                && vehicleInspectionService.findByChassisNo(chassisNo) == null) {
            return "redirect:/inspection/" + PreparationStageUrls.encode(chassisNo);
        }
        if (FlowStage.WORKSHOP.getStageKey().equals(stageKey) && workshopService.findByChassisNo(chassisNo) == null) {
            return "redirect:/workshop/" + PreparationStageUrls.encode(chassisNo);
        }
        if (FlowStage.YARD.getStageKey().equals(stageKey) && !status.isCanEnterYard()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Finish every workshop job before opening the yard section.");
            return "redirect:/workshop/" + PreparationStageUrls.encode(chassisNo);
        }
        if (FlowStage.YARD.getStageKey().equals(stageKey) && yardService.findByChassisNo(chassisNo) == null) {
            return "redirect:/yard/" + PreparationStageUrls.encode(chassisNo);
        }
        List<PipelineStage> importKeys = pipelineStageService.list(PipelineStageService.FLOW_PREP);
        NavLinks nav = viewLinks(chassisNo, stageIndex, status, importKeys, FlowPipeline.PREP);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("workshop", workshopService.findByChassisNo(chassisNo));
        model.addAttribute("yard", yardService.findByChassisNo(chassisNo));
        model.addAttribute("inspection", vehicleInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        model.addAttribute("stageIndex", stageIndex);
        model.addAttribute("stageKey", stageKey);
        model.addAttribute("stageNav", nav);
        return "preparation-pipeline/detail";
    }

    public static final class PreparationListRow {
        private final Vehicle vehicle;
        private final PreparationProgress status;

        public PreparationListRow(Vehicle vehicle, PreparationProgress status) {
            this.vehicle = vehicle;
            this.status = status;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public PreparationProgress getStatus() {
            return status;
        }
    }
}
