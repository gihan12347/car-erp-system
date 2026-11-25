package com.carsale.erp.importpipeline.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

/**
 * Shared redirect flow for import-stage forms.
 * Callers still decide the chassis value, the saved record, and the success copy.
 */
public final class ImportStageSave {

    public static final String NEW_LOT = "redirect:/auction/new";

    private ImportStageSave() {
    }

    public static String requireChassis(String chassisNo, Object record, RedirectAttributes redirectAttributes) {
        if (chassisNo == null || chassisNo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Chassis number is required.");
            if (record != null) {
                redirectAttributes.addFlashAttribute("record", record);
            }
            return NEW_LOT;
        }
        return null;
    }

    public static String openExistingOrCreate(
            String chassisNo,
            Object record,
            RedirectAttributes redirectAttributes,
            VehicleService vehicleService,
            ImportProgressService importProgressService,
            CustomsProgressService customsProgressService,
            PipelineStageService pipelineStageService,
            Runnable beforeCreate,
            Consumer<Vehicle> customizeNewVehicle,
            Runnable saveStageRecord,
            String successMessage,
            Function<List<String>, String> redirectAfterSave
    ) {
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
            if (beforeCreate != null) {
                beforeCreate.run();
            }
            Vehicle vehicle = new Vehicle();
            vehicle.setChassisNo(chassisNo);
            if (customizeNewVehicle != null) {
                customizeNewVehicle.accept(vehicle);
            }
            vehicleService.save(vehicle);
            saveStageRecord.run();
            importProgressService.syncVehicleStage(chassisNo);
            customsProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:" + redirectAfterSave.apply(
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            if (record != null) {
                redirectAttributes.addFlashAttribute("record", record);
            }
            return NEW_LOT;
        }
    }

    public static String saveAndSync(
            String chassisNo,
            boolean hub,
            String editPath,
            RedirectAttributes redirectAttributes,
            Runnable save,
            ImportProgressService importProgressService,
            CustomsProgressService customsProgressService,
            String successMessage,
            Supplier<String> redirectAfterSave
    ) {
        try {
            save.run();
            importProgressService.syncVehicleStage(chassisNo);
            customsProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:" + redirectAfterSave.get();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + editPath + Utils.encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }
}
