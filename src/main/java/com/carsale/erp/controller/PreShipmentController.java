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
import com.carsale.erp.entity.PreShipmentInspection;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.service.ClearancePipelineService;
import com.carsale.erp.service.ImportPipelineService;
import com.carsale.erp.service.ImportPipelineService.ImportStatus;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreShipmentOcrService;
import com.carsale.erp.service.PreShipmentService;
import com.carsale.erp.service.SheetDocumentStorageService;
import com.carsale.erp.service.VehicleService;

@Controller
@RequestMapping("/shipping")
public class PreShipmentController {

    private final PreShipmentService preShipmentService;
    private final VehicleService vehicleService;
    private final PreShipmentOcrService ocrService;
    private final SheetDocumentStorageService documentStorageService;
    private final ImportPipelineService importPipelineService;
    private final ClearancePipelineService clearancePipelineService;
    private final PipelineStageService pipelineStageService;

    public PreShipmentController(
            PreShipmentService preShipmentService,
            VehicleService vehicleService,
            PreShipmentOcrService ocrService,
            SheetDocumentStorageService documentStorageService,
            ImportPipelineService importPipelineService,
            ClearancePipelineService clearancePipelineService,
            PipelineStageService pipelineStageService
    ) {
        this.preShipmentService = preShipmentService;
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
        Resource resource = documentStorageService.loadPreShipmentAsResource(storedName);
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
            @ModelAttribute PreShipmentInspection record,
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
            preShipmentService.save(record);
            importPipelineService.syncVehicleStage(chassisNo);
            clearancePipelineService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Pre-shipment certificate saved.");
            ImportStatus status = importPipelineService.statusFor(chassisNo);
            return "redirect:" + ImportStageNavigation.redirectAfterPreshipSave(
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
        PreShipmentInspection record = preShipmentService.prepareForm(chassisNo);
        ImportStatus status = importPipelineService.statusFor(vehicle);
        model.addAttribute("pageTitle", "Pre-shipment certificate");
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
                    pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, PipelineStageService.STAGE_PRESHIP),
                    status,
                    pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
            ));
        }
        return "shipping/form";
    }

    @PostMapping("/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return documentStorageService.storePreShipment(file);
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
            @ModelAttribute PreShipmentInspection record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            preShipmentService.save(record);
            importPipelineService.syncVehicleStage(chassisNo);
            clearancePipelineService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Pre-shipment certificate saved.");
            ImportStatus status = importPipelineService.statusFor(chassisNo);
            return "redirect:" + ImportStageNavigation.redirectAfterPreshipSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shipping/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
