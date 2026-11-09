package com.carsale.erp.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import com.carsale.erp.customspipeline.model.CustomsDocument;
import com.carsale.erp.customspipeline.repository.CustomsDocumentRepository;
import com.carsale.erp.importpipeline.model.ExportCertificate;
import com.carsale.erp.importpipeline.model.GradeSearch;
import com.carsale.erp.importpipeline.model.InspectionCertificate;
import com.carsale.erp.importpipeline.model.PreShipmentInspection;
import com.carsale.erp.importpipeline.model.StandardsCertificate;
import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.repository.ExportCertificateRepository;
import com.carsale.erp.importpipeline.repository.GradeSearchRepository;
import com.carsale.erp.importpipeline.repository.InspectionCertificateRepository;
import com.carsale.erp.importpipeline.repository.PreShipmentInspectionRepository;
import com.carsale.erp.importpipeline.repository.StandardsCertificateRepository;
import com.carsale.erp.importpipeline.repository.VehiclePhotoRepository;
import com.carsale.erp.preparationpipeline.dto.InspectionResults;
import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import com.carsale.erp.preparationpipeline.model.VehicleInspection;
import com.carsale.erp.preparationpipeline.model.VehicleInspectionLine;
import com.carsale.erp.preparationpipeline.model.WorkshopJob;
import com.carsale.erp.preparationpipeline.model.WorkshopJobLine;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.repository.RegistrationCertificateRepository;
import com.carsale.erp.preparationpipeline.repository.VehicleInspectionRepository;
import com.carsale.erp.preparationpipeline.repository.WorkshopJobRepository;
import com.carsale.erp.preparationpipeline.repository.YardRecordRepository;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.VehicleRegistration;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.readypipeline.repository.VehicleRegistrationRepository;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleService;
import com.carsale.erp.shared.vehicle.VehicleTotalCost;

@Service
public class StandardReportService {

    public static final String STOCK = "stock";
    public static final String SALES = "sales";
    public static final String LANDED = "landed";
    public static final String DUTY = "duty";
    public static final String DOCUMENTS = "documents";
    public static final String EXPIRY = "expiry";
    public static final String WORKSHOP = "workshop";
    public static final String PIPELINE = "pipeline";

    private static final DateTimeFormatter PRINTED = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final List<String> KINDS = Collections.unmodifiableList(Arrays.asList(
            STOCK, SALES, LANDED, DUTY, DOCUMENTS, EXPIRY, WORKSHOP, PIPELINE
    ));

    private final VehicleService vehicleService;
    private final ReportService reportService;
    private final SaleListingRepository saleListingRepository;
    private final CustomsDocumentRepository customsDocumentRepository;
    private final WorkshopJobRepository workshopJobRepository;
    private final YardRecordRepository yardRecordRepository;
    private final VehicleRegistrationRepository vehicleRegistrationRepository;
    private final ExportCertificateRepository exportCertificateRepository;
    private final StandardsCertificateRepository standardsCertificateRepository;
    private final GradeSearchRepository gradeSearchRepository;
    private final VehiclePhotoRepository vehiclePhotoRepository;
    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final RegistrationCertificateRepository registrationCertificateRepository;
    private final InspectionCertificateRepository inspectionCertificateRepository;
    private final PreShipmentInspectionRepository preShipmentInspectionRepository;

    public StandardReportService(
            VehicleService vehicleService,
            ReportService reportService,
            SaleListingRepository saleListingRepository,
            CustomsDocumentRepository customsDocumentRepository,
            WorkshopJobRepository workshopJobRepository,
            YardRecordRepository yardRecordRepository,
            VehicleRegistrationRepository vehicleRegistrationRepository,
            ExportCertificateRepository exportCertificateRepository,
            StandardsCertificateRepository standardsCertificateRepository,
            GradeSearchRepository gradeSearchRepository,
            VehiclePhotoRepository vehiclePhotoRepository,
            VehicleInspectionRepository vehicleInspectionRepository,
            RegistrationCertificateRepository registrationCertificateRepository,
            InspectionCertificateRepository inspectionCertificateRepository,
            PreShipmentInspectionRepository preShipmentInspectionRepository
    ) {
        this.vehicleService = vehicleService;
        this.reportService = reportService;
        this.saleListingRepository = saleListingRepository;
        this.customsDocumentRepository = customsDocumentRepository;
        this.workshopJobRepository = workshopJobRepository;
        this.yardRecordRepository = yardRecordRepository;
        this.vehicleRegistrationRepository = vehicleRegistrationRepository;
        this.exportCertificateRepository = exportCertificateRepository;
        this.standardsCertificateRepository = standardsCertificateRepository;
        this.gradeSearchRepository = gradeSearchRepository;
        this.vehiclePhotoRepository = vehiclePhotoRepository;
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.registrationCertificateRepository = registrationCertificateRepository;
        this.inspectionCertificateRepository = inspectionCertificateRepository;
        this.preShipmentInspectionRepository = preShipmentInspectionRepository;
    }

