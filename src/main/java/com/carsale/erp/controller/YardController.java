package com.carsale.erp.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.YardRecord;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreparationPipelineService;
import com.carsale.erp.service.PreparationPipelineService.PrepStatus;
import com.carsale.erp.service.VehicleService;
import com.carsale.erp.service.YardService;

@Controller
@RequestMapping("/yard")
public class YardController {

    private final YardService yardService;
    private final VehicleService vehicleService;
    private final PreparationPipelineService preparationPipelineService;
    private final PipelineStageService pipelineStageService;

    public YardController(
            YardService yardService,
            VehicleService vehicleService,
            PreparationPipelineService preparationPipelineService,
            PipelineStageService pipelineStageService
    ) {
        this.yardService = yardService;
        this.vehicleService = vehicleService;
        this.preparationPipelineService = preparationPipelineService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping
    public String list() {
        return "redirect:/workshop-yard";
    }

    @GetMapping("/{chassisNo}")
    public String form(
            @PathVariable String chassisNo,
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
        YardRecord record = yardService.prepareForm(chassisNo);
        PrepStatus status = preparationPipelineService.statusFor(vehicle);
        model.addAttribute("pageTitle", "Yard");
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("prepComplete", status.isPrepComplete());
        model.addAttribute("stageNav", ReadyStageNavigation.viewLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, PipelineStageService.STAGE_YARD),
                status,
                pipelineStageService.keys(PipelineStageService.FLOW_PREP)
        ));
        return "yard/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute YardRecord record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            yardService.save(record);
            preparationPipelineService.syncVehicleStage(chassisNo);
            PrepStatus status = preparationPipelineService.statusFor(chassisNo);
            if (status.isPrepComplete()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Preparation pipeline is complete. Set the asking price for sale.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Yard record saved.");
            }
            return "redirect:" + ReadyStageNavigation.redirectAfterYardSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/yard/" + encodeChassis(chassisNo);
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
