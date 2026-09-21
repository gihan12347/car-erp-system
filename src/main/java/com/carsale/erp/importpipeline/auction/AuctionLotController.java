package com.carsale.erp.importpipeline.auction;

import com.carsale.erp.importpipeline.ImportStageUrls;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.carsale.erp.shared.document.document.WorkingSheet;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
import com.carsale.erp.shared.pipeline.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.ChassisCheckResult;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.customspipeline.CustomsDocument;
import com.carsale.erp.importpipeline.equipment.EquipmentInspection;
import com.carsale.erp.importpipeline.exportcert.ExportCertificate;
import com.carsale.erp.importpipeline.standards.StandardsCertificate;
import com.carsale.erp.importpipeline.preshipment.PreShipmentInspection;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.customspipeline.CustomsProgressService;
import com.carsale.erp.customspipeline.CustomsDocumentService;
import com.carsale.erp.importpipeline.equipment.EquipmentInspectionFields;
import com.carsale.erp.importpipeline.equipment.EquipmentInspectionService;
import com.carsale.erp.importpipeline.ImportProgressService;
import com.carsale.erp.importpipeline.ImportProgressService.ImportProgress;
import com.carsale.erp.importpipeline.coi.CertificateOfInspectionService;
import com.carsale.erp.importpipeline.exportcert.ExportCertificateService;
import com.carsale.erp.importpipeline.photos.VehiclePhotoService;
import com.carsale.erp.importpipeline.standards.StandardsCertificateService;
import com.carsale.erp.importpipeline.preshipment.PreShipmentService;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.VehicleService;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

@Controller
@RequestMapping("/auction")
public class AuctionLotController {

    private final VehicleService vehicleService;
    private final SheetDocumentStorageService documentStorageService;
    private final PreShipmentService preShipmentService;
    private final EquipmentInspectionService equipmentInspectionService;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final CustomsDocumentService customsDocumentService;
    private final CertificateOfInspectionService certificateOfInspectionService;
    private final StandardsCertificateService standardsCertificateService;
    private final ExportCertificateService exportCertificateService;
    private final VehiclePhotoService vehiclePhotoService;
    private final PipelineStageService pipelineStageService;
    private final WorkingSheet.AuctionSheetParser parser;
    private final OcrImagePreparer imagePreparer;

