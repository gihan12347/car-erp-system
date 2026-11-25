package com.carsale.erp.importpipeline.controller;

import com.carsale.erp.importpipeline.util.ImportStageSave;
import com.carsale.erp.shared.web.ListSearchRedirect;
import com.carsale.erp.shared.document.DocumentUploads;
import com.carsale.erp.shared.document.InlineDocumentResponse;
import java.io.IOException;

import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.importpipeline.util.ImportStageUrls;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.importpipeline.model.GradeSearch;
import com.carsale.erp.importpipeline.service.GradeSearchService;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.document.document.GradeSearchParser;
import com.carsale.erp.shared.ocr.OcrClient;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/grade")
public class GradeSearchController {

    private final GradeSearchService gradeSearchService;
    private final VehicleService vehicleService;
    private final SheetDocumentStorageService documentStorageService;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PipelineStageService pipelineStageService;
    private final GradeSearchParser parser;
    private final OcrImagePreparer imagePreparer;

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        return ListSearchRedirect.to("/import", query);
    }

    @GetMapping("/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadGradeSearchAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        return InlineDocumentResponse.of(resource, storedName, contentType);
    }

    @GetMapping("/new")
    public String newLot() {
        return "redirect:/auction/new";
    }

    @PostMapping("/new")
    public String saveNew(
            @ModelAttribute GradeSearch record,
            RedirectAttributes redirectAttributes
    ) {
        String chassisNo = record.getChassisNo() == null ? "" : record.getChassisNo().trim();
        if (chassisNo.isEmpty() && record.getChassisNumber() != null) {
            chassisNo = record.getChassisNumber().trim();
            record.setChassisNo(chassisNo);
        }
        final String resolvedChassis = chassisNo;
        String missingChassis = ImportStageSave.requireChassis(resolvedChassis, record, redirectAttributes);
        if (missingChassis != null) {
            return missingChassis;
        }
        return ImportStageSave.openExistingOrCreate(
                resolvedChassis,
                record,
                redirectAttributes,
                vehicleService,
                importProgressService,
                customsProgressService,
                pipelineStageService,
                null,
                vehicle -> {
                    if (record.getGrade() != null && !record.getGrade().trim().isEmpty()) {
                        vehicle.setGrade(record.getGrade().trim());
                    }
                },
                () -> {
                    record.setChassisNo(resolvedChassis);
                    gradeSearchService.save(record);
                },
                "Grade search saved.",
                keys -> ImportStageUrls.redirectAfterGradeSave(resolvedChassis, keys)
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
        GradeSearch record = gradeSearchService.prepareForm(chassisNo);
        ImportProgress status = importProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_IMPORT, FlowStage.GRADE.getStageKey()));
        model.addAttribute("activeMenu", "import");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        PipelineStageUtils.addPipelineFlags(model, status);
        model.addAttribute("stageNav", ImportStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, FlowStage.GRADE.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
        ));
        return "import-pipeline/grade/form";
    }

    @PostMapping("/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        return DocumentUploads.store(() -> documentStorageService.storeGradeSearch(file), "Could not save the document. ");
    }

    @PostMapping("/parse-document")
    @ResponseBody
    public AuctionParseResult parseDocument(@RequestParam("file") MultipartFile file) {
        return imagePreparer.parsePage(file, parser, OcrClient.GOOGLE);
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute GradeSearch record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        return ImportStageSave.saveAndSync(
                chassisNo,
                hub,
                "/grade/",
                redirectAttributes,
                () -> gradeSearchService.save(record),
                importProgressService,
                customsProgressService,
                "Grade search saved.",
                () -> ImportStageUrls.redirectAfterGradeSave(
                        chassisNo,
                        pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
                )
        );
    }
}
