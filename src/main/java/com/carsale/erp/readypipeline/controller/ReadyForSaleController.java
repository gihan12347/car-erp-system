package com.carsale.erp.readypipeline.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.readypipeline.service.VehicleRegistrationService;
import com.carsale.erp.readypipeline.util.Period;
import com.carsale.erp.readypipeline.util.ReadyStageUrls;
import com.carsale.erp.readypipeline.util.SaleGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import com.carsale.erp.preparationpipeline.util.Yard;
import com.carsale.erp.shared.dto.SaleProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.vehicle.VehicleSummary;
import com.carsale.erp.shared.vehicle.VehicleSummaryService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ready-for-sale")
public class ReadyForSaleController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingService saleListingService;
    private final SaleLocationService saleLocationService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final VehiclePhotoService vehiclePhotoService;
    private final VehicleSummaryService vehicleSummaryService;
    private final PipelineStageService pipelineStageService;

    @GetMapping
    public String hub(
            @RequestParam(value = "view", required = false) String view,
            Model model
    ) {
        boolean vehicleView = view != null && "vehicles".equalsIgnoreCase(view.trim());
        List<SaleLocation> sales = saleLocationService.listAll();
        SaleGroup unassigned = null;
        for (SaleGroup group : SaleGroup.group(sales, rows())) {
            if (group != null && group.isUnassigned()) {
                unassigned = group;
                break;
            }
        }
        model.addAttribute("pageTitle", "Sale");
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("sales", sales);
        model.addAttribute("unassigned", unassigned);
        model.addAttribute("allocated", cards(null, ReadyStageUrls.PERIOD_ALL));
        model.addAttribute("vehicleView", vehicleView);
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
        String activeTab = normalizeStatus(tab);
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
            if ("all".equals(activeTab) || activeTab.equals(card.getTab())) {
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

    @PostMapping("/{chassisNo}/skip")
    public String skip(
            @PathVariable String chassisNo,
            @RequestParam("stage") String stage,
            RedirectAttributes redirectAttributes
    ) {
        try {
            saleListingService.skipStage(chassisNo, stage);
            redirectAttributes.addFlashAttribute("successMessage", "Stage skipped.");
            return "redirect:" + redirectAfterReadySkip(chassisNo, stage.trim());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            String editUrl = PipelineStageService.editUrlFor(PipelineStageUtils.encode(chassisNo), stage);
            return "redirect:" + (editUrl != null ? editUrl : "/ready-for-sale");
        }
    }

    private String redirectAfterReadySkip(String chassisNo, String stage) {
        List<String> keys = pipelineStageService.keys(PipelineStageService.FLOW_READY);
        int index = keys.indexOf(stage);
        if (index >= 0 && index < keys.size() - 1) {
            String nextKey = keys.get(index + 1);
            if (!FlowStage.DETAILS.getStageKey().equals(nextKey)
                    && saleListingService.isStageSatisfied(chassisNo, nextKey)) {
                String editUrl = PipelineStageService.editUrlFor(PipelineStageUtils.encode(chassisNo), nextKey);
                if (editUrl != null) {
                    return editUrl;
                }
            }
        }
        SaleListing listing = saleListingService.findByChassisNo(chassisNo);
        String saleCode = listing == null ? null : listing.getSaleCode();
        return ReadyStageUrls.saleVehicle(saleCode, chassisNo);
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

        VehicleSummary summary = vehicleSummaryService.build(chassisNo);
        if (summary == null) {
            return "redirect:" + ReadyStageUrls.saleLocation(saleCode);
        }
        SaleProgress status = saleListingService.progressFor(chassisNo);
        SaleListing listing = saleListingService.listingForView(chassisNo);
        SaleRow row = new SaleRow(vehicle, status, listing);
        String tab = row.getTab();
        SaleLocation sale = saleLocationService.findByCode(saleCode);
        String saleTitle = sale != null && sale.getSaleName() != null && !sale.getSaleName().trim().isEmpty()
                ? sale.getSaleName()
                : saleCode;
        model.addAttribute("pageTitle", vehicleLabel(vehicle));
        model.addAttribute("activeMenu", "ready-for-sale");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("summary", summary);
        model.addAttribute("listing", listing);
        model.addAttribute("registration", vehicleRegistrationService.findByChassisNo(chassisNo));
        model.addAttribute("saleCode", saleCode);
        model.addAttribute("saleTitle", saleTitle);
        model.addAttribute("tab", tab);
        model.addAttribute("sold", row.isSold());
        model.addAttribute("registered", row.isRegistered());
        model.addAttribute("canRegister", !row.isSold() && !row.isRegistered());
        model.addAttribute("canSell", !row.isSold());
        return "ready-for-sale/detail";
    }

    private List<SaleVehicleCard> cards(String query, String period) {
        Map<String, String> titles = saleTitles();
        List<SaleVehicleCard> cards = new ArrayList<>();
        for (Vehicle vehicle : preparationProgressService.listReadyForSale(query)) {
            SaleProgress status = saleListingService.progressFor(vehicle.getChassisNo());
            SaleListing listing = saleListingService.findByChassisNo(vehicle.getChassisNo());
            if (Period.matchesPeriod(listing == null ? null : listing.getListedOn(), period)) {
                continue;
            }
            SaleRow row = new SaleRow(vehicle, status, listing);
            String code = listing == null ? null : listing.getSaleCode();
            cards.add(new SaleVehicleCard(
                    vehicle,
                    listing,
                    coverPhoto(vehicle.getChassisNo()),
                    row.getTab(),
                    titleFor(titles, code)
            ));
        }
        Collections.sort(cards, new Comparator<SaleVehicleCard>() {
            @Override
            public int compare(SaleVehicleCard left, SaleVehicleCard right) {
                return VehicleService.compareNewestFirst(left.getVehicle(), right.getVehicle());
            }
        });
        return cards;
    }

    private Map<String, String> saleTitles() {
        Map<String, String> titles = new HashMap<String, String>();
        for (SaleLocation sale : saleLocationService.listAll()) {
            if (sale == null || sale.getSaleCode() == null || sale.getSaleCode().trim().isEmpty()) {
                continue;
            }
            titles.put(sale.getSaleCode().trim().toUpperCase(Locale.ROOT), saleTitle(sale, sale.getSaleCode()));
        }
        return titles;
    }

    private static String titleFor(Map<String, String> titles, String code) {
        if (code == null || code.trim().isEmpty()) {
            return "Unassigned";
        }
        String title = titles.get(code.trim().toUpperCase(Locale.ROOT));
        return title == null || title.trim().isEmpty() ? code.trim() : title;
    }

    private static String normalizeStatus(String tab) {
        if (tab == null || tab.trim().isEmpty() || "all".equalsIgnoreCase(tab.trim())) {
            return "all";
        }
        return ReadyStageUrls.normalizeTab(tab);
    }

    private List<SaleRow> rows() {
        List<SaleRow> rows = new ArrayList<>();
        for (Vehicle vehicle : preparationProgressService.listReadyForSale(null)) {
            SaleListing listing = saleListingService.findByChassisNo(vehicle.getChassisNo());
            if (Period.matchesPeriod(listing == null ? null : listing.getListedOn(), ReadyStageUrls.PERIOD_ALL)) {
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

    @Getter
    public static final class SaleVehicleCard {
        private final Vehicle vehicle;
        private final SaleListing listing;
        private final VehiclePhoto photo;
        private final String tab;
        private final String saleTitle;

        public SaleVehicleCard(
                Vehicle vehicle,
                SaleListing listing,
                VehiclePhoto photo,
                String tab,
                String saleTitle
        ) {
            this.vehicle = vehicle;
            this.listing = listing;
            this.photo = photo;
            this.tab = tab;
            this.saleTitle = saleTitle == null || saleTitle.trim().isEmpty() ? "Unassigned" : saleTitle;
        }

        public String getMakeModel() {
            return Yard.makeModel(vehicle);
        }

        public String getStatusLabel() {
            if (ReadyStageUrls.TAB_SOLD.equals(tab)) {
                return "Sold";
            }
            if (ReadyStageUrls.TAB_REGISTERED.equals(tab)) {
                return "Registered";
            }
            return "Unregistered";
        }

        public String getStatusTone() {
            if (ReadyStageUrls.TAB_SOLD.equals(tab)) {
                return "rust";
            }
            if (ReadyStageUrls.TAB_REGISTERED.equals(tab)) {
                return "sea";
            }
            return "gold";
        }

        public String getSaleCode() {
            if (listing == null || listing.getSaleCode() == null || listing.getSaleCode().trim().isEmpty()) {
                return SaleGroup.UNASSIGNED_KEY;
            }
            return listing.getSaleCode();
        }

        public String getListedOn() {
            return listing == null ? null : listing.getListedOn();
        }

        public boolean isSold() {
            return ReadyStageUrls.TAB_SOLD.equals(tab);
        }

        public String getSearchText() {
            StringBuilder text = new StringBuilder();
            if (vehicle != null) {
                appendSearch(text, vehicle.getChassisNo());
                appendSearch(text, vehicle.getStockNo());
                appendSearch(text, vehicle.getMake());
                appendSearch(text, vehicle.getModel());
                appendSearch(text, vehicle.getYear());
                appendSearch(text, vehicle.getColor());
            }
            appendSearch(text, getSaleCode());
            appendSearch(text, saleTitle);
            appendSearch(text, getStatusLabel());
            return text.toString().trim().toLowerCase(Locale.ROOT);
        }

        private static void appendSearch(StringBuilder text, String value) {
            if (value == null || value.trim().isEmpty()) {
                return;
            }
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(value.trim());
        }
    }

    @Getter
    @AllArgsConstructor
    public static final class SaleRow {
        private final Vehicle vehicle;
        private final SaleProgress status;
        private final SaleListing listing;

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
