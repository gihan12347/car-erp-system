package com.carsale.erp.preparationpipeline.controller;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.preparationpipeline.util.PreparationStageUrls;
import com.carsale.erp.shared.pipeline.*;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService.PreparationProgress;
import com.carsale.erp.preparationpipeline.service.VehicleInspectionService;
import com.carsale.erp.shared.vehicle.VehicleService;
import com.carsale.erp.preparationpipeline.service.WorkshopService;
import com.carsale.erp.preparationpipeline.service.YardService;
import com.carsale.erp.readypipeline.service.SaleListingService;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

@Controller
public class PreparationPipelineListController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final WorkshopService workshopService;
    private final YardService yardService;
    private final VehicleInspectionService vehicleInspectionService;
    private final SaleListingService saleListingService;
    private final PipelineStageService pipelineStageService;

    public PreparationPipelineListController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            WorkshopService workshopService,
            YardService yardService,
            VehicleInspectionService vehicleInspectionService,
            SaleListingService saleListingService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.workshopService = workshopService;
        this.yardService = yardService;
        this.vehicleInspectionService = vehicleInspectionService;
        this.saleListingService = saleListingService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/workshop-yard")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = preparationProgressService.listEligible(query);
        List<PreparationListRow> rows = new ArrayList<>();
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
                    "Preparation data deleted. Sale pipeline data was also removed.");
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

        preparationProgressService.syncVehicleStage(chassisNo);
        vehicle = vehicleService.findByChassisNo(chassisNo);

        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        List<String> prepKeys = pipelineStageService.keys(PipelineStageService.FLOW_PREP);
        List<PipelineStage> prepStages = pipelineStageService.list(PipelineStageService.FLOW_PREP);
        int stageIndex = PipelineStageUtils.clampStageIndex(
                PipelineStageUtils.parseStageIndex(prepKeys, requestedStage),
                status,
                prepKeys
        );
        String stageKey = PipelineStageUtils.stageKeyAt(prepStages, stageIndex);
        if (stageKey == null) {
            return "redirect:/workshop-yard";
        }
        if (FlowStage.WORKSHOP.getStageKey().equals(stageKey) && !workshopService.hasJobs(chassisNo)) {
            preparationProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage",
                    "No workshop jobs. Workshop is complete. Continue with yard.");
            return "redirect:/yard/" + UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8)
                    + "?hub=1";
        }
        NavLinks nav = viewLinks(chassisNo, stageIndex, status, prepStages, FlowPipeline.PREP);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("workshop", workshopService.findByChassisNo(chassisNo));
        model.addAttribute("yard", yardService.findByChassisNo(chassisNo));
        model.addAttribute("inspection", vehicleInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("listing", saleListingService.findByChassisNo(chassisNo));
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("saleReady", status.isSaleReady());
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
