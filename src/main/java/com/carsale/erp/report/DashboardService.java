package com.carsale.erp.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.service.YardBayService;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.readypipeline.util.Period;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@Service
public class DashboardService {

    private static final int TABLE_LIMIT = 8;

    private final ReportService reportService;
    private final SaleLocationService saleLocationService;
    private final YardBayService yardBayService;
    private final VehicleService vehicleService;

    public DashboardService(
            ReportService reportService,
            SaleLocationService saleLocationService,
            YardBayService yardBayService,
            VehicleService vehicleService
    ) {
        this.reportService = reportService;
        this.saleLocationService = saleLocationService;
        this.yardBayService = yardBayService;
        this.vehicleService = vehicleService;
    }

    @Transactional(readOnly = true)
    public DashboardPage build(String rawPeriod) {
        final String period = normalizePeriod(rawPeriod);
        ReportService.ReportPage report = reportService.build(null, ReportService.ALL);
        List<SaleLocation> locations = saleLocationService.listAll();

        BigDecimal stockCost = BigDecimal.ZERO;
        BigDecimal listedAsking = BigDecimal.ZERO;
        BigDecimal listedSpread = BigDecimal.ZERO;
        int listedMissingPrice = 0;
        BigDecimal soldRevenue = BigDecimal.ZERO;
        BigDecimal soldCost = BigDecimal.ZERO;
        BigDecimal grossMargin = BigDecimal.ZERO;
        int soldPriced = 0;
        int soldMissingPrice = 0;
        int marginCount = 0;
        boolean anyStockCost = false;
        boolean anyListedAsking = false;
        boolean anyListedSpread = false;
        boolean anySoldRevenue = false;

        Map<String, Integer> soldByLocation = new HashMap<String, Integer>();
        Map<String, BigDecimal> soldValueByLocation = new HashMap<String, BigDecimal>();
        List<SaleLine> openLines = new ArrayList<SaleLine>();
        List<SaleLine> soldLines = new ArrayList<SaleLine>();

        int importCount = 0;
        int customsCount = 0;
        int preparationCount = 0;
        int readyCount = 0;
        int listedCount = 0;
        int soldCount = 0;
        Map<String, Integer> makeCounts = new HashMap<String, Integer>();
        Map<String, Vehicle> vehiclesByChassis = vehiclesByChassis();
        List<ReportService.ReportRow> periodRows = new ArrayList<ReportService.ReportRow>();

        for (ReportService.ReportRow row : report.getRows()) {
            if (!inDashboardPeriod(row.getListedOn(), period)) {
                continue;
            }
            periodRows.add(row);
            boolean sold = ReportService.SOLD.equals(row.getSection());
            boolean listed = ReportService.LISTED.equals(row.getSection());
            if (ReportService.IMPORT.equals(row.getSection())) {
                importCount++;
            } else if (ReportService.CUSTOMS.equals(row.getSection())) {
                customsCount++;
            } else if (ReportService.PREPARATION.equals(row.getSection())) {
                preparationCount++;
            } else if (ReportService.READY.equals(row.getSection())) {
                readyCount++;
            } else if (listed) {
                listedCount++;
            } else if (sold) {
                soldCount++;
            }
            Vehicle vehicle = vehiclesByChassis.get(row.getChassisNo());
            String makeName = vehicle == null ? "Unknown" : blank(vehicle.getMake(), "Unknown");
            Integer makeSoFar = makeCounts.get(makeName);
            makeCounts.put(makeName, makeSoFar == null ? 1 : makeSoFar + 1);
            BigDecimal landing = row.getLandingAmount();
            BigDecimal asking = row.getAskingAmount();
            BigDecimal advertised = row.getAdvertisedAmount();
            BigDecimal salePrice = advertised != null ? advertised : asking;

            if (!sold && landing != null) {
                stockCost = stockCost.add(landing);
                anyStockCost = true;
            }
            if (listed) {
                if (asking == null) {
                    listedMissingPrice++;
                } else {
                    listedAsking = listedAsking.add(asking);
                    anyListedAsking = true;
                    if (landing != null) {
                        listedSpread = listedSpread.add(asking.subtract(landing));
                        anyListedSpread = true;
                    }
                }
                openLines.add(line(row, asking, landing, asking, landing));
            }
            if (sold) {
                if (salePrice == null) {
                    soldMissingPrice++;
                } else {
                    soldRevenue = soldRevenue.add(salePrice);
                    anySoldRevenue = true;
                    soldPriced++;
                    if (landing != null) {
                        soldCost = soldCost.add(landing);
                        grossMargin = grossMargin.add(salePrice.subtract(landing));
                        marginCount++;
                    }
                }
                soldLines.add(line(row, salePrice, landing, salePrice, landing));
                String locationKey = locationKey(row.getSaleLocation());
                if (!locationKey.isEmpty()) {
                    Integer current = soldByLocation.get(locationKey);
                    soldByLocation.put(locationKey, current == null ? 1 : current + 1);
                    if (salePrice != null) {
                        BigDecimal soFar = soldValueByLocation.get(locationKey);
                        soldValueByLocation.put(locationKey, soFar == null ? salePrice : soFar.add(salePrice));
                    }
                }
            }
        }

        int onHand = periodRows.size() - soldCount;

        Collections.sort(openLines, new Comparator<SaleLine>() {
            @Override
            public int compare(SaleLine left, SaleLine right) {
                int byDate = compareListed(right.getListedOn(), left.getListedOn());
                if (byDate != 0) {
                    return byDate;
                }
                return left.getChassisNo().compareToIgnoreCase(right.getChassisNo());
            }
        });
        Collections.sort(soldLines, new Comparator<SaleLine>() {
            @Override
            public int compare(SaleLine left, SaleLine right) {
                int byAmount = compareAmount(right.getSortAmount(), left.getSortAmount());
                if (byAmount != 0) {
                    return byAmount;
                }
                return left.getChassisNo().compareToIgnoreCase(right.getChassisNo());
            }
        });

        int openTotal = openLines.size();
        int soldTotal = soldLines.size();
        if (openLines.size() > TABLE_LIMIT) {
            openLines = new ArrayList<SaleLine>(openLines.subList(0, TABLE_LIMIT));
        }
        if (soldLines.size() > TABLE_LIMIT) {
            soldLines = new ArrayList<SaleLine>(soldLines.subList(0, TABLE_LIMIT));
        }

        int capacity = 0;
        int occupied = 0;
        int fullLocations = 0;
        int lowLocations = 0;
        List<LocationRow> locationRows = new ArrayList<LocationRow>();
        for (SaleLocation location : locations) {
            if (!location.isActive()) {
                continue;
            }
            capacity += Math.max(0, location.getCapacity());
            occupied += location.getOccupied();
            if (location.isFull()) {
                fullLocations++;
            } else if ("is-low".equals(location.getFillState())) {
                lowLocations++;
            }
            String key = locationKey(location.getLocation());
            Integer soldHere = soldByLocation.get(key);
            BigDecimal soldValue = soldValueByLocation.get(key);
            locationRows.add(new LocationRow(
                    location.getSaleCode(),
                    blank(location.getSaleName(), location.getSaleCode()),
                    blank(location.getLocation(), "—"),
                    "/ready-for-sale/" + location.getSaleCode(),
                    location.getOccupied(),
                    location.getCapacity(),
                    location.getRemaining(),
                    location.getFillPercent(),
                    location.getFillState(),
                    soldHere == null ? 0 : soldHere,
                    soldValue == null ? "—" : money(soldValue)
            ));
        }

        int soldForRate = soldCount;
        int saleReady = readyCount + listedCount + soldCount;
        String sellThrough = saleReady <= 0
                ? "—"
                : percent(soldForRate * 100.0 / saleReady);
        String averageSold = soldPriced <= 0
                ? "—"
                : money(soldRevenue.divide(BigDecimal.valueOf(soldPriced), 2, RoundingMode.HALF_UP));
        String marginPercent = marginCount <= 0 || soldRevenue.compareTo(BigDecimal.ZERO) == 0
                ? "—"
                : percent(grossMargin.multiply(BigDecimal.valueOf(100))
                        .divide(soldRevenue, 1, RoundingMode.HALF_UP).doubleValue());

        String periodName = periodLabel(period);
        String listedHint = anyListedAsking ? money(listedAsking) + " asking" : "No asking prices yet";
        String soldHint = soldTotal + (soldTotal == 1 ? " vehicle" : " vehicles");
        if (!"all".equals(period)) {
            listedHint = periodName + " · " + listedHint;
            soldHint = periodName + " · " + soldHint;
        }

        int periodTotal = periodRows.size();
        List<Map.Entry<String, Integer>> makeEntries = new ArrayList<Map.Entry<String, Integer>>(makeCounts.entrySet());
        Collections.sort(makeEntries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                int byCount = Integer.compare(right.getValue().intValue(), left.getValue().intValue());
                if (byCount != 0) {
                    return byCount;
                }
                return left.getKey().compareToIgnoreCase(right.getKey());
            }
        });
        int makeMax = 0;
        for (Map.Entry<String, Integer> make : makeEntries) {
            if (make.getValue().intValue() > makeMax) {
                makeMax = make.getValue().intValue();
            }
        }
        List<MakeBar> makes = new ArrayList<MakeBar>();
        int shownMakes = 0;
        for (Map.Entry<String, Integer> make : makeEntries) {
            if (shownMakes >= 8) {
                break;
            }
            int count = make.getValue().intValue();
            int width = makeMax <= 0 ? 0 : (int) Math.round(count * 100.0 / makeMax);
            int share = periodTotal <= 0 ? 0 : (int) Math.round(count * 100.0 / periodTotal);
            makes.add(new MakeBar(make.getKey(), count, width, share));
            shownMakes++;
        }

        List<StockRow> stock = stockRows(periodRows, vehiclesByChassis, !"all".equals(period));
        List<PlaceOption> places = placeOptions(locations);

        int available = readyCount + listedCount;
        String yardHint = "all".equals(period) ? "Every chassis on file" : periodName;
        String availableHint = "all".equals(period) ? "Ready for sale or listed" : periodName;
        String workshopHint = "all".equals(period) ? "Inspection, repairs, and yard" : periodName;
        String customsHint = "all".equals(period) ? "Clearance still open" : periodName;
        String importHint = "all".equals(period) ? "Still moving through import" : periodName;
        List<Metric> metrics = new ArrayList<Metric>();
        metrics.add(new Metric("Total vehicles", String.valueOf(periodTotal), yardHint, "/reports", "is-blue", "fa-car"));
        metrics.add(new Metric("Available", String.valueOf(available), availableHint, "/reports?section=listed", "is-green", "fa-circle-check"));
        metrics.add(new Metric("In workshop", String.valueOf(preparationCount), workshopHint, "/workshop-yard", "is-orange", "fa-wrench"));
        metrics.add(new Metric("Customs", String.valueOf(customsCount), customsHint, "/customs", "is-gold", "fa-file-invoice"));
        metrics.add(new Metric("Import", String.valueOf(importCount), importHint, "/import", "is-red", "fa-ship"));
        metrics.add(new Metric("Sold", String.valueOf(soldCount), soldHint, "/reports?section=sold", "is-slate", "fa-flag-checkered"));

        List<Stage> stages = new ArrayList<Stage>();
        stages.add(stage("Import", importCount, periodTotal, "/import", "rust"));
        stages.add(stage("Customs", customsCount, periodTotal, "/customs", "gold"));
        stages.add(stage("Preparation", preparationCount, periodTotal, "/workshop-yard", "sea"));
        stages.add(stage("Ready", readyCount, periodTotal, "/reports?section=ready", "sky"));
        stages.add(stage("Listed", listedCount, periodTotal, "/reports?section=listed", "violet"));
        stages.add(stage("Sold", soldCount, periodTotal, "/reports?section=sold", "leaf"));

        List<MoneyLine> moneyLines = new ArrayList<MoneyLine>();
        moneyLines.add(new MoneyLine("Stock at landing cost", anyStockCost ? money(stockCost) : "—", ""));
        moneyLines.add(new MoneyLine("Listed asking", anyListedAsking ? money(listedAsking) : "—", ""));
        moneyLines.add(new MoneyLine(
                "Spread on priced listings",
                anyListedSpread ? money(listedSpread) : "—",
                tone(anyListedSpread ? listedSpread : null)
        ));
        moneyLines.add(new MoneyLine("Sold revenue", anySoldRevenue ? money(soldRevenue) : "—", "up"));
        moneyLines.add(new MoneyLine(
                "Landing cost of priced sales",
                marginCount == 0 ? "—" : money(soldCost),
                ""
        ));
        moneyLines.add(new MoneyLine(
                "Gross margin",
                marginCount == 0 ? "—" : money(grossMargin),
                tone(marginCount == 0 ? null : grossMargin)
        ));
        moneyLines.add(new MoneyLine("Sell-through", sellThrough, ""));
        moneyLines.add(new MoneyLine("Average sold price", averageSold, ""));

        List<ActionItem> actions = new ArrayList<ActionItem>();
        addAction(actions, listedMissingPrice, "Listed without an asking price",
                "These vehicles are on a sale floor with no selling price.",
                "/reports?section=listed");
        addAction(actions, soldMissingPrice, "Sold without a price",
                "Mark the advertised or asking price so revenue is complete.",
                "/reports?section=sold");
        addAction(actions, readyCount, "Ready and not listed",
                "Preparation is finished. Move them onto a sale floor.",
                "/yards");
        addAction(actions, fullLocations, "Sale location is full",
                "No free spaces left.",
                "/ready-for-sale");
        addAction(actions, lowLocations, "Sale location is nearly full",
                "Three or fewer spaces left.",
                "/ready-for-sale");

        String summary = "all".equals(period)
                ? onHand + " on hand, "
                    + openTotal + " listed"
                    + (anyListedAsking ? " at " + money(listedAsking) : "")
                    + ", "
                    + soldTotal + " sold"
                    + (anySoldRevenue ? " for " + money(soldRevenue) : "")
                    + "."
                : periodName + " applies to every total, chart, and list. Unlisted stock stays on the board. "
                    + periodTotal + " vehicles, "
                    + openTotal + " listed"
                    + (anyListedAsking ? " at " + money(listedAsking) : "")
                    + ", "
                    + soldTotal + " sold"
                    + (anySoldRevenue ? " for " + money(soldRevenue) : "")
                    + ".";

        String chartJson = chartJson(stages, places, makes, stockCost, listedAsking, soldRevenue, grossMargin);
        String marginText = marginCount == 0 ? "—" : money(grossMargin);

        return new DashboardPage(
                summary,
                period,
                periodName,
                anyStockCost ? money(stockCost) : "—",
                sellThrough,
                averageSold,
                marginText,
                marginPercent,
                anyListedAsking ? money(listedAsking) : "—",
                anySoldRevenue ? money(soldRevenue) : "—",
                chartJson,
                metrics,
                stages,
                moneyLines,
                actions,
                locationRows,
                openLines,
                soldLines,
                openTotal,
                soldTotal,
                makes,
                stock,
                places
        );
    }

    @Transactional
    public void pin(String kind, Long id, Double latitude, Double longitude) {
        if (id == null || latitude == null || longitude == null
                || latitude.doubleValue() < -90 || latitude.doubleValue() > 90
                || longitude.doubleValue() < -180 || longitude.doubleValue() > 180) {
            throw new IllegalArgumentException("Click the map to choose a point.");
        }
        if ("yard".equals(kind)) {
            yardBayService.pin(id, latitude, longitude);
            return;
        }
        if ("sale".equals(kind)) {
            saleLocationService.pin(id, latitude, longitude);
            return;
        }
        throw new IllegalArgumentException("Choose a sale location or a yard.");
    }

    private Map<String, Vehicle> vehiclesByChassis() {
        Map<String, Vehicle> vehicles = new HashMap<String, Vehicle>();
        for (Vehicle vehicle : vehicleService.findAll()) {
            if (vehicle.getChassisNo() != null) {
                vehicles.put(vehicle.getChassisNo(), vehicle);
            }
        }
        return vehicles;
    }

    private List<StockRow> stockRows(
            List<ReportService.ReportRow> rows,
            Map<String, Vehicle> vehicles,
            boolean includeSold
    ) {
        List<StockRow> stock = new ArrayList<StockRow>();
        for (ReportService.ReportRow row : rows) {
            if (!includeSold && ReportService.SOLD.equals(row.getSection())) {
                continue;
            }
            Vehicle vehicle = vehicles.get(row.getChassisNo());
            String id = vehicle == null ? row.getChassisNo() : blank(vehicle.getStockNo(), row.getChassisNo());
            String type = vehicle == null ? "—" : blank(vehicle.getBodyStyle(), blank(vehicle.getMake(), "—"));
            String model = vehicle == null ? row.getVehicleLabel() : blank(vehicle.getModel(), row.getVehicleLabel());
            String meter = vehicle == null ? "—" : blank(vehicle.getMileage(), "—");
            String place = row.getSaleLocation();
            if (place == null || place.trim().isEmpty() || "—".equals(place.trim())) {
                place = row.getSectionLabel();
            }
            stock.add(new StockRow(
                    id,
                    row.getChassisNo(),
                    row.getViewPath(),
                    type,
                    model,
                    row.getYear(),
                    meter,
                    row.getSectionLabel(),
                    row.getSection(),
                    place
            ));
            if (stock.size() >= TABLE_LIMIT) {
                break;
            }
        }
        return stock;
    }

    private List<PlaceOption> placeOptions(List<SaleLocation> locations) {
        List<PlaceOption> places = new ArrayList<PlaceOption>();
        for (SaleLocation location : locations) {
            if (!location.isActive()) {
                continue;
            }
            places.add(new PlaceOption(
                    "sale",
                    location.getId(),
                    location.getSaleCode(),
                    blank(location.getSaleName(), location.getSaleCode()),
                    blank(location.getLocation(), ""),
                    "/ready-for-sale/" + location.getSaleCode(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getOccupied(),
                    location.getCapacity()
            ));
        }
        for (YardBay bay : yardBayService.listActive()) {
            places.add(new PlaceOption(
                    "yard",
                    bay.getId(),
                    bay.getBayCode(),
                    blank(bay.getYardName(), bay.getBayCode()),
                    blank(bay.getLocation(), ""),
                    "/yards/" + bay.getBayCode(),
                    bay.getLatitude(),
                    bay.getLongitude(),
                    bay.getOccupied(),
                    bay.getCapacity()
            ));
        }
        return places;
    }

    private static boolean inDashboardPeriod(String listedOn, String period) {
        if ("all".equals(period)) {
            return true;
        }
        if (listedOn == null || listedOn.trim().isEmpty() || "—".equals(listedOn.trim())) {
            return true;
        }
        return !Period.matchesPeriod(listedOn, period);
    }

    private static String normalizePeriod(String period) {
        if (period == null) {
            return "all";
        }
        String value = period.trim().toLowerCase(Locale.ROOT);
        if ("today".equals(value) || "week".equals(value) || "month".equals(value)
                || "quarter".equals(value) || "year".equals(value)) {
            return value;
        }
        return "all";
    }

    private static String periodLabel(String period) {
        if ("today".equals(period)) {
            return "Today";
        }
        if ("week".equals(period)) {
            return "This week";
        }
        if ("month".equals(period)) {
            return "This month";
        }
        if ("quarter".equals(period)) {
            return "This quarter";
        }
        if ("year".equals(period)) {
            return "This year";
        }
        return "All time";
    }

    private static String chartJson(
            List<Stage> stages,
            List<PlaceOption> places,
            List<MakeBar> makes,
            BigDecimal stockCost,
            BigDecimal listedAsking,
            BigDecimal soldRevenue,
            BigDecimal grossMargin
    ) {
        Map<String, Object> root = new HashMap<String, Object>();
        List<String> stageLabels = new ArrayList<String>();
        List<Integer> stageValues = new ArrayList<Integer>();
        for (Stage stage : stages) {
            stageLabels.add(stage.getLabel());
            stageValues.add(Integer.valueOf(stage.getCount()));
        }
        Map<String, Object> stageChart = new HashMap<String, Object>();
        stageChart.put("labels", stageLabels);
        stageChart.put("values", stageValues);
        root.put("stages", stageChart);

        Map<String, Object> moneyChart = new HashMap<String, Object>();
        List<String> moneyLabels = new ArrayList<String>();
        moneyLabels.add("Stock cost");
        moneyLabels.add("Listed asking");
        moneyLabels.add("Sold revenue");
        moneyLabels.add("Gross margin");
        List<Double> moneyValues = new ArrayList<Double>();
        moneyValues.add(Double.valueOf(stockCost.doubleValue()));
        moneyValues.add(Double.valueOf(listedAsking.doubleValue()));
        moneyValues.add(Double.valueOf(soldRevenue.doubleValue()));
        moneyValues.add(Double.valueOf(grossMargin.doubleValue()));
        moneyChart.put("labels", moneyLabels);
        moneyChart.put("values", moneyValues);
        root.put("money", moneyChart);

        List<String> makeLabels = new ArrayList<String>();
        List<Integer> makeValues = new ArrayList<Integer>();
        for (MakeBar make : makes) {
            makeLabels.add(make.getMake());
            makeValues.add(Integer.valueOf(make.getCount()));
        }
        Map<String, Object> makeChart = new HashMap<String, Object>();
        makeChart.put("labels", makeLabels);
        makeChart.put("values", makeValues);
        root.put("makes", makeChart);

        List<Map<String, Object>> pins = new ArrayList<Map<String, Object>>();
        for (PlaceOption place : places) {
            if (place.getLatitude() == null || place.getLongitude() == null) {
                continue;
            }
            Map<String, Object> pin = new HashMap<String, Object>();
            pin.put("kind", place.getKind());
            pin.put("name", place.getName());
            pin.put("code", place.getCode());
            pin.put("detail", place.getDetail());
            pin.put("lat", place.getLatitude());
            pin.put("lng", place.getLongitude());
            pin.put("occupied", Integer.valueOf(place.getOccupied()));
            pin.put("capacity", Integer.valueOf(place.getCapacity()));
            pins.add(pin);
        }
        root.put("places", pins);

        try {
            return new ObjectMapper().writeValueAsString(root);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static SaleLine line(
            ReportService.ReportRow row,
            BigDecimal price,
            BigDecimal landing,
            BigDecimal left,
            BigDecimal right
    ) {
        BigDecimal spread = left != null && right != null ? left.subtract(right) : null;
        return new SaleLine(
                row.getChassisNo(),
                row.getVehicleLabel(),
                row.getYear(),
                row.getSaleLocation(),
                row.getListedOn(),
                row.getViewPath(),
                price == null ? "—" : money(price),
                landing == null ? "—" : money(landing),
                spread == null ? "—" : money(spread),
                tone(spread),
                price
        );
    }

    private static Stage stage(String label, int count, int total, String href, String tone) {
        int width = total <= 0 || count <= 0 ? 0 : (int) Math.round(count * 100.0 / total);
        if (count > 0 && width < 4) {
            width = 4;
        }
        return new Stage(label, count, width, href, tone);
    }

    private static void addAction(List<ActionItem> actions, int count, String title, String detail, String href) {
        if (count <= 0) {
            return;
        }
        String phrase = count == 1 ? title : title.replace("location is", "locations are").replace("vehicle", "vehicles");
        if (count == 1 && title.startsWith("Listed")) {
            phrase = "1 vehicle is listed without an asking price";
        } else if (count > 1 && title.startsWith("Listed")) {
            phrase = count + " vehicles are listed without an asking price";
        } else if (title.startsWith("Sold")) {
            phrase = count + (count == 1 ? " sale has no price" : " sales have no price");
        } else if (title.startsWith("Ready")) {
            phrase = count + (count == 1 ? " vehicle is ready and not listed" : " vehicles are ready and not listed");
        } else if (title.startsWith("Sale") && title.contains("full") && !title.contains("nearly")) {
            phrase = count + (count == 1 ? " sale location is full" : " sale locations are full");
        } else if (title.contains("nearly")) {
            phrase = count + (count == 1 ? " sale location is nearly full" : " sale locations are nearly full");
        }
        actions.add(new ActionItem(phrase, detail, href));
    }

    private static int compareListed(String left, String right) {
        boolean leftBlank = isBlankDate(left);
        boolean rightBlank = isBlankDate(right);
        if (leftBlank && rightBlank) {
            return 0;
        }
        if (leftBlank) {
            return 1;
        }
        if (rightBlank) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static int compareAmount(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static boolean isBlankDate(String value) {
        return value == null || value.trim().isEmpty() || "—".equals(value.trim());
    }

    private static String locationKey(String location) {
        if (location == null) {
            return "";
        }
        String text = location.trim();
        if (text.isEmpty() || "—".equals(text) || "-".equals(text)) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT);
    }

    private static String blank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static String tone(BigDecimal value) {
        if (value == null || value.signum() == 0) {
            return "";
        }
        return value.signum() > 0 ? "up" : "down";
    }

    private static String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return "Rs. " + format.format(value);
    }

    private static String percent(double value) {
        DecimalFormat format = new DecimalFormat("0.#");
        return format.format(value) + "%";
    }

    public static final class DashboardPage {
        private final String summary;
        private final String period;
        private final String periodLabel;
        private final String stockCost;
        private final String sellThrough;
        private final String averageSold;
        private final String grossMargin;
        private final String marginPercent;
        private final String listedAsking;
        private final String soldRevenue;
        private final String chartJson;
        private final List<Metric> metrics;
        private final List<Stage> stages;
        private final List<MoneyLine> moneyLines;
        private final List<ActionItem> actions;
        private final List<LocationRow> locations;
        private final List<SaleLine> listings;
        private final List<SaleLine> soldVehicles;
        private final int openTotal;
        private final int soldTotal;
        private final List<MakeBar> makes;
        private final List<StockRow> stock;
        private final List<PlaceOption> places;

        public DashboardPage(
                String summary,
                String period,
                String periodLabel,
                String stockCost,
                String sellThrough,
                String averageSold,
                String grossMargin,
                String marginPercent,
                String listedAsking,
                String soldRevenue,
                String chartJson,
                List<Metric> metrics,
                List<Stage> stages,
                List<MoneyLine> moneyLines,
                List<ActionItem> actions,
                List<LocationRow> locations,
                List<SaleLine> listings,
                List<SaleLine> soldVehicles,
                int openTotal,
                int soldTotal,
                List<MakeBar> makes,
                List<StockRow> stock,
                List<PlaceOption> places
        ) {
            this.summary = summary;
            this.period = period;
            this.periodLabel = periodLabel;
            this.stockCost = stockCost;
            this.sellThrough = sellThrough;
            this.averageSold = averageSold;
            this.grossMargin = grossMargin;
            this.marginPercent = marginPercent;
            this.listedAsking = listedAsking;
            this.soldRevenue = soldRevenue;
            this.chartJson = chartJson;
            this.metrics = metrics;
            this.stages = stages;
            this.moneyLines = moneyLines;
            this.actions = actions;
            this.locations = locations;
            this.listings = listings;
            this.soldVehicles = soldVehicles;
            this.openTotal = openTotal;
            this.soldTotal = soldTotal;
            this.makes = makes;
            this.stock = stock;
            this.places = places;
        }

        public String getSummary() { return summary; }
        public String getPeriod() { return period; }
        public String getPeriodLabel() { return periodLabel; }
        public String getStockCost() { return stockCost; }
        public String getSellThrough() { return sellThrough; }
        public String getAverageSold() { return averageSold; }
        public String getGrossMargin() { return grossMargin; }
        public String getMarginPercent() { return marginPercent; }
        public String getListedAsking() { return listedAsking; }
        public String getSoldRevenue() { return soldRevenue; }
        public String getChartJson() { return chartJson; }
        public List<Metric> getMetrics() { return metrics; }
        public List<Stage> getStages() { return stages; }
        public List<MoneyLine> getMoneyLines() { return moneyLines; }
        public List<ActionItem> getActions() { return actions; }
        public List<LocationRow> getLocations() { return locations; }
        public List<SaleLine> getListings() { return listings; }
        public List<SaleLine> getSoldVehicles() { return soldVehicles; }
        public int getOpenTotal() { return openTotal; }
        public int getSoldTotal() { return soldTotal; }
        public List<MakeBar> getMakes() { return makes; }
        public List<StockRow> getStock() { return stock; }
        public List<PlaceOption> getPlaces() { return places; }
    }

    public static final class Metric {
        private final String label;
        private final String value;
        private final String hint;
        private final String href;
        private final String tone;
        private final String icon;

        public Metric(String label, String value, String hint, String href, String tone, String icon) {
            this.label = label;
            this.value = value;
            this.hint = hint;
            this.href = href;
            this.tone = tone;
            this.icon = icon;
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
        public String getHint() { return hint; }
        public String getHref() { return href; }
        public String getTone() { return tone; }
        public String getIcon() { return icon; }
    }

    public static final class Stage {
        private final String label;
        private final int count;
        private final int width;
        private final String href;
        private final String tone;

        public Stage(String label, int count, int width, String href, String tone) {
            this.label = label;
            this.count = count;
            this.width = width;
            this.href = href;
            this.tone = tone;
        }

        public String getLabel() { return label; }
        public int getCount() { return count; }
        public int getWidth() { return width; }
        public String getHref() { return href; }
        public String getTone() { return tone; }
    }

    public static final class MoneyLine {
        private final String label;
        private final String value;
        private final String tone;

        public MoneyLine(String label, String value, String tone) {
            this.label = label;
            this.value = value;
            this.tone = tone;
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
        public String getTone() { return tone; }
    }

    public static final class ActionItem {
        private final String title;
        private final String detail;
        private final String href;

        public ActionItem(String title, String detail, String href) {
            this.title = title;
            this.detail = detail;
            this.href = href;
        }

        public String getTitle() { return title; }
        public String getDetail() { return detail; }
        public String getHref() { return href; }
    }

    public static final class LocationRow {
        private final String code;
        private final String name;
        private final String location;
        private final String href;
        private final int occupied;
        private final int capacity;
        private final int remaining;
        private final int fill;
        private final String state;
        private final int soldCount;
        private final String soldValue;

        public LocationRow(
                String code,
                String name,
                String location,
                String href,
                int occupied,
                int capacity,
                int remaining,
                int fill,
                String state,
                int soldCount,
                String soldValue
        ) {
            this.code = code;
            this.name = name;
            this.location = location;
            this.href = href;
            this.occupied = occupied;
            this.capacity = capacity;
            this.remaining = remaining;
            this.fill = fill;
            this.state = state;
            this.soldCount = soldCount;
            this.soldValue = soldValue;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getLocation() { return location; }
        public String getHref() { return href; }
        public int getOccupied() { return occupied; }
        public int getCapacity() { return capacity; }
        public int getRemaining() { return remaining; }
        public int getFill() { return fill; }
        public String getState() { return state; }
        public int getSoldCount() { return soldCount; }
        public String getSoldValue() { return soldValue; }
    }

    public static final class SaleLine {
        private final String chassisNo;
        private final String vehicleLabel;
        private final String year;
        private final String location;
        private final String listedOn;
        private final String viewPath;
        private final String price;
        private final String landing;
        private final String spread;
        private final String spreadTone;
        private final BigDecimal sortAmount;

        public SaleLine(
                String chassisNo,
                String vehicleLabel,
                String year,
                String location,
                String listedOn,
                String viewPath,
                String price,
                String landing,
                String spread,
                String spreadTone,
                BigDecimal sortAmount
        ) {
            this.chassisNo = chassisNo;
            this.vehicleLabel = vehicleLabel;
            this.year = year;
            this.location = location;
            this.listedOn = listedOn;
            this.viewPath = viewPath;
            this.price = price;
            this.landing = landing;
            this.spread = spread;
            this.spreadTone = spreadTone;
            this.sortAmount = sortAmount;
        }

        public String getChassisNo() { return chassisNo; }
        public String getVehicleLabel() { return vehicleLabel; }
        public String getYear() { return year; }
        public String getLocation() { return location; }
        public String getListedOn() { return listedOn; }
        public String getViewPath() { return viewPath; }
        public String getPrice() { return price; }
        public String getLanding() { return landing; }
        public String getSpread() { return spread; }
        public String getSpreadTone() { return spreadTone; }
        public BigDecimal getSortAmount() { return sortAmount; }
    }

    public static final class MakeBar {
        private final String make;
        private final int count;
        private final int width;
        private final int share;

        public MakeBar(String make, int count, int width, int share) {
            this.make = make;
            this.count = count;
            this.width = width;
            this.share = share;
        }

        public String getMake() { return make; }
        public int getCount() { return count; }
        public int getWidth() { return width; }
        public int getShare() { return share; }
    }

    public static final class StockRow {
        private final String idLabel;
        private final String chassisNo;
        private final String href;
        private final String type;
        private final String model;
        private final String year;
        private final String meter;
        private final String status;
        private final String statusKey;
        private final String place;

        public StockRow(
                String idLabel,
                String chassisNo,
                String href,
                String type,
                String model,
                String year,
                String meter,
                String status,
                String statusKey,
                String place
        ) {
            this.idLabel = idLabel;
            this.chassisNo = chassisNo;
            this.href = href;
            this.type = type;
            this.model = model;
            this.year = year;
            this.meter = meter;
            this.status = status;
            this.statusKey = statusKey;
            this.place = place;
        }

        public String getIdLabel() { return idLabel; }
        public String getChassisNo() { return chassisNo; }
        public String getHref() { return href; }
        public String getType() { return type; }
        public String getModel() { return model; }
        public String getYear() { return year; }
        public String getMeter() { return meter; }
        public String getStatus() { return status; }
        public String getStatusKey() { return statusKey; }
        public String getPlace() { return place; }
    }

    public static final class PlaceOption {
        private final String kind;
        private final Long id;
        private final String code;
        private final String name;
        private final String detail;
        private final String href;
        private final Double latitude;
        private final Double longitude;
        private final int occupied;
        private final int capacity;

        public PlaceOption(
                String kind,
                Long id,
                String code,
                String name,
                String detail,
                String href,
                Double latitude,
                Double longitude,
                int occupied,
                int capacity
        ) {
            this.kind = kind;
            this.id = id;
            this.code = code;
            this.name = name;
            this.detail = detail;
            this.href = href;
            this.latitude = latitude;
            this.longitude = longitude;
            this.occupied = occupied;
            this.capacity = capacity;
        }

        public String getKind() { return kind; }
        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDetail() { return detail; }
        public String getHref() { return href; }
        public Double getLatitude() { return latitude; }
        public Double getLongitude() { return longitude; }
        public int getOccupied() { return occupied; }
        public int getCapacity() { return capacity; }
        public boolean isPinned() { return latitude != null && longitude != null; }
    }
}
