package com.carsale.erp.shared.vehicle;

import java.util.List;

import com.carsale.erp.customer.PersonalImportLinker;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.shared.service.IntakePlanService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class VehicleBasicsController {

    private final VehicleService vehicleService;
    private final VehiclePhotoService photoService;
    private final PersonalImportLinker personalImportLinker;
    private final IntakePlanService intakePlanService;
    private final ImportProgressService importProgressService;

    @GetMapping("/intake/start")
    public String start(
            @RequestParam(value = "intake", defaultValue = "company") String intake,
            @RequestParam(value = "importPipeline", defaultValue = "0") String importPipeline,
            @RequestParam(value = "customs", defaultValue = "0") String customs,
            @RequestParam(value = "prep", defaultValue = "0") String prep
    ) {
        intakePlanService.remember(intake, on(importPipeline), on(customs), on(prep));
        return "redirect:" + intakePlanService.startPath();
    }

    @GetMapping("/vehicle/basics/new")
    public String createForm(
            @RequestParam(value = "customer", required = false) Long customerId,
            @RequestParam(value = "intake", required = false) String intake,
            Model model
    ) {
        if (customerId != null) {
            personalImportLinker.remember(customerId);
        } else if (intake != null && !intake.trim().isEmpty()) {
            personalImportLinker.clear();
        }
        model.addAttribute("pageTitle", "Vehicle details");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("editing", false);
        model.addAttribute("basicsStage", true);
        model.addAttribute("personalCustomer", personalImportLinker.current());
        model.addAttribute("vehicle", new Vehicle());
        model.addAttribute("photos", java.util.Collections.emptyList());
        model.addAttribute("maxPhotos", VehiclePhotoService.MAX_PHOTOS);
        return "vehicle/basics";
    }

    @PostMapping("/vehicle/basics")
    public String create(
            @RequestParam("chassisNo") String chassisNo,
            @RequestParam(value = "model", required = false) String modelName,
            @RequestParam(value = "storedNames", required = false) List<String> storedNames,
            @RequestParam(value = "originalNames", required = false) List<String> originalNames,
            @RequestParam(value = "contentTypes", required = false) List<String> contentTypes,
            RedirectAttributes redirectAttributes
    ) {
        String trimmed = chassisNo == null ? "" : chassisNo.trim();
        if (trimmed.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chassis number is required.");
            return "redirect:/vehicle/basics/new";
        }
        if (vehicleService.existsByChassisNo(trimmed)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Chassis number " + trimmed + " is already on file.");
            return "redirect:/import/" + Utils.encodeChassis(trimmed);
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo(trimmed);
        vehicle.setModel(blankToNull(modelName));
        vehicleService.save(vehicle);
        if (storedNames != null && !storedNames.isEmpty()) {
            photoService.attachPending(trimmed, storedNames, originalNames, contentTypes);
        }
        intakePlanService.applyIfPending(trimmed);
        importProgressService.syncVehicleStage(trimmed);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle details saved. Skipped stages can be filled later.");
        return "redirect:" + intakePlanService.destinationAfterBasics(trimmed);
    }

    @GetMapping("/vehicle/basics/{chassisNo}")
    public String editForm(@PathVariable String chassisNo, Model model) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        model.addAttribute("pageTitle", "Vehicle details");
        model.addAttribute("activeMenu", "import");
        model.addAttribute("editing", true);
        model.addAttribute("basicsStage", IntakePlanService.usesBasics(vehicle));
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("photos", photoService.list(vehicle.getChassisNo()));
        model.addAttribute("maxPhotos", VehiclePhotoService.MAX_PHOTOS);
        PipelineStageUtils.addPipelineFlags(model, importProgressService.progressFor(vehicle));
        return "vehicle/basics";
    }

    @PostMapping("/vehicle/basics/{chassisNo}")
    public String update(
            @PathVariable String chassisNo,
            @RequestParam(value = "model", required = false) String modelName,
            @RequestParam(value = "storedNames", required = false) List<String> storedNames,
            @RequestParam(value = "originalNames", required = false) List<String> originalNames,
            @RequestParam(value = "contentTypes", required = false) List<String> contentTypes,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        vehicle.setModel(blankToNull(modelName));
        vehicleService.save(vehicle);
        if (storedNames != null && !storedNames.isEmpty()) {
            photoService.attachPending(vehicle.getChassisNo(), storedNames, originalNames, contentTypes);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle details saved.");
        return "redirect:/vehicle/basics/" + Utils.encodeChassis(vehicle.getChassisNo());
    }

    private static boolean on(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
