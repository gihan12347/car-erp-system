package com.carsale.erp.importpipeline.controller;

import com.carsale.erp.importpipeline.util.ImportStageSave;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.web.ListSearchRedirect;
import com.carsale.erp.shared.document.DocumentUploads;
import com.carsale.erp.shared.document.InlineDocumentResponse;
import com.carsale.erp.importpipeline.util.ImportStageUrls;
import java.io.IOException;

import com.carsale.erp.importpipeline.model.PreShipmentInspection;
import com.carsale.erp.importpipeline.service.PreShipmentService;
import com.carsale.erp.shared.document.document.PreShipmentParser;
import com.carsale.erp.shared.ocr.OcrClient;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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


import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shipping")
public class PreShipmentCertificateController {

    private final PreShipmentService preShipmentService;
    private final VehicleService vehicleService;
    private final SheetDocumentStorageService documentStorageService;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PipelineStageService pipelineStageService;
    private final PreShipmentParser parser;
    private final OcrImagePreparer imagePreparer;

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        return ListSearchRedirect.to("/import", query);
    }

    @GetMapping("/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadPreShipmentAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        return InlineDocumentResponse.of(resource, storedName, contentType);
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
        String missingChassis = ImportStageSave.requireChassis(chassisNo, record, redirectAttributes);
        if (missingChassis != null) {
            return missingChassis;
        }
        return ImportStageSave.openExistingOrCreate(
                chassisNo,
                record,
                redirectAttributes,
                vehicleService,
                importProgressService,
                customsProgressService,
                pipelineStageService,
                null,
                null,
                () -> {
                    record.setChassisNo(chassisNo);
                    preShipmentService.save(record);
                },
                "Pre-shipment certificate saved.",
                keys -> ImportStageUrls.redirectAfterPreshipSave(chassisNo, keys)
        );
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
        ImportProgress status = importProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", "Pre-shipment certificate");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        PipelineStageUtils.addPipelineFlags(model, status);
        model.addAttribute("stageNav", ImportStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, FlowStage.PRESHIP.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
        ));
        return "import-pipeline/preshipment/form";
    }

    @PostMapping("/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        return DocumentUploads.store(() -> documentStorageService.storePreShipment(file), "Could not save the document. ");
    }

    @PostMapping("/parse-document")
    @ResponseBody
    public AuctionParseResult parseDocument(@RequestParam("file") MultipartFile file) {
        return imagePreparer.parsePage(file, parser, OcrClient.GOOGLE);
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute PreShipmentInspection record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        return ImportStageSave.saveAndSync(
                chassisNo,
                hub,
                "/shipping/",
                redirectAttributes,
                () -> preShipmentService.save(record),
                importProgressService,
                customsProgressService,
                "Pre-shipment certificate saved.",
                () -> ImportStageUrls.redirectAfterPreshipSave(
                        chassisNo,
                        pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
                )
        );
    }
}
