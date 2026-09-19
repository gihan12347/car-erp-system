package com.carsale.erp.readypipeline;

import java.nio.charset.StandardCharsets;

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

import com.carsale.erp.preparationpipeline.PreparationProgressService;
import com.carsale.erp.readypipeline.SaleListingService.SaleProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/registration")
public class VehicleRegistrationController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingService saleListingService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final PipelineStageService pipelineStageService;

    public VehicleRegistrationController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            SaleListingService saleListingService,
            VehicleRegistrationService vehicleRegistrationService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.saleListingService = saleListingService;
        this.vehicleRegistrationService = vehicleRegistrationService;
        this.pipelineStageService = pipelineStageService;
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
            return "redirect:/ready-for-sale";
        }
        if (!preparationProgressService.isEligibleForSale(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Complete the preparation pipeline before registering this vehicle.");
            return "redirect:/workshop-yard/" + encodeChassis(chassisNo);
        }
        VehicleRegistration record = vehicleRegistrationService.prepareForm(chassisNo);
        SaleProgress status = saleListingService.progressFor(chassisNo);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_READY, FlowStage.REGISTRATION.getStageKey()));
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        addSaleStatus(model, chassisNo, status);
        return "ready-for-sale/registration";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute VehicleRegistration record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            VehicleRegistration saved = vehicleRegistrationService.save(record);
            if (VehicleRegistrationService.isComplete(saved)) {
                redirectAttributes.addFlashAttribute("successMessage", "Registration saved.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Registration draft saved. Add the plate number and date to complete this step.");
            }
            return "redirect:" + ReadyStageUrls.redirectAfterRegistrationSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_READY)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/registration/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private void addSaleStatus(Model model, String chassisNo, SaleProgress status) {
        model.addAttribute("listingReady", status.isListingReady());
        model.addAttribute("detailsReady", status.isDetailsReady());
        model.addAttribute("registrationReady", status.isRegistrationReady());
        model.addAttribute("saleSold", status.isSold());
        model.addAttribute("saleComplete", status.isPipelineCompleted());
        model.addAttribute("stageNav", ReadyStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_READY, FlowStage.REGISTRATION.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_READY)
        ));
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
