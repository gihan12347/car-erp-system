package com.carsale.erp.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.service.ImportPipelineService;
import com.carsale.erp.service.ImportPipelineService.ImportStatus;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.VehicleService;

@Controller
public class ImportPipelineController {

    private final VehicleService vehicleService;
    private final ImportPipelineService importPipelineService;

    public ImportPipelineController(VehicleService vehicleService, ImportPipelineService importPipelineService) {
        this.vehicleService = vehicleService;
        this.importPipelineService = importPipelineService;
    }

    @GetMapping("/import")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = vehicleService.search(query);
        List<PipelineRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            ImportStatus status = importPipelineService.statusFor(vehicle);
            rows.add(new PipelineRow(vehicle, status));
        }
        model.addAttribute("pageTitle", "Import pipeline");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("rows", rows);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "import/list";
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

    public static final class PipelineRow {
        private final Vehicle vehicle;
        private final ImportStatus status;

        public PipelineRow(Vehicle vehicle, ImportStatus status) {
            this.vehicle = vehicle;
            this.status = status;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public ImportStatus getStatus() {
            return status;
        }
    }
}
