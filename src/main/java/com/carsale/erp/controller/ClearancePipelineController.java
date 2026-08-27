package com.carsale.erp.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.dto.NavLinks;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.service.ClearancePipelineService;
import com.carsale.erp.service.ClearancePipelineService.ClearanceStatus;
import com.carsale.erp.service.ClearanceService;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.VehicleService;

@Controller
public class ClearancePipelineController {

    private final VehicleService vehicleService;
    private final ClearancePipelineService clearancePipelineService;
    private final ClearanceService clearanceService;
    private final PipelineStageService pipelineStageService;

    public ClearancePipelineController(
            VehicleService vehicleService,
            ClearancePipelineService clearancePipelineService,
            ClearanceService clearanceService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.clearancePipelineService = clearancePipelineService;
        this.clearanceService = clearanceService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/customs")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = clearancePipelineService.listEligible(query);
        List<ClearanceRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            rows.add(new ClearanceRow(vehicle, clearancePipelineService.statusFor(vehicle)));
        }
        model.addAttribute("pageTitle", "Customs clearance pipeline");
        model.addAttribute("activeMenu", "customs");
        model.addAttribute("rows", rows);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "customs/list";
    }

    @PostMapping("/customs/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (vehicleService.deleteFromFlow(chassisNo, PipelineStageService.FLOW_CUSTOMS)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Customs clearance data deleted. Preparation and ready-for-sale data were also removed.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete customs clearance data.");
        }
        return "redirect:/customs";
    }

    @GetMapping("/customs/{chassisNo}")
    public String view(
            @PathVariable String chassisNo,
            @RequestParam(value = "stage", required = false) Integer requestedStage,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/customs";
        }
        if (hub) {
            return "redirect:/customs/" + ClearanceStageNavigation.encode(chassisNo) + "/edit";
        }
        if (!clearancePipelineService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Finish the import pipeline before the customs clearance pipeline.");
            return "redirect:" + clearancePipelineService.ineligibleHubPath(vehicle);
        }

        clearancePipelineService.syncVehicleStage(chassisNo);
        vehicle = vehicleService.findByChassisNo(chassisNo);

        ClearanceStatus status = clearancePipelineService.statusFor(vehicle);
        List<String> customsKeys = pipelineStageService.keys(PipelineStageService.FLOW_CUSTOMS);
        int stageIndex = ClearanceStageNavigation.clampStageIndex(requestedStage, status, customsKeys);
        String stageKey = ClearanceStageNavigation.keyAt(customsKeys, stageIndex);

        if (clearanceService.findByChassisNo(chassisNo) == null) {
            return "redirect:" + ClearanceStageNavigation.editUrlFor(
                    ClearanceStageNavigation.encode(chassisNo),
                    stageKey
            );
        }

        NavLinks nav = ClearanceStageNavigation.viewLinks(chassisNo, stageIndex, customsKeys);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "customs");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("clearance", clearanceService.findByChassisNo(chassisNo));
        model.addAttribute("jevicReady", status.isJevicReady());
        model.addAttribute("declarationReady", status.isDeclarationReady());
        model.addAttribute("assessmentReady", status.isAssessmentReady());
        model.addAttribute("clearanceComplete", status.isClearanceComplete());
        model.addAttribute("stageIndex", stageIndex);
        model.addAttribute("stageKey", stageKey);
        model.addAttribute("stageNav", nav);
        return "customs/detail";
    }

    public static final class ClearanceRow {
        private final Vehicle vehicle;
        private final ClearanceStatus status;

        public ClearanceRow(Vehicle vehicle, ClearanceStatus status) {
            this.vehicle = vehicle;
            this.status = status;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public ClearanceStatus getStatus() {
            return status;
        }
    }
}
