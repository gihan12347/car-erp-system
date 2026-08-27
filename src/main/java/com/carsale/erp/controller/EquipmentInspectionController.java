package com.carsale.erp.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
import com.carsale.erp.dto.SheetUploadResult;
import com.carsale.erp.entity.EquipmentInspection;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.service.ClearancePipelineService;
import com.carsale.erp.service.EquipmentInspectionFields;
import com.carsale.erp.service.EquipmentInspectionOcrService;
import com.carsale.erp.service.EquipmentInspectionService;
import com.carsale.erp.service.ImportPipelineService;
import com.carsale.erp.service.ImportPipelineService.ImportStatus;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.SheetDocumentStorageService;
import com.carsale.erp.service.VehicleService;

@Controller
@RequestMapping("/equipment")
public class EquipmentInspectionController {

    private final EquipmentInspectionService equipmentInspectionService;
    private final VehicleService vehicleService;
    private final EquipmentInspectionOcrService ocrService;
    private final SheetDocumentStorageService documentStorageService;
    private final ImportPipelineService importPipelineService;
    private final ClearancePipelineService clearancePipelineService;
    private final PipelineStageService pipelineStageService;

    public EquipmentInspectionController(
            EquipmentInspectionService equipmentInspectionService,
            VehicleService vehicleService,
            EquipmentInspectionOcrService ocrService,
            SheetDocumentStorageService documentStorageService,
            ImportPipelineService importPipelineService,
            ClearancePipelineService clearancePipelineService,
            PipelineStageService pipelineStageService
    ) {
        this.equipmentInspectionService = equipmentInspectionService;
        this.vehicleService = vehicleService;
        this.ocrService = ocrService;
        this.documentStorageService = documentStorageService;
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

    @GetMapping("/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadEquipmentAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/new")
    public String newLot() {
        return "redirect:/auction/new";
    }

    @PostMapping("/new")
    public String saveNew(
            @ModelAttribute EquipmentInspection record,
            RedirectAttributes redirectAttributes
    ) {
        String chassisNo = record.getChassisNo() == null ? "" : record.getChassisNo().trim();
        if (chassisNo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Chassis number is required.");
            redirectAttributes.addFlashAttribute("record", record);
            return "redirect:/auction/new";
        }
        Vehicle existing = vehicleService.findByChassisNo(chassisNo);
        if (existing != null) {
            ImportStatus status = importPipelineService.statusFor(existing);
            redirectAttributes.addFlashAttribute("notice",
                    "Chassis number " + chassisNo + " is already in the import pipeline. "
                            + "No new vehicle was created — opened the existing record instead.");
            return "redirect:" + ImportStageNavigation.pipelineHubUrl(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        }
        try {
            Vehicle vehicle = new Vehicle();
            vehicle.setChassisNo(chassisNo);
            vehicleService.save(vehicle);
            record.setChassisNo(chassisNo);
            equipmentInspectionService.save(record);
            importPipelineService.syncVehicleStage(chassisNo);
            clearancePipelineService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Equipment condition saved.");
            ImportStatus status = importPipelineService.statusFor(chassisNo);
            return "redirect:" + ImportStageNavigation.redirectAfterEquipmentSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("record", record);
            return "redirect:/auction/new";
        }
    }

    @GetMapping("/{chassisNo}")
    public String form(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        EquipmentInspection record = equipmentInspectionService.prepareForm(chassisNo);
        ImportStatus status = importPipelineService.statusFor(vehicle);
        addFieldCatalog(model);
        model.addAttribute("pageTitle", "Equipment condition");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("auctionDocReady", status.isAuctionReady());
        model.addAttribute("preshipReady", status.isPreshipReady());
        model.addAttribute("equipmentReady", status.isEquipmentReady());
        if (hub) {
            model.addAttribute("stageNav", ImportStageNavigation.editLinks(
                    chassisNo,
                    pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, PipelineStageService.STAGE_EQUIPMENT),
                    status,
                    pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
            ));
        }
        return "equipment/form";
    }

    @PostMapping("/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return documentStorageService.storeEquipment(file);
        } catch (IOException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Could not save the document. " + ex.getMessage());
            return failed;
        }
    }

    @PostMapping("/parse-document")
    @ResponseBody
    public AuctionParseResult parseDocument(@RequestParam("file") MultipartFile file) {
        return ocrService.parseDocument(file);
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute EquipmentInspection record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            equipmentInspectionService.save(record);
            importPipelineService.syncVehicleStage(chassisNo);
            clearancePipelineService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Equipment condition saved.");
            ImportStatus status = importPipelineService.statusFor(chassisNo);
            return "redirect:" + ImportStageNavigation.redirectAfterEquipmentSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/equipment/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private void addFieldCatalog(Model model) {
        model.addAttribute("interiorFields", EquipmentInspectionFields.interior());
        model.addAttribute("exteriorFields", EquipmentInspectionFields.ungrouped(EquipmentInspectionFields.exterior()));
        model.addAttribute("bodyKitFields", EquipmentInspectionFields.group(
                EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_BODY_KIT));
        model.addAttribute("truckBodyFields", EquipmentInspectionFields.group(
                EquipmentInspectionFields.exterior(), EquipmentInspectionFields.GROUP_TRUCK_BODY));
        model.addAttribute("safetyFields", EquipmentInspectionFields.safety());
        model.addAttribute("equipmentFieldKeys", EquipmentInspectionFields.allKeys());
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
