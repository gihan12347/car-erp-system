package com.carsale.erp.preparationpipeline.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.carsale.erp.preparationpipeline.dto.YardVehicleCard;
import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.service.YardBayService;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.service.YardService;
import com.carsale.erp.preparationpipeline.util.Yard;
import com.carsale.erp.readypipeline.util.Period;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.carsale.erp.readypipeline.controller.ReadyForSaleController;
import com.carsale.erp.readypipeline.util.ReadyStageUrls;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;
import com.carsale.erp.shared.vehicle.VehicleSummary;
import com.carsale.erp.shared.vehicle.VehicleSummaryService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/yards")
public class YardHubController {

    private final YardBayService yardBayService;
    private final YardService yardService;
    private final VehicleService vehicleService;
    private final VehicleSummaryService vehicleSummaryService;
    private final SaleListingService saleListingService;
    private final SaleLocationService saleLocationService;

    @GetMapping
    public String yards(
            @RequestParam(value = "view", required = false) String view,
            Model model
    ) {
        boolean vehicleView = view != null && "vehicles".equalsIgnoreCase(view.trim());
        model.addAttribute("pageTitle", "Yard");
        model.addAttribute("activeMenu", "yards");
        model.addAttribute("yards", yardBayService.listAll());
        model.addAttribute("allocated", yardService.listAllocated());
        model.addAttribute("vehicleView", vehicleView);
        return "yard-hub/list";
    }

    @GetMapping("/{yardCode}")
    public String vehicles(
            @PathVariable String yardCode,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "page", required = false) String page,
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
            cards.add(new YardVehicleCard(
                    vehicle,
                    yardService.coverPhoto(vehicle.getChassisNo()),
                    yard.getBayCode(),
                    Yard.yardTitle(yard),
                    "/yards/" + PipelineStageUtils.encode(yard.getBayCode())
                            + "/" + PipelineStageUtils.encode(vehicle.getChassisNo()),
                    record.getArrivalDate()
            ));
        }
        Collections.sort(cards, new Comparator<YardVehicleCard>() {
            @Override
            public int compare(YardVehicleCard left, YardVehicleCard right) {
                return VehicleService.compareNewestFirst(left.getVehicle(), right.getVehicle());
            }
        });
        List<YardBay> destYards = new ArrayList<>();
        for (YardBay other : yardBayService.listAll()) {
            if (other != null && other.getBayCode() != null
                    && !other.getBayCode().equalsIgnoreCase(yard.getBayCode())) {
                destYards.add(other);
            }
        }
        model.addAttribute("pageTitle", Yard.yardTitle(yard) + " · Yard");
        model.addAttribute("activeMenu", "yards");
        model.addAttribute("yard", yard);
        model.addAttribute("cards", cards);
        model.addAttribute("assignable", yardService.listAssignable());
        model.addAttribute("destYards", destYards);
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
        VehicleSummary summary = vehicleSummaryService.build(chassisNo);
        if (summary == null) {
            redirectAttributes.addFlashAttribute("error", "Vehicle is not in this yard.");
            return "redirect:/yards";
        }
        SaleListing listing = saleListingService.prepareForm(chassisNo);
        model.addAttribute("pageTitle", Yard.vehicleLabel(vehicle));
        model.addAttribute("activeMenu", "yards");
        model.addAttribute("yard", yard);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("summary", summary);
        model.addAttribute("record", listing);
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

    @PostMapping("/{yardCode}/assign")
    public String assign(
            @PathVariable String yardCode,
            @RequestParam(value = "chassisNos", required = false) List<String> chassisNos,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int assigned = yardService.assignVehicles(yardCode, chassisNos);
            redirectAttributes.addFlashAttribute("notice",
                    assigned == 1
                            ? "1 vehicle assigned to the yard."
                            : assigned + " vehicles assigned to the yard.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + safeReturn(returnTo, "/yards/" + PipelineStageUtils.encode(yardCode));
    }

    @PostMapping("/{yardCode}/{chassisNo}/remove")
    public String remove(
            @PathVariable String yardCode,
            @PathVariable String chassisNo,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            yardService.removeFromYard(yardCode, chassisNo);
            redirectAttributes.addFlashAttribute("notice", "Vehicle removed from the yard.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + safeReturn(returnTo, "/yards/" + PipelineStageUtils.encode(yardCode));
    }

    @PostMapping("/{yardCode}/{chassisNo}/transfer")
    public String transfer(
            @PathVariable String yardCode,
            @PathVariable String chassisNo,
            @RequestParam(value = "destYardCode", required = false) String destYardCode,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            yardService.transferVehicle(yardCode, chassisNo, destYardCode);
            redirectAttributes.addFlashAttribute("notice", "Vehicle transferred.");
            if (destYardCode != null && !destYardCode.trim().isEmpty()
                    && (returnTo == null || returnTo.trim().isEmpty())) {
                return "redirect:/yards/" + PipelineStageUtils.encode(destYardCode.trim());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + safeReturn(returnTo, "/yards/" + PipelineStageUtils.encode(yardCode));
    }

    private static String safeReturn(String returnTo, String fallback) {
        if (returnTo == null) {
            return fallback;
        }
        String path = returnTo.trim();
        if (path.startsWith("/yards")) {
            return path;
        }
        return fallback;
    }

}
