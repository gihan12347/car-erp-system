package com.carsale.erp.shared.vehicle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.customspipeline.model.CustomsDocument;
import com.carsale.erp.customspipeline.service.CustomsDocumentService;
import com.carsale.erp.importpipeline.model.EquipmentInspection;
import com.carsale.erp.importpipeline.model.ExportCertificate;
import com.carsale.erp.importpipeline.model.GradeSearch;
import com.carsale.erp.importpipeline.model.InspectionCertificate;
import com.carsale.erp.importpipeline.model.PreShipmentInspection;
import com.carsale.erp.importpipeline.model.StandardsCertificate;
import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.service.CertificateOfInspectionService;
import com.carsale.erp.importpipeline.service.EquipmentInspectionService;
import com.carsale.erp.importpipeline.service.ExportCertificateService;
import com.carsale.erp.importpipeline.service.GradeSearchService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService.ImportProgress;
import com.carsale.erp.importpipeline.service.PreShipmentService;
import com.carsale.erp.importpipeline.service.StandardsCertificateService;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.vehicle.VehicleSummary.DocumentCard;
import com.carsale.erp.shared.vehicle.VehicleSummary.PhotoCard;
import com.carsale.erp.shared.vehicle.VehicleSummary.PriceBuilder;
import com.carsale.erp.shared.vehicle.VehicleSummary.PriceGroup;
import com.carsale.erp.shared.vehicle.VehicleSummary.SpecLine;

@Service
public class VehicleSummaryService {

    private final VehicleService vehicleService;
    private final ImportProgressService importProgressService;
    private final PreShipmentService preShipmentService;
    private final EquipmentInspectionService equipmentInspectionService;
    private final CertificateOfInspectionService certificateOfInspectionService;
    private final StandardsCertificateService standardsCertificateService;
    private final ExportCertificateService exportCertificateService;
    private final GradeSearchService gradeSearchService;
    private final VehiclePhotoService vehiclePhotoService;
    private final CustomsDocumentService customsDocumentService;
    private final SaleListingService saleListingService;

    public VehicleSummaryService(
            VehicleService vehicleService,
            ImportProgressService importProgressService,
            PreShipmentService preShipmentService,
            EquipmentInspectionService equipmentInspectionService,
            CertificateOfInspectionService certificateOfInspectionService,
            StandardsCertificateService standardsCertificateService,
            ExportCertificateService exportCertificateService,
            GradeSearchService gradeSearchService,
            VehiclePhotoService vehiclePhotoService,
            CustomsDocumentService customsDocumentService,
            SaleListingService saleListingService
    ) {
        this.vehicleService = vehicleService;
        this.importProgressService = importProgressService;
        this.preShipmentService = preShipmentService;
        this.equipmentInspectionService = equipmentInspectionService;
        this.certificateOfInspectionService = certificateOfInspectionService;
        this.standardsCertificateService = standardsCertificateService;
        this.exportCertificateService = exportCertificateService;
        this.gradeSearchService = gradeSearchService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.customsDocumentService = customsDocumentService;
        this.saleListingService = saleListingService;
    }

    public VehicleSummary build(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo.trim());
        if (vehicle == null) {
            return null;
        }
        String chassis = vehicle.getChassisNo();
        ImportProgress progress = importProgressService.progressFor(vehicle);
        CustomsDocument clearance = customsDocumentService.findByChassisNo(chassis);
        SaleListing listing = saleListingService.findByChassisNo(chassis);

        List<PhotoCard> photos = photosFor(chassis);
        List<DocumentCard> documents = documentsFor(vehicle, progress, clearance, photos, chassis);
        List<PriceGroup> priceGroups = pricesFor(vehicle, clearance, listing);

