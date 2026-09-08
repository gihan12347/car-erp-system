package com.carsale.erp.importpipeline.odometer;

import com.carsale.erp.importpipeline.ImportStageUrls;
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

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.customspipeline.CustomsDocument;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.customspipeline.CustomsDocumentOcrService;
import com.carsale.erp.customspipeline.CustomsProgressService;
import com.carsale.erp.customspipeline.CustomsDocumentService;
import com.carsale.erp.importpipeline.ImportProgressService;
import com.carsale.erp.importpipeline.ImportProgressService.ImportProgress;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/jevic")
public class OdometerCertificateController {

    private final CustomsDocumentService customsDocumentService;
    private final VehicleService vehicleService;
    private final CustomsDocumentOcrService ocrService;
    private final SheetDocumentStorageService documentStorageService;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PipelineStageService pipelineStageService;

    public OdometerCertificateController(
            CustomsDocumentService customsDocumentService,
            VehicleService vehicleService,
            CustomsDocumentOcrService ocrService,
            SheetDocumentStorageService documentStorageService,
            ImportProgressService importProgressService,
            CustomsProgressService customsProgressService,
            PipelineStageService pipelineStageService
    ) {
        this.customsDocumentService = customsDocumentService;
        this.vehicleService = vehicleService;
        this.ocrService = ocrService;
        this.documentStorageService = documentStorageService;
        this.importProgressService = importProgressService;
        this.customsProgressService = customsProgressService;
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
        Resource resource = documentStorageService.loadClearanceAsResource(storedName);
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
            @ModelAttribute CustomsDocument record,
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
            ImportProgress status = importProgressService.progressFor(existing);
            redirectAttributes.addFlashAttribute("notice",
                    "Chassis number " + chassisNo + " is already in the import pipeline. "
                            + "No new vehicle was created — opened the existing record instead.");
            return "redirect:" + ImportStageUrls.pipelineHubUrl(
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
            customsDocumentService.saveOdometerCertificate(record);
            importProgressService.syncVehicleStage(chassisNo);
            customsProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "JEVIC certificate saved.");
            return "redirect:" + ImportStageUrls.redirectAfterOdometerSave(
                    chassisNo,
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
        CustomsDocument record = customsDocumentService.prepareForm(chassisNo);
        ImportProgress status = importProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_IMPORT, FlowStage.JEVIC.getStageKey()));
        model.addAttribute("activeMenu", "import");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("auctionDocReady", status.isAuctionReady());
        model.addAttribute("preshipReady", status.isPreshipReady());
        model.addAttribute("equipmentReady", status.isEquipmentReady());
        model.addAttribute("jevicReady", status.isOdometerReady());
        model.addAttribute("coiReady", status.isCoiReady());
        model.addAttribute("standardsReady", status.isStandardsReady());
        model.addAttribute("exportReady", status.isExportReady());
        model.addAttribute("photosReady", status.isPhotosReady());
        model.addAttribute("stageNav", ImportStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, FlowStage.JEVIC.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
        ));
        return "import-pipeline/odometer/form";
    }

    @PostMapping("/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return documentStorageService.storeClearance(file);
        } catch (IOException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Could not save the document. " + ex.getMessage());
            return failed;
        }
    }

    @PostMapping("/parse-document")
    @ResponseBody
    public AuctionParseResult parseDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "provider", required = false) String provider
    ) {
        return ocrService.parsePage(file, 1, provider);
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute CustomsDocument record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            customsDocumentService.saveOdometerCertificate(record);
            importProgressService.syncVehicleStage(chassisNo);
            customsProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "JEVIC certificate saved.");
            return "redirect:" + ImportStageUrls.redirectAfterOdometerSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/jevic/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
