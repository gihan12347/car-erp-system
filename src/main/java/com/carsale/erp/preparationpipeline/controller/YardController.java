package com.carsale.erp.preparationpipeline.controller;

import com.carsale.erp.preparationpipeline.util.PreparationStageUrls;
import java.nio.charset.StandardCharsets;

import com.carsale.erp.preparationpipeline.service.YardBayService;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.service.YardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService.PreparationProgress;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/yard")
public class YardController {

    private final YardService yardService;
    private final YardBayService yardBayService;
    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final PipelineStageService pipelineStageService;

    public YardController(
            YardService yardService,
            YardBayService yardBayService,
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            PipelineStageService pipelineStageService
    ) {
        this.yardService = yardService;
        this.yardBayService = yardBayService;
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return "redirect:/workshop-yard?q=" + UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
        }
        return "redirect:/workshop-yard";
    }

    @GetMapping("/{chassisNo}")
    public String form(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
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
        YardRecord record = yardService.prepareForm(chassisNo);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_PREP, FlowStage.YARD.getStageKey()));
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("yardBays", yardBayService.listActive());
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("saleReady", status.isSaleReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        model.addAttribute("stageNav", PreparationStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, FlowStage.YARD.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_PREP)
        ));
        return "preparation-pipeline/yard/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute YardRecord record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            yardService.save(record);
            preparationProgressService.syncVehicleStage(chassisNo);
            PreparationProgress status = preparationProgressService.progressFor(chassisNo);
            if (status.isPipelineCompleted()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Preparation pipeline is complete. Open the yard to move the vehicle to sale.");
                YardRecord saved = yardService.findByChassisNo(chassisNo);
                if (saved != null && saved.getBayNo() != null && !saved.getBayNo().trim().isEmpty()) {
                    return "redirect:/yards/" + encodeChassis(saved.getBayNo());
                }
                return "redirect:/yards";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Yard record saved.");
            return "redirect:" + PreparationStageUrls.redirectAfterYardSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/yard/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
