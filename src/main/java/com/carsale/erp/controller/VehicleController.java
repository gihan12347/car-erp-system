package com.carsale.erp.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.carsale.erp.dto.NavLinks;
import com.carsale.erp.entity.PipelineStage;
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

import com.carsale.erp.dto.AuctionParseResult;
import com.carsale.erp.dto.ChassisCheckResult;
import com.carsale.erp.dto.SheetUploadResult;
import com.carsale.erp.entity.EquipmentInspection;
import com.carsale.erp.entity.PreShipmentInspection;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.VehicleStage;
import com.carsale.erp.service.AuctionSheetOcrService;
import com.carsale.erp.service.ClearancePipelineService;
import com.carsale.erp.service.EquipmentInspectionFields;
import com.carsale.erp.service.EquipmentInspectionService;
import com.carsale.erp.service.ImportPipelineService;
import com.carsale.erp.service.ImportPipelineService.ImportStatus;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreShipmentService;
import com.carsale.erp.service.SheetDocumentStorageService;
import com.carsale.erp.service.VehicleService;

@Controller
@RequestMapping("/auction")
public class VehicleController {

    private final VehicleService vehicleService;
    private final AuctionSheetOcrService ocrService;
    private final SheetDocumentStorageService documentStorageService;
    private final PreShipmentService preShipmentService;
    private final EquipmentInspectionService equipmentInspectionService;
    private final ImportPipelineService importPipelineService;
    private final ClearancePipelineService clearancePipelineService;
    private final PipelineStageService pipelineStageService;

    public VehicleController(
            VehicleService vehicleService,
            AuctionSheetOcrService ocrService,
            SheetDocumentStorageService documentStorageService,
            PreShipmentService preShipmentService,
            EquipmentInspectionService equipmentInspectionService,
            ImportPipelineService importPipelineService,
            ClearancePipelineService clearancePipelineService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.ocrService = ocrService;
        this.documentStorageService = documentStorageService;
        this.preShipmentService = preShipmentService;
        this.equipmentInspectionService = equipmentInspectionService;
        this.importPipelineService = importPipelineService;
        this.clearancePipelineService = clearancePipelineService;
        this.pipelineStageService = pipelineStageService;
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
        Map<Integer, String> StageKeys = ImportStageNavigation.getStageKeyBySortOrder(
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT));
        String startStage = StageKeys.isEmpty()
                ? PipelineStageService.STAGE_AUCTION
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
        model.addAttribute("auctionDocReady", importPipelineService.hasAuctionDocument(vehicle));
        model.addAttribute("preshipReady", false);
        model.addAttribute("equipmentReady", false);
        model.addAttribute("StageKeys", StageKeys);
        prepareStartStageForm(model, startStage);
        return "vehicles/form";
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

