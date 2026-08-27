package com.carsale.erp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.carsale.erp.service.ImportPipelineService.ImportStatus;

class ImportStageNavigationTest {

    @Test
    void newVehicleOpensFirstIncompleteStageInConfiguredOrder() {
        ImportStatus status = new ImportStatus(true, false, false);
        List<String> keys = Arrays.asList("equipment", "auction", "preshipment");

        String url = ImportStageNavigation.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/equipment/ABC123?hub=1", url);
    }

    @Test
    void newVehicleWhenAuctionIsFirstGoesToNextIncompleteStage() {
        ImportStatus status = new ImportStatus(true, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment");

        String url = ImportStageNavigation.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/shipping/ABC123?hub=1", url);
    }

    @Test
    void newVehicleWithoutAuctionSheetStaysOnFirstConfiguredStage() {
        ImportStatus status = new ImportStatus(false, false, false);
        List<String> keys = Arrays.asList("auction", "equipment", "preshipment");

        String url = ImportStageNavigation.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/auction/ABC123/edit?hub=1", url);
    }

    @Test
    void newVehicleWithoutAuctionSheetOpensEquipmentWhenThatStageIsFirst() {
        ImportStatus status = new ImportStatus(false, false, false);
        List<String> keys = Arrays.asList("equipment", "preshipment", "auction");

        String url = ImportStageNavigation.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/equipment/ABC123?hub=1", url);
    }
}
