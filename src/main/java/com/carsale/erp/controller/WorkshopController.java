package com.carsale.erp.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.WorkshopJob;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreparationPipelineService;
import com.carsale.erp.service.PreparationPipelineService.PrepStatus;
import com.carsale.erp.service.VehicleService;
import com.carsale.erp.service.WorkshopService;

@Controller
@RequestMapping("/workshop")
public class WorkshopController {

    private final WorkshopService workshopService;
    private final VehicleService vehicleService;
    private final PreparationPipelineService preparationPipelineService;
    private final PipelineStageService pipelineStageService;

    public WorkshopController(
            WorkshopService workshopService,
            VehicleService vehicleService,
            PreparationPipelineService preparationPipelineService,
            PipelineStageService pipelineStageService
    ) {
        this.workshopService = workshopService;
        this.vehicleService = vehicleService;
        this.preparationPipelineService = preparationPipelineService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping
    public String list() {
        return "redirect:/workshop-yard";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
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
        WorkshopJob record = workshopService.prepareForm(chassisNo);
        PrepStatus status = preparationPipelineService.statusFor(vehicle);
        model.addAttribute("pageTitle", "Workshop");
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("prepComplete", status.isPrepComplete());
        model.addAttribute("stageNav", ReadyStageNavigation.viewLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, PipelineStageService.STAGE_WORKSHOP),
                status,
                pipelineStageService.keys(PipelineStageService.FLOW_PREP)
        ));
        return "workshop/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute WorkshopJob record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            workshopService.save(record);
            preparationPipelineService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Workshop job saved.");
            PrepStatus status = preparationPipelineService.statusFor(chassisNo);
            return "redirect:" + ReadyStageNavigation.redirectAfterWorkshopSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/workshop/" + encodeChassis(chassisNo);
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
