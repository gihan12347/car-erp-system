package com.carsale.erp.report;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.customspipeline.model.CustomsDocument;
import com.carsale.erp.customspipeline.repository.CustomsDocumentRepository;
import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

import java.nio.charset.StandardCharsets;

@Service
public class ReportService {

    public static final String ALL = "all";
    public static final String IMPORT = "import";
    public static final String CUSTOMS = "customs";
    public static final String PREPARATION = "preparation";
    public static final String READY = "ready";
    public static final String LISTED = "listed";
    public static final String SOLD = "sold";

    private final VehicleService vehicleService;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PreparationProgressService preparationProgressService;
    private final SaleListingRepository saleListingRepository;
    private final CustomsDocumentRepository customsDocumentRepository;

    public ReportService(
            VehicleService vehicleService,
            ImportProgressService importProgressService,
            CustomsProgressService customsProgressService,
            PreparationProgressService preparationProgressService,
            SaleListingRepository saleListingRepository,
            CustomsDocumentRepository customsDocumentRepository
    ) {
        this.vehicleService = vehicleService;
        this.importProgressService = importProgressService;
        this.customsProgressService = customsProgressService;
        this.preparationProgressService = preparationProgressService;
        this.saleListingRepository = saleListingRepository;
        this.customsDocumentRepository = customsDocumentRepository;
    }

    @Transactional(readOnly = true)
    public ReportPage build(String query, String section) {
        String search = query == null ? "" : query.trim();
        String selected = normalizeSection(section);
        List<Vehicle> vehicles = vehicleService.search(search);
        vehicles.sort(Comparator.comparing(Vehicle::getChassisNo, Comparator.nullsLast(String::compareToIgnoreCase)));

        Map<String, SaleListing> listings = new HashMap<String, SaleListing>();
        for (SaleListing listing : saleListingRepository.findAll()) {
            if (listing.getChassisNo() != null) {
                listings.put(listing.getChassisNo(), listing);
            }
        }
        Map<String, CustomsDocument> documents = new HashMap<String, CustomsDocument>();
        for (CustomsDocument document : customsDocumentRepository.findAll()) {
            if (document.getChassisNo() != null) {
                documents.put(document.getChassisNo(), document);
            }
        }

        int importCount = 0;
        int customsCount = 0;
        int preparationCount = 0;
        int readyCount = 0;
        int listedCount = 0;
        int soldCount = 0;
        BigDecimal landingTotal = BigDecimal.ZERO;
        BigDecimal askingTotal = BigDecimal.ZERO;
        BigDecimal soldTotal = BigDecimal.ZERO;
        boolean anyLanding = false;
        boolean anyAsking = false;
        boolean anySoldPrice = false;
        Map<String, Integer> makeCounts = new HashMap<String, Integer>();
        List<ReportRow> rows = new ArrayList<ReportRow>();

        for (Vehicle vehicle : vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = listings.get(chassis);
            CustomsDocument document = documents.get(chassis);
            String place = placeOf(vehicle, listing);
            if (IMPORT.equals(place)) {
                importCount++;
            } else if (CUSTOMS.equals(place)) {
                customsCount++;
            } else if (PREPARATION.equals(place)) {
                preparationCount++;
            } else if (READY.equals(place)) {
                readyCount++;
            } else if (LISTED.equals(place)) {
                listedCount++;
            } else if (SOLD.equals(place)) {
                soldCount++;
            }

            String landingRaw = document == null ? null : document.getLandingCostLkr();
            String askingRaw = listing == null ? null : listing.getAskingPrice();
            String advertisedRaw = listing == null ? null : listing.getAdvertisedPrice();
            BigDecimal landing = parseMoney(landingRaw);
            BigDecimal asking = parseMoney(askingRaw);
            BigDecimal advertised = parseMoney(advertisedRaw);
            if (landing != null) {
                landingTotal = landingTotal.add(landing);
                anyLanding = true;
            }
            if (asking != null) {
                askingTotal = askingTotal.add(asking);
                anyAsking = true;
            }
            if (SOLD.equals(place)) {
                BigDecimal soldPrice = advertised != null ? advertised : asking;
                if (soldPrice != null) {
                    soldTotal = soldTotal.add(soldPrice);
                    anySoldPrice = true;
                }
            }

            String make = blankTo(vehicle.getMake(), "Unknown");
            Integer current = makeCounts.get(make);
            makeCounts.put(make, current == null ? 1 : current + 1);

            if (ALL.equals(selected) || selected.equals(place)) {
                rows.add(new ReportRow(
                        chassis,
                        vehicleLabel(vehicle),
                        blankTo(vehicle.getYear(), "—"),
                        place,
                        labelFor(place),
                        pillFor(place),
                        viewPath(place, chassis),
                        displayMoney(landingRaw),
                        displayMoney(askingRaw),
                        displayMoney(advertisedRaw),
                        blankTo(listing == null ? null : listing.getSaleLocation(), "—")
                ));
            }
        }

        List<MakeCount> makes = new ArrayList<MakeCount>();
        for (Map.Entry<String, Integer> entry : makeCounts.entrySet()) {
            makes.add(new MakeCount(entry.getKey(), entry.getValue()));
        }
        Collections.sort(makes, new Comparator<MakeCount>() {
            @Override
            public int compare(MakeCount left, MakeCount right) {
                int byCount = Integer.compare(right.getCount(), left.getCount());
                if (byCount != 0) {
                    return byCount;
                }
                return left.getMake().compareToIgnoreCase(right.getMake());
            }
        });
        if (makes.size() > 8) {
            makes = new ArrayList<MakeCount>(makes.subList(0, 8));
        }

        return new ReportPage(
                search,
                selected,
                vehicles.size(),
                importCount,
                customsCount,
                preparationCount,
                readyCount,
                listedCount,
                soldCount,
                anyLanding ? formatMoney(landingTotal) : "—",
                anyAsking ? formatMoney(askingTotal) : "—",
                anySoldPrice ? formatMoney(soldTotal) : "—",
                rows,
                makes
        );
    }

