package com.carsale.erp.importpipeline;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.importpipeline.ImportProgressService;
import com.carsale.erp.importpipeline.ImportProgressService.ImportProgress;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.vehicle.VehicleService;

import static com.carsale.erp.shared.pipeline.PipelineStageService.shortTitleFor;

@Controller
public class ImportPipelineListController {

    private final VehicleService vehicleService;
    private final ImportProgressService importProgressService;
    private final PipelineStageService pipelineStageService;

    public ImportPipelineListController(
            VehicleService vehicleService,
            ImportProgressService importProgressService,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.importProgressService = importProgressService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/import")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = vehicleService.search(query);
        List<ImportListRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
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

        public String stageHref(String stageKey) {
            String encoded = UriUtils.encodePathSegment(vehicle.getChassisNo(), StandardCharsets.UTF_8);
            if (status.isStageComplete(stageKey)) {
                return "/auction/" + encoded + "?stage=" + stageKey;
            }
            return ImportStageUrls.editUrlFor(encoded, stageKey);
        }

        public String stageShortTitle(String stageKey) {
            return shortTitleFor(stageKey);
        }
    }
}
