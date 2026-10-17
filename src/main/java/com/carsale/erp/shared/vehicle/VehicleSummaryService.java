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
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.importpipeline.service.PreShipmentService;
import com.carsale.erp.importpipeline.service.StandardsCertificateService;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.preparationpipeline.model.PreviousOwner;
import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.service.PreviousOwnerService;
import com.carsale.erp.preparationpipeline.service.RegistrationCertificateService;
import com.carsale.erp.preparationpipeline.service.YardBayService;
import com.carsale.erp.preparationpipeline.service.YardService;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.readypipeline.util.ReadyStageUrls;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.service.SkippedStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.VehicleSummary.DocumentCard;
import com.carsale.erp.shared.vehicle.VehicleSummary.PhotoCard;
import com.carsale.erp.shared.vehicle.VehicleSummary.PlaceCard;
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
    private final SaleLocationService saleLocationService;
    private final YardService yardService;
    private final YardBayService yardBayService;
    private final RegistrationCertificateService registrationCertificateService;
    private final PreviousOwnerService previousOwnerService;
    private final SkippedStageService skippedStageService;

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
            SaleListingService saleListingService,
            SaleLocationService saleLocationService,
            YardService yardService,
            YardBayService yardBayService,
            RegistrationCertificateService registrationCertificateService,
            PreviousOwnerService previousOwnerService,
            SkippedStageService skippedStageService
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
        this.saleLocationService = saleLocationService;
        this.yardService = yardService;
        this.yardBayService = yardBayService;
        this.registrationCertificateService = registrationCertificateService;
        this.previousOwnerService = previousOwnerService;
        this.skippedStageService = skippedStageService;
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
                notesFor(vehicle, listing),
                yardPlace(chassis, listing),
                salePlace(chassis, listing)
        );
    }

    private PlaceCard yardPlace(String chassis, SaleListing listing) {
        YardRecord record = yardService.findByChassisNo(chassis);
        String bayCode = record == null ? null : text(record.getBayNo());
        if (bayCode == null) {
            return PlaceCard.empty("Yard", "/images/hub/yard.jpg", "Not in a yard yet.");
        }
        boolean moved = listing != null && text(listing.getSaleCode()) != null;
        YardBay yard = yardBayService.findByCode(bayCode);
        String code = yard != null && text(yard.getBayCode()) != null ? yard.getBayCode() : bayCode;
        String name = yard != null && text(yard.getYardName()) != null ? yard.getYardName() : code;
        String location = yard != null ? text(yard.getLocation()) : null;
        if (location == null && record != null) {
            location = text(record.getYardSection());
        }
        List<SpecLine> lines = new ArrayList<SpecLine>();
        String arrived = record == null ? null : text(record.getArrivalDate());
        if (arrived != null) {
            lines.add(new SpecLine("Arrived", arrived));
        }
        String encoded = PipelineStageUtils.encode(code);
        String url = moved
                ? "/yards/" + encoded
                : "/yards/" + encoded + "/" + PipelineStageUtils.encode(chassis);
        return new PlaceCard(
                "Yard",
                true,
                code,
                name,
                location == null ? "—" : location,
                moved ? "Moved to sale" : "In yard",
                moved ? "gold" : "sea",
                "/images/hub/yard.jpg",
                url,
                "Open yard",
                null,
                lines
        );
    }

    private PlaceCard salePlace(String chassis, SaleListing listing) {
        String saleCode = listing == null ? null : text(listing.getSaleCode());
        if (saleCode == null) {
            return PlaceCard.empty("Sale", "/images/hub/sale.png", "Not assigned to a sale yet.");
        }
        SaleLocation sale = saleLocationService.findByCode(saleCode);
        String code = sale != null && text(sale.getSaleCode()) != null ? sale.getSaleCode() : saleCode;
        String name = sale != null && text(sale.getSaleName()) != null ? sale.getSaleName() : code;
        String location = text(listing.getSaleLocation());
        if (location == null && sale != null) {
            location = text(sale.getLocation());
        }
        List<SpecLine> lines = new ArrayList<SpecLine>();
        String price = askingHighlight(listing);
        if (price != null) {
            lines.add(new SpecLine("Selling price", price));
        }
        String sellingDate = text(listing.getListedOn());
        if (sellingDate != null) {
            lines.add(new SpecLine("Selling date", sellingDate));
        }
        String buyer = text(listing.getBuyerFullName());
        if (buyer != null) {
            lines.add(new SpecLine("Buyer", buyer));
        }
        boolean sold = listing.isSold();
        return new PlaceCard(
                "Sale",
                true,
                code,
                name,
                location == null ? "—" : location,
                sold ? "Sold" : "On sale",
                sold ? "rust" : "sea",
                "/images/hub/sale.png",
                ReadyStageUrls.saleVehicle(code, chassis),
                "Open sale",
                null,
                lines
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
        if (vehicle.getIntake() == VehicleIntake.LOCAL_REGISTERED) {
            return registeredDocuments(photos, chassis);
        }
        List<DocumentCard> documents = new ArrayList<DocumentCard>();
        PreShipmentInspection preship = preShipmentService.findByChassisNo(chassis);
        EquipmentInspection equipment = equipmentInspectionService.findByChassisNo(chassis);
        InspectionCertificate coi = certificateOfInspectionService.findByChassisNo(chassis);
        StandardsCertificate standards = standardsCertificateService.findByChassisNo(chassis);
        ExportCertificate exportCertificate = exportCertificateService.findByChassisNo(chassis);
        GradeSearch grade = gradeSearchService.findByChassisNo(chassis);
        RegistrationCertificate registration = registrationCertificateService.findByChassisNo(chassis);

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
                FlowStage.BILL_OF_LADING.getStageKey(),
                clearance != null && text(clearance.getPage5StoredName()) != null,
                clearance == null ? null : clearance.getPage5OriginalName(),
                clearance == null ? null : clearance.getPage5ContentType(),
                clearance == null ? null : clearance.getPage5StoredName(),
                "/bl/documents/", "/bl/", chassis
        );
        addDocument(
                documents, null, "Customs declaration(CUSDEC)", "Customs", "fa-solid fa-file-invoice",
                FlowStage.DECLARATION.getStageKey(),
                clearance != null && text(clearance.getPage2StoredName()) != null,
                clearance == null ? null : clearance.getPage2OriginalName(),
                clearance == null ? null : clearance.getPage2ContentType(),
                clearance == null ? null : clearance.getPage2StoredName(),
                "/declaration/documents/", "/declaration/", chassis
        );
        addDocument(
                documents, null, "Assessment notice", "Customs", "fa-solid fa-scale-balanced",
                FlowStage.ASSESSMENT.getStageKey(),
                clearance != null && text(clearance.getPage3StoredName()) != null,
                clearance == null ? null : clearance.getPage3OriginalName(),
                clearance == null ? null : clearance.getPage3ContentType(),
                clearance == null ? null : clearance.getPage3StoredName(),
                "/assessment/documents/", "/assessment/", chassis
        );
        boolean bookOnFile = registration != null && text(registration.getDocumentStoredName()) != null;
        addDocument(
                documents, null, "Vehicle book", "Sale", "fa-solid fa-id-card",
                FlowStage.REGISTRATION.getStageKey(),
                bookOnFile,
                registration == null ? null : registration.getDocumentOriginalName(),
                registration == null ? null : registration.getDocumentContentType(),
                registration == null ? null : registration.getDocumentStoredName(),
                "/workshop-yard/documents/", "/registration/", chassis
        );
        return documents;
    }

    private List<DocumentCard> registeredDocuments(List<PhotoCard> photos, String chassis) {
        List<DocumentCard> documents = new ArrayList<DocumentCard>();
        RegistrationCertificate registration = registrationCertificateService.findByChassisNo(chassis);
        boolean bookOnFile = registration != null && text(registration.getDocumentStoredName()) != null;
        String bookStored = registration == null ? null : registration.getDocumentStoredName();
        String encoded = UriUtils.encodePathSegment(chassis, StandardCharsets.UTF_8);
        addRegisteredDocument(
                documents,
                "Vehicle book",
                "fa-solid fa-id-card",
                SkippedStageService.REG_BOOK,
                bookOnFile,
                registration == null ? null : registration.getDocumentOriginalName(),
                registration == null ? null : registration.getDocumentContentType(),
                bookOnFile ? fileUrl("/workshop-yard/documents/", bookStored) : null,
                "/workshop-yard/" + encoded + "/certificate",
                chassis
        );
        addRegisteredDocument(
                documents,
                "Images",
                "fa-solid fa-images",
                SkippedStageService.REG_PHOTOS,
                !photos.isEmpty(),
                photos.isEmpty() ? null : photos.size() + (photos.size() == 1 ? " image" : " images"),
                null,
                null,
                "/registered/" + encoded + "/photos",
                chassis
        );
        PreviousOwner owner = previousOwnerService.findByChassisNo(chassis);
        boolean ownerOnFile = owner != null && text(owner.getDocumentStoredName()) != null;
        addRegisteredDocument(
                documents,
                "Owner document",
                "fa-solid fa-address-card",
                SkippedStageService.REG_OWNER,
                ownerOnFile,
                owner == null ? null : owner.getDocumentOriginalName(),
                owner == null ? null : owner.getDocumentContentType(),
                ownerOnFile ? fileUrl("/registered/documents/", owner.getDocumentStoredName()) : null,
                "/registered/" + encoded + "/owner",
                chassis
        );
        return documents;
    }

    private void addRegisteredDocument(
            List<DocumentCard> documents,
            String title,
            String icon,
            String stageKey,
            boolean onFile,
            String fileName,
            String contentType,
            String fileUrl,
            String stageUrl,
            String chassis
    ) {
        boolean skipped = !onFile && skippedStageService.isMarked(chassis, stageKey);
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
        documents.add(new DocumentCard(
                title,
                "Registered",
                status,
                statusLabel,
                text(fileName),
                fileUrl,
                stageUrl,
                fileUrl != null && isPdf(contentType, fileName),
                icon
        ));
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
        boolean skipped = !onFile && stageKey != null && (
                (progress != null && progress.isStageSkipped(stageKey))
                        || skippedStageService.isMarked(chassis, stageKey)
        );
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
            sale.add("Selling price", listing.getAskingPrice());
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
        RegistrationCertificate book = registrationCertificateService.findByChassisNo(vehicle.getChassisNo());
        if (book != null) {
            return bookSpecs(book, vehicle);
        }
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

    private List<SpecLine> bookSpecs(RegistrationCertificate book, Vehicle vehicle) {
        List<SpecLine> lines = new ArrayList<SpecLine>();
        addSpec(lines, "CR number", book.getCrNo());
        addSpec(lines, "Registration number", book.getRegistrationNo());
        addSpec(lines, "Chassis", book.getChassisNo());
        addSpec(lines, "Engine", book.getEngineNo());
        addSpec(lines, "Class of vehicle", book.getClassOfVehicle());
        addSpec(lines, "Taxation class", book.getTaxationClass());
        addSpec(lines, "Status when registered", book.getStatusWhenRegistered());
        addSpec(lines, "Make", firstText(book.getMake(), vehicle.getMake()));
        addSpec(lines, "Model", firstText(book.getModel(), vehicle.getModel()));
        addSpec(lines, "Manufacturer description", book.getManufacturesDescription());
        addSpec(lines, "Colour", firstText(book.getColour(), vehicle.getColor()));
        addSpec(lines, "Type of body", book.getTypeOfBody());
        addSpec(lines, "Fuel", book.getFuelType());
        addSpec(lines, "Cylinder capacity", book.getCylinderCapacity());
        addSpec(lines, "Seats", book.getSeatingCapacity());
        addSpec(lines, "Year of manufacture", firstText(book.getYearOfManufacture(), vehicle.getYear()));
        addSpec(lines, "Country of origin", book.getCountryOfOrigin());
        addSpec(lines, "Date of first registration", book.getDateOfFirstRegistration());
        addSpec(lines, "Mileage", firstText(book.getMileage(), vehicle.getMileage()));
        addSpec(lines, "Length", book.getLength());
        addSpec(lines, "Width", book.getWidth());
        addSpec(lines, "Height", book.getHeight());
        addSpec(lines, "Wheel base", book.getWheelBase());
        addSpec(lines, "Over hang", book.getOverHang());
        addSpec(lines, "Unladen weight", book.getWeightUnladen());
        addSpec(lines, "Gross weight", book.getWeightGross());
        addSpec(lines, "Tyre size front", book.getTyreSizeFront());
        addSpec(lines, "Tyre size rear", book.getTyreSizeRear());
        addSpec(lines, "Tyre size single", book.getTyreSizeSingle());
        addSpec(lines, "Tyre size dual", book.getTyreSizeDual());
        addSpec(lines, "Absolute owner", book.getAbsoluteOwner());
        addSpec(lines, "Current owner", book.getCurrentOwnerAddressIdNo());
        addSpec(lines, "Previous owners", book.getPreviousOwners());
        addSpec(lines, "Conditions", book.getConditionsSpecialNotes());
        addSpec(lines, "Provincial council", book.getProvincialCouncil());
        addSpec(lines, "Taxes payable", book.getTaxesPayable());
        addSpec(lines, "Printed date", book.getPrintedDate());
        return lines;
    }

    private static String firstText(String primary, String fallback) {
        String value = text(primary);
        return value != null ? value : text(fallback);
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