    private String placeOf(Vehicle vehicle, SaleListing listing) {
        if (listing != null && listing.isSold()) {
            return SOLD;
        }
        if (listing != null && listing.isListed()) {
            return LISTED;
        }
        if (!importProgressService.progressFor(vehicle).isPipelineCompleted()) {
            return IMPORT;
        }
        if (!customsProgressService.progressFor(vehicle).isPipelineCompleted()) {
            return CUSTOMS;
        }
        if (!preparationProgressService.progressFor(vehicle).isPipelineCompleted()) {
            return PREPARATION;
        }
        return READY;
    }

    private static String normalizeSection(String section) {
        if (section == null) {
            return ALL;
        }
        String value = section.trim().toLowerCase();
        if (IMPORT.equals(value) || CUSTOMS.equals(value) || PREPARATION.equals(value)
                || READY.equals(value) || LISTED.equals(value) || SOLD.equals(value)) {
            return value;
        }
        return ALL;
    }

    private static String labelFor(String place) {
        if (IMPORT.equals(place)) {
            return "Import";
        }
        if (CUSTOMS.equals(place)) {
            return "Customs";
        }
        if (PREPARATION.equals(place)) {
            return "Preparation";
        }
        if (READY.equals(place)) {
            return "Ready for sale";
        }
        if (LISTED.equals(place)) {
            return "Listed";
        }
        if (SOLD.equals(place)) {
            return "Sold";
        }
        return "Import";
    }

    private static String pillFor(String place) {
        if (CUSTOMS.equals(place)) {
            return "rust";
        }
        if (PREPARATION.equals(place)) {
            return "gold";
        }
        if (READY.equals(place) || LISTED.equals(place)) {
            return "sea";
        }
        if (SOLD.equals(place)) {
            return "leaf";
        }
        return "muted-pill";
    }

    private static String viewPath(String place, String chassis) {
        String encoded = UriUtils.encodePathSegment(chassis == null ? "" : chassis, StandardCharsets.UTF_8);
        if (CUSTOMS.equals(place)) {
            return "/customs/" + encoded;
        }
        if (PREPARATION.equals(place)) {
            return "/workshop-yard/" + encoded;
        }
        if (READY.equals(place) || LISTED.equals(place) || SOLD.equals(place)) {
            return "/ready-for-sale/" + encoded;
        }
        return "/auction/" + encoded;
    }

    private static String vehicleLabel(Vehicle vehicle) {
        String make = vehicle.getMake() == null ? "" : vehicle.getMake().trim();
        String model = vehicle.getModel() == null ? "" : vehicle.getModel().trim();
        String label = (make + " " + model).trim();
        return label.isEmpty() ? "—" : label;
    }

