package com.carsale.erp.readypipeline.controller;

import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.VehicleRegistration;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.readypipeline.service.VehicleRegistrationService;
import com.carsale.erp.readypipeline.util.ReadyStageUrls;
import com.carsale.erp.shared.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.preparationpipeline.service.RegistrationCertificateService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/registration")
public class VehicleRegistrationController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final RegistrationCertificateService registrationCertificateService;
    private final SaleListingService saleListingService;

    public VehicleRegistrationController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            VehicleRegistrationService vehicleRegistrationService,
            RegistrationCertificateService registrationCertificateService,
            SaleListingService saleListingService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.vehicleRegistrationService = vehicleRegistrationService;
        this.registrationCertificateService = registrationCertificateService;
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
        RegistrationCertificate certificate = registrationCertificateService.prepareForm(chassisNo);
        SaleListing listing = saleListingService.findByChassisNo(chassisNo);
        model.addAttribute("pageTitle", "Register");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("certificate", certificate);
        model.addAttribute("saleCode", listing == null ? null : listing.getSaleCode());
        return "ready-for-sale/registration";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute("certificate") RegistrationCertificate certificate,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        certificate.setChassisNo(chassisNo);
        try {
            registrationCertificateService.save(certificate);
            VehicleRegistration saved = vehicleRegistrationService.save(saleRecord(chassisNo, certificate));
            if (vehicleRegistrationService.isComplete(saved.getChassisNo())) {
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
                    vehicleRegistrationService.isComplete(saved.getChassisNo())
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("certificate", certificate);
            return "redirect:/registration/" + Utils.encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private VehicleRegistration saleRecord(String chassisNo, RegistrationCertificate certificate) {
        VehicleRegistration record = vehicleRegistrationService.prepareForm(chassisNo);
        if (record == null) {
            record = new VehicleRegistration();
            record.setChassisNo(chassisNo);
        }
        record.setRegistrationNo(trimTo(certificate.getRegistrationNo(), 40));
        record.setRegisteredDate(trimTo(firstText(
                certificate.getDateOfFirstRegistration(),
                certificate.getPrintedDate()
        ), 40));
        record.setRegisteredOwner(trimTo(firstText(
                certificate.getCurrentOwnerAddressIdNo(),
                certificate.getAbsoluteOwner()
        ), 120));
        record.setRmvOffice(trimTo(certificate.getProvincialCouncil(), 80));
        record.setFileNo(trimTo(certificate.getCrNo(), 40));
        return record;
    }

    private static String firstText(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback.trim();
        }
        return null;
    }

    private static String trimTo(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
