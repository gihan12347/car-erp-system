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
import com.carsale.erp.entity.ClearanceDocument;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.service.ClearanceOcrService;
import com.carsale.erp.service.ClearancePipelineService;
import com.carsale.erp.service.ClearancePipelineService.ClearanceStatus;
import com.carsale.erp.service.ClearanceService;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.SheetDocumentStorageService;
import com.carsale.erp.service.VehicleService;

@Controller
@RequestMapping("/customs")
public class ClearanceController {

    private final ClearanceService clearanceService;
    private final VehicleService vehicleService;
    private final ClearanceOcrService ocrService;
    private final SheetDocumentStorageService documentStorageService;
    private final ClearancePipelineService clearancePipelineService;
    private final PipelineStageService pipelineStageService;

    public ClearanceController(
            ClearanceService clearanceService,
            VehicleService vehicleService,
            ClearanceOcrService ocrService,
            SheetDocumentStorageService documentStorageService,
            ClearancePipelineService clearancePipelineService,
            PipelineStageService pipelineStageService
    ) {
        this.clearanceService = clearanceService;
        this.vehicleService = vehicleService;
        this.ocrService = ocrService;
        this.documentStorageService = documentStorageService;
        this.clearancePipelineService = clearancePipelineService;
        this.pipelineStageService = pipelineStageService;
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

    @GetMapping("/{chassisNo}/edit")
    public String form(
            @PathVariable String chassisNo,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/customs";
        }
        if (!clearancePipelineService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Finish the import pipeline before the customs clearance pipeline.");
            return "redirect:" + clearancePipelineService.ineligibleHubPath(vehicle);
        }
        ClearanceDocument record = clearanceService.prepareForm(chassisNo);
        ClearanceStatus status = clearancePipelineService.statusFor(vehicle);
        model.addAttribute("pageTitle", "Customs clearance");
        model.addAttribute("activeMenu", "customs");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("jevicReady", status.isJevicReady());
        model.addAttribute("declarationReady", status.isDeclarationReady());
        model.addAttribute("assessmentReady", status.isAssessmentReady());
        model.addAttribute("clearanceComplete", status.isClearanceComplete());
        return "customs/form";
    }

    @PostMapping("/{chassisNo}/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(
            @PathVariable String chassisNo,
            @RequestParam("file") MultipartFile file,
            @RequestParam("page") int page
    ) {
        if (page < 1 || page > 3) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Invalid page number. Use page 1, 2, or 3.");
            return failed;
        }
        try {
            SheetUploadResult result = documentStorageService.storeClearance(file);
            if (result.isSuccess()) {
                clearanceService.savePageDocument(
                        chassisNo,
                        page,
                        result.getOriginalName(),
                        result.getStoredName(),
                        result.getContentType()
                );
                clearancePipelineService.syncVehicleStage(chassisNo);
            }
            return result;
        } catch (IOException | IllegalArgumentException ex) {
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
            @RequestParam("page") int page,
            @RequestParam(value = "provider", required = false) String provider
    ) {
        if (page < 1 || page > 3) {
            AuctionParseResult failed = new AuctionParseResult();
            failed.setSuccess(false);
            failed.setMessage("Invalid page number. Use page 1, 2, or 3.");
            return failed;
        }
        return ocrService.parsePage(file, page, provider);
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute ClearanceDocument record,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            clearanceService.save(record);
            clearancePipelineService.syncVehicleStage(chassisNo);
            ClearanceStatus status = clearancePipelineService.statusFor(chassisNo);
            if (status.isClearanceComplete()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Customs clearance is complete. Continue with the preparation pipeline.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Clearance documents saved.");
            }
            return "redirect:" + ClearanceStageNavigation.redirectAfterClearanceSave(
                    chassisNo,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_CUSTOMS)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customs/" + encodeChassis(chassisNo) + "/edit";
        }
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
