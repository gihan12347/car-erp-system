package com.carsale.erp.preparationpipeline.controller;

import com.carsale.erp.preparationpipeline.util.PreparationStageUrls;
import java.nio.charset.StandardCharsets;

import com.carsale.erp.preparationpipeline.dto.InspectionFailRequest;
import com.carsale.erp.preparationpipeline.dto.InspectionFailResult;
import com.carsale.erp.preparationpipeline.dto.InspectionResults;
import com.carsale.erp.preparationpipeline.service.VehicleInspectionService;
import com.carsale.erp.preparationpipeline.model.VehicleInspection;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService.PreparationProgress;
import com.carsale.erp.preparationpipeline.service.WorkshopService;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/inspection")
public class VehicleInspectionController {

    private final VehicleInspectionService vehicleInspectionService;
    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final PipelineStageService pipelineStageService;
    private final WorkshopService workshopService;

    public VehicleInspectionController(
            VehicleInspectionService vehicleInspectionService,
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            PipelineStageService pipelineStageService,
            WorkshopService workshopService
    ) {
        this.vehicleInspectionService = vehicleInspectionService;
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.pipelineStageService = pipelineStageService;
        this.workshopService = workshopService;
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
        VehicleInspection record = vehicleInspectionService.prepareForm(chassisNo);
        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_PREP, FlowStage.INSPECTION.getStageKey()));
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("resultOptions", InspectionResults.options());
        addPrepStatus(model, chassisNo, status);
        return "preparation-pipeline/inspection/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute VehicleInspection record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            vehicleInspectionService.save(record);
            preparationProgressService.syncVehicleStage(chassisNo);
            if (!workshopService.hasJobs(chassisNo)) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Inspection saved. No workshop jobs — workshop is complete. Continue with yard.");
                return "redirect:/yard/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Inspection saved.");
            return "redirect:" + PreparationStageUrls.redirectAfterInspectionSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/inspection/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    @PostMapping("/{chassisNo}/fail-item")
    @ResponseBody
    public ResponseEntity<InspectionFailResult> failItem(
            @PathVariable String chassisNo,
            @RequestBody InspectionFailRequest request
    ) {
        try {
            return ResponseEntity.ok(vehicleInspectionService.recordNoSelection(chassisNo, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(InspectionFailResult.fail(ex.getMessage()));
        }
    }

    @PostMapping("/{chassisNo}/clear-fail-item")
    @ResponseBody
    public ResponseEntity<InspectionFailResult> clearFailItem(
            @PathVariable String chassisNo,
            @RequestBody InspectionFailRequest request
    ) {
        try {
            return ResponseEntity.ok(vehicleInspectionService.clearNoSelection(chassisNo, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(InspectionFailResult.fail(ex.getMessage()));
        }
    }

    private void addPrepStatus(Model model, String chassisNo, PreparationProgress status) {
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("saleReady", status.isSaleReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        model.addAttribute("stageNav", PreparationStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, FlowStage.INSPECTION.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_PREP)
        ));
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
