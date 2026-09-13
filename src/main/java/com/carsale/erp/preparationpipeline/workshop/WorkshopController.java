package com.carsale.erp.preparationpipeline.workshop;

import com.carsale.erp.preparationpipeline.PreparationStageUrls;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.preparationpipeline.PreparationProgressService;
import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/workshop")
public class WorkshopController {

    private final WorkshopService workshopService;
    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final PipelineStageService pipelineStageService;

    public WorkshopController(
            WorkshopService workshopService,
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            PipelineStageService pipelineStageService
    ) {
        this.workshopService = workshopService;
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

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
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
        WorkshopJob record = workshopService.prepareForm(chassisNo);
        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_PREP, FlowStage.WORKSHOP.getStageKey()));
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        model.addAttribute("stageNav", PreparationStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, FlowStage.WORKSHOP.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_PREP)
        ));
        return "preparation-pipeline/workshop/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute WorkshopJob record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            workshopService.save(record);
            preparationProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Workshop job saved.");
            PreparationProgress status = preparationProgressService.progressFor(chassisNo);
            return "redirect:" + PreparationStageUrls.redirectAfterWorkshopSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/workshop/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
