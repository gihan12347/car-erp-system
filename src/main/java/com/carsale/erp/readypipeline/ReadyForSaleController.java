package com.carsale.erp.readypipeline;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.importpipeline.photos.VehiclePhotoService;
import com.carsale.erp.preparationpipeline.PreparationProgressService;
import com.carsale.erp.readypipeline.SaleListingService.SaleProgress;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

@Controller
@RequestMapping("/ready-for-sale")
public class ReadyForSaleController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingService saleListingService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final VehiclePhotoService vehiclePhotoService;
    private final PipelineStageService pipelineStageService;

    public ReadyForSaleController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            SaleListingService saleListingService,
            VehicleRegistrationService vehicleRegistrationService,
            VehiclePhotoService vehiclePhotoService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.saleListingService = saleListingService;
        this.vehicleRegistrationService = vehicleRegistrationService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = preparationProgressService.listReadyForSale(query);
        List<SaleRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            rows.add(new SaleRow(
                    vehicle,
                    saleListingService.progressFor(vehicle.getChassisNo()),
                    saleListingService.findByChassisNo(vehicle.getChassisNo())
            ));
        }
        model.addAttribute("pageTitle", "Sale pipeline");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("rows", rows);
        model.addAttribute("pipelineStages", pipelineStageService.list(PipelineStageService.FLOW_READY));
        model.addAttribute("pipelineKeys", pipelineStageService.keys(PipelineStageService.FLOW_READY));
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
    public String view(
            @PathVariable String chassisNo,
            @RequestParam(value = "stage", required = false) String requestedStage,
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

        SaleProgress status = saleListingService.progressFor(chassisNo);
        List<String> saleKeys = pipelineStageService.keys(PipelineStageService.FLOW_READY);
        int stageIndex = ReadyStageUrls.clampStageIndex(
                PipelineStageService.parseStageIndex(saleKeys, requestedStage),
                saleKeys
        );
        String stageKey = PipelineStageService.keyAt(saleKeys, stageIndex);
        if (stageKey == null) {
            return "redirect:/ready-for-sale";
        }

        List<PipelineStage> saleStages = pipelineStageService.list(PipelineStageService.FLOW_READY);
        NavLinks nav = viewLinks(chassisNo, stageIndex, status, saleStages, FlowPipeline.READY);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("listing", saleListingService.findByChassisNo(chassisNo));
        model.addAttribute("registration", vehicleRegistrationService.findByChassisNo(chassisNo));
        model.addAttribute("photos", vehiclePhotoService.list(chassisNo));
        model.addAttribute("detailsReady", status.isDetailsReady());
        model.addAttribute("listingReady", status.isListingReady());
        model.addAttribute("registrationReady", status.isRegistrationReady());
        model.addAttribute("saleSold", status.isSold());
        model.addAttribute("saleComplete", status.isPipelineCompleted());
        model.addAttribute("stageIndex", stageIndex);
        model.addAttribute("stageKey", stageKey);
        model.addAttribute("stageNav", nav);
        return "ready-for-sale/detail";
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }

    public static final class SaleRow {
        private final Vehicle vehicle;
        private final SaleProgress status;
        private final SaleListing listing;

        public SaleRow(Vehicle vehicle, SaleProgress status, SaleListing listing) {
            this.vehicle = vehicle;
            this.status = status;
            this.listing = listing;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public SaleProgress getStatus() {
            return status;
        }

        public SaleListing getListing() {
            return listing;
        }

        public boolean isListed() {
            return listing != null && listing.isListed() && !listing.isSold();
        }

        public boolean isSold() {
            return listing != null && listing.isSold();
        }
    }
}