    public AuctionLotController(
            VehicleService vehicleService,
            SheetDocumentStorageService documentStorageService,
            PreShipmentService preShipmentService,
            EquipmentInspectionService equipmentInspectionService,
            ImportProgressService importProgressService,
            CustomsProgressService customsProgressService,
            CustomsDocumentService customsDocumentService,
            CertificateOfInspectionService certificateOfInspectionService,
            StandardsCertificateService standardsCertificateService,
            ExportCertificateService exportCertificateService,
            VehiclePhotoService vehiclePhotoService,
            PipelineStageService pipelineStageService, WorkingSheet.AuctionSheetParser parser, OcrImagePreparer imagePreparer
    ) {
        this.vehicleService = vehicleService;
        this.documentStorageService = documentStorageService;
        this.preShipmentService = preShipmentService;
        this.equipmentInspectionService = equipmentInspectionService;
        this.importProgressService = importProgressService;
        this.customsProgressService = customsProgressService;
        this.customsDocumentService = customsDocumentService;
        this.certificateOfInspectionService = certificateOfInspectionService;
        this.standardsCertificateService = standardsCertificateService;
        this.exportCertificateService = exportCertificateService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.pipelineStageService = pipelineStageService;
        this.parser = parser;
        this.imagePreparer = imagePreparer;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return "redirect:/import?q=" + UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
        }
        return "redirect:/import";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Map<Integer, String> StageKeys = ImportStageUrls.getStageKeyBySortOrder(
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT));
        String startStage = StageKeys.isEmpty()
                ? FlowStage.AUCTION.getStageKey()
                : StageKeys.values().iterator().next();
        Vehicle vehicle;
        if (model.containsAttribute("vehicle")) {
            vehicle = (Vehicle) model.asMap().get("vehicle");
        } else {
            vehicle = new Vehicle();
            model.addAttribute("vehicle", vehicle);
        }
        model.addAttribute("activeMenu", "import");
        model.addAttribute("editMode", false);
        model.addAttribute("hubMode", false);
        model.addAttribute("stages", VehicleStage.values());
        model.addAttribute("auctionDocReady", importProgressService.hasAuctionDocument(vehicle));
        model.addAttribute("preshipReady", false);
        model.addAttribute("equipmentReady", false);
        model.addAttribute("jevicReady", false);
        model.addAttribute("coiReady", false);
        model.addAttribute("standardsReady", false);
        model.addAttribute("exportReady", false);
        model.addAttribute("photosReady", false);
        model.addAttribute("StageKeys", StageKeys);
        prepareStartStageForm(model, startStage);
        return "import-pipeline/auction/form";
    }

    @GetMapping("/check-chassis")
    @ResponseBody
    public ChassisCheckResult checkChassis(@RequestParam("chassisNo") String chassisNo) {
        ChassisCheckResult result = new ChassisCheckResult();
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            result.setExists(false);
            return result;
        }
        Vehicle existing = vehicleService.findByChassisNo(chassisNo.trim());
        if (existing == null) {
            result.setExists(false);
            return result;
        }

        ImportProgress status = importProgressService.progressFor(existing);
        result.setExists(true);
        result.setCompletedSteps(status.completedCount());
        result.setVehicleLabel(buildVehicleLabel(existing));
        result.setPipelineUrl(ImportStageUrls.pipelineHubUrl(
                chassisNo.trim(),
                status,
                pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
        ));
        result.setMessage(buildDuplicateChassisMessage(chassisNo.trim(), existing, status));
        return result;
    }

    @GetMapping("/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/{chassisNo}")
    public String detail(
            @PathVariable String chassisNo,
            @RequestParam(value = "stage", required = false) String stage,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model
    ) {
        if (hub) {
            return "redirect:/auction/" + encodeChassis(chassisNo) + "/edit?hub=1";
        }
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        ImportProgress importStatus = importProgressService.progressFor(vehicle);
        List<PipelineStage> importKeys = pipelineStageService.list(PipelineStageService.FLOW_IMPORT);
        int stageIndex = ImportStageUrls.resolveStageIndex(importKeys, stage);
        String stageKey = ImportStageUrls.stageKeyAt(importKeys, stageIndex);
        NavLinks nav = viewLinks(chassisNo, stageIndex, importStatus, importKeys, FlowPipeline.IMPORT);

        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "import");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("preShipmentReady", importStatus.isPreshipReady());
        model.addAttribute("preShipment", preShipmentService.findByChassisNo(chassisNo));
        model.addAttribute("equipmentReady", importStatus.isEquipmentReady());
        model.addAttribute("equipmentInspection", equipmentInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("jevicReady", importStatus.isOdometerReady());
        model.addAttribute("coiReady", importStatus.isCoiReady());
        model.addAttribute("standardsReady", importStatus.isStandardsReady());
        model.addAttribute("exportReady", importStatus.isExportReady());
        model.addAttribute("photosReady", importStatus.isPhotosReady());
        model.addAttribute("clearance", customsDocumentService.findByChassisNo(chassisNo));
        model.addAttribute("inspectionCertificate", certificateOfInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("standardsCertificate", standardsCertificateService.findByChassisNo(chassisNo));
        model.addAttribute("exportCertificate", exportCertificateService.findByChassisNo(chassisNo));
        model.addAttribute("vehiclePhotos", vehiclePhotoService.list(chassisNo));
        model.addAttribute("interiorFields", EquipmentInspectionFields.interior());
        model.addAttribute("exteriorFields", EquipmentInspectionFields.ungrouped(EquipmentInspectionFields.exterior()));
        model.addAttribute("bodyKitFields", EquipmentInspectionFields.group(
                EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_BODY_KIT));
        model.addAttribute("truckBodyFields", EquipmentInspectionFields.group(
                EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_TRUCK_BODY));
        model.addAttribute("safetyFields", EquipmentInspectionFields.safety());
        model.addAttribute("stages", VehicleStage.values());
        model.addAttribute("auctionDocReady", importStatus.isAuctionReady());
        model.addAttribute("importComplete", importStatus.isPipelineCompleted());
        model.addAttribute("importDoneCount", importStatus.completedCount());
        model.addAttribute("stageIndex", stageIndex);
        model.addAttribute("stageKey", stageKey);
        model.addAttribute("stageNav", nav);
        return "import-pipeline/auction/detail";
    }

    @GetMapping("/{chassisNo}/edit")
    public String editForm(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model
    ) {
        Vehicle vehicle;
        if (model.containsAttribute("vehicle")) {
            vehicle = (Vehicle) model.asMap().get("vehicle");
        } else {
            vehicle = vehicleService.findByChassisNo(chassisNo);
        }
        if (vehicle == null) {
            return "redirect:/auction";
        }
        ImportProgress importStatus = importProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", "Edit auction lot");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("editMode", true);
        model.addAttribute("hubMode", hub);
        model.addAttribute("stages", VehicleStage.values());
        model.addAttribute("auctionDocReady", importStatus.isAuctionReady());
        model.addAttribute("preshipReady", importStatus.isPreshipReady());
        model.addAttribute("equipmentReady", importStatus.isEquipmentReady());
        model.addAttribute("jevicReady", importStatus.isOdometerReady());
        model.addAttribute("coiReady", importStatus.isCoiReady());
        model.addAttribute("standardsReady", importStatus.isStandardsReady());
        model.addAttribute("exportReady", importStatus.isExportReady());
        model.addAttribute("photosReady", importStatus.isPhotosReady());
        model.addAttribute("StageKeys", ImportStageUrls.getStageKeyBySortOrder(
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT)));
        model.addAttribute("stageNav", ImportStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, FlowStage.AUCTION.getStageKey()),
                importStatus,
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
        ));
        return "import-pipeline/auction/form";
    }

    @PostMapping("/upload-sheet")
    @ResponseBody
    public SheetUploadResult uploadSheet(@RequestParam("file") MultipartFile file) {
        try {
            return documentStorageService.store(file);
        } catch (IOException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Could not save the document. " + ex.getMessage());
            return failed;
        }
    }

    @PostMapping("/parse-sheet")
    @ResponseBody
    public AuctionParseResult parseSheet(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "provider", required = false) String provider
    ) {
        return imagePreparer.parsePage(file, parser, provider);
    }

    @PostMapping
    public String save(
            @ModelAttribute Vehicle vehicle,
            @RequestParam(value = "editMode", defaultValue = "false") boolean editMode,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String chassisNo = vehicle.getChassisNo() == null ? "" : vehicle.getChassisNo().trim();
            if (chassisNo.isEmpty()) {
                throw new IllegalArgumentException("Chassis number is required.");
            }
            Vehicle existing = vehicleService.findByChassisNo(chassisNo);
            if (!editMode && existing != null) {
                ImportProgress status = importProgressService.progressFor(existing);
                List<String> importKeys = pipelineStageService.keys(PipelineStageService.FLOW_IMPORT);
                redirectAttributes.addFlashAttribute("notice", buildDuplicateChassisMessage(chassisNo, existing, status));
                return "redirect:" + ImportStageUrls.pipelineHubUrl(chassisNo, status, importKeys);
            }
            Vehicle saved = vehicleService.save(vehicle);
            importProgressService.syncVehicleStage(saved.getChassisNo());
            customsProgressService.syncVehicleStage(saved.getChassisNo());
            redirectAttributes.addFlashAttribute("successMessage", "Auction lot saved.");
            ImportProgress status = importProgressService.progressFor(saved);
            List<String> importKeys = pipelineStageService.keys(PipelineStageService.FLOW_IMPORT);
            if (!editMode) {
                return "redirect:" + ImportStageUrls.redirectAfterNewVehicle(
                        saved.getChassisNo(),
                        status,
                        importKeys
                );
            }
            return "redirect:" + ImportStageUrls.redirectAfterAuctionSave(
                    saved.getChassisNo(),
                    importKeys
            );
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Could not save the vehicle. A record with the same stock number may already exist.");
            redirectAttributes.addFlashAttribute("vehicle", vehicle);
            if (vehicle.getChassisNo() == null || vehicle.getChassisNo().trim().isEmpty()) {
                return "redirect:/auction/new";
            }
            return "redirect:/auction/" + encodeChassis(vehicle.getChassisNo()) + "/edit?hub=1";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("vehicle", vehicle);
            if (vehicle.getChassisNo() == null || vehicle.getChassisNo().trim().isEmpty()) {
                return "redirect:/auction/new";
            }
            if (editMode || vehicleService.existsByChassisNo(vehicle.getChassisNo())) {
                return "redirect:/auction/" + encodeChassis(vehicle.getChassisNo()) + "/edit?hub=1";
            }
            return "redirect:/auction/new";
        }
    }

    @PostMapping("/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (vehicleService.delete(chassisNo)) {
            redirectAttributes.addFlashAttribute("notice", "Vehicle and later pipeline data deleted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete auction lot.");
        }
        return "redirect:/import";
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }

    private void prepareStartStageForm(Model model, String startStage) {
        if (FlowStage.JEVIC.getStageKey().equals(startStage)) {
            if (!model.containsAttribute("record")) {
                model.addAttribute("record", new CustomsDocument());
            }
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, FlowStage.JEVIC.getStageKey()));
            return;
        }
        if (FlowStage.COI.getStageKey().equals(startStage)) {
            if (!model.containsAttribute("record")) {
                model.addAttribute("record", certificateOfInspectionService.newBlank());
            }
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, FlowStage.COI.getStageKey()));
            return;
        }
        if (FlowStage.STANDARDS.getStageKey().equals(startStage)) {
            if (!model.containsAttribute("record")) {
                model.addAttribute("record", new StandardsCertificate());
            }
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, FlowStage.STANDARDS.getStageKey()));
            return;
        }
        if (FlowStage.EXPORT.getStageKey().equals(startStage)) {
            if (!model.containsAttribute("record")) {
                model.addAttribute("record", new ExportCertificate());
            }
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, FlowStage.EXPORT.getStageKey()));
            return;
        }
        if (FlowStage.PHOTOS.getStageKey().equals(startStage)) {
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, FlowStage.PHOTOS.getStageKey()));
            return;
        }
        if (FlowStage.EQUIPMENT.getStageKey().equals(startStage)) {
            if (!model.containsAttribute("record")) {
                model.addAttribute("record", new EquipmentInspection());
            }
            model.addAttribute("interiorFields", EquipmentInspectionFields.interior());
            model.addAttribute("exteriorFields", EquipmentInspectionFields.ungrouped(EquipmentInspectionFields.exterior()));
            model.addAttribute("bodyKitFields", EquipmentInspectionFields.group(
                    EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_BODY_KIT));
            model.addAttribute("truckBodyFields", EquipmentInspectionFields.group(
                    EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_TRUCK_BODY));
            model.addAttribute("safetyFields", EquipmentInspectionFields.safety());
            model.addAttribute("equipmentFieldKeys", EquipmentInspectionFields.allKeys());
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, FlowStage.EQUIPMENT.getStageKey()));
            return;
        }
        if (FlowStage.PRESHIP.getStageKey().equals(startStage)) {
            if (!model.containsAttribute("record")) {
                model.addAttribute("record", new PreShipmentInspection());
            }
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, FlowStage.PRESHIP.getStageKey()));
            return;
        }
        model.addAttribute("pageTitle", "Add auction lot");
    }

    private static String buildVehicleLabel(Vehicle vehicle) {
        if (vehicle == null) {
            return "";
        }
        String model = vehicle.getModel() == null ? "" : vehicle.getModel().trim();
        String stock = vehicle.getStockNo() == null ? "" : vehicle.getStockNo().trim();
        if (!model.isEmpty() && !stock.isEmpty()) {
            return stock + " · " + model;
        } else if (!model.isEmpty()) {
            return model;
        } else if (!stock.isEmpty()) {
            return stock;
        } else {
            return "";
        }
    }

    private String buildDuplicateChassisMessage(String chassisNo, Vehicle existing, ImportProgress status) {
        String label = buildVehicleLabel(existing);
        String progress = status.completedCount() + " of "
                + pipelineStageService.size(PipelineStageService.FLOW_IMPORT) + " import steps complete";
        if (label.isEmpty()) {
            return "Chassis number " + chassisNo + " is already in the import pipeline (" + progress + "). "
                    + "No new vehicle was created — opened the existing record instead.";
        }
        return "Chassis number " + chassisNo + " already exists (" + label + ", " + progress + "). "
                + "No new vehicle was created — opened the existing import pipeline instead.";
    }
}
