package com.carsale.erp.shared.vehicle;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class VehicleSummaryController {

    private final VehicleSummaryService vehicleSummaryService;

    public VehicleSummaryController(VehicleSummaryService vehicleSummaryService) {
        this.vehicleSummaryService = vehicleSummaryService;
    }

    @GetMapping("/vehicle/{chassisNo}")
    public String summary(@PathVariable String chassisNo, Model model) {
        VehicleSummary summary = vehicleSummaryService.build(chassisNo);
        if (summary == null) {
            return "redirect:/import";
        }
        model.addAttribute("pageTitle", summary.getVehicle().getChassisNo());
        model.addAttribute("activeMenu", "");
        model.addAttribute("summary", summary);
        return "vehicle/summary";
    }
}
