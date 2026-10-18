package com.carsale.erp.importpipeline.controller;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.customspipeline.service.CustomsDocumentService;
import com.carsale.erp.importpipeline.service.CertificateOfInspectionService;
import com.carsale.erp.importpipeline.service.EquipmentInspectionService;
import com.carsale.erp.importpipeline.service.ExportCertificateService;
import com.carsale.erp.importpipeline.service.GradeSearchService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.importpipeline.service.PreShipmentService;
import com.carsale.erp.importpipeline.service.StandardsCertificateService;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.importpipeline.util.EquipmentInspectionFields;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.shared.model.PipelineStage;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequiredArgsConstructor
public class ImportPipelineListController {

    private final VehicleService vehicleService;
    private final ImportProgressService importProgressService;
    private final PipelineStageService pipelineStageService;
    private final PreShipmentService preShipmentService;
    private final EquipmentInspectionService equipmentInspectionService;
    private final CustomsDocumentService customsDocumentService;
    private final CertificateOfInspectionService certificateOfInspectionService;
    private final StandardsCertificateService standardsCertificateService;
    private final ExportCertificateService exportCertificateService;
    private final GradeSearchService gradeSearchService;
    private final VehiclePhotoService vehiclePhotoService;

    @GetMapping("/import")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = vehicleService.search(query);
        List<ImportListRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getIntake() == VehicleIntake.LOCAL_REGISTERED) {
                continue;
            }
            ImportProgress status = importProgressService.progressFor(vehicle);
            rows.add(new ImportListRow(vehicle, status));
        }
        model.addAttribute("pageTitle", "Import pipeline");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("rows", rows);
        model.addAttribute("importStages", pipelineStageService.list(PipelineStageService.FLOW_IMPORT));
        model.addAttribute("importKeys", pipelineStageService.keys(PipelineStageService.FLOW_IMPORT));
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "import-pipeline/list";
    }

    @PostMapping("/import/{chassisNo}/skip")
    public String skip(
            @PathVariable String chassisNo,
            @RequestParam("stage") String stage,
            RedirectAttributes redirectAttributes
    ) {
        try {
            importProgressService.skipStage(chassisNo, stage);
            redirectAttributes.addFlashAttribute("successMessage", "Stage skipped.");
            return "redirect:" + PipelineStageUtils.redirectAfterStageSave(
                    chassisNo,
                    FlowPipeline.IMPORT.getCurrentBase(),
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT),
                    stage.trim()
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            String editUrl = PipelineStageService.editUrlFor(
                    PipelineStageUtils.encode(chassisNo),
                    stage
            );
            return "redirect:" + (editUrl != null ? editUrl : "/import");
        }
    }

    @PostMapping("/import/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (vehicleService.deleteFromFlow(chassisNo, PipelineStageService.FLOW_IMPORT)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Import data deleted. Later pipeline data was also removed.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete import data.");
        }
        return "redirect:/import";
    }

    @GetMapping("/import/{chassisNo}")
    public String view(
            @PathVariable String chassisNo,
            @RequestParam(value = "stage", required = false) String stage,
            Model model
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        ImportProgress importStatus = importProgressService.progressFor(vehicle);
        List<String> keys = pipelineStageService.keys(PipelineStageService.FLOW_IMPORT);
        int stageIndex = PipelineStageUtils.clampStageIndex(
                PipelineStageUtils.parseStageIndex(keys, stage),
                importStatus,
                keys
        );

        List<PipelineStage> importKeys = pipelineStageService.list(PipelineStageService.FLOW_IMPORT);
        String stageKey = PipelineStageUtils.stageKeyAt(importKeys, stageIndex);
        NavLinks nav = PipelineStageService.viewLinks(chassisNo, stageIndex, importStatus, importKeys, FlowPipeline.IMPORT);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "import");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("preShipmentReady", importStatus.isPreshipReady());
        model.addAttribute("preShipment", preShipmentService.findByChassisNo(chassisNo));
        model.addAttribute("equipmentReady", importStatus.isEquipmentReady());
        model.addAttribute("equipmentInspection", equipmentInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("jevicReady", importStatus.isJevicReady());
        model.addAttribute("coiReady", importStatus.isCoiReady());
        model.addAttribute("standardsReady", importStatus.isStandardsReady());
        model.addAttribute("exportReady", importStatus.isExportReady());
        model.addAttribute("gradeReady", importStatus.isGradeReady());
        model.addAttribute("photosReady", importStatus.isPhotosReady());
        model.addAttribute("worksheetReady", importStatus.isWorksheetReady());
        model.addAttribute("clearance", customsDocumentService.findByChassisNo(chassisNo));
        model.addAttribute("inspectionCertificate", certificateOfInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("standardsCertificate", standardsCertificateService.findByChassisNo(chassisNo));
        model.addAttribute("exportCertificate", exportCertificateService.findByChassisNo(chassisNo));
        model.addAttribute("gradeSearch", gradeSearchService.findByChassisNo(chassisNo));
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

    public static final class ImportListRow {
        private final Vehicle vehicle;
        private final ImportProgress status;

        public ImportListRow(Vehicle vehicle, ImportProgress status) {
            this.vehicle = vehicle;
            this.status = status;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public ImportProgress getStatus() {
            return status;
        }
    }
}
