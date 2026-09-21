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
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/registration")
public class VehicleRegistrationController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final SaleListingService saleListingService;

    public VehicleRegistrationController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            VehicleRegistrationService vehicleRegistrationService,
            SaleListingService saleListingService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.vehicleRegistrationService = vehicleRegistrationService;
        this.saleListingService = saleListingService;
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
                    "Move this vehicle from the yard to an available sale first.");
            return "redirect:/yards";
        }
        VehicleRegistration record = vehicleRegistrationService.prepareForm(chassisNo);
        SaleListing listing = saleListingService.findByChassisNo(chassisNo);
        model.addAttribute("pageTitle", "Register");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("saleCode", listing == null ? null : listing.getSaleCode());
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
            SaleListing listing = saleListingService.findByChassisNo(chassisNo);
            String saleCode = listing == null ? null : listing.getSaleCode();
            return "redirect:" + ReadyStageUrls.redirectAfterRegistrationSave(
                    saleCode,
                    chassisNo,
                    VehicleRegistrationService.isComplete(saved)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/registration/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
