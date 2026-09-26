package com.carsale.erp.preparationpipeline.controller;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.preparationpipeline.dto.YardVehicleCard;
import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.service.YardBayService;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.service.YardService;
import com.carsale.erp.preparationpipeline.util.Yard;
import com.carsale.erp.readypipeline.util.Period;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.readypipeline.controller.ReadyForSaleController;
import com.carsale.erp.readypipeline.util.ReadyStageUrls;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/yards")
public class YardHubController {

    private final YardBayService yardBayService;
    private final YardService yardService;
    private final VehicleService vehicleService;
    private final VehiclePhotoService vehiclePhotoService;
    private final SaleListingService saleListingService;
    private final SaleLocationService saleLocationService;

    public YardHubController(
            YardBayService yardBayService,
            YardService yardService,
            VehicleService vehicleService,
            VehiclePhotoService vehiclePhotoService,
            SaleListingService saleListingService,
            SaleLocationService saleLocationService
    ) {
        this.yardBayService = yardBayService;
        this.yardService = yardService;
        this.vehicleService = vehicleService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.saleListingService = saleListingService;
        this.saleLocationService = saleLocationService;
    }

    @GetMapping
    public String yards(Model model) {
        model.addAttribute("pageTitle", "Yard");
        model.addAttribute("activeMenu", "yards");
        model.addAttribute("yards", yardBayService.listAll());
        return "yard-hub/list";
    }

    @GetMapping("/{yardCode}")
    public String vehicles(
            @PathVariable String yardCode,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "period", required = false) String period,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        YardBay yard = yardBayService.findByCode(yardCode);
        if (yard == null) {
            redirectAttributes.addFlashAttribute("error", "Unknown yard.");
            return "redirect:/yards";
        }
        String searchQuery = query == null ? "" : query.trim();
        String activePeriod = ReadyForSaleController.normalizePeriod(period);
        List<YardVehicleCard> cards = new ArrayList<>();
        for (YardRecord record : yardBayService.listPresentInYard(yard.getBayCode())) {
            Vehicle vehicle = vehicleService.findByChassisNo(record.getChassisNo());
            if (vehicle == null) {
                continue;
            }
            if (!Yard.matchesQuery(vehicle, searchQuery)) {
                continue;
            }
            if (Period.matchesPeriod(record.getArrivalDate(), activePeriod)) {
                continue;
            }
            cards.add(new YardVehicleCard(vehicle, yardService.coverPhoto(vehicle.getChassisNo())));
        }
        model.addAttribute("pageTitle", Yard.yardTitle(yard) + " · Yard");
        model.addAttribute("activeMenu", "yards");
        model.addAttribute("yard", yard);
        model.addAttribute("cards", cards);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("period", activePeriod);
        return "yard-hub/vehicles";
    }

    @GetMapping("/{yardCode}/{chassisNo}")
    public String detail(
            @PathVariable String yardCode,
            @PathVariable String chassisNo,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        YardBay yard = yardBayService.findByCode(yardCode);
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        YardRecord record = yardService.findByChassisNo(chassisNo);
        if (yard == null || vehicle == null || record == null
                || record.getBayNo() == null
                || !record.getBayNo().equalsIgnoreCase(yard.getBayCode())) {
            redirectAttributes.addFlashAttribute("error", "Vehicle is not in this yard.");
            return "redirect:/yards";
        }
        if (saleListingService.isAssignedToSale(chassisNo)) {
            SaleListing assigned = saleListingService.findByChassisNo(chassisNo);
            String saleCode = assigned == null ? null : assigned.getSaleCode();
            return "redirect:" + ReadyStageUrls.saleVehicle(saleCode, chassisNo);
        }
        SaleListing listing = saleListingService.prepareForm(chassisNo);
        model.addAttribute("pageTitle", Yard.vehicleLabel(vehicle));
        model.addAttribute("activeMenu", "yards");
        model.addAttribute("yard", yard);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", listing);
        model.addAttribute("photos", vehiclePhotoService.list(chassisNo));
        model.addAttribute("saleLocations", saleLocationService.listActive());
        return "yard-hub/detail";
    }

    @PostMapping("/{yardCode}/{chassisNo}/move")
    public String moveToSale(
            @PathVariable String yardCode,
            @PathVariable String chassisNo,
            @ModelAttribute SaleListing record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        record.setAssignmentComplete(true);
        try {
            saleListingService.saveAssignment(record);
            redirectAttributes.addFlashAttribute("notice", "Vehicle moved to sale.");
            return "redirect:" + ReadyStageUrls.saleVehicles(record.getSaleCode(), "unregistered");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/yards/" + PipelineStageUtils.encode(yardCode) + "/" + PipelineStageUtils.encode(chassisNo);
        }
    }

}
