package com.carsale.erp.readypipeline;

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

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.preparationpipeline.PreparationProgressService;
import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/ready-for-sale")
public class ReadyForSaleController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingService saleListingService;

    public ReadyForSaleController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            SaleListingService saleListingService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.saleListingService = saleListingService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = preparationProgressService.listReadyForSale(query);
        List<SaleRow> rows = new ArrayList<SaleRow>();
        for (Vehicle vehicle : vehicles) {
            rows.add(new SaleRow(
                    vehicle,
                    preparationProgressService.progressFor(vehicle),
                    saleListingService.findByChassisNo(vehicle.getChassisNo())
            ));
        }
        model.addAttribute("pageTitle", "Ready for sale pipeline");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("rows", rows);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "ready-for-sale/list";
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
        if (!preparationProgressService.isEligibleForSale(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Complete the preparation pipeline before listing this vehicle for sale.");
            return "redirect:/workshop-yard/" + encodeChassis(chassisNo);
        }
        SaleListing record = saleListingService.prepareForm(chassisNo);
        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", "Ready for sale pipeline");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        return "ready-for-sale/form";
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
        private final PreparationProgress prepStatus;
        private final SaleListing listing;

        public SaleRow(Vehicle vehicle, PreparationProgress prepStatus, SaleListing listing) {
            this.vehicle = vehicle;
            this.prepStatus = prepStatus;
            this.listing = listing;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public PreparationProgress getPrepStatus() {
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
