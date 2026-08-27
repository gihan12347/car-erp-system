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
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreparationPipelineService;
import com.carsale.erp.service.PreparationPipelineService.PrepStatus;
import com.carsale.erp.service.VehicleService;
import com.carsale.erp.service.WorkshopService;
import com.carsale.erp.service.YardService;

@Controller
public class WorkshopYardController {

    private final VehicleService vehicleService;
    private final PreparationPipelineService preparationPipelineService;
    private final WorkshopService workshopService;
    private final YardService yardService;
    private final PipelineStageService pipelineStageService;

    public WorkshopYardController(
            VehicleService vehicleService,
            PreparationPipelineService preparationPipelineService,
            WorkshopService workshopService,
            YardService yardService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.preparationPipelineService = preparationPipelineService;
        this.workshopService = workshopService;
        this.yardService = yardService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/workshop-yard")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = preparationPipelineService.listEligible(query);
        List<PrepRow> rows = new ArrayList<PrepRow>();
        for (Vehicle vehicle : vehicles) {
            rows.add(new PrepRow(vehicle, preparationPipelineService.statusFor(vehicle)));
        }
        model.addAttribute("pageTitle", "Preparation pipeline");
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("rows", rows);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "workshop-yard/list";
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
            @RequestParam(value = "stage", required = false) Integer requestedStage,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/workshop-yard";
        }
        if (!preparationPipelineService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    preparationPipelineService.ineligibleNotice(vehicle));
            return "redirect:" + preparationPipelineService.ineligibleHubPath(vehicle);
        }

        PrepStatus status = preparationPipelineService.statusFor(vehicle);
        java.util.List<String> prepKeys = pipelineStageService.keys(PipelineStageService.FLOW_PREP);
        int stageIndex = ReadyStageNavigation.clampStageIndex(requestedStage, status, prepKeys);
        String stageKey = ReadyStageNavigation.keyAt(prepKeys, stageIndex);

        if (PipelineStageService.STAGE_WORKSHOP.equals(stageKey) && workshopService.findByChassisNo(chassisNo) == null) {
            return "redirect:/workshop/" + ReadyStageNavigation.encode(chassisNo);
        }
        if (PipelineStageService.STAGE_YARD.equals(stageKey) && yardService.findByChassisNo(chassisNo) == null) {
            return "redirect:/yard/" + ReadyStageNavigation.encode(chassisNo);
        }

        NavLinks nav = ReadyStageNavigation.viewLinks(chassisNo, stageIndex, status, prepKeys);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("workshop", workshopService.findByChassisNo(chassisNo));
        model.addAttribute("yard", yardService.findByChassisNo(chassisNo));
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("prepComplete", status.isPrepComplete());
        model.addAttribute("stageIndex", stageIndex);
        model.addAttribute("stageKey", stageKey);
        model.addAttribute("stageNav", nav);
        return "workshop-yard/detail";
    }

    public static final class PrepRow {
        private final Vehicle vehicle;
        private final PrepStatus status;

        public PrepRow(Vehicle vehicle, PrepStatus status) {
            this.vehicle = vehicle;
            this.status = status;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public PrepStatus getStatus() {
            return status;
        }
    }
}
