package com.carsale.erp.readypipeline.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.readypipeline.service.VehicleRegistrationService;
import com.carsale.erp.readypipeline.util.Period;
import com.carsale.erp.readypipeline.util.ReadyStageUrls;
import com.carsale.erp.readypipeline.util.SaleGroup;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.readypipeline.service.SaleListingService.SaleProgress;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;
import com.carsale.erp.shared.vehicle.VehicleStage;

@Controller
@RequestMapping("/ready-for-sale")
public class ReadyForSaleController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingService saleListingService;
    private final SaleLocationService saleLocationService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final VehiclePhotoService vehiclePhotoService;

    public ReadyForSaleController(
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            SaleListingService saleListingService,
            SaleLocationService saleLocationService,
            VehicleRegistrationService vehicleRegistrationService,
            VehiclePhotoService vehiclePhotoService
    ) {
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.saleListingService = saleListingService;
        this.saleLocationService = saleLocationService;
        this.vehicleRegistrationService = vehicleRegistrationService;
        this.vehiclePhotoService = vehiclePhotoService;
    }

    @GetMapping
    public String hub(Model model) {
        List<SaleGroup> groups = SaleGroup.group(saleLocationService.listAll(), rows(null, ReadyStageUrls.PERIOD_ALL));
        model.addAttribute("pageTitle", "Sale");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("groups", groups);
        return "ready-for-sale/groups";
    }

    @GetMapping("/pipeline")
    public String pipeline(@RequestParam(value = "sale", required = false) String saleCode) {
        if (saleCode != null && !saleCode.trim().isEmpty()) {
            return "redirect:" + ReadyStageUrls.saleLocation(saleCode);
        }
        return "redirect:/ready-for-sale";
    }

    @GetMapping("/{saleCode}")
    public String vehicles(
            @PathVariable String saleCode,
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "period", required = false) String period,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String bounce = bounceIfVehiclePath(saleCode);
        if (bounce != null) {
            return bounce;
        }
        SaleLocation sale = saleLocationService.findByCode(saleCode);
        if (sale == null && !SaleGroup.UNASSIGNED_KEY.equalsIgnoreCase(saleCode)) {
            redirectAttributes.addFlashAttribute("error", "Unknown sale.");
            return "redirect:/ready-for-sale";
        }
        String activeTab = ReadyStageUrls.normalizeTab(tab);
        String activePeriod = normalizePeriod(period);
        List<SaleVehicleCard> all = new ArrayList<>();
        for (SaleVehicleCard card : cards(query, activePeriod)) {
            if (!SaleGroup.matches(toRow(card), saleCode)) {
                continue;
            }
            all.add(card);
        }
        List<SaleVehicleCard> cards = new ArrayList<>();
        int unregistered = 0;
        int registered = 0;
        int sold = 0;
        for (SaleVehicleCard card : all) {
            if (ReadyStageUrls.TAB_SOLD.equals(card.getTab())) {
                sold++;
            } else if (ReadyStageUrls.TAB_REGISTERED.equals(card.getTab())) {
                registered++;
            } else {
                unregistered++;
            }
            if (activeTab.equals(card.getTab())) {
                cards.add(card);
            }
        }
        model.addAttribute("pageTitle", saleTitle(sale, saleCode) + " · Sale");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("sale", sale);
        model.addAttribute("saleCode", sale == null ? saleCode : sale.getSaleCode());
        model.addAttribute("saleTitle", saleTitle(sale, saleCode));
        model.addAttribute("tab", activeTab);
        model.addAttribute("period", activePeriod);
        model.addAttribute("cards", cards);
        model.addAttribute("unregisteredCount", unregistered);
        model.addAttribute("registeredCount", registered);
        model.addAttribute("soldCount", sold);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "ready-for-sale/vehicles";
    }

    @PostMapping("/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        SaleListing listing = saleListingService.findByChassisNo(chassisNo);
        String saleCode = listing == null ? null : listing.getSaleCode();
        if (vehicleService.deleteFromFlow(chassisNo, PipelineStageService.FLOW_READY)) {
            redirectAttributes.addFlashAttribute("notice", "Sale listing deleted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete the sale listing.");
        }
        return "redirect:" + ReadyStageUrls.saleLocation(saleCode);
    }

    @GetMapping("/{saleCode}/{chassisNo}")
    public String view(
            @PathVariable String saleCode,
            @PathVariable String chassisNo,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:" + ReadyStageUrls.saleLocation(saleCode);
        }
        if (!preparationProgressService.isEligibleForSale(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Move this vehicle from the yard to an available sale first.");
            return "redirect:/yards";
        }

        SaleProgress status = saleListingService.progressFor(chassisNo);
        SaleListing listing = saleListingService.findByChassisNo(chassisNo);
        SaleRow row = new SaleRow(vehicle, status, listing);
        String tab = row.getTab();
        model.addAttribute("pageTitle", vehicleLabel(vehicle));
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("listing", listing);
        model.addAttribute("registration", vehicleRegistrationService.findByChassisNo(chassisNo));
        model.addAttribute("photos", vehiclePhotoService.list(chassisNo));
        model.addAttribute("saleCode", saleCode);
        model.addAttribute("tab", tab);
        model.addAttribute("sold", row.isSold());
        model.addAttribute("registered", row.isRegistered());
        model.addAttribute("canRegister", !row.isSold() && !row.isRegistered());
        model.addAttribute("canSell", !row.isSold());
        return "ready-for-sale/detail";
    }

    private List<SaleVehicleCard> cards(String query, String period) {
        List<SaleVehicleCard> cards = new ArrayList<>();
        for (Vehicle vehicle : preparationProgressService.listReadyForSale(query)) {
            SaleProgress status = saleListingService.progressFor(vehicle.getChassisNo());
            SaleListing listing = saleListingService.findByChassisNo(vehicle.getChassisNo());
            if (Period.matchesPeriod(listing == null ? null : listing.getListedOn(), period)) {
                continue;
            }
            SaleRow row = new SaleRow(vehicle, status, listing);
            cards.add(new SaleVehicleCard(vehicle, listing, coverPhoto(vehicle.getChassisNo()), row.getTab()));
        }
        return cards;
    }

    private List<SaleRow> rows(String query, String period) {
        List<SaleRow> rows = new ArrayList<>();
        for (Vehicle vehicle : preparationProgressService.listReadyForSale(query)) {
            SaleListing listing = saleListingService.findByChassisNo(vehicle.getChassisNo());
            if (Period.matchesPeriod(listing == null ? null : listing.getListedOn(), period)) {
                continue;
            }
            SaleProgress status = saleListingService.progressFor(vehicle.getChassisNo());
            rows.add(new SaleRow(vehicle, status, listing));
        }
        return rows;
    }

    private static SaleRow toRow(SaleVehicleCard card) {
        return new SaleRow(card.getVehicle(), null, card.getListing());
    }

    private String bounceIfVehiclePath(String saleCode) {
        if (saleLocationService.findByCode(saleCode) != null) {
            return null;
        }
        if (SaleGroup.UNASSIGNED_KEY.equalsIgnoreCase(saleCode)) {
            return null;
        }
        Vehicle vehicle = vehicleService.findByChassisNo(saleCode);
        if (vehicle == null) {
            return null;
        }
        SaleListing listing = saleListingService.findByChassisNo(saleCode);
        String code = listing == null ? null : listing.getSaleCode();
        return "redirect:" + ReadyStageUrls.saleVehicle(code, saleCode);
    }

    private static String saleTitle(SaleLocation sale, String saleCode) {
        if (sale != null && sale.getSaleName() != null && !sale.getSaleName().trim().isEmpty()) {
            return sale.getSaleName();
        }
        if (SaleGroup.UNASSIGNED_KEY.equalsIgnoreCase(saleCode)) {
            return "Unassigned";
        }
        if (sale != null && sale.getSaleCode() != null) {
            return sale.getSaleCode();
        }
        return saleCode;
    }

    private VehiclePhoto coverPhoto(String chassisNo) {
        List<VehiclePhoto> photos = vehiclePhotoService.list(chassisNo);
        return photos.isEmpty() ? null : photos.get(0);
    }

    public static String normalizePeriod(String period) {
        if (period == null) {
            return ReadyStageUrls.PERIOD_ALL;
        }
        String value = period.trim().toLowerCase();
        if ("today".equals(value)
                || "week".equals(value)
                || "month".equals(value)
                || "quarter".equals(value)
                || "year".equals(value)) {
            return value;
        }
        return ReadyStageUrls.PERIOD_ALL;
    }

    private static String vehicleLabel(Vehicle vehicle) {
        if (vehicle.getModel() != null && !vehicle.getModel().trim().isEmpty()) {
            return vehicle.getModel();
        }
        return vehicle.getChassisNo();
    }

    public static final class SaleVehicleCard {
        private final Vehicle vehicle;
        private final SaleListing listing;
        private final VehiclePhoto photo;
        private final String tab;

        public SaleVehicleCard(Vehicle vehicle, SaleListing listing, VehiclePhoto photo, String tab) {
            this.vehicle = vehicle;
            this.listing = listing;
            this.photo = photo;
            this.tab = tab;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public SaleListing getListing() {
            return listing;
        }

        public VehiclePhoto getPhoto() {
            return photo;
        }

        public String getTab() {
            return tab;
        }
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
            return listing != null && listing.isListed() && !isSold();
        }

        public boolean isSold() {
            if (listing != null && listing.isSold()) {
                return true;
            }
            return vehicle != null && vehicle.getStage() == VehicleStage.SOLD;
        }

        public boolean isRegistered() {
            return status != null && status.isRegistrationReady();
        }

        public String getTab() {
            if (isSold()) {
                return ReadyStageUrls.TAB_SOLD;
            }
            if (isRegistered()) {
                return ReadyStageUrls.TAB_REGISTERED;
            }
            return ReadyStageUrls.TAB_UNREGISTERED;
        }
    }
}
