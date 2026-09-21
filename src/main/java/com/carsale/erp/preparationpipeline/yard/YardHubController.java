package com.carsale.erp.preparationpipeline.yard;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

import com.carsale.erp.importpipeline.photos.VehiclePhoto;
import com.carsale.erp.importpipeline.photos.VehiclePhotoService;
import com.carsale.erp.readypipeline.ReadyForSaleController;
import com.carsale.erp.readypipeline.ReadyStageUrls;
import com.carsale.erp.readypipeline.SaleListing;
import com.carsale.erp.readypipeline.SaleListingService;
import com.carsale.erp.readypipeline.SaleLocationService;
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
            if (!matchesQuery(vehicle, searchQuery)) {
                continue;
            }
            if (!ReadyForSaleController.matchesPeriod(record.getArrivalDate(), activePeriod)) {
                continue;
            }
            cards.add(new YardVehicleCard(vehicle, record, coverPhoto(vehicle.getChassisNo())));
        }
        model.addAttribute("pageTitle", yardTitle(yard) + " · Yard");
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
        model.addAttribute("pageTitle", vehicleLabel(vehicle));
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
            return "redirect:/yards/" + encode(yardCode) + "/" + encode(chassisNo);
        }
    }

    private VehiclePhoto coverPhoto(String chassisNo) {
        List<VehiclePhoto> photos = vehiclePhotoService.list(chassisNo);
        return photos.isEmpty() ? null : photos.get(0);
    }

    private static String yardTitle(YardBay yard) {
        if (yard.getYardName() != null && !yard.getYardName().trim().isEmpty()) {
            return yard.getYardName();
        }
        return yard.getBayCode();
    }

    private static String vehicleLabel(Vehicle vehicle) {
        if (vehicle.getModel() != null && !vehicle.getModel().trim().isEmpty()) {
            return vehicle.getModel();
        }
        return vehicle.getChassisNo();
    }

    private static String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private static boolean matchesQuery(Vehicle vehicle, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return contains(vehicle.getChassisNo(), needle)
                || contains(vehicle.getModel(), needle)
                || contains(vehicle.getMake(), needle)
                || contains(vehicle.getLotNo(), needle)
                || contains(vehicle.getStockNo(), needle)
                || contains(vehicle.getColor(), needle)
                || contains(vehicle.getYear(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    public static final class YardVehicleCard {
        private final Vehicle vehicle;
        private final YardRecord yardRecord;
        private final VehiclePhoto photo;

        public YardVehicleCard(Vehicle vehicle, YardRecord yardRecord, VehiclePhoto photo) {
            this.vehicle = vehicle;
            this.yardRecord = yardRecord;
            this.photo = photo;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public YardRecord getYardRecord() {
            return yardRecord;
        }

        public VehiclePhoto getPhoto() {
            return photo;
        }
    }
}
