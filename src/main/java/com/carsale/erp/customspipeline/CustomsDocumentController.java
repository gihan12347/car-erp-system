package com.carsale.erp.customspipeline;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.vehicle.VehicleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.customspipeline.CustomsProgressService.CustomsProgress;

@Controller
public class CustomsDocumentController {

    private final CustomsDocumentService customsDocumentService;
    private final CustomsDocumentOcrService ocrService;
    private final SheetDocumentStorageService documentStorageService;
    private final CustomsProgressService customsProgressService;
    private final PipelineStageService pipelineStageService;
    private final VehicleService vehicleService;

    public CustomsDocumentController(
            CustomsDocumentService customsDocumentService,
            CustomsDocumentOcrService ocrService,
            SheetDocumentStorageService documentStorageService,
            CustomsProgressService customsProgressService,
            PipelineStageService pipelineStageService,
            VehicleService vehicleService
    ) {
        this.customsDocumentService = customsDocumentService;
        this.ocrService = ocrService;
        this.documentStorageService = documentStorageService;
        this.customsProgressService = customsProgressService;
        this.pipelineStageService = pipelineStageService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/customs/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadClearanceAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/declaration/{chassisNo}")
    public String declarationForm(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return showForm(chassisNo, FlowStage.DECLARATION, hub, model, redirectAttributes);
    }

    @GetMapping("/assessment/{chassisNo}")
    public String assessmentForm(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return showForm(chassisNo, FlowStage.ASSESSMENT, hub, model, redirectAttributes);
    }

    @GetMapping("/worksheet/{chassisNo}")
    public String worksheetForm(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return showForm(chassisNo, FlowStage.WORKSHEET, hub, model, redirectAttributes);
    }

    @GetMapping("/customs/{chassisNo}/edit")
    public String legacyForm(
            @PathVariable String chassisNo,
            @RequestParam(value = "tab", required = false) Integer tab
    ) {
        if (tab != null && tab == 1) {
            return "redirect:/jevic/" + encodeChassis(chassisNo);
        }
        String stageKey = FlowStage.DECLARATION.getStageKey();
        if (tab != null && tab == 3) {
            stageKey = FlowStage.ASSESSMENT.getStageKey();
        } else if (tab != null && tab == 4) {
            stageKey = FlowStage.WORKSHEET.getStageKey();
        }
        return "redirect:" + PipelineStageService.editUrlFor(encodeChassis(chassisNo), stageKey);
    }

    private String showForm(
            String chassisNo,
            FlowStage stage,
            boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        if (!customsProgressService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Finish the import pipeline before the customs clearance pipeline.");
            return "redirect:" + customsProgressService.redirectWhenNotEligible(vehicle);
        }
        CustomsDocument record = customsDocumentService.prepareForm(chassisNo);
        CustomsProgress status = customsProgressService.progressFor(chassisNo);
        model.addAttribute("pageTitle", PipelineStageService.titleFor(stage.getStageKey()));
        model.addAttribute("activeMenu", "customs");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("currentStep", stage.getStageKey());
        model.addAttribute("declarationReady", status.isDeclarationReady());
        model.addAttribute("assessmentReady", status.isAssessmentReady());
        model.addAttribute("worksheetReady", status.isWorksheetReady());
        return "customs-pipeline/form";
    }

    @PostMapping("/customs/{chassisNo}/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(
            @PathVariable String chassisNo,
            @RequestParam("file") MultipartFile file,
            @RequestParam("page") int page
    ) {
        if (page < 1 || page > 4) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Invalid page number. Use page 1, 2, 3, or 4.");
            return failed;
        }
        try {
            SheetUploadResult result = documentStorageService.storeClearance(file);
            if (result.isSuccess()) {
                customsDocumentService.savePageDocument(
                        chassisNo,
                        page,
                        result.getOriginalName(),
                        result.getStoredName(),
                        result.getContentType()
                );
                customsProgressService.syncVehicleStage(chassisNo);
            }
            return result;
        } catch (IOException | IllegalArgumentException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Could not save the document. " + ex.getMessage());
            return failed;
        }
    }

    @PostMapping("/customs/parse-document")
    @ResponseBody
    public AuctionParseResult parseDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("page") int page,
            @RequestParam(value = "provider", required = false) String provider
    ) {
        if (page < 1 || page > 4) {
            AuctionParseResult failed = new AuctionParseResult();
            failed.setSuccess(false);
            failed.setMessage("Invalid page number. Use page 1, 2, 3, or 4.");
            return failed;
        }
        return ocrService.parsePage(file, page, provider);
    }

    @PostMapping("/customs/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute CustomsDocument record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            customsDocumentService.save(record);
            customsProgressService.syncVehicleStage(chassisNo);
            CustomsProgress status = customsProgressService.progressFor(chassisNo);
            if (status.isClearanceComplete()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Customs clearance is complete. Continue with the preparation pipeline.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Clearance documents saved.");
            }
            return "redirect:" + CustomsStageUrls.redirectAfterClearanceSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_CUSTOMS)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + PipelineStageService.editUrlFor(
                    encodeChassis(chassisNo),
                    FlowStage.DECLARATION.getStageKey()
            );
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