    public static boolean isKnown(String kind) {
        return kind == null || !KINDS.contains(kind.trim().toLowerCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public List<ReportCard> catalog() {
        Snapshot snap = load("");
        List<ReportCard> cards = new ArrayList<ReportCard>();
        cards.add(new ReportCard(
                STOCK, "Inventory", "Vehicles on stock", "IAS 2 inventory listing",
                "On-hand inventory by location at landing cost.",
                "fa-warehouse", "is-blue", snap.onHand, "on hand",
                moneyOrDash(snap.onHandLanding, snap.anyOnHandLanding)
        ));
        cards.add(new ReportCard(
                SALES, "Sales", "Sold vehicles", "Dealer deal jacket",
                "Sold units, buyers, and gross profit per chassis.",
                "fa-receipt", "is-green", snap.soldCount, "sold",
                moneyOrDash(snap.soldRevenue, snap.anySoldRevenue)
        ));
        cards.add(new ReportCard(
                LANDED, "Customs", "Landed cost sheet", "Incoterms / CIF roll-up",
                "Shipping cost plus the customs declaration, in LKR.",
                "fa-ship", "is-red", snap.landedRows, "vehicles",
                moneyOrDash(snap.landingTotal, snap.anyLanding)
        ));
        cards.add(new ReportCard(
                DUTY, "Customs", "Duty and tax", "Sri Lanka CUSDEC / ASYCUDA",
                "CID, SUR, XID, VAT, VEL, and total tax.",
                "fa-file-invoice-dollar", "is-gold", snap.vehicles.size(), "vehicles",
                moneyOrDash(snap.taxTotal, snap.anyTax)
        ));
        cards.add(new ReportCard(
                DOCUMENTS, "Compliance", "Document completeness", "Compliance register",
                "Export, JEVIC, CUSDEC, assessment, BL, standards, and RMV.",
                "fa-folder-open", "is-orange", snap.vehicles.size(), "vehicles",
                snap.completeDocs + " complete"
        ));
        cards.add(new ReportCard(
                WORKSHOP, "Operations", "Workshop WIP", "Repair order cost",
                "Repairs, total cost, and completed or pending.",
                "fa-wrench", "is-orange", snap.workshopVehicles, "vehicles",
                moneyOrDash(snap.workshopActual, snap.anyWorkshopActual)
        ));
        return cards;
    }

    @Transactional(readOnly = true)
    public StandardReport build(String kind, String query, String section) {
        return build(kind, query, section, null, null);
    }

    @Transactional(readOnly = true)
    public StandardReport build(String kind, String query, String section, String from, String to) {
        String key = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
        Snapshot snap = load(query);
        String fromDate = blankDate(from);
        String toDate = blankDate(to);
        StandardReport report = null;
        switch (key) {
            case STOCK:
                report = stockBook(snap, query, fromDate, toDate);
                break;
            case SALES:
                report = salesRegister(snap, query, fromDate, toDate);
                break;
            case LANDED:
                report = landedCost(snap, query, fromDate, toDate);
                break;
            case DUTY:
                report = dutyTax(snap, query, fromDate, toDate);
                break;
            case DOCUMENTS:
                report = documents(snap, query, fromDate, toDate);
                break;
            case EXPIRY:
                report = expiry(snap, query, fromDate, toDate);
                break;
            case WORKSHOP:
                report = workshop(snap, query, fromDate, toDate);
                break;
            case PIPELINE:
                report = pipeline(snap, query, section, fromDate, toDate);
                break;
        }
        if (report != null) {
            report.fromDate = fromDate;
            report.toDate = toDate;
            report.periodLabel = workshopPeriod(key, fromDate, toDate);
        }
        return report;
    }

    public static String csv(StandardReport report) {
        StringBuilder out = new StringBuilder();
        out.append('\uFEFF');
        for (int i = 0; i < report.columns.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(csvCell(report.columns.get(i).label));
        }
        out.append('\n');
        for (DataRow row : report.rows) {
            for (int i = 0; i < row.cells.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(csvCell(row.cells.get(i).text));
            }
            out.append('\n');
        }
        return out.toString();
    }

    public static String csvFileName(StandardReport report) {
        return report.kind + "-report-" + LocalDate.now() + ".csv";
    }

    private StandardReport stockBook(Snapshot snap, String query, String from, String to) {
        List<Column> columns = columns(
                "Vehicle", "Year", "Mileage",
                "Location", "Total cost", "Days in stock"
        );
        numeric(columns, 4, 5);
        List<DataRow> rows = new ArrayList<>();
        int missingCost = 0;
        int onSale = 0;
        int onYard = 0;
        int onHand = 0;
        BigDecimal onHandTotal = BigDecimal.ZERO;
        boolean anyOnHandTotal = false;
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = snap.listings.get(chassis);
            YardRecord yard = snap.yards.get(chassis);
            if (!inRange(stockDate(listing, yard), from, to)) {
                continue;
            }
            String place = reportService.placeOf(vehicle, listing);
            if (ReportService.SOLD.equals(place)) {
                continue;
            }
            onHand++;
            boolean inSale = listing != null && !listing.isSold()
                    && (filled(listing.getSaleCode()) || filled(listing.getSaleLocation()));
            boolean inYard = yard != null && filled(yard.getBayNo()) && !inSale;
            if (inSale) {
                onSale++;
            }
            if (inYard) {
                onYard++;
            }
            VehicleTotalCost cost = totalCost(snap, chassis);
            BigDecimal landing = cost.getLanding();
            BigDecimal cusdec = cost.getCusdec();
            BigDecimal assessment = cost.getAssessment();
            BigDecimal repair = cost.getRepair();
            BigDecimal total = cost.getTotal();
            if (cost.isMissingRequired()) {
                missingCost++;
            }
            if (total != null) {
                onHandTotal = onHandTotal.add(total);
                anyOnHandTotal = true;
            }
            String location = locationOf(listing, yard, place);
            Integer days = daysInStock(listing, yard);
            DataRow row = new DataRow();
            row.sortDays = days == null ? -1 : days;
            row.cells.add(stockVehicle(vehicle, chassis));
            row.cells.add(text(blank(vehicle.getYear(), "—")));
            row.cells.add(text(blank(vehicle.getMileage(), "—")));
            row.cells.add(text(location));
            row.cells.add(costBreakdown(landing, cusdec, assessment, repair, total));
            Cell daysCell = text(days == null ? "—" : String.valueOf(days));
            if (days != null && days > 90) {
                daysCell.tone = "due";
                row.tone = "is-due";
            } else if (days != null && days > 60) {
                daysCell.tone = "soon";
            }
            row.cells.add(daysCell);
            rows.add(row);
        }
        rows.sort((left, right) -> Integer.compare(right.sortDays, left.sortDays));
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(new Kpi("On hand", String.valueOf(onHand), "Excludes sold units"));
        kpis.add(new Kpi("Total cost", moneyOrDash(onHandTotal, anyOnHandTotal), "Landing + CUSDEC + assessment + repair"));
        kpis.add(new Kpi("On sale", String.valueOf(onSale), "Vehicles on a sale floor"));
        kpis.add(new Kpi("On yard", String.valueOf(onYard), "Vehicles in a yard bay"));
        kpis.add(new Kpi("Missing total cost", String.valueOf(missingCost), "Landing, CUSDEC, or assessment missing"));
        return page(
                STOCK, "Vehicles on stock",
                "Vehicles still in stock, valued at total cost.",
                "IAS 2 inventory listing",
                query, kpis, columns, rows,
                "Total cost is landing (shipping, LKR) plus the CUSDEC value in LKR plus assessment total tax plus workshop repair cost. "
                        + "Repair cost is the actual amount, or the estimate when the actual is not set. "
                        + "A vehicle counts as missing a total cost when landing, CUSDEC, or assessment is absent. "
                        + "Days in stock uses the listing date, or the yard arrival date when the unit is not listed."
        );
    }

    private StandardReport salesRegister(Snapshot snap, String query, String from, String to) {
        List<Column> columns = columns(
                "Vehicle details", "Buyer details", "Location",
                "Sale price", "Total cost", "Gross profit", "Margin"
        );
        numeric(columns, 3, 4, 5, 6);
        List<DataRow> rows = new ArrayList<DataRow>();
        int missingPrice = 0;
        int priced = 0;
        int soldCount = 0;
        BigDecimal soldRevenue = BigDecimal.ZERO;
        BigDecimal soldCost = BigDecimal.ZERO;
        BigDecimal grossProfit = BigDecimal.ZERO;
        boolean anySoldRevenue = false;
        boolean anySoldCost = false;
        boolean anyGrossProfit = false;
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = snap.listings.get(chassis);
            if (!inRange(listing == null ? null : listing.getListedOn(), from, to)) {
                continue;
            }
            String place = reportService.placeOf(vehicle, listing);
            if (!ReportService.SOLD.equals(place)) {
                continue;
            }
            soldCount++;
            VehicleTotalCost cost = totalCost(snap, chassis);
            BigDecimal landing = cost.getLanding();
            BigDecimal cusdec = cost.getCusdec();
            BigDecimal assessment = cost.getAssessment();
            BigDecimal repair = cost.getRepair();
            BigDecimal total = cost.getTotal();
            BigDecimal sale = salePrice(listing);
            BigDecimal gp = sale != null && total != null ? sale.subtract(total) : null;
            String margin = "—";
            if (gp != null && sale != null && sale.compareTo(BigDecimal.ZERO) != 0) {
                margin = gp.multiply(BigDecimal.valueOf(100))
                        .divide(sale, 1, RoundingMode.HALF_UP).toPlainString() + "%";
            }
            if (sale == null) {
                missingPrice++;
            } else {
                priced++;
                soldRevenue = soldRevenue.add(sale);
                anySoldRevenue = true;
                if (total != null) {
                    soldCost = soldCost.add(total);
                    anySoldCost = true;
                }
            }
            if (gp != null) {
                grossProfit = grossProfit.add(gp);
                anyGrossProfit = true;
            }
            DataRow row = new DataRow();
            row.cells.add(vehicleDetails(vehicle, chassis));
            row.cells.add(buyerDetails(listing));
            row.cells.add(placeDetails(listing));
            row.cells.add(text(sale == null ? "—" : ReportService.formatMoney(sale)));
            row.cells.add(costBreakdown(landing, cusdec, assessment, repair, total));
            row.cells.add(moneyCell(gp));
            row.cells.add(toneText(margin, gp));
            rows.add(row);
        }
        String avg = priced == 0
                ? "—"
                : ReportService.formatMoney(soldRevenue.divide(BigDecimal.valueOf(priced), 2, RoundingMode.HALF_UP));
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(new Kpi("Units sold", String.valueOf(soldCount), "All sold chassis"));
        kpis.add(new Kpi("Revenue", moneyOrDash(soldRevenue, anySoldRevenue), "Advertised, else asking"));
        kpis.add(new Kpi("Total cost", moneyOrDash(soldCost, anySoldCost), "Landing + CUSDEC + assessment + repair"));
        kpis.add(new Kpi("Gross profit", moneyOrDash(grossProfit, anyGrossProfit), "Revenue minus total cost"));
        kpis.add(new Kpi("Average sold", avg, missingPrice + " without a price"));
        return page(
                SALES, "Sold vehicles",
                "Completed sales, valued at total cost.",
                "Dealer deal jacket",
                query, kpis, columns, rows,
                "Total cost is landing (shipping, LKR) plus the CUSDEC value in LKR plus assessment total tax plus workshop repair cost. "
                        + "Repair cost is the actual amount, or the estimate when the actual is not set. "
                        + "Revenue is the advertised price, or the asking price when it is not set. "
                        + "Gross profit is revenue minus that total cost. "
                        + "Sale date is not stored separately, so the listing date is shown."
        );
    }

