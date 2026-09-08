package com.carsale.erp.preparationpipeline.yard;

import com.carsale.erp.preparationpipeline.PreparationStageUrls;
import java.nio.charset.StandardCharsets;

import com.carsale.erp.shared.pipeline.FlowPipeline;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.preparationpipeline.PreparationProgressService;
import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/yard")
public class YardController {

    private final YardService yardService;
    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final PipelineStageService pipelineStageService;

    public YardController(
            YardService yardService,
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            PipelineStageService pipelineStageService
    ) {
        this.yardService = yardService;
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
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
        if (!preparationProgressService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    preparationProgressService.ineligibleNotice(vehicle));
            return "redirect:" + preparationProgressService.redirectWhenNotEligible(vehicle);
        }
        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        if (!status.isCanEnterYard()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Finish every workshop job before opening the yard section.");
            return "redirect:/workshop/" + encodeChassis(chassisNo);
        }
        YardRecord record = yardService.prepareForm(chassisNo);
        model.addAttribute("pageTitle", "Yard");
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("canEnterYard", true);
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        model.addAttribute("stageNav", PipelineStageService.viewLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, FlowStage.YARD.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_PREP),
                FlowPipeline.PREP
        ));
        return "preparation-pipeline/yard/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute YardRecord record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        if (!preparationProgressService.progressFor(chassisNo).isCanEnterYard()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Finish every workshop job before opening the yard section.");
            return "redirect:/workshop/" + encodeChassis(chassisNo);
        }
        try {
            yardService.save(record);
            preparationProgressService.syncVehicleStage(chassisNo);
            PreparationProgress status = preparationProgressService.progressFor(chassisNo);
            if (status.isPipelineCompleted()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Preparation pipeline is complete. Set the asking price for sale.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Yard record saved.");
            }
            return "redirect:" + PreparationStageUrls.redirectAfterYardSave(
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
