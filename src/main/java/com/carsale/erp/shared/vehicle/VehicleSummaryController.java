package com.carsale.erp.shared.vehicle;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.readypipeline.service.SaleListingService;

@Controller
public class VehicleSummaryController {

    private final VehicleSummaryService vehicleSummaryService;
    private final SaleListingService saleListingService;
    private final VehicleExtraCostService vehicleExtraCostService;

    public VehicleSummaryController(
            VehicleSummaryService vehicleSummaryService,
            SaleListingService saleListingService,
            VehicleExtraCostService vehicleExtraCostService
    ) {
        this.vehicleSummaryService = vehicleSummaryService;
        this.saleListingService = saleListingService;
        this.vehicleExtraCostService = vehicleExtraCostService;
    }

    @GetMapping("/vehicle/{chassisNo}")
    public String summary(@PathVariable String chassisNo, Model model) {
        VehicleSummary summary = vehicleSummaryService.build(chassisNo);
        if (summary == null) {
            return "redirect:/import";
        }
        model.addAttribute("pageTitle", summary.getVehicle().getChassisNo());
        model.addAttribute("activeMenu", summary.getVehicle().getStage() == VehicleStage.SOLD ? "sold" : "");
        model.addAttribute("summary", summary);
        return "vehicle/summary";
    }

    @PostMapping("/vehicle/{chassisNo}/prices")
    public String updatePrice(
            @PathVariable String chassisNo,
            @RequestParam(value = "priceKind", required = false) String priceKind,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            saleListingService.updatePrice(chassisNo, priceKind, price);
            redirectAttributes.addFlashAttribute("notice",
                    "asking".equals(priceKind) ? "Asking price updated." : "Actual price updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + safeReturn(returnTo, chassisNo);
    }

    @PostMapping("/vehicle/{chassisNo}/extra-costs")
    public String addExtraCost(
            @PathVariable String chassisNo,
            @RequestParam(value = "costDate", required = false) String costDate,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "cost", required = false) String cost,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            vehicleExtraCostService.add(chassisNo, costDate, reason, cost);
            redirectAttributes.addFlashAttribute("notice", "Extra cost added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + safeReturn(returnTo, chassisNo);
    }

    private static String safeReturn(String returnTo, String chassisNo) {
        String fallback = "/vehicle/" + (chassisNo == null ? "" : chassisNo.trim());
        if (returnTo == null) {
            return fallback;
        }
        String path = returnTo.trim();
        if (path.indexOf("://") >= 0 || path.indexOf("//") >= 0 || path.indexOf('\\') >= 0 || path.indexOf('?') >= 0) {
            return fallback;
        }
        if (path.startsWith("/vehicle/") || path.startsWith("/ready-for-sale/") || path.startsWith("/yards/")) {
            return path;
        }
        return fallback;
    }
}
