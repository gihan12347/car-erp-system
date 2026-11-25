package com.carsale.erp.importpipeline.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@ExtendWith(MockitoExtension.class)
class ImportStageSaveTest {

    @Mock
    private VehicleService vehicleService;
    @Mock
    private ImportProgressService importProgressService;
    @Mock
    private CustomsProgressService customsProgressService;
    @Mock
    private PipelineStageService pipelineStageService;
    @Mock
    private RedirectAttributes redirectAttributes;

    @Test
    void asksForAChassisNumberBeforeCreatingAVehicle() {
        Object record = new Object();

        String view = ImportStageSave.requireChassis("  ".trim(), record, redirectAttributes);

        assertEquals(ImportStageSave.NEW_LOT, view);
        verify(redirectAttributes).addFlashAttribute("error", "Chassis number is required.");
        verify(redirectAttributes).addFlashAttribute("record", record);
    }

    @Test
    void opensTheExistingVehicleWithoutSavingAgain() {
        Vehicle existing = new Vehicle();
        existing.setChassisNo("CH-1");
        ImportProgress progress = new ImportProgress(false, false, false, false, false, false, false, false, false, false);
        when(vehicleService.findByChassisNo("CH-1")).thenReturn(existing);
        when(importProgressService.progressFor(existing)).thenReturn(progress);
        when(pipelineStageService.keys(PipelineStageService.FLOW_IMPORT))
                .thenReturn(Collections.singletonList(FlowStage.AUCTION.getStageKey()));

        String view = ImportStageSave.openExistingOrCreate(
                "CH-1",
                "record",
                redirectAttributes,
                vehicleService,
                importProgressService,
                customsProgressService,
                pipelineStageService,
                null,
                null,
                () -> {
                    throw new AssertionError("stage record must not be saved");
                },
                "Saved.",
                keys -> "/unused"
        );

        assertEquals(true, view.startsWith("redirect:"));
        verify(redirectAttributes).addFlashAttribute(
                "notice",
                "Chassis number CH-1 is already in the import pipeline. "
                        + "No new vehicle was created — opened the existing record instead."
        );
        verify(vehicleService, never()).save(any(Vehicle.class));
    }

    @Test
    void createsTheVehicleThenSavesTheStage() {
        when(vehicleService.findByChassisNo("CH-2")).thenReturn(null);
        boolean[] saved = new boolean[] {false};

        String view = ImportStageSave.openExistingOrCreate(
                "CH-2",
                "record",
                redirectAttributes,
                vehicleService,
                importProgressService,
                customsProgressService,
                pipelineStageService,
                null,
                vehicle -> vehicle.setMake("Toyota"),
                () -> saved[0] = true,
                "Export certificate saved.",
                keys -> "/next"
        );

        ArgumentCaptor<Vehicle> created = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleService).save(created.capture());
        assertEquals("CH-2", created.getValue().getChassisNo());
        assertEquals("Toyota", created.getValue().getMake());
        assertEquals(true, saved[0]);
        verify(importProgressService).syncVehicleStage("CH-2");
        verify(customsProgressService).syncVehicleStage("CH-2");
        verify(redirectAttributes).addFlashAttribute("successMessage", "Export certificate saved.");
        assertEquals("redirect:/next", view);
    }

    @Test
    void doesNotCreateAVehicleWhenPreparationFails() {
        when(vehicleService.findByChassisNo("CH-3")).thenReturn(null);

        String view = ImportStageSave.openExistingOrCreate(
                "CH-3",
                "record",
                redirectAttributes,
                vehicleService,
                importProgressService,
                customsProgressService,
                pipelineStageService,
                () -> {
                    throw new IllegalArgumentException("Upload at least one vehicle image.");
                },
                null,
                () -> {
                    throw new AssertionError("stage record must not be saved");
                },
                "Vehicle images saved.",
                keys -> "/unused"
        );

        assertEquals(ImportStageSave.NEW_LOT, view);
        verify(vehicleService, never()).save(any(Vehicle.class));
        verify(redirectAttributes).addFlashAttribute("error", "Upload at least one vehicle image.");
        verify(redirectAttributes).addFlashAttribute("record", "record");
    }

    @Test
    void returnsToTheEditFormWhenASaveIsRejected() {
        String view = ImportStageSave.saveAndSync(
                "CH 4",
                true,
                "/export/",
                redirectAttributes,
                () -> {
                    throw new IllegalArgumentException("Chassis number is required.");
                },
                importProgressService,
                customsProgressService,
                "Export certificate saved.",
                () -> "/unused"
        );

        assertEquals("redirect:/export/CH%204?hub=1", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Chassis number is required.");
        verify(importProgressService, never()).syncVehicleStage(any());
    }
}
