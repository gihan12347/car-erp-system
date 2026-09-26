package com.carsale.erp.importpipeline;

import static com.carsale.erp.shared.pipeline.PipelineStageService.shortTitleFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.carsale.erp.importpipeline.util.ImportStageUrls;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import org.junit.jupiter.api.Test;

import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.importpipeline.service.ImportProgressService.ImportProgress;

class ImportStageUrlsTest {

    @Test
    void newVehicleOpensFirstIncompleteStageInConfiguredOrder() {
        ImportProgress status = new ImportProgress(true, false, false, false, false, false, false, false, false);
        List<String> keys = Arrays.asList("equipment", "auction", "preshipment");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/equipment/ABC123?hub=1", url);
    }

    @Test
    void newVehicleWhenAuctionIsFirstGoesToNextIncompleteStage() {
        ImportProgress status = new ImportProgress(true, false, false, false, false, false, false, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/shipping/ABC123?hub=1", url);
    }

    @Test
    void newVehicleWithoutAuctionSheetStaysOnFirstConfiguredStage() {
        ImportProgress status = new ImportProgress(false, false, false, false, false, false, false, false, false);
        List<String> keys = Arrays.asList("auction", "equipment", "preshipment");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/auction/ABC123/edit?hub=1", url);
    }

    @Test
    void newVehicleWithoutAuctionSheetOpensEquipmentWhenThatStageIsFirst() {
        ImportProgress status = new ImportProgress(false, false, false, false, false, false, false, false, false);
        List<String> keys = Arrays.asList("equipment", "preshipment", "auction");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/equipment/ABC123?hub=1", url);
    }

    @Test
    void jevicStageOpensDedicatedImportForm() {
        ImportProgress status = new ImportProgress(true, true, true, false, false, false, false, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/jevic/ABC123?hub=1", url);
    }

    @Test
    void hubOpensJevicByStageKeyEvenWhenSortOrderDoesNotMatchIndex() {
        List<PipelineStage> stages = Arrays.asList(
                stage("auction", 0),
                stage("equipment", 2),
                stage("jevic", 9),
                stage("preshipment", 1)
        );
        assertEquals("jevic", PipelineStageUtils.stageKeyAt(stages, 2));
        assertEquals("jevic", ImportStageUrls.getStageKeyBySortOrder(stages, 2));
    }

    @Test
    void completeImportGoesToCustoms() {
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards");

        String url = ImportStageUrls.redirectAfterOdometerSave("ABC123", keys);

        assertEquals("/customs/ABC123", url);
    }

    @Test
    void coiStageOpensDedicatedImportForm() {
        ImportProgress status = new ImportProgress(true, true, true, true, false, false, false, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/coi/ABC123?hub=1", url);
    }

    @Test
    void completeImportAfterCoiGoesToCustoms() {
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards");

        String url = ImportStageUrls.redirectAfterCoiSave("ABC123", keys);

        assertEquals("/customs/ABC123", url);
    }

    @Test
    void standardsStageOpensDedicatedImportForm() {
        ImportProgress status = new ImportProgress(true, true, true, true, true, false, false, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/standards/ABC123?hub=1", url);
    }

    @Test
    void auctionEditOpensAuctionFormNotTheHub() {
        assertEquals("/auction/ABC123/edit?hub=1", ImportStageUrls.editUrlFor("ABC123", "auction"));
        assertEquals("/photos/ABC123?hub=1", ImportStageUrls.editUrlFor("ABC123", "photos"));
        assertEquals("/shipping/ABC123?hub=1", ImportStageUrls.editUrlFor("ABC123", "preshipment"));
    }

    @Test
    void shortTitlesStayCompactOnTheList() {
        assertEquals("Auction", shortTitleFor("auction"));
        assertEquals("Pre-ship", shortTitleFor("preshipment"));
        assertEquals("Equip", shortTitleFor("equipment"));
        assertEquals("JEVIC", shortTitleFor("jevic"));
        assertEquals("COI", shortTitleFor("coi"));
        assertEquals("Standards", shortTitleFor("standards"));
        assertEquals("Export", shortTitleFor("export"));
        assertEquals("Grade", shortTitleFor("grade"));
        assertEquals("Images", shortTitleFor("photos"));
    }

    @Test
    void completeImportAfterStandardsGoesToExport() {
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export");

        String url = ImportStageUrls.redirectAfterStandardsSave("ABC123", keys);

        assertEquals("/export/ABC123?hub=1", url);
    }

    @Test
    void exportStageOpensDedicatedImportForm() {
        ImportProgress status = new ImportProgress(true, true, true, true, true, true, false, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/export/ABC123?hub=1", url);
    }

    @Test
    void completeImportAfterExportGoesToPhotos() {
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "photos");

        String url = ImportStageUrls.redirectAfterExportSave("ABC123", keys);

        assertEquals("/photos/ABC123?hub=1", url);
    }

    @Test
    void photosStageOpensDedicatedImportForm() {
        ImportProgress status = new ImportProgress(true, true, true, true, true, true, true, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "photos");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/photos/ABC123?hub=1", url);
    }

    @Test
    void gradeEditOpensDedicatedForm() {
        assertEquals("/grade/ABC123?hub=1", ImportStageUrls.editUrlFor("ABC123", "grade"));
    }

    @Test
    void completeImportAfterPhotosGoesToCustoms() {
       List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "photos");

        String url = ImportStageUrls.redirectAfterPhotosSave("ABC123", keys);

        assertEquals("/customs/ABC123", url);
    }

    @Test
    void completeImportAfterExportGoesToGrade() {
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "grade", "photos");

        String url = ImportStageUrls.redirectAfterExportSave("ABC123", keys);

        assertEquals("/auction/ABC123?stage=grade", url);
    }

    @Test
    void gradeStageOpensDedicatedImportForm() {
        ImportProgress status = new ImportProgress(true, true, true, true, true, true, true, false, false);
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "grade", "photos");

        String url = ImportStageUrls.redirectAfterNewVehicle("ABC123", status, keys);

        assertEquals("/grade/ABC123?hub=1", url);
    }

    @Test
    void completeImportAfterGradeGoesToPhotos() {
        List<String> keys = Arrays.asList("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "grade", "photos");

        String url = ImportStageUrls.redirectAfterGradeSave("ABC123", keys);

        assertEquals("/auction/ABC123?stage=photos", url);
    }

    private static PipelineStage stage(String key, int sortOrder) {
        PipelineStage stage = new PipelineStage();
        stage.setStageKey(key);
        stage.setSortOrder(sortOrder);
        return stage;
    }
}