    private StandardReport landedCost(Snapshot snap, String query, String from, String to) {
        List<Column> columns = columns(
                "Vehicle", "Shipping cost", "CUSDEC", "Total landing"
        );
        numeric(columns, 1, 2, 3);
        List<DataRow> rows = new ArrayList<DataRow>();
        BigDecimal shippingTotal = BigDecimal.ZERO;
        BigDecimal cusdecTotal = BigDecimal.ZERO;
        BigDecimal landingTotal = BigDecimal.ZERO;
        boolean anyShipping = false;
        boolean anyCusdec = false;
        boolean anyLanding = false;
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = snap.listings.get(chassis);
            YardRecord yard = snap.yards.get(chassis);
            if (!inStockPeriod(stockDate(listing, yard), from, to)) {
                continue;
            }
            CustomsDocument customs = snap.customs.get(chassis);
            BigDecimal shipping = landingOf(customs);
            BigDecimal cusdec = customs == null ? null : ReportService.parseMoney(customs.getValueNcy());
            BigDecimal total = sumMoney(shipping, cusdec);
            if (shipping != null) {
                shippingTotal = shippingTotal.add(shipping);
                anyShipping = true;
            }
            if (cusdec != null) {
                cusdecTotal = cusdecTotal.add(cusdec);
                anyCusdec = true;
            }
            if (total != null) {
                landingTotal = landingTotal.add(total);
                anyLanding = true;
            }
            DataRow row = new DataRow();
            row.cells.add(stockVehicle(vehicle, chassis));
            row.cells.add(shippingBreakdown(customs));
            row.cells.add(cusdecBreakdown(customs));
            row.cells.add(landingTotalBreakdown(shipping, cusdec, total));
            rows.add(row);
        }
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(new Kpi("Vehicles", String.valueOf(rows.size()), "Every vehicle in the period"));
        kpis.add(new Kpi("Shipping cost", moneyOrDash(shippingTotal, anyShipping), "Landing cost in LKR"));
        kpis.add(new Kpi("CUSDEC", moneyOrDash(cusdecTotal, anyCusdec), "Declaration value in LKR"));
        kpis.add(new Kpi("Total landing", moneyOrDash(landingTotal, anyLanding), "Shipping cost + CUSDEC"));
        return page(
                LANDED, "Landed cost sheet",
                "Every vehicle. Shipping cost plus the customs declaration, totaled in LKR.",
                "Incoterms / CIF roll-up",
                query, kpis, columns, rows,
                "Shipping cost is the bill of lading: landing cost in USD, the exchange rate, and landing cost in LKR. "
                        + "CUSDEC is the customs declaration price breakdown, and its value in LKR is the amount added. "
                        + "Total landing is that shipping cost plus the CUSDEC value. "
                        + "The period uses the stock date: the listing date, or the yard arrival date when the unit is not listed."
        );
    }

    private StandardReport dutyTax(Snapshot snap, String query, String from, String to) {
        List<Column> columns = columns(
                "Vehicle", "Engine capacity", "CID", "SUR", "XID", "VAT", "VEL", "Total tax"
        );
        numeric(columns, 2, 3, 4, 5, 6, 7);
        List<DataRow> rows = new ArrayList<DataRow>();
        BigDecimal cidTotal = BigDecimal.ZERO;
        BigDecimal surTotal = BigDecimal.ZERO;
        BigDecimal xidTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal velTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        boolean anyCid = false;
        boolean anySur = false;
        boolean anyXid = false;
        boolean anyVat = false;
        boolean anyVel = false;
        boolean anyTax = false;
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = snap.listings.get(chassis);
            YardRecord yard = snap.yards.get(chassis);
            if (!inStockPeriod(stockDate(listing, yard), from, to)) {
                continue;
            }
            CustomsDocument customs = snap.customs.get(chassis);
            BigDecimal cid = customs == null ? null : dutyAmount(customs.getAssessmentTaxCid(), customs.getTaxCid());
            BigDecimal sur = customs == null ? null : dutyAmount(customs.getAssessmentTaxSur(), customs.getTaxSur());
            BigDecimal xid = customs == null ? null : dutyAmount(customs.getAssessmentTaxXid(), customs.getTaxXid());
            BigDecimal vat = customs == null ? null : dutyAmount(customs.getAssessmentTaxVat(), customs.getTaxVat());
            BigDecimal vel = customs == null ? null : dutyAmount(customs.getAssessmentTaxVel(), customs.getTaxVel());
            BigDecimal tax = dutyTotal(customs);
            if (cid != null) {
                cidTotal = cidTotal.add(cid);
                anyCid = true;
            }
            if (sur != null) {
                surTotal = surTotal.add(sur);
                anySur = true;
            }
            if (xid != null) {
                xidTotal = xidTotal.add(xid);
                anyXid = true;
            }
            if (vat != null) {
                vatTotal = vatTotal.add(vat);
                anyVat = true;
            }
            if (vel != null) {
                velTotal = velTotal.add(vel);
                anyVel = true;
            }
            if (tax != null) {
                taxTotal = taxTotal.add(tax);
                anyTax = true;
            }
            DataRow row = new DataRow();
            row.cells.add(stockVehicle(vehicle, chassis));
            row.cells.add(text(engineCapacity(customs)));
            row.cells.add(text(cid == null ? "—" : ReportService.formatMoney(cid)));
            row.cells.add(text(sur == null ? "—" : ReportService.formatMoney(sur)));
            row.cells.add(text(xid == null ? "—" : ReportService.formatMoney(xid)));
            row.cells.add(text(vat == null ? "—" : ReportService.formatMoney(vat)));
            row.cells.add(text(vel == null ? "—" : ReportService.formatMoney(vel)));
            row.cells.add(text(tax == null ? "—" : ReportService.formatMoney(tax)));
            rows.add(row);
        }
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(new Kpi("CID", moneyOrDash(cidTotal, anyCid), "Customs import duty"));
        kpis.add(new Kpi("SUR", moneyOrDash(surTotal, anySur), "Surcharge"));
        kpis.add(new Kpi("XID", moneyOrDash(xidTotal, anyXid), "Excise duty"));
        kpis.add(new Kpi("VAT", moneyOrDash(vatTotal, anyVat), "Value added tax"));
        kpis.add(new Kpi("VEL", moneyOrDash(velTotal, anyVel), "Vehicle entitlement levy"));
        kpis.add(new Kpi("Total tax", moneyOrDash(taxTotal, anyTax), "Assessment total"));
        return page(
                DUTY, "Duty and tax summary",
                "Every vehicle. Customs duty and tax from the assessment notice.",
                "Sri Lanka CUSDEC / ASYCUDA",
                query, kpis, columns, rows,
                "CID, SUR, XID, VAT, and VEL come from the assessment notice. "
                        + "Total tax is the assessment total. "
                        + "Engine capacity is the customs capacity, or the JEVIC capacity when that is the one on file. "
                        + "The period uses the stock date: the listing date, or the yard arrival date when the unit is not listed."
        );
    }

    private StandardReport documents(Snapshot snap, String query, String from, String to) {
        List<Column> columns = columns(
                "Vehicle", "Export", "JEVIC", "CUSDEC", "Assessment", "BL", "Standards", "RMV"
        );
        List<DataRow> rows = new ArrayList<DataRow>();
        int exportCount = 0;
        int jevicCount = 0;
        int cusdecCount = 0;
        int assessmentCount = 0;
        int blCount = 0;
        int standardsCount = 0;
        int rmvCount = 0;
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = snap.listings.get(chassis);
            YardRecord yard = snap.yards.get(chassis);
            if (!inStockPeriod(stockDate(listing, yard), from, to)) {
                continue;
            }
            CustomsDocument customs = snap.customs.get(chassis);
            ExportCertificate export = snap.exports.get(chassis);
            StandardsCertificate standards = snap.standards.get(chassis);
            VehicleRegistration registration = snap.registrations.get(chassis);
            boolean exportOk = present(
                    export == null ? null : export.getCertificateNo(),
                    export == null ? null : export.getDocumentStoredName());
            boolean jevicOk = customs != null && present(customs.getJevicCertificateNo(), customs.getPage1StoredName());
            boolean cusdecOk = customs != null && present(customs.getCustomsReference(), customs.getPage2StoredName());
            boolean assessmentOk = customs != null && present(customs.getAssessmentNoticeRef(), customs.getPage3StoredName());
            boolean blOk = customs != null && present(customs.getBlNo(), customs.getPage5StoredName());
            boolean standardsOk = present(
                    standards == null ? null : standards.getCertificateNo(),
                    standards == null ? null : standards.getDocumentStoredName());
            boolean rmvOk = registration != null && filled(registration.getRegistrationNo());
            if (exportOk) {
                exportCount++;
            }
            if (jevicOk) {
                jevicCount++;
            }
            if (cusdecOk) {
                cusdecCount++;
            }
            if (assessmentOk) {
                assessmentCount++;
            }
            if (blOk) {
                blCount++;
            }
            if (standardsOk) {
                standardsCount++;
            }
            if (rmvOk) {
                rmvCount++;
            }
            String encoded = chassisPath(chassis);
            DataRow row = new DataRow();
            row.cells.add(stockVehicle(vehicle, chassis));
            row.cells.add(docLink(
                    exportOk,
                    viewHref(fileHref("/export/documents/", export == null ? null : export.getDocumentStoredName()), "/export/" + encoded),
                    "/export/" + encoded));
            row.cells.add(docLink(
                    jevicOk,
                    viewHref(fileHref("/customs/documents/", customs == null ? null : customs.getPage1StoredName()), "/jevic/" + encoded),
                    "/jevic/" + encoded));
            row.cells.add(docLink(
                    cusdecOk,
                    viewHref(fileHref("/customs/documents/", customs == null ? null : customs.getPage2StoredName()), "/declaration/" + encoded),
                    "/declaration/" + encoded));
            row.cells.add(docLink(
                    assessmentOk,
                    viewHref(fileHref("/customs/documents/", customs == null ? null : customs.getPage3StoredName()), "/assessment/" + encoded),
                    "/assessment/" + encoded));
            row.cells.add(docLink(
                    blOk,
                    viewHref(fileHref("/customs/documents/", customs == null ? null : customs.getPage5StoredName()), "/bl/" + encoded),
                    "/bl/" + encoded));
            row.cells.add(docLink(
                    standardsOk,
                    viewHref(fileHref("/standards/documents/", standards == null ? null : standards.getDocumentStoredName()), "/standards/" + encoded),
                    "/standards/" + encoded));
            row.cells.add(docLink(rmvOk, "/registration/" + encoded, "/registration/" + encoded));
            rows.add(row);
        }
        int total = rows.size();
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(docKpi("Export", exportCount, total));
        kpis.add(docKpi("JEVIC", jevicCount, total));
        kpis.add(docKpi("CUSDEC", cusdecCount, total));
        kpis.add(docKpi("Assessment", assessmentCount, total));
        kpis.add(docKpi("BL", blCount, total));
        kpis.add(docKpi("Standards", standardsCount, total));
        kpis.add(docKpi("RMV", rmvCount, total));
        return page(
                DOCUMENTS, "Document completeness",
                "Every vehicle, with a view or upload link for each document.",
                "Compliance register",
                query, kpis, columns, rows,
                "View opens the stored file, or the document form when the record is saved without a file. "
                        + "Upload opens the form. "
                        + "The period uses the stock date: the listing date, or the yard arrival date when the unit is not listed."
        );
    }

    private StandardReport expiry(Snapshot snap, String query, String from, String to) {
        List<Column> columns = columns(
                "Engine number", "Chassis", "Vehicle", "Reg. no", "Insurance", "Policy",
                "Insurance expiry", "Revenue licence", "Licence expiry", "Status"
        );
        List<DataRow> rows = new ArrayList<DataRow>();
        int expired = 0;
        int due = 0;
        int ok = 0;
        int missing = 0;
        LocalDate today = LocalDate.now();
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            VehicleRegistration registration = snap.registrations.get(chassis);
            if (registration == null) {
                continue;
            }
            if (!inRange(registration.getInsuranceExpiry(), from, to)
                    && !inRange(registration.getRevenueLicenseExpiry(), from, to)
                    && hasPeriod(from, to)) {
                continue;
            }
            String insuranceStatus = expiryStatus(registration.getInsuranceExpiry(), today);
            String licenceStatus = expiryStatus(registration.getRevenueLicenseExpiry(), today);
            String combined = worstExpiry(insuranceStatus, licenceStatus);
            if ("Expired".equals(combined)) {
                expired++;
            } else if ("Due in 30 days".equals(combined)) {
                due++;
            } else if ("Current".equals(combined)) {
                ok++;
            } else {
                missing++;
            }
            DataRow row = new DataRow();
            row.cells.add(text(blank(engineLabel(snap, chassis), "—")));
            row.cells.add(summaryLink(chassis, chassis));
            row.cells.add(text(vehicleLabel(vehicle)));
            row.cells.add(text(blank(registration.getRegistrationNo(), "—")));
            row.cells.add(text(blank(registration.getInsuranceCompany(), "—")));
            row.cells.add(text(blank(registration.getInsurancePolicyNo(), "—")));
            row.cells.add(text(slashDate(registration.getInsuranceExpiry())));
            row.cells.add(text(blank(registration.getRevenueLicenseNo(), "—")));
            row.cells.add(text(slashDate(registration.getRevenueLicenseExpiry())));
            row.cells.add(pill(combined, expiryPill(combined)));
            rows.add(row);
        }
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(new Kpi("On register", String.valueOf(rows.size()), "Vehicles with an RMV record"));
        kpis.add(new Kpi("Expired", String.valueOf(expired), "Insurance or licence past due"));
        kpis.add(new Kpi("Due in 30 days", String.valueOf(due), "Renew soon"));
        kpis.add(new Kpi("Current", String.valueOf(ok), "Both dates in the clear"));
        kpis.add(new Kpi("Missing dates", String.valueOf(missing), "Fill insurance or licence expiry"));
        return page(
                EXPIRY, "Licence and insurance expiry",
                "Revenue licence and insurance diary from the RMV record.",
                "Statutory expiry register",
                query, kpis, columns, rows,
                "Only vehicles with a registration record appear here."
        );
    }

    private StandardReport workshop(Snapshot snap, String query, String from, String to) {
        List<Column> columns = columns(
                "Vehicle", "Repairs", "Total repair cost", "Status"
        );
        numeric(columns, 2);
        List<DataRow> rows = new ArrayList<DataRow>();
        int repairs = 0;
        int completed = 0;
        int pending = 0;
        BigDecimal repairTotal = BigDecimal.ZERO;
        boolean anyRepairTotal = false;
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            WorkshopJob job = snap.jobs.get(chassis);
            List<WorkshopJobLine> lines = workshopLines(job, from, to);
            if (lines.isEmpty()) {
                continue;
            }
            BigDecimal total = null;
            for (WorkshopJobLine line : lines) {
                repairs++;
                BigDecimal cost = lineCost(line);
                if (cost != null) {
                    total = total == null ? cost : total.add(cost);
                }
            }
            if (total != null) {
                repairTotal = repairTotal.add(total);
                anyRepairTotal = true;
            }
            boolean done = job.isCompleted() || "COMPLETE".equalsIgnoreCase(blank(job.getJobStatus(), ""));
            if (done) {
                completed++;
            } else {
                pending++;
            }
            DataRow row = new DataRow();
            row.cells.add(stockVehicle(vehicle, chassis));
            row.cells.add(repairBreakdown(lines));
            row.cells.add(text(total == null ? "—" : ReportService.formatMoney(total)));
            row.cells.add(done ? pill("Completed", "leaf") : pill("Pending", "gold"));
            rows.add(row);
        }
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(new Kpi("Repairs", String.valueOf(repairs), "Repair lines in this period"));
        kpis.add(new Kpi("Total repair cost", moneyOrDash(repairTotal, anyRepairTotal), "Actual, else the estimate"));
        kpis.add(new Kpi("Completed", String.valueOf(completed), "Workshop marked complete"));
        kpis.add(new Kpi("Pending", String.valueOf(pending), "Workshop still open"));
        return page(
                WORKSHOP, "Workshop WIP and cost",
                "Vehicles with at least one repair.",
                "Repair order cost",
                query, kpis, columns, rows,
                "Each repair shows the name, cost, start date, and completion date. "
                        + "Cost is the actual amount, or the estimate when the actual is not set. "
                        + "The period uses the workshop date: the repair start date, or the completion date when the repair has not started."
        );
    }

    private StandardReport pipeline(Snapshot snap, String query, String section, String from, String to) {
        String selected = normalizeSection(section);
        List<Column> columns = columns(
                "Engine number", "Chassis", "Vehicle", "Year", "Stage", "Landing cost",
                "Asking", "Advertised", "Location", "Listed on"
        );
        numeric(columns, 5, 6, 7);
        List<DataRow> rows = new ArrayList<DataRow>();
        int importCount = 0;
        int customsCount = 0;
        int preparationCount = 0;
        int readyCount = 0;
        int listedCount = 0;
        int soldCount = 0;
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = snap.listings.get(chassis);
            YardRecord yard = snap.yards.get(chassis);
            if (!inRange(stockDate(listing, yard), from, to)) {
                continue;
            }
            String place = reportService.placeOf(vehicle, listing);
            if (ReportService.IMPORT.equals(place)) {
                importCount++;
            } else if (ReportService.CUSTOMS.equals(place)) {
                customsCount++;
            } else if (ReportService.PREPARATION.equals(place)) {
                preparationCount++;
            } else if (ReportService.READY.equals(place)) {
                readyCount++;
            } else if (ReportService.LISTED.equals(place)) {
                listedCount++;
            } else if (ReportService.SOLD.equals(place)) {
                soldCount++;
            }
            if (!ReportService.ALL.equals(selected) && !selected.equals(place)) {
                continue;
            }
            CustomsDocument customs = snap.customs.get(chassis);
            DataRow row = new DataRow();
            row.cells.add(text(blank(engineLabel(snap, chassis), "—")));
            row.cells.add(summaryLink(chassis, chassis));
            row.cells.add(text(vehicleLabel(vehicle)));
            row.cells.add(text(blank(vehicle.getYear(), "—")));
            row.cells.add(pill(labelFor(place), pillFor(place)));
            row.cells.add(text(displayMoney(customs == null ? null : customs.getLandingCostLkr())));
            row.cells.add(text(listing == null ? "—" : displayMoney(listing.getAskingPrice())));
            row.cells.add(text(listing == null ? "—" : displayMoney(listing.getAdvertisedPrice())));
            row.cells.add(text(listing == null ? "—" : blank(listing.getSaleLocation(), "—")));
            row.cells.add(text(listing == null ? "—" : slashDate(listing.getListedOn())));
            rows.add(row);
        }
        List<Kpi> kpis = new ArrayList<Kpi>();
        kpis.add(filterKpi("Import", importCount, "import", query, from, to));
        kpis.add(filterKpi("Customs", customsCount, "customs", query, from, to));
        kpis.add(filterKpi("Preparation", preparationCount, "preparation", query, from, to));
        kpis.add(filterKpi("Ready", readyCount, "ready", query, from, to));
        kpis.add(filterKpi("Listed", listedCount, "listed", query, from, to));
        kpis.add(filterKpi("Sold", soldCount, "sold", query, from, to));
        StandardReport report = page(
                PIPELINE, "Pipeline board",
                "Live position of every chassis in the operating flow.",
                "WIP / cycle board",
                query, kpis, columns, rows,
                "Use the stage totals to filter this board. Money columns follow the bill of lading and sale listing."
        );
        report.section = selected;
        report.filterLinks = true;
        report.allHref = pipelineHref(ReportService.ALL, query, from, to);
        return report;
    }

    private static Kpi filterKpi(String label, int count, String section, String query, String from, String to) {
        return new Kpi(label, String.valueOf(count), "Open this stage", pipelineHref(section, query, from, to), section);
    }

    private static String pipelineHref(String section, String query, String from, String to) {
        String href = "/reports/pipeline?section=" + section;
        if (query != null && !query.trim().isEmpty()) {
            href += "&q=" + UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
        }
        if (from != null && !from.isEmpty()) {
            href += "&from=" + UriUtils.encodeQueryParam(from, StandardCharsets.UTF_8);
        }
        if (to != null && !to.isEmpty()) {
            href += "&to=" + UriUtils.encodeQueryParam(to, StandardCharsets.UTF_8);
        }
        return href;
    }

    public Snapshot load(String query) {
        Snapshot snap = new Snapshot();
        snap.vehicles = vehicleService.search(query == null ? "" : query.trim());
        snap.vehicles.sort((left, right) -> {
            String a = left.getChassisNo() == null ? "" : left.getChassisNo();
            String b = right.getChassisNo() == null ? "" : right.getChassisNo();
            return a.compareToIgnoreCase(b);
        });
        for (SaleListing listing : saleListingRepository.findAll()) {
            if (listing.getChassisNo() != null) {
                snap.listings.put(listing.getChassisNo(), listing);
            }
        }
        for (CustomsDocument document : customsDocumentRepository.findAll()) {
            if (document.getChassisNo() != null) {
                snap.customs.put(document.getChassisNo(), document);
            }
        }
        for (WorkshopJob job : workshopJobRepository.findAll()) {
            if (job.getChassisNo() != null) {
                snap.jobs.put(job.getChassisNo(), job);
            }
        }
        for (YardRecord yard : yardRecordRepository.findAll()) {
            if (yard.getChassisNo() != null) {
                snap.yards.put(yard.getChassisNo(), yard);
            }
        }
        for (VehicleRegistration registration : vehicleRegistrationRepository.findAll()) {
            if (registration.getChassisNo() != null) {
                snap.registrations.put(registration.getChassisNo(), registration);
            }
        }
        for (ExportCertificate export : exportCertificateRepository.findAll()) {
            if (export.getChassisNo() != null) {
                snap.exports.put(export.getChassisNo(), export);
            }
        }
        for (StandardsCertificate standards : standardsCertificateRepository.findAll()) {
            if (standards.getChassisNo() != null) {
                snap.standards.put(standards.getChassisNo(), standards);
            }
        }
        for (GradeSearch grade : gradeSearchRepository.findAll()) {
            if (grade.getChassisNo() != null) {
                snap.grades.put(grade.getChassisNo(), grade);
            }
        }
        for (VehicleInspection inspection : vehicleInspectionRepository.findAll()) {
            if (inspection.getChassisNo() != null) {
                snap.inspections.put(inspection.getChassisNo(), inspection);
            }
        }
        for (RegistrationCertificate book : registrationCertificateRepository.findAll()) {
            rememberEngine(snap, book.getChassisNo(), book.getEngineNo());
        }
        for (InspectionCertificate certificate : inspectionCertificateRepository.findAll()) {
            rememberEngine(snap, certificate.getChassisNo(), certificate.getEngineNo());
        }
        for (PreShipmentInspection inspection : preShipmentInspectionRepository.findAll()) {
            rememberEngine(snap, inspection.getChassisNo(), inspection.getEngineNo());
        }
        for (CustomsDocument customs : snap.customs.values()) {
            rememberEngine(snap, customs.getChassisNo(), customs.getJevicEngineNo());
            rememberEngine(snap, customs.getChassisNo(), customs.getClearanceEngineNo());
        }
        for (VehiclePhoto photo : vehiclePhotoRepository.findAll()) {
            if (photo.getChassisNo() != null) {
                snap.photos.compute(photo.getChassisNo(), (k, soFar) -> soFar == null ? 1 : soFar + 1);
            }
        }
        snap.photoChassis = snap.photos.size();
        LocalDate today = LocalDate.now();
        for (Vehicle vehicle : snap.vehicles) {
            String chassis = vehicle.getChassisNo();
            SaleListing listing = snap.listings.get(chassis);
            CustomsDocument customs = snap.customs.get(chassis);
            String place = reportService.placeOf(vehicle, listing);
            snap.landedRows++;
            BigDecimal landing = landingOf(customs);
            BigDecimal cusdec = customs == null ? null : ReportService.parseMoney(customs.getValueNcy());
            BigDecimal totalLanding = sumMoney(landing, cusdec);
            if (totalLanding != null) {
                snap.landingTotal = snap.landingTotal.add(totalLanding);
                snap.anyLanding = true;
            }
            if (customs != null) {
                BigDecimal invoice = ReportService.parseMoney(customs.getInvoiceTotal());
                if (invoice != null) {
                    snap.invoiceTotal = snap.invoiceTotal.add(invoice);
                    snap.anyInvoice = true;
                }
                BigDecimal tax = dutyTotal(customs);
                if (tax != null) {
                    snap.taxTotal = snap.taxTotal.add(tax);
                    snap.anyTax = true;
                }
                if (hasDuty(customs)) {
                    snap.dutyRows++;
                }
                BigDecimal paid = ReportService.parseMoney(customs.getAssessmentTotalPaid());
                if (paid != null) {
                    snap.paidTotal = snap.paidTotal.add(paid);
                    snap.anyPaid = true;
                }
            }
            if (ReportService.SOLD.equals(place)) {
                snap.soldCount++;
                BigDecimal sale = salePrice(listing);
                if (sale != null) {
                    snap.soldRevenue = snap.soldRevenue.add(sale);
                    snap.anySoldRevenue = true;
                    if (landing != null) {
                        snap.soldLanding = snap.soldLanding.add(landing);
                        snap.anySoldLanding = true;
                        snap.grossProfit = snap.grossProfit.add(sale.subtract(landing));
                        snap.anyGrossProfit = true;
                    }
                }
            } else {
                snap.onHand++;
                if (landing != null) {
                    snap.onHandLanding = snap.onHandLanding.add(landing);
                    snap.anyOnHandLanding = true;
                }
            }
            WorkshopJob job = snap.jobs.get(chassis);
            if (job != null) {
                boolean anyLine = false;
                for (WorkshopJobLine line : job.getLines()) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    anyLine = true;
                    BigDecimal cost = lineCost(line);
                    if (cost != null) {
                        snap.workshopActual = snap.workshopActual.add(cost);
                        snap.anyWorkshopActual = true;
                    }
                }
                if (anyLine) {
                    snap.workshopVehicles++;
                }
                if (!job.isCompleted()) {
                    snap.openJobs++;
                }
            }
            if (documentComplete(vehicle, snap)) {
                snap.completeDocs++;
            } else {
                snap.incompleteDocs++;
            }
            VehicleRegistration registration = snap.registrations.get(chassis);
            if (registration != null) {
                String status = worstExpiry(
                        expiryStatus(registration.getInsuranceExpiry(), today),
                        expiryStatus(registration.getRevenueLicenseExpiry(), today)
                );
                if ("Expired".equals(status) || "Due in 30 days".equals(status)) {
                    snap.expiryWatch++;
                } else if ("Current".equals(status)) {
                    snap.expiryOk++;
                }
            }
        }
        return snap;
    }

    private boolean documentComplete(Vehicle vehicle, Snapshot snap) {
        String chassis = vehicle.getChassisNo();
        CustomsDocument customs = snap.customs.get(chassis);
        ExportCertificate export = snap.exports.get(chassis);
        StandardsCertificate standards = snap.standards.get(chassis);
        VehicleRegistration registration = snap.registrations.get(chassis);
        return present(export == null ? null : export.getCertificateNo(), export == null ? null : export.getDocumentStoredName())
                && customs != null && present(customs.getJevicCertificateNo(), customs.getPage1StoredName())
                && present(customs.getCustomsReference(), customs.getPage2StoredName())
                && present(customs.getAssessmentNoticeRef(), customs.getPage3StoredName())
                && present(customs.getBlNo(), customs.getPage5StoredName())
                && present(standards == null ? null : standards.getCertificateNo(), standards == null ? null : standards.getDocumentStoredName())
                && registration != null && filled(registration.getRegistrationNo());
    }

    private static StandardReport page(
            String kind,
            String title,
            String lead,
            String standard,
            String query,
            List<Kpi> kpis,
            List<Column> columns,
            List<DataRow> rows,
            String note
    ) {
        StandardReport report = new StandardReport();
        report.kind = kind;
        report.title = title;
        report.lead = lead;
        report.standard = standard;
        report.generatedOn = LocalDate.now().format(PRINTED);
        report.searchQuery = query == null ? "" : query.trim();
        report.kpis = kpis;
        report.columns = columns;
        report.rows = rows;
        report.note = note;
        report.section = ReportService.ALL;
        return report;
    }

    private static List<Column> columns(String... labels) {
        List<Column> columns = new ArrayList<Column>();
        for (String label : labels) {
            columns.add(new Column(label, false));
        }
        return columns;
    }

    private static void numeric(List<Column> columns, int... indexes) {
        for (int index : indexes) {
            if (index >= 0 && index < columns.size()) {
                columns.get(index).numeric = true;
            }
        }
    }

    private static Cell text(String value) {
        return new Cell(value, null, null, "");
    }

    private static Cell link(String value, String href) {
        return new Cell(value, href, null, "");
    }

    private static Cell docLink(boolean present, String viewHref, String uploadHref) {
        Cell cell = link(present ? "View" : "Upload", present ? viewHref : uploadHref);
        cell.tone = present ? "is-view" : "is-upload";
        return cell;
    }

    private static Kpi docKpi(String label, int present, int total) {
        int missing = total - present;
        return new Kpi(label, String.valueOf(present), missing + " to upload");
    }

    private static boolean present(String... values) {
        for (String value : values) {
            if (filled(value)) {
                return true;
            }
        }
        return false;
    }

    private static String chassisPath(String chassis) {
        return UriUtils.encodePathSegment(chassis == null ? "" : chassis, StandardCharsets.UTF_8);
    }

    private static String fileHref(String prefix, String storedName) {
        if (!filled(storedName)) {
            return null;
        }
        return prefix + UriUtils.encodePathSegment(storedName.trim(), StandardCharsets.UTF_8);
    }

    private static String viewHref(String file, String form) {
        return file == null ? form : file;
    }

    private static Cell summaryLink(String value, String chassis) {
        return link(filled(value) ? value.trim() : "—", summaryHref(chassis));
    }

    private static String summaryHref(String chassis) {
        String encoded = UriUtils.encodePathSegment(chassis == null ? "" : chassis, StandardCharsets.UTF_8);
        return "/vehicle/" + encoded;
    }

    private static Cell vehicleDetails(Vehicle vehicle, String chassis) {
        String name = vehicleLabel(vehicle);
        String year = blank(vehicle.getYear(), "—");
        String identity = filled(chassis) ? chassis.trim() : "—";
        Cell cell = text(identity + " · " + name + " · " + year);
        List<CostLine> lines = new ArrayList<CostLine>();
        lines.add(new CostLine("", identity, false, summaryHref(chassis)));
        lines.add(new CostLine("", name, false));
        lines.add(new CostLine("", year, false));
        cell.lines = lines;
        cell.tone = "is-details";
        return cell;
    }

    private static Cell stockVehicle(Vehicle vehicle, String chassis) {
        String name = vehicleLabel(vehicle);
        String identity = filled(chassis) ? chassis.trim() : "—";
        Cell cell = text(identity + " · " + name);
        List<CostLine> lines = new ArrayList<CostLine>();
        lines.add(new CostLine("", identity, false, summaryHref(chassis)));
        lines.add(new CostLine("", name, false));
        cell.lines = lines;
        cell.tone = "is-details";
        return cell;
    }

    private static Cell buyerDetails(SaleListing listing) {
        String name = listing == null ? "—" : blank(listing.getBuyerFullName(), "—");
        String nic = listing == null ? "—" : blank(listing.getBuyerNicNo(), "—");
        Cell cell = text(name + " · " + nic);
        List<CostLine> lines = new ArrayList<CostLine>();
        lines.add(new CostLine("", name, false));
        lines.add(new CostLine("", nic, false));
        cell.lines = lines;
        cell.tone = "is-details";
        return cell;
    }

    private static Cell placeDetails(SaleListing listing) {
        String location = listing == null ? "—" : blank(listing.getSaleLocation(), "—");
        String listed = listing == null ? "—" : slashDate(listing.getListedOn());
        Cell cell = text(location + " · " + listed);
        List<CostLine> lines = new ArrayList<CostLine>();
        lines.add(new CostLine("", location, false));
        lines.add(new CostLine("Listed", listed, false));
        cell.lines = lines;
        cell.tone = "is-details";
        return cell;
    }

    private static String engineLabel(Snapshot snap, String chassis) {
        if (!filled(chassis)) {
            return null;
        }
        return snap.engines.get(chassis.trim());
    }

    private static void rememberEngine(Snapshot snap, String chassis, String engineNo) {
        if (!filled(chassis) || !filled(engineNo) || snap.engines.containsKey(chassis.trim())) {
            return;
        }
        snap.engines.put(chassis.trim(), engineNo.trim());
    }

    private static Cell pill(String value, String pill) {
        return new Cell(value, null, pill, "");
    }

    private static Cell flag(boolean ok, boolean applicable) {
        if (!applicable) {
            return pill("N/A", "muted-pill");
        }
        return ok ? pill("On file", "leaf") : pill("Missing", "gold");
    }

    private static Cell moneyCell(BigDecimal value) {
        if (value == null) {
            return text("—");
        }
        Cell cell = text(ReportService.formatMoney(value));
        cell.tone = value.signum() < 0 ? "down" : (value.signum() > 0 ? "up" : "");
        return cell;
    }

    private static Cell toneText(String value, BigDecimal amount) {
        Cell cell = text(value);
        if (amount != null) {
            cell.tone = amount.signum() < 0 ? "down" : (amount.signum() > 0 ? "up" : "");
        }
        return cell;
    }

    private static VehicleTotalCost totalCost(Snapshot snap, String chassis) {
        return VehicleTotalCost.of(snap.customs.get(chassis), snap.jobs.get(chassis));
    }

    private static BigDecimal landingOf(CustomsDocument customs) {
        return VehicleTotalCost.landingOf(customs);
    }

    private static BigDecimal sumMoney(BigDecimal... parts) {
        BigDecimal total = null;
        for (BigDecimal part : parts) {
            if (part == null) {
                continue;
            }
            total = total == null ? part : total.add(part);
        }
        return total;
    }

    private static Cell costBreakdown(
            BigDecimal landing,
            BigDecimal cusdec,
            BigDecimal assessment,
            BigDecimal repair,
            BigDecimal total
    ) {
        if (total == null) {
            return text("—");
        }
        List<CostLine> lines = new ArrayList<CostLine>();
        addCostLine(lines, "Landing", landing);
        addCostLine(lines, "CUSDEC", cusdec);
        addCostLine(lines, "Assessment", assessment);
        addCostLine(lines, "Repair", repair);
        addCostLine(lines, "Total", total);
        StringBuilder plain = new StringBuilder();
        for (CostLine line : lines) {
            if (plain.length() > 0) {
                plain.append("; ");
            }
            plain.append(line.label).append(' ').append(line.amount);
        }
        Cell cell = text(plain.toString());
        cell.lines = lines;
        return cell;
    }

    private static Cell shippingBreakdown(CustomsDocument customs) {
        if (customs == null) {
            return text("—");
        }
        List<CostLine> lines = new ArrayList<CostLine>();
        addTextLine(lines, "Landing cost (USD)", foreignAmount(customs.getLandingCostUsd(), "USD"), false);
        addTextLine(lines, "Exchange rate", rateAmount(customs.getBlExchangeRate()), false);
        addTextLine(lines, "Landing cost (LKR)", moneyText(customs.getLandingCostLkr()), true);
        return priceStack(lines);
    }

    private static Cell cusdecBreakdown(CustomsDocument customs) {
        if (customs == null) {
            return text("—");
        }
        List<CostLine> lines = new ArrayList<CostLine>();
        addTextLine(lines, "FOB", foreignAmount(customs.getInvoiceFob(), "JPY"), false);
        addTextLine(lines, "Freight", foreignAmount(customs.getInvoiceFreight(), "JPY"), false);
        addTextLine(lines, "Insurance", foreignAmount(customs.getInvoiceInsurance(), "JPY"), false);
        addTextLine(lines, "Other", foreignAmount(customs.getInvoiceOther(), "JPY"), false);
        addTextLine(lines, "Invoice total", foreignAmount(customs.getInvoiceTotal(), "JPY"), false);
        addTextLine(lines, "Exchange rate", rateAmount(customs.getExchangeRate()), false);
        addTextLine(lines, "Value (LKR)", moneyText(customs.getValueNcy()), true);
        return priceStack(lines);
    }

    private static Cell landingTotalBreakdown(BigDecimal shipping, BigDecimal cusdec, BigDecimal total) {
        List<CostLine> lines = new ArrayList<CostLine>();
        if (shipping != null) {
            lines.add(new CostLine("Shipping", ReportService.formatMoney(shipping), false));
        }
        if (cusdec != null) {
            lines.add(new CostLine("CUSDEC", ReportService.formatMoney(cusdec), false));
        }
        if (total != null) {
            lines.add(new CostLine("Total", ReportService.formatMoney(total), true));
        }
        return priceStack(lines);
    }

    private static Cell priceStack(List<CostLine> lines) {
        if (lines.isEmpty()) {
            return text("—");
        }
        StringBuilder plain = new StringBuilder();
        for (CostLine line : lines) {
            if (plain.length() > 0) {
                plain.append("; ");
            }
            plain.append(line.label).append(' ').append(line.amount);
        }
        Cell cell = text(plain.toString());
        cell.lines = lines;
        return cell;
    }

    private static void addTextLine(List<CostLine> lines, String label, String amount, boolean total) {
        if (amount == null) {
            return;
        }
        lines.add(new CostLine(label, amount, total));
    }

    private static String moneyText(String raw) {
        BigDecimal parsed = ReportService.parseMoney(raw);
        return parsed == null ? null : ReportService.formatMoney(parsed);
    }

    private static String foreignAmount(String raw, String currency) {
        BigDecimal parsed = ReportService.parseMoney(raw);
        if (parsed == null) {
            return null;
        }
        return currency + " " + new DecimalFormat("#,##0.##").format(parsed);
    }

    private static String rateAmount(String raw) {
        BigDecimal parsed = ReportService.parseMoney(raw);
        if (parsed == null) {
            return null;
        }
        return new DecimalFormat("#,##0.####").format(parsed);
    }

    private static void addCostLine(List<CostLine> lines, String label, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        lines.add(new CostLine(label, ReportService.formatMoney(amount), "Total".equals(label)));
    }

    private static BigDecimal salePrice(SaleListing listing) {
        if (listing == null) {
            return null;
        }
        BigDecimal advertised = ReportService.parseMoney(listing.getAdvertisedPrice());
        if (advertised != null) {
            return advertised;
        }
        return ReportService.parseMoney(listing.getAskingPrice());
    }

    private static String locationOf(SaleListing listing, YardRecord yard, String place) {
        if (listing != null && filled(listing.getSaleLocation())) {
            return listing.getSaleLocation().trim();
        }
        if (yard != null && filled(yard.getBayNo())) {
            String bay = yard.getBayNo().trim();
            if (filled(yard.getYardSection())) {
                return yard.getYardSection().trim() + " · " + bay;
            }
            return bay;
        }
        return labelFor(place);
    }

    private static String stockDate(SaleListing listing, YardRecord yard) {
        if (listing != null && filled(listing.getListedOn())) {
            return listing.getListedOn();
        }
        return yard == null ? null : yard.getArrivalDate();
    }

    private static String customsDate(CustomsDocument customs) {
        if (customs == null) {
            return null;
        }
        if (filled(customs.getWorksheetBlDate())) {
            return customs.getWorksheetBlDate();
        }
        if (filled(customs.getJevicInspectionDate())) {
            return customs.getJevicInspectionDate();
        }
        return customs.getDeclarantDate();
    }

    private static String documentDate(Vehicle vehicle, Snapshot snap) {
        String chassis = vehicle.getChassisNo();
        VehicleRegistration registration = snap.registrations.get(chassis);
        if (registration != null && filled(registration.getRegisteredDate())) {
            return registration.getRegisteredDate();
        }
        return customsDate(snap.customs.get(chassis));
    }

    private static Integer daysInStock(SaleListing listing, YardRecord yard) {
        return daysInStock(
                listing == null ? null : listing.getListedOn(),
                yard == null ? null : yard.getArrivalDate()
        );
    }

    public static Integer daysInStock(String listedOn, String arrivalDate) {
        String raw = filled(listedOn) ? listedOn : arrivalDate;
        LocalDate date = parseDate(raw);
        if (date == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days < 0) {
            days = 0;
        }
        return Integer.valueOf((int) days);
    }

    private static LocalDate parseDate(String raw) {
        if (!filled(raw)) {
            return null;
        }
        String text = raw.trim().replace('.', '-').replace('/', '-');
        String[] parts = text.split("-");
        try {
            if (parts.length >= 3 && parts[0].length() == 4) {
                return LocalDate.parse(parts[0] + "-" + pad2(parts[1]) + "-" + pad2(parts[2]).substring(0, 2));
            }
            if (parts.length >= 3 && parts[2].length() >= 4) {
                return LocalDate.parse(parts[2].substring(0, 4) + "-" + pad2(parts[1]) + "-" + pad2(parts[0]));
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static String pad2(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 1) {
            return "0" + digits;
        }
        if (digits.length() >= 2) {
            return digits.substring(0, 2);
        }
        return "01";
    }

    private static String expiryStatus(String raw, LocalDate today) {
        LocalDate date = parseDate(raw);
        if (date == null) {
            return "Missing date";
        }
        if (date.isBefore(today)) {
            return "Expired";
        }
        if (!date.isAfter(today.plusDays(30))) {
            return "Due in 30 days";
        }
        return "Current";
    }

    private static String worstExpiry(String left, String right) {
        int leftRank = expiryRank(left);
        int rightRank = expiryRank(right);
        return leftRank >= rightRank ? left : right;
    }

    private static int expiryRank(String status) {
        if ("Expired".equals(status)) {
            return 3;
        }
        if ("Due in 30 days".equals(status)) {
            return 2;
        }
        if ("Missing date".equals(status)) {
            return 1;
        }
        return 0;
    }

    private static String expiryPill(String status) {
        if ("Expired".equals(status)) {
            return "rust";
        }
        if ("Due in 30 days".equals(status)) {
            return "gold";
        }
        if ("Current".equals(status)) {
            return "leaf";
        }
        return "muted-pill";
    }

    private static int failCount(VehicleInspection inspection) {
        if (inspection == null || inspection.getLines() == null) {
            return 0;
        }
        int count = 0;
        for (VehicleInspectionLine line : inspection.getLines()) {
            if (InspectionResults.isNo(line.getResult())) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasDuty(CustomsDocument customs) {
        return dutyAmount(customs.getAssessmentTaxCid(), customs.getTaxCid()) != null
                || dutyAmount(customs.getAssessmentTaxSur(), customs.getTaxSur()) != null
                || dutyAmount(customs.getAssessmentTaxXid(), customs.getTaxXid()) != null
                || dutyAmount(customs.getAssessmentTaxVat(), customs.getTaxVat()) != null
                || dutyAmount(customs.getAssessmentTaxVel(), customs.getTaxVel()) != null
                || dutyTotal(customs) != null;
    }

    private static BigDecimal dutyAmount(String assessment, String cusdec) {
        BigDecimal parsed = ReportService.parseMoney(assessment);
        if (parsed != null) {
            return parsed;
        }
        return ReportService.parseMoney(cusdec);
    }

    private static BigDecimal dutyTotal(CustomsDocument customs) {
        if (customs == null) {
            return null;
        }
        BigDecimal assessed = ReportService.parseMoney(customs.getAssessmentTotalAssessed());
        if (assessed != null) {
            return assessed;
        }
        BigDecimal stated = ReportService.parseMoney(customs.getTotalTaxAmount());
        if (stated != null) {
            return stated;
        }
        return sumMoney(
                dutyAmount(customs.getAssessmentTaxCid(), customs.getTaxCid()),
                dutyAmount(customs.getAssessmentTaxSur(), customs.getTaxSur()),
                dutyAmount(customs.getAssessmentTaxXid(), customs.getTaxXid()),
                dutyAmount(customs.getAssessmentTaxVat(), customs.getTaxVat()),
                dutyAmount(customs.getAssessmentTaxVel(), customs.getTaxVel())
        );
    }

    private static String engineCapacity(CustomsDocument customs) {
        if (customs == null) {
            return "—";
        }
        if (filled(customs.getEngineCapacityCc())) {
            return customs.getEngineCapacityCc().trim();
        }
        if (filled(customs.getJevicEngineCapacity())) {
            return customs.getJevicEngineCapacity().trim();
        }
        return "—";
    }

    private static String intakeLabel(VehicleIntake intake) {
        if (intake == null) {
            return "—";
        }
        if (intake == VehicleIntake.COMPANY_IMPORT) {
            return "Company import";
        }
        if (intake == VehicleIntake.PERSONAL_IMPORT) {
            return "Personal import";
        }
        if (intake == VehicleIntake.LOCAL_UNREGISTERED) {
            return "Local unregistered";
        }
        return "Local registered";
    }

    private static String vehicleLabel(Vehicle vehicle) {
        String make = vehicle.getMake() == null ? "" : vehicle.getMake().trim();
        String model = vehicle.getModel() == null ? "" : vehicle.getModel().trim();
        String label = (make + " " + model).trim();
        return label.isEmpty() ? "—" : label;
    }

    private static String labelFor(String place) {
        if (ReportService.CUSTOMS.equals(place)) {
            return "Customs";
        }
        if (ReportService.PREPARATION.equals(place)) {
            return "Preparation";
        }
        if (ReportService.READY.equals(place)) {
            return "Ready for sale";
        }
        if (ReportService.LISTED.equals(place)) {
            return "Listed";
        }
        if (ReportService.SOLD.equals(place)) {
            return "Sold";
        }
        return "Import";
    }

    private static String pillFor(String place) {
        if (ReportService.CUSTOMS.equals(place)) {
            return "rust";
        }
        if (ReportService.PREPARATION.equals(place)) {
            return "gold";
        }
        if (ReportService.READY.equals(place) || ReportService.LISTED.equals(place)) {
            return "sea";
        }
        if (ReportService.SOLD.equals(place)) {
            return "leaf";
        }
        return "muted-pill";
    }

    private static String normalizeSection(String section) {
        if (section == null) {
            return ReportService.ALL;
        }
        String value = section.trim().toLowerCase(Locale.ROOT);
        if (ReportService.IMPORT.equals(value) || ReportService.CUSTOMS.equals(value)
                || ReportService.PREPARATION.equals(value) || ReportService.READY.equals(value)
                || ReportService.LISTED.equals(value) || ReportService.SOLD.equals(value)) {
            return value;
        }
        return ReportService.ALL;
    }

    private static String slashDate(String raw) {
        if (!filled(raw)) {
            return "—";
        }
        return raw.trim().replace('-', '/');
    }

    private static String displayMoney(String raw) {
        BigDecimal parsed = ReportService.parseMoney(raw);
        if (parsed != null) {
            return ReportService.formatMoney(parsed);
        }
        return filled(raw) ? raw.trim() : "—";
    }

    private static String moneyOrDash(BigDecimal value, boolean any) {
        return any ? ReportService.formatMoney(value) : "—";
    }

    private static boolean filled(String value) {
        return value != null && !value.trim().isEmpty() && !"—".equals(value.trim()) && !"-".equals(value.trim());
    }

    private static String blankDate(String value) {
        return value == null ? "" : value.trim();
    }

    static boolean hasPeriod(String from, String to) {
        return (from != null && !from.trim().isEmpty()) || (to != null && !to.trim().isEmpty());
    }

    static boolean inStockPeriod(String rawDate, String from, String to) {
        if (!hasPeriod(from, to)) {
            return true;
        }
        if (parseDate(rawDate) == null) {
            return false;
        }
        return inRange(rawDate, from, to);
    }

    private static String landedPeriodLabel(String from, String to) {
        if (!hasPeriod(from, to)) {
            return "All stock dates";
        }
        return "Stock date " + periodLabel(from, to);
    }

    private static String workshopPeriod(String key, String from, String to) {
        if (LANDED.equals(key) || DUTY.equals(key) || DOCUMENTS.equals(key)) {
            return landedPeriodLabel(from, to);
        }
        if (WORKSHOP.equals(key)) {
            if (!hasPeriod(from, to)) {
                return "All workshop dates";
            }
            return "Workshop date " + periodLabel(from, to);
        }
        return periodLabel(from, to);
    }

    private static List<WorkshopJobLine> workshopLines(WorkshopJob job, String from, String to) {
        List<WorkshopJobLine> lines = new ArrayList<WorkshopJobLine>();
        if (job == null) {
            return lines;
        }
        for (WorkshopJobLine line : job.getLines()) {
            if (line.isEmpty()) {
                continue;
            }
            if (!inStockPeriod(workshopDate(line), from, to)) {
                continue;
            }
            lines.add(line);
        }
        return lines;
    }

    private static String workshopDate(WorkshopJobLine line) {
        if (filled(line.getStartedOn())) {
            return line.getStartedOn();
        }
        return line.getCompletedOn();
    }

    private static BigDecimal lineCost(WorkshopJobLine line) {
        return VehicleTotalCost.lineCost(line);
    }

    private static Cell repairBreakdown(List<WorkshopJobLine> lines) {
        List<CostLine> stack = new ArrayList<CostLine>();
        boolean first = true;
        for (WorkshopJobLine line : lines) {
            BigDecimal cost = lineCost(line);
            stack.add(new CostLine(
                    blank(line.getJobSummary(), "Repair"),
                    cost == null ? "—" : ReportService.formatMoney(cost),
                    !first
            ));
            stack.add(new CostLine("Start on", slashDate(line.getStartedOn()), false));
            stack.add(new CostLine("Complete on", slashDate(line.getCompletedOn()), false));
            first = false;
        }
        return priceStack(stack);
    }

    static boolean inRange(String rawDate, String from, String to) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if (fromDate == null && toDate == null) {
            return true;
        }
        LocalDate date = parseDate(rawDate);
        if (date == null) {
            return true;
        }
        if (fromDate != null && date.isBefore(fromDate)) {
            return false;
        }
        return toDate == null || !date.isAfter(toDate);
    }

    private static String periodLabel(String from, String to) {
        boolean hasFrom = from != null && !from.isEmpty();
        boolean hasTo = to != null && !to.isEmpty();
        if (!hasFrom && !hasTo) {
            return "All dates";
        }
        String start = hasFrom ? formatPrinted(from) : "Start";
        String end = hasTo ? formatPrinted(to) : "Today";
        return start + " – " + end;
    }

    private static String formatPrinted(String raw) {
        LocalDate date = parseDate(raw);
        return date == null ? raw : date.format(PRINTED);
    }

    private static String blank(String value, String fallback) {
        return filled(value) ? value.trim() : fallback;
    }

    static String csvCell(String value) {
        String text = value == null ? "" : value;
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    public static final class Snapshot {
        private List<Vehicle> vehicles = new ArrayList<>();
        private final Map<String, SaleListing> listings = new HashMap<>();
        private final Map<String, CustomsDocument> customs = new HashMap<>();
        private final Map<String, WorkshopJob> jobs = new HashMap<String, WorkshopJob>();
        private final Map<String, YardRecord> yards = new HashMap<String, YardRecord>();
        private final Map<String, VehicleRegistration> registrations = new HashMap<>();
        private final Map<String, ExportCertificate> exports = new HashMap<>();
        private final Map<String, StandardsCertificate> standards = new HashMap<>();
        private final Map<String, GradeSearch> grades = new HashMap<String, GradeSearch>();
        private final Map<String, VehicleInspection> inspections = new HashMap<>();
        private final Map<String, Integer> photos = new HashMap<String, Integer>();
        private final Map<String, String> engines = new HashMap<String, String>();
        private int photoChassis;
        private int onHand;
        private int soldCount;
        private int landedRows;
        private int dutyRows;
        private int completeDocs;
        private int incompleteDocs;
        private int expiryWatch;
        private int expiryOk;
        private int openJobs;
        private int workshopVehicles;
        private BigDecimal onHandLanding = BigDecimal.ZERO;
        private BigDecimal landingTotal = BigDecimal.ZERO;
        private BigDecimal invoiceTotal = BigDecimal.ZERO;
        private BigDecimal taxTotal = BigDecimal.ZERO;
        private BigDecimal paidTotal = BigDecimal.ZERO;
        private BigDecimal soldRevenue = BigDecimal.ZERO;
        private BigDecimal soldLanding = BigDecimal.ZERO;
        private BigDecimal grossProfit = BigDecimal.ZERO;
        private BigDecimal workshopActual = BigDecimal.ZERO;
        private boolean anyOnHandLanding;
        private boolean anyLanding;
        private boolean anyInvoice;
        private boolean anyTax;
        private boolean anyPaid;
        private boolean anySoldRevenue;
        private boolean anySoldLanding;
        private boolean anyGrossProfit;
        private boolean anyWorkshopActual;
    }

    public static final class ReportCard {
        public final String kind;
        public final String group;
        public final String title;
        public final String standard;
        public final String description;
        public final String icon;
        public final String tone;
        public final int metric;
        public final String metricLabel;
        public final String extra;

        public ReportCard(
                String kind,
                String group,
                String title,
                String standard,
                String description,
                String icon,
                String tone,
                int metric,
                String metricLabel,
                String extra
        ) {
            this.kind = kind;
            this.group = group;
            this.title = title;
            this.standard = standard;
            this.description = description;
            this.icon = icon;
            this.tone = tone;
            this.metric = metric;
            this.metricLabel = metricLabel;
            this.extra = extra;
        }

        public String getKind() { return kind; }
        public String getGroup() { return group; }
        public String getTitle() { return title; }
        public String getStandard() { return standard; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public String getTone() { return tone; }
        public int getMetric() { return metric; }
        public String getMetricLabel() { return metricLabel; }
        public String getExtra() { return extra; }
    }

    @Getter
    public static final class StandardReport {
        private String kind;
        private String title;
        private String lead;
        private String standard;
        private String generatedOn;
        private String searchQuery;
        private String fromDate = "";
        private String toDate = "";
        private String periodLabel = "All dates";
        private String section = ReportService.ALL;
        private boolean filterLinks;
        private String allHref;
        private List<Kpi> kpis = new ArrayList<>();
        private List<Column> columns = new ArrayList<>();
        private List<DataRow> rows = new ArrayList<>();
        private String note;

        public int getRowCount() { return rows.size(); }
    }

    @Getter
    public static final class Kpi {
        public final String label;
        public final String value;
        public final String hint;
        public final String href;
        public final String filterKey;

        public Kpi(String label, String value, String hint) {
            this(label, value, hint, null, null);
        }

        public Kpi(String label, String value, String hint, String href) {
            this(label, value, hint, href, null);
        }

        public Kpi(String label, String value, String hint, String href, String filterKey) {
            this.label = label;
            this.value = value;
            this.hint = hint;
            this.href = href;
            this.filterKey = filterKey;
        }

        public boolean isLink() { return href != null && !href.isEmpty(); }
        public boolean isActive(String section) {
            return filterKey != null && filterKey.equals(section);
        }
    }

    @Getter
    public static final class Column {
        public final String label;
        public boolean numeric;

        public Column(String label, boolean numeric) {
            this.label = label;
            this.numeric = numeric;
        }

    }

    public static final class DataRow {
        @Getter
        public final List<Cell> cells = new ArrayList<>();
        @Getter
        public String tone = "";
        public int sortDays = -1;
    }

    @Getter
    public static final class Cell {
        public final String text;
        public final String href;
        public final String pill;
        public String tone;
        public List<CostLine> lines;

        public Cell(String text, String href, String pill, String tone) {
            this.text = text;
            this.href = href;
            this.pill = pill;
            this.tone = tone;
        }

    }

    @Getter
    public static final class CostLine {
        public final String label;
        public final String amount;
        public final boolean total;

        public final String href;

        public CostLine(String label, String amount, boolean total) {
            this(label, amount, total, null);
        }

        public CostLine(String label, String amount, boolean total, String href) {
            this.label = label;
            this.amount = amount;
            this.total = total;
            this.href = href;
        }
    }

}
