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
@RequestMapping("/listing")
public class SaleListingController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingService saleListingService;
    private final SaleLocationService saleLocationService;
    private final PipelineStageService pipelineStageService;

    public SaleListingController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            SaleListingService saleListingService,
            SaleLocationService saleLocationService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.saleListingService = saleListingService;
        this.saleLocationService = saleLocationService;
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
                    "Complete the preparation pipeline before listing this vehicle for sale.");
            return "redirect:/workshop-yard/" + encodeChassis(chassisNo);
        }
        SaleListing record = saleListingService.prepareForm(chassisNo);
        SaleProgress status = saleListingService.progressFor(chassisNo);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_READY, FlowStage.LISTING.getStageKey()));
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("saleLocations", saleLocationService.listActive());
        model.addAttribute("listingReady", status.isListingReady());
        model.addAttribute("detailsReady", status.isDetailsReady());
        model.addAttribute("registrationReady", status.isRegistrationReady());
        model.addAttribute("saleSold", status.isSold());
        model.addAttribute("saleComplete", status.isPipelineCompleted());
        model.addAttribute("stageNav", ReadyStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_READY, FlowStage.LISTING.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_READY)
        ));
        return "ready-for-sale/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute SaleListing record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            saleListingService.save(record);
            if (record.isSold()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Vehicle sold. Sale capacity was freed.");
            } else if (record.isListed()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Vehicle is listed. Sale capacity was reduced.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Sale listing saved.");
            }
            return "redirect:" + ReadyStageUrls.redirectAfterListingSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_READY)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/listing/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
