package com.carsale.erp.customspipeline.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.carsale.erp.shared.document.document.BillOfLading;
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

import com.carsale.erp.customspipeline.model.CustomsDocument;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
import com.carsale.erp.customspipeline.service.CustomsDocumentService;
import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.customspipeline.service.CustomsProgressService.CustomsProgress;
import com.carsale.erp.customspipeline.util.CustomsStageUrls;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/bl")
public class BillOfLadingController {

    private final CustomsDocumentService customsDocumentService;
    private final VehicleService vehicleService;
    private final OcrImagePreparer ocrService;
    private final SheetDocumentStorageService documentStorageService;
    private final CustomsProgressService customsProgressService;
    private final PipelineStageService pipelineStageService;
    private final BillOfLading billOfLading;

    public BillOfLadingController(
            CustomsDocumentService customsDocumentService,
            VehicleService vehicleService,
            OcrImagePreparer ocrService,
            SheetDocumentStorageService documentStorageService,
            CustomsProgressService customsProgressService,
            PipelineStageService pipelineStageService,
            BillOfLading billOfLading
    ) {
        this.customsDocumentService = customsDocumentService;
        this.vehicleService = vehicleService;
        this.ocrService = ocrService;
        this.documentStorageService = documentStorageService;
        this.customsProgressService = customsProgressService;
        this.pipelineStageService = pipelineStageService;
        this.billOfLading = billOfLading;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return "redirect:/customs?q=" + UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
        }
        return "redirect:/customs";
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

    @GetMapping("/{chassisNo}")
    public String form(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/customs";
        }
        if (!customsProgressService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Finish the import pipeline before the customs clearance pipeline.");
            return "redirect:" + customsProgressService.redirectWhenNotEligible(vehicle);
        }
        CustomsDocument record = customsDocumentService.prepareForm(chassisNo);
        CustomsProgress status = customsProgressService.progressFor(chassisNo);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_CUSTOMS, FlowStage.BILL_OF_LADING.getStageKey()));
        model.addAttribute("activeMenu", "customs");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        addPipelineFlags(model, status);
        if (hub) {
            model.addAttribute("stageNav", CustomsStageUrls.editLinks(
                    chassisNo,
                    pipelineStageService.indexOf(PipelineStageService.FLOW_CUSTOMS, FlowStage.BILL_OF_LADING.getStageKey()),
                    status,
                    pipelineStageService.list(PipelineStageService.FLOW_CUSTOMS)
            ));
        }
        return "customs-pipeline/bl/form";
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
        return ocrService.parsePage(file, this.billOfLading, provider);
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
            customsDocumentService.saveBillOfLading(record);
            customsProgressService.syncVehicleStage(chassisNo);
            CustomsProgress status = customsProgressService.progressFor(chassisNo);
            if (status.isPipelineCompleted()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Customs clearance is complete. Continue with the preparation pipeline.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Bill of lading saved.");
            }
            return "redirect:" + CustomsStageUrls.redirectAfterBillOfLadingSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_CUSTOMS)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bl/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private void addPipelineFlags(Model model, CustomsProgress status) {
        model.addAttribute("blReady", status.isBlReady());
        model.addAttribute("declarationReady", status.isDeclarationReady());
        model.addAttribute("assessmentReady", status.isAssessmentReady());
        model.addAttribute("worksheetReady", status.isWorksheetReady());
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
