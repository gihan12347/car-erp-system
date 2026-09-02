package com.carsale.erp.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

import com.carsale.erp.entity.SaleListing;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreparationPipelineService;
import com.carsale.erp.service.PreparationPipelineService.PrepStatus;
import com.carsale.erp.service.SaleListingService;
import com.carsale.erp.service.VehicleService;

@Controller
@RequestMapping("/ready-for-sale")
public class ReadyForSaleController {

    private final VehicleService vehicleService;
    private final PreparationPipelineService preparationPipelineService;
    private final SaleListingService saleListingService;

    public ReadyForSaleController(
            VehicleService vehicleService,
            PreparationPipelineService preparationPipelineService,
            SaleListingService saleListingService
    ) {
        this.vehicleService = vehicleService;
        this.preparationPipelineService = preparationPipelineService;
        this.saleListingService = saleListingService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = preparationPipelineService.listReadyForSale(query);
        List<SaleRow> rows = new ArrayList<SaleRow>();
        for (Vehicle vehicle : vehicles) {
            rows.add(new SaleRow(
                    vehicle,
                    preparationPipelineService.statusFor(vehicle),
                    saleListingService.findByChassisNo(vehicle.getChassisNo())
            ));
        }
        model.addAttribute("pageTitle", "Ready for sale pipeline");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("rows", rows);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "ready/list";
    }

    @PostMapping("/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (vehicleService.deleteFromFlow(chassisNo, PipelineStageService.FLOW_READY)) {
            redirectAttributes.addFlashAttribute("notice", "Sale listing deleted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete the sale listing.");
        }
        return "redirect:/ready-for-sale";
    }

    @GetMapping("/{chassisNo}")
    public String form(
            @PathVariable String chassisNo,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/ready-for-sale";
        }
        if (!preparationPipelineService.isEligibleForSale(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Complete the preparation pipeline before listing this vehicle for sale.");
            return "redirect:/workshop-yard/" + encodeChassis(chassisNo);
        }
        SaleListing record = saleListingService.prepareForm(chassisNo);
        PrepStatus status = preparationPipelineService.statusFor(vehicle);
        model.addAttribute("pageTitle", "Ready for sale pipeline");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPrepComplete());
        return "ready/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute SaleListing record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            saleListingService.save(record);
            if (record.isListed()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Vehicle is listed and ready for sale.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Sale listing saved.");
            }
            return "redirect:/ready-for-sale/" + encodeChassis(chassisNo);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/ready-for-sale/" + encodeChassis(chassisNo);
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }

    public static final class SaleRow {
        private final Vehicle vehicle;
        private final PrepStatus prepStatus;
        private final SaleListing listing;

        public SaleRow(Vehicle vehicle, PrepStatus prepStatus, SaleListing listing) {
            this.vehicle = vehicle;
            this.prepStatus = prepStatus;
            this.listing = listing;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public PrepStatus getPrepStatus() {
            return prepStatus;
        }

        public SaleListing getListing() {
            return listing;
        }

        public boolean isListed() {
            return listing != null && listing.isListed();
        }
    }
}