    private static String blankTo(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static String displayMoney(String raw) {
        BigDecimal parsed = parseMoney(raw);
        if (parsed != null) {
            return formatMoney(parsed);
        }
        if (raw == null || raw.trim().isEmpty()) {
            return "—";
        }
        return raw.trim();
    }

    private static String formatMoney(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return "Rs. " + format.format(value);
    }

    private static BigDecimal parseMoney(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "-".equals(trimmed) || "—".equals(trimmed)) {
            return null;
        }
        String digits = trimmed.replaceAll("[^0-9.]", "");
        if (digits.isEmpty() || ".".equals(digits)) {
            return null;
        }
        int firstDot = digits.indexOf('.');
        if (firstDot >= 0) {
            digits = digits.substring(0, firstDot + 1) + digits.substring(firstDot + 1).replace(".", "");
        }
        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static final class ReportPage {
        private final String searchQuery;
        private final String section;
        private final int total;
        private final int importCount;
        private final int customsCount;
        private final int preparationCount;
        private final int readyCount;
        private final int listedCount;
        private final int soldCount;
        private final String landingCostTotal;
        private final String askingTotal;
        private final String soldTotal;
        private final List<ReportRow> rows;
        private final List<MakeCount> makes;

        public ReportPage(
                String searchQuery,
                String section,
                int total,
                int importCount,
                int customsCount,
                int preparationCount,
                int readyCount,
                int listedCount,
                int soldCount,
                String landingCostTotal,
                String askingTotal,
                String soldTotal,
                List<ReportRow> rows,
                List<MakeCount> makes
        ) {
            this.searchQuery = searchQuery;
            this.section = section;
            this.total = total;
            this.importCount = importCount;
            this.customsCount = customsCount;
            this.preparationCount = preparationCount;
            this.readyCount = readyCount;
            this.listedCount = listedCount;
            this.soldCount = soldCount;
            this.landingCostTotal = landingCostTotal;
            this.askingTotal = askingTotal;
            this.soldTotal = soldTotal;
            this.rows = rows;
            this.makes = makes;
        }

        public String getSearchQuery() { return searchQuery; }
        public String getSection() { return section; }
        public int getTotal() { return total; }
        public int getImportCount() { return importCount; }
        public int getCustomsCount() { return customsCount; }
        public int getPreparationCount() { return preparationCount; }
        public int getReadyCount() { return readyCount; }
        public int getListedCount() { return listedCount; }
        public int getSoldCount() { return soldCount; }
        public String getLandingCostTotal() { return landingCostTotal; }
        public String getAskingTotal() { return askingTotal; }
        public String getSoldTotal() { return soldTotal; }
        public List<ReportRow> getRows() { return rows; }
        public List<MakeCount> getMakes() { return makes; }
    }

    public static final class ReportRow {
        private final String chassisNo;
        private final String vehicleLabel;
        private final String year;
        private final String section;
        private final String sectionLabel;
        private final String pillClass;
        private final String viewPath;
        private final String landingCost;
        private final String askingPrice;
        private final String advertisedPrice;
        private final String saleLocation;

        public ReportRow(
                String chassisNo,
                String vehicleLabel,
                String year,
                String section,
                String sectionLabel,
                String pillClass,
                String viewPath,
                String landingCost,
                String askingPrice,
                String advertisedPrice,
                String saleLocation
        ) {
            this.chassisNo = chassisNo;
            this.vehicleLabel = vehicleLabel;
            this.year = year;
            this.section = section;
            this.sectionLabel = sectionLabel;
            this.pillClass = pillClass;
            this.viewPath = viewPath;
            this.landingCost = landingCost;
            this.askingPrice = askingPrice;
            this.advertisedPrice = advertisedPrice;
            this.saleLocation = saleLocation;
        }

        public String getChassisNo() { return chassisNo; }
        public String getVehicleLabel() { return vehicleLabel; }
        public String getYear() { return year; }
        public String getSection() { return section; }
        public String getSectionLabel() { return sectionLabel; }
        public String getPillClass() { return pillClass; }
        public String getViewPath() { return viewPath; }
        public String getLandingCost() { return landingCost; }
        public String getAskingPrice() { return askingPrice; }
        public String getAdvertisedPrice() { return advertisedPrice; }
        public String getSaleLocation() { return saleLocation; }
    }

    public static final class MakeCount {
        private final String make;
        private final int count;

        public MakeCount(String make, int count) {
            this.make = make;
            this.count = count;
        }

        public String getMake() { return make; }
        public int getCount() { return count; }
    }
}
