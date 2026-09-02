package com.carsale.erp.controller;

import java.nio.charset.StandardCharsets;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.dto.InspectionFailRequest;
import com.carsale.erp.dto.InspectionFailResult;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.VehicleInspection;
import com.carsale.erp.service.InspectionResults;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreparationPipelineService;
import com.carsale.erp.service.PreparationPipelineService.PrepStatus;
import com.carsale.erp.service.VehicleInspectionService;
import com.carsale.erp.service.VehicleService;

@Controller
@RequestMapping("/inspection")
public class InspectionController {

    private final VehicleInspectionService vehicleInspectionService;
    private final VehicleService vehicleService;
    private final PreparationPipelineService preparationPipelineService;
    private final PipelineStageService pipelineStageService;

    public InspectionController(
            VehicleInspectionService vehicleInspectionService,
            VehicleService vehicleService,
            PreparationPipelineService preparationPipelineService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleInspectionService = vehicleInspectionService;
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
        VehicleInspection record = vehicleInspectionService.prepareForm(chassisNo);
        PrepStatus status = preparationPipelineService.statusFor(vehicle);
        model.addAttribute("pageTitle", "Inspection");
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("resultOptions", InspectionResults.options());
        addPrepStatus(model, chassisNo, status);
        return "inspection/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute VehicleInspection record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            vehicleInspectionService.save(record);
            preparationPipelineService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Inspection saved.");
            PrepStatus status = preparationPipelineService.statusFor(chassisNo);
            return "redirect:" + ReadyStageNavigation.redirectAfterInspectionSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/inspection/" + encodeChassis(chassisNo);
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

    private void addPrepStatus(Model model, String chassisNo, PrepStatus status) {
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPrepComplete());
        model.addAttribute("stageNav", ReadyStageNavigation.viewLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, PipelineStageService.STAGE_INSPECTION),
                status,
                pipelineStageService.keys(PipelineStageService.FLOW_PREP)
        ));
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