        ImportStatus status = importPipelineService.statusFor(existing);
        result.setExists(true);
        result.setCompletedSteps(status.completedCount());
        result.setVehicleLabel(buildVehicleLabel(existing));
        result.setPipelineUrl(ImportStageNavigation.pipelineHubUrl(
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
            @RequestParam(value = "stage", required = false, defaultValue = "0") Integer stage,
            Model model
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        vehicle = vehicleService.findByChassisNo(chassisNo);

        ImportStatus importStatus = importPipelineService.statusFor(vehicle);
        List<PipelineStage> importKeys = pipelineStageService.list(PipelineStageService.FLOW_IMPORT);
        NavLinks nav = ImportStageNavigation.viewLinks(chassisNo, stage, importStatus, importKeys);

        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "import");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("preShipmentReady", importStatus.isPreshipReady());
        model.addAttribute("preShipment", preShipmentService.findByChassisNo(chassisNo));
        model.addAttribute("equipmentReady", importStatus.isEquipmentReady());
        model.addAttribute("equipmentInspection", equipmentInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("interiorFields", EquipmentInspectionFields.interior());
        model.addAttribute("exteriorFields", EquipmentInspectionFields.ungrouped(EquipmentInspectionFields.exterior()));
        model.addAttribute("bodyKitFields", EquipmentInspectionFields.group(
                EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_BODY_KIT));
        model.addAttribute("truckBodyFields", EquipmentInspectionFields.group(
                EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_TRUCK_BODY));
        model.addAttribute("safetyFields", EquipmentInspectionFields.safety());
        model.addAttribute("stages", VehicleStage.values());
        model.addAttribute("auctionDocReady", importStatus.isAuctionReady());
        model.addAttribute("importComplete", importStatus.isImportComplete());
        model.addAttribute("importDoneCount", importStatus.completedCount());
        model.addAttribute("stageIndex", stage);
        model.addAttribute("stageKey", ImportStageNavigation.getStageKeyBySortOrder(importKeys, stage));
        model.addAttribute("stageNav", nav);
        return "vehicles/detail";
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
        ImportStatus importStatus = importPipelineService.statusFor(vehicle);
        model.addAttribute("pageTitle", "Edit auction lot");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("editMode", true);
        model.addAttribute("hubMode", hub);
        model.addAttribute("stages", VehicleStage.values());
        model.addAttribute("auctionDocReady", importStatus.isAuctionReady());
        model.addAttribute("preshipReady", importStatus.isPreshipReady());
        model.addAttribute("equipmentReady", importStatus.isEquipmentReady());
        model.addAttribute("StageKeys", ImportStageNavigation.getStageKeyBySortOrder(
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT)));
        if (hub) {
            model.addAttribute("stageNav", ImportStageNavigation.editLinks(
                    chassisNo,
                    pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, PipelineStageService.STAGE_AUCTION),
                    importStatus,
                    pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
            ));
        }
        return "vehicles/form";
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
        return ocrService.parseSheet(file, provider);
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
                ImportStatus status = importPipelineService.statusFor(existing);
                List<String> importKeys = pipelineStageService.keys(PipelineStageService.FLOW_IMPORT);
                redirectAttributes.addFlashAttribute("notice", buildDuplicateChassisMessage(chassisNo, existing, status));
                return "redirect:" + ImportStageNavigation.pipelineHubUrl(chassisNo, status, importKeys);
            }
            Vehicle saved = vehicleService.save(vehicle);
            importPipelineService.syncVehicleStage(saved.getChassisNo());
            clearancePipelineService.syncVehicleStage(saved.getChassisNo());
            redirectAttributes.addFlashAttribute("successMessage", "Auction lot saved.");
            ImportStatus status = importPipelineService.statusFor(saved);
            List<String> importKeys = pipelineStageService.keys(PipelineStageService.FLOW_IMPORT);
            if (!editMode) {
                return "redirect:" + ImportStageNavigation.redirectAfterNewVehicle(
                        saved.getChassisNo(),
                        status,
                        importKeys
                );
            }
            return "redirect:" + ImportStageNavigation.redirectAfterAuctionSave(
                    saved.getChassisNo(),
                    status,
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
        if (PipelineStageService.STAGE_EQUIPMENT.equals(startStage)) {
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
                    PipelineStageService.FLOW_IMPORT, PipelineStageService.STAGE_EQUIPMENT));
            return;
        }
        if (PipelineStageService.STAGE_PRESHIP.equals(startStage)) {
            if (!model.containsAttribute("record")) {
                model.addAttribute("record", new PreShipmentInspection());
            }
            model.addAttribute("pageTitle", pipelineStageService.title(
                    PipelineStageService.FLOW_IMPORT, PipelineStageService.STAGE_PRESHIP));
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
        }
        if (!model.isEmpty()) {
            return model;
        }
        if (!stock.isEmpty()) {
            return stock;
        }
        return "";
    }

    private static String buildDuplicateChassisMessage(String chassisNo, Vehicle existing, ImportStatus status) {
        String label = buildVehicleLabel(existing);
        String progress = status.completedCount() + " of 3 import steps complete";
        if (label.isEmpty()) {
            return "Chassis number " + chassisNo + " is already in the import pipeline (" + progress + "). "
                    + "No new vehicle was created — opened the existing record instead.";
        }
        return "Chassis number " + chassisNo + " already exists (" + label + ", " + progress + "). "
                + "No new vehicle was created — opened the existing import pipeline instead.";
    }
}
