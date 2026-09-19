package com.carsale.erp.customspipeline;

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
import com.carsale.erp.customspipeline.CustomsProgressService.CustomsProgress;
import com.carsale.erp.shared.vehicle.VehicleService;


@Controller
public class CustomsPipelineListController {

    private final VehicleService vehicleService;
    private final CustomsProgressService customsProgressService;
    private final CustomsDocumentService customsDocumentService;
    private final PipelineStageService pipelineStageService;

    public CustomsPipelineListController(
            VehicleService vehicleService,
            CustomsProgressService customsProgressService,
            CustomsDocumentService customsDocumentService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.customsProgressService = customsProgressService;
        this.customsDocumentService = customsDocumentService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/customs")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = customsProgressService.listEligible(query);
        List<CustomsListRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            rows.add(new CustomsListRow(vehicle, customsProgressService.progressFor(vehicle)));
        }
        model.addAttribute("pageTitle", "Customs clearance pipeline");
        model.addAttribute("activeMenu", "customs");
        model.addAttribute("rows", rows);
        model.addAttribute("pipelineStages", pipelineStageService.list(PipelineStageService.FLOW_CUSTOMS));
        model.addAttribute("pipelineKeys", pipelineStageService.keys(PipelineStageService.FLOW_CUSTOMS));
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "customs-pipeline/list";
    }

    @PostMapping("/customs/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (vehicleService.deleteFromFlow(chassisNo, PipelineStageService.FLOW_CUSTOMS)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Customs clearance data deleted. Preparation and sale pipeline data were also removed.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete customs clearance data.");
        }
        return "redirect:/customs";
    }

    @GetMapping("/customs/{chassisNo}")
    public String view(
            @PathVariable String chassisNo,
            @RequestParam(value = "stage", required = false) String requestedStage,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/customs";
        }
        if (hub) {
            return "redirect:" + PipelineStageService.editUrlFor(
                    CustomsStageUrls.encode(chassisNo),
                    FlowStage.BILL_OF_LADING.getStageKey()
            );
        }
        if (!customsProgressService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Finish the import pipeline before the customs clearance pipeline.");
            return "redirect:" + customsProgressService.redirectWhenNotEligible(vehicle);
        }

        customsProgressService.syncVehicleStage(chassisNo);
        vehicle = vehicleService.findByChassisNo(chassisNo);

        CustomsProgress status = customsProgressService.progressFor(vehicle);
        List<String> customsKeys = pipelineStageService.keys(PipelineStageService.FLOW_CUSTOMS);
        int stageIndex = CustomsStageUrls.clampStageIndex(
                PipelineStageService.parseStageIndex(customsKeys, requestedStage),
                status,
                customsKeys
        );
        String stageKey = PipelineStageService.keyAt(customsKeys, stageIndex);
        if (stageKey == null) {
            return "redirect:/customs";
        }

        if (customsDocumentService.findByChassisNo(chassisNo) == null) {
            return "redirect:" + PipelineStageService.editUrlFor(
                    CustomsStageUrls.encode(chassisNo),
                    stageKey
            );
        }
        List<PipelineStage> importKeys = pipelineStageService.list(PipelineStageService.FLOW_CUSTOMS);
        NavLinks nav = PipelineStageService.viewLinks(chassisNo, stageIndex, status, importKeys, FlowPipeline.CUSTOMS);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "customs");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("clearance", customsDocumentService.findByChassisNo(chassisNo));
        model.addAttribute("blReady", status.isBlReady());
        model.addAttribute("declarationReady", status.isDeclarationReady());
        model.addAttribute("assessmentReady", status.isAssessmentReady());
        model.addAttribute("worksheetReady", status.isWorksheetReady());
        model.addAttribute("clearanceComplete", status.isClearanceComplete());
        model.addAttribute("stageIndex", stageIndex);
        model.addAttribute("stageKey", stageKey);
        model.addAttribute("stageNav", nav);
        return "customs-pipeline/detail";
    }

    public static final class CustomsListRow {
        private final Vehicle vehicle;
        private final CustomsProgress status;

        public CustomsListRow(Vehicle vehicle, CustomsProgress status) {
            this.vehicle = vehicle;
            this.status = status;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public CustomsProgress getStatus() {
            return status;
        }
    }
}