        int onFile = 0;
        int pending = 0;
        int skipped = 0;
        for (DocumentCard document : documents) {
            if ("on-file".equals(document.getStatus())) {
                onFile++;
            } else if ("pending".equals(document.getStatus())) {
                pending++;
            } else {
                skipped++;
            }
        }
        int tracked = onFile + pending;
        int percent = tracked == 0 ? 100 : (onFile * 100 / tracked);

        return new VehicleSummary(
                vehicle,
                titleFor(vehicle),
                stageLabel(vehicle),
                text(vehicle.getStockNo()),
                onFile,
                pending,
                skipped,
                percent,
                landingHighlight(clearance),
                askingHighlight(listing),
                highlightsFor(vehicle),
                specsFor(vehicle),
                photos,
                documents,
                priceGroups,
                notesFor(vehicle, listing)
        );
    }

    private List<PhotoCard> photosFor(String chassis) {
        List<PhotoCard> photos = new ArrayList<PhotoCard>();
        List<VehiclePhoto> stored = vehiclePhotoService.list(chassis);
        int index = 1;
        for (VehiclePhoto photo : stored) {
            String storedName = text(photo.getStoredName());
            if (storedName == null) {
                continue;
            }
            String name = text(photo.getOriginalName());
            if (name == null) {
                name = "Photo " + index;
            }
            photos.add(new PhotoCard(
                    fileUrl("/photos/documents/", storedName),
                    name,
                    isPdf(photo.getContentType(), storedName)
            ));
            index++;
        }
        return photos;
    }

    private List<DocumentCard> documentsFor(
            Vehicle vehicle,
            ImportProgress progress,
            CustomsDocument clearance,
            List<PhotoCard> photos,
            String chassis
    ) {
        List<DocumentCard> documents = new ArrayList<DocumentCard>();
        PreShipmentInspection preship = preShipmentService.findByChassisNo(chassis);
        EquipmentInspection equipment = equipmentInspectionService.findByChassisNo(chassis);
        InspectionCertificate coi = certificateOfInspectionService.findByChassisNo(chassis);
        StandardsCertificate standards = standardsCertificateService.findByChassisNo(chassis);
        ExportCertificate exportCertificate = exportCertificateService.findByChassisNo(chassis);
        GradeSearch grade = gradeSearchService.findByChassisNo(chassis);

        addDocument(
                documents, progress, "Auction sheet", "Import", "fa-solid fa-gavel",
                FlowStage.AUCTION.getStageKey(),
                text(vehicle.getSheetStoredName()) != null,
                vehicle.getSheetOriginalName(), vehicle.getSheetContentType(), vehicle.getSheetStoredName(),
                "/auction/documents/", "/auction/", chassis
        );
        addDocument(
                documents, progress, "Pre-shipment certificate", "Import", "fa-solid fa-ship",
                FlowStage.PRESHIP.getStageKey(),
                preship != null && text(preship.getDocumentStoredName()) != null,
                preship == null ? null : preship.getDocumentOriginalName(),
                preship == null ? null : preship.getDocumentContentType(),
                preship == null ? null : preship.getDocumentStoredName(),
                "/shipping/documents/", "/shipping/", chassis
        );
        addDocument(
                documents, progress, "Equipment condition", "Import", "fa-solid fa-list-check",
                FlowStage.EQUIPMENT.getStageKey(),
                equipment != null && text(equipment.getDocumentStoredName()) != null,
                equipment == null ? null : equipment.getDocumentOriginalName(),
                equipment == null ? null : equipment.getDocumentContentType(),
                equipment == null ? null : equipment.getDocumentStoredName(),
                "/equipment/documents/", "/equipment/", chassis
        );
        addDocument(
                documents, progress, "Odometer certificate", "Import", "fa-solid fa-gauge-high",
                FlowStage.JEVIC.getStageKey(),
                clearance != null && text(clearance.getPage1StoredName()) != null,
                clearance == null ? null : clearance.getPage1OriginalName(),
                clearance == null ? null : clearance.getPage1ContentType(),
                clearance == null ? null : clearance.getPage1StoredName(),
                "/jevic/documents/", "/jevic/", chassis
        );
        addDocument(
                documents, progress, "Certificate of inspection", "Import", "fa-solid fa-clipboard-check",
                FlowStage.COI.getStageKey(),
                coi != null && text(coi.getDocumentStoredName()) != null,
                coi == null ? null : coi.getDocumentOriginalName(),
                coi == null ? null : coi.getDocumentContentType(),
                coi == null ? null : coi.getDocumentStoredName(),
                "/coi/documents/", "/coi/", chassis
        );
        addDocument(
                documents, progress, "Standards certificate", "Import", "fa-solid fa-shield-halved",
                FlowStage.STANDARDS.getStageKey(),
                standards != null && text(standards.getDocumentStoredName()) != null,
                standards == null ? null : standards.getDocumentOriginalName(),
                standards == null ? null : standards.getDocumentContentType(),
                standards == null ? null : standards.getDocumentStoredName(),
                "/standards/documents/", "/standards/", chassis
        );
        addDocument(
                documents, progress, "Export certificate", "Import", "fa-solid fa-file-export",
                FlowStage.EXPORT.getStageKey(),
                exportCertificate != null && text(exportCertificate.getDocumentStoredName()) != null,
                exportCertificate == null ? null : exportCertificate.getDocumentOriginalName(),
                exportCertificate == null ? null : exportCertificate.getDocumentContentType(),
                exportCertificate == null ? null : exportCertificate.getDocumentStoredName(),
                "/export/documents/", "/export/", chassis
        );
        addDocument(
                documents, progress, "Grade search", "Import", "fa-solid fa-star",
                FlowStage.GRADE.getStageKey(),
                grade != null && text(grade.getDocumentStoredName()) != null,
                grade == null ? null : grade.getDocumentOriginalName(),
                grade == null ? null : grade.getDocumentContentType(),
                grade == null ? null : grade.getDocumentStoredName(),
                "/grade/documents/", "/grade/", chassis
        );
        addDocument(
                documents, progress, "Vehicle images", "Import", "fa-solid fa-images",
                FlowStage.PHOTOS.getStageKey(),
                !photos.isEmpty(),
                photos.isEmpty() ? null : photos.size() + (photos.size() == 1 ? " image" : " images"),
                null,
                null,
                null, "/photos/", chassis
        );
        addDocument(
                documents, progress, "Working sheet", "Customs", "fa-solid fa-table",
                FlowStage.WORKSHEET.getStageKey(),
                clearance != null && text(clearance.getPage4StoredName()) != null,
                clearance == null ? null : clearance.getPage4OriginalName(),
                clearance == null ? null : clearance.getPage4ContentType(),
                clearance == null ? null : clearance.getPage4StoredName(),
                "/worksheet/documents/", "/worksheet/", chassis
        );
        addDocument(
                documents, null, "Bill of lading", "Customs", "fa-solid fa-file-lines",
                null,
                clearance != null && text(clearance.getPage5StoredName()) != null,
                clearance == null ? null : clearance.getPage5OriginalName(),
                clearance == null ? null : clearance.getPage5ContentType(),
                clearance == null ? null : clearance.getPage5StoredName(),
                "/bl/documents/", "/bl/", chassis
        );
        addDocument(
                documents, null, "Customs declaration(CUSDEC)", "Customs", "fa-solid fa-file-invoice",
                null,
                clearance != null && text(clearance.getPage2StoredName()) != null,
                clearance == null ? null : clearance.getPage2OriginalName(),
                clearance == null ? null : clearance.getPage2ContentType(),
                clearance == null ? null : clearance.getPage2StoredName(),
                "/declaration/documents/", "/declaration/", chassis
        );
        addDocument(
                documents, null, "Assessment notice", "Customs", "fa-solid fa-scale-balanced",
                null,
                clearance != null && text(clearance.getPage3StoredName()) != null,
                clearance == null ? null : clearance.getPage3OriginalName(),
                clearance == null ? null : clearance.getPage3ContentType(),
                clearance == null ? null : clearance.getPage3StoredName(),
                "/assessment/documents/", "/assessment/", chassis
        );
        return documents;
    }

    private void addDocument(
            List<DocumentCard> documents,
            ImportProgress progress,
            String title,
            String group,
            String icon,
            String stageKey,
            boolean onFile,
            String fileName,
            String contentType,
            String storedName,
            String filePrefix,
            String stagePath,
            String chassis
    ) {
        boolean skipped = !onFile && progress != null && stageKey != null && progress.isStageSkipped(stageKey);
        String status;
        String statusLabel;
        if (onFile) {
            status = "on-file";
            statusLabel = "On file";
        } else if (skipped) {
            status = "skipped";
            statusLabel = "Skipped";
        } else {
            status = "pending";
            statusLabel = "Pending";
        }
        String fileUrl = onFile && filePrefix != null ? fileUrl(filePrefix, storedName) : null;
        documents.add(new DocumentCard(
                title,
                group,
                status,
                statusLabel,
                text(fileName),
                fileUrl,
                stageUrl(stagePath, chassis),
                fileUrl != null && isPdf(contentType, storedName),
                icon
        ));
    }

    private List<PriceGroup> pricesFor(Vehicle vehicle, CustomsDocument clearance, SaleListing listing) {
        List<PriceGroup> groups = new ArrayList<PriceGroup>();
        PriceBuilder landing = new PriceBuilder("Landing");
        PriceBuilder invoice = new PriceBuilder("Customs declaration(CUSDEC)");
        PriceBuilder assessment = new PriceBuilder("Assessment");
        PriceBuilder worksheet = new PriceBuilder("Working sheet");
        PriceBuilder sale = new PriceBuilder("Sale");

        if (clearance != null) {
            landing.add("Landing cost (USD)", clearance.getLandingCostUsd());
            landing.add("Exchange rate", clearance.getBlExchangeRate());
            landing.add("Landing cost (LKR)", clearance.getLandingCostLkr());

            invoice.add("FOB", withCurrency(clearance.getInvoiceFob(), "JPY"));
            invoice.add("Freight", withCurrency(clearance.getInvoiceFreight(), "JPY"));
            invoice.add("Insurance", withCurrency(clearance.getInvoiceInsurance(), "JPY"));
            invoice.add("Other", withCurrency(clearance.getInvoiceOther(), "JPY"));
            invoice.addTotal("Invoice total", withCurrency(clearance.getInvoiceTotal(), "JPY"));
            invoice.add("Exchange rate", clearance.getExchangeRate());
            invoice.add("Value (NCY)", withCurrency(clearance.getValueNcy(), "LKR"));

            assessment.add("OTC", withCurrency(clearance.getAssessmentTaxOtc(), "LKR"));
            assessment.add("COM", withCurrency(clearance.getAssessmentTaxCom(), "LKR"));
            assessment.add("EXM", withCurrency(clearance.getAssessmentTaxExm(), "LKR"));
            assessment.add("CID", withCurrency(clearance.getAssessmentTaxCid(), "LKR"));
            assessment.add("SUR", withCurrency(clearance.getAssessmentTaxSur(), "LKR"));
            assessment.add("XID", withCurrency(clearance.getAssessmentTaxXid(), "LKR"));
            assessment.add("VAT", withCurrency(clearance.getAssessmentTaxVat(), "LKR"));
            assessment.add("VEL", withCurrency(clearance.getAssessmentTaxVel(), "LKR"));
            assessment.addTotal("Total assessed", withCurrency(clearance.getAssessmentTotalAssessed(), "LKR"));
            assessment.addTotal("Total paid", withCurrency(clearance.getAssessmentTotalPaid(), "LKR"));

            worksheet.add("Agents freight", clearance.getWorksheetAgentsFreight());
            worksheet.add("Agents insurance", clearance.getWorksheetAgentsInsurance());
            worksheet.add("B/L freight", clearance.getWorksheetBlFreightAmount());
            worksheet.add("Website value", clearance.getWorksheetWebsiteValue());
            worksheet.add("Local taxes", clearance.getWorksheetLocalTaxes());
            worksheet.add("85% FOB", withCurrency(clearance.getWorksheetFobValue85(), clearance.getWorksheetFobValue85Currency()));
            worksheet.add("LC amount", clearance.getWorksheetLcAmount());
            worksheet.add("Fiscal FOB", clearance.getWorksheetFiscalFob());
            worksheet.add("Fiscal freight", clearance.getWorksheetFiscalFreight());
            worksheet.add("Fiscal insurance", clearance.getWorksheetFiscalInsurance());
            worksheet.add("Fiscal options", clearance.getWorksheetFiscalOptions());
            worksheet.addTotal("Fiscal total", withCurrency(clearance.getWorksheetFiscalTotal(), clearance.getWorksheetFiscalTotalCurrency()));
        }

        sale.add("Recycle fee", vehicle.getRecycleFee());
        if (listing != null) {
            sale.add("Asking price", listing.getAskingPrice());
            sale.add("Advertised price", listing.getAdvertisedPrice());
        }

        addGroup(groups, landing.build());
        addGroup(groups, invoice.build());
        addGroup(groups, assessment.build());
        addGroup(groups, worksheet.build());
        addGroup(groups, sale.build());
        return groups;
    }

    private static void addGroup(List<PriceGroup> groups, PriceGroup group) {
        if (group != null) {
            groups.add(group);
        }
    }

    private List<SpecLine> highlightsFor(Vehicle vehicle) {
        List<SpecLine> lines = new ArrayList<SpecLine>();
        addSpec(lines, "Year", vehicle.getYear());
        addSpec(lines, "Mileage", vehicle.getMileage());
        addSpec(lines, "Grade", vehicle.getAuctionGrade());
        addSpec(lines, "Transmission", vehicle.getTransmission());
        addSpec(lines, "Fuel", vehicle.getFuel());
        return lines;
    }

    private List<SpecLine> specsFor(Vehicle vehicle) {
        List<SpecLine> lines = new ArrayList<SpecLine>();
        addSpec(lines, "Make", vehicle.getMake());
        addSpec(lines, "Model", vehicle.getModel());
        addSpec(lines, "Grade", vehicle.getGrade());
        addSpec(lines, "Model code", vehicle.getModelCode());
        addSpec(lines, "Year", vehicle.getYear());
        addSpec(lines, "Mileage", vehicle.getMileage());
        addSpec(lines, "Color", vehicle.getColor());
        addSpec(lines, "Engine", vehicle.getEngineSize());
        addSpec(lines, "Transmission", vehicle.getTransmission());
        addSpec(lines, "Fuel", vehicle.getFuel());
        addSpec(lines, "Doors", vehicle.getDoors());
        addSpec(lines, "Seats", vehicle.getSeats());
        addSpec(lines, "Drive", vehicle.getDriveSystem());
        addSpec(lines, "Body", vehicle.getBodyStyle());
        addSpec(lines, "Auction house", vehicle.getAuctionHouse());
        addSpec(lines, "Lot", vehicle.getLotNo());
        addSpec(lines, "Auction grade", vehicle.getAuctionGrade());
        addSpec(lines, "Exterior grade", vehicle.getExteriorGrade());
        addSpec(lines, "Interior grade", vehicle.getInteriorGrade());
        addSpec(lines, "Interior color", vehicle.getInteriorColor());
        addSpec(lines, "Inspection", vehicle.getInspection());
        addSpec(lines, "History", vehicle.getHistory());
        addSpec(lines, "Registration", vehicle.getRegistrationMonth());
        addSpec(lines, "Warranty", vehicle.getWarranty());
        addSpec(lines, "A/C", vehicle.getAcType());
        String size = dimensions(vehicle);
        if (size != null) {
            lines.add(new SpecLine("Size (cm)", size));
        }
        addSpec(lines, "Equipment", vehicle.getEquipment());
        return lines;
    }

    private List<String> notesFor(Vehicle vehicle, SaleListing listing) {
        List<String> notes = new ArrayList<String>();
        addNote(notes, "Sales points", vehicle.getSalesPoints());
        addNote(notes, "Inspector notes", vehicle.getInspectorNotes());
        addNote(notes, "Notes", vehicle.getNotes());
        if (listing != null) {
            addNote(notes, "Listing notes", listing.getListingNotes());
        }
        return notes;
    }

    private static void addNote(List<String> notes, String label, String value) {
        String text = text(value);
        if (text != null) {
            notes.add(label + ": " + text);
        }
    }

    private static void addSpec(List<SpecLine> lines, String label, String value) {
        String text = text(value);
        if (text != null) {
            lines.add(new SpecLine(label, text));
        }
    }

    private static String dimensions(Vehicle vehicle) {
        String length = text(vehicle.getLengthCm());
        String width = text(vehicle.getWidthCm());
        String height = text(vehicle.getHeightCm());
        if (length == null && width == null && height == null) {
            return null;
        }
        return (length == null ? "—" : length) + " × " + (width == null ? "—" : width) + " × " + (height == null ? "—" : height);
    }

    private static String titleFor(Vehicle vehicle) {
        String model = text(vehicle.getModel());
        String make = text(vehicle.getMake());
        String name;
        if (model != null && make != null && model.toLowerCase().indexOf(make.toLowerCase()) < 0) {
            name = make + " " + model;
        } else if (model != null) {
            name = model;
        } else if (make != null) {
            name = make;
        } else {
            name = "Vehicle";
        }
        String year = text(vehicle.getYear());
        return year == null ? name : year + " " + name;
    }

    private static String stageLabel(Vehicle vehicle) {
        if (vehicle.getStage() == null) {
            return VehicleStage.PURCHASED.getDisplayName();
        }
        return vehicle.getStage().getDisplayName();
    }

    private static String landingHighlight(CustomsDocument clearance) {
        if (clearance == null) {
            return null;
        }
        String lkr = text(clearance.getLandingCostLkr());
        if (lkr != null) {
            return lkr;
        }
        String usd = text(clearance.getLandingCostUsd());
        if (usd != null) {
            return usd;
        }
        return withCurrency(clearance.getWorksheetFiscalTotal(), clearance.getWorksheetFiscalTotalCurrency());
    }

    private static String askingHighlight(SaleListing listing) {
        if (listing == null) {
            return null;
        }
        String asking = text(listing.getAskingPrice());
        if (asking != null) {
            return asking;
        }
        return text(listing.getAdvertisedPrice());
    }

    private static String withCurrency(String amount, String currency) {
        String value = text(amount);
        if (value == null) {
            return null;
        }
        String unit = text(currency);
        if (unit == null || value.toUpperCase().indexOf(unit.toUpperCase()) >= 0) {
            return value;
        }
        return value + " " + unit;
    }

    private static String fileUrl(String prefix, String storedName) {
        String name = text(storedName);
        if (name == null) {
            return null;
        }
        return prefix + UriUtils.encodePathSegment(name, StandardCharsets.UTF_8);
    }

    private static String stageUrl(String path, String chassis) {
        return path + UriUtils.encodePathSegment(chassis, StandardCharsets.UTF_8);
    }

    private static boolean isPdf(String contentType, String storedName) {
        if (contentType != null && contentType.toLowerCase().indexOf("pdf") >= 0) {
            return true;
        }
        return storedName != null && storedName.toLowerCase().endsWith(".pdf");
    }

    private static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
