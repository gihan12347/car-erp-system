package com.carsale.erp.shared.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.pipeline.PipelineFlow;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineFlowGroup;
import com.carsale.erp.shared.pipeline.PipelineFlowRepository;
import com.carsale.erp.shared.pipeline.PipelineStageRepository;

@SpringBootTest
class PipelineStageServiceTest {

    @Autowired
    private PipelineStageService pipelineStageService;

    @Autowired
    private PipelineFlowRepository pipelineFlowRepository;

    @Autowired
    private PipelineStageRepository pipelineStageRepository;

    @Test
    void flowGroupsAreLoadedFromPipelineFlowsTable() {
        List<PipelineFlowGroup> groups = pipelineStageService.flowGroups();
        assertThat(groups).hasSize(4);
        assertThat(groups.get(0).getId()).isNotNull();
        assertThat(groups.get(0).getTitle()).isEqualTo("Import pipeline");
        assertThat(pipelineFlowRepository.findByFlowKey("IMPORT")).isPresent();
        assertThat(pipelineFlowRepository.findByFlowKey("CUSTOMS")).isPresent();
        assertThat(pipelineFlowRepository.findByFlowKey("PREP")).isPresent();
        assertThat(pipelineFlowRepository.findByFlowKey("READY")).isPresent();
    }

    @Test
    @Transactional
    void reorderPersistsAgainstFlowId() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        List<String> original = pipelineStageService.keys("IMPORT");
        List<String> reversed = new ArrayList<String>(original);
        Collections.reverse(reversed);

        pipelineStageService.reorder(importFlow.getId(), reversed);

        assertThat(pipelineStageService.keys("IMPORT")).containsExactlyElementsOf(reversed);
        assertThat(
                pipelineStageService.flowGroups().stream()
                        .filter(group -> importFlow.getId().equals(group.getId()))
                        .findFirst()
                        .orElseThrow(IllegalStateException::new)
                        .getStages()
                        .stream()
                        .map(stage -> stage.getStageKey())
                        .collect(Collectors.toList())
        ).containsExactlyElementsOf(reversed);
    }

    @Test
    @Transactional
    void stagesJoinToPipelineFlowById() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(importFlow.getId())
                .orElseThrow(IllegalStateException::new);

        assertThat(joined.getId()).isEqualTo(importFlow.getId());
        assertThat(joined.getStages()).isNotEmpty();
        for (int i = 0; i < joined.getStages().size(); i++) {
            assertThat(joined.getStages().get(i).getFlow().getId()).isEqualTo(importFlow.getId());
        }
    }

    @Test
    void importPipelineHasAuctionPreshipmentEquipmentJevicCoiStandardsExportAndPhotosStages() {
        assertThat(pipelineStageService.keys("IMPORT"))
                .containsExactlyInAnyOrder("auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "photos");
    }

    @Test
    void customsPipelineHasDeclarationAssessmentAndWorksheetStages() {
        assertThat(pipelineStageService.keys("CUSTOMS"))
                .containsExactlyInAnyOrder("declaration", "assessment", "worksheet");
    }

    @Test
    void prepPipelineStartsWithInspectionThenWorkshopAndYard() {
        assertThat(pipelineStageService.keys("PREP"))
                .containsExactly("inspection", "workshop", "yard");
    }

    @Test
    @Transactional
    void ensureDefaultsAddsMissingCoiStage() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(importFlow.getId())
                .orElseThrow(IllegalStateException::new);
        joined.getStages().removeIf(stage -> "coi".equals(stage.getStageKey()));
        pipelineFlowRepository.saveAndFlush(joined);

        assertThat(pipelineStageService.keys("IMPORT")).doesNotContain("coi");

        pipelineStageService.ensureDefaults();

        assertThat(pipelineStageService.keys("IMPORT")).contains("coi");
    }

    @Test
    @Transactional
    void ensureDefaultsAddsMissingStandardsStage() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(importFlow.getId())
                .orElseThrow(IllegalStateException::new);
        joined.getStages().removeIf(stage -> "standards".equals(stage.getStageKey()));
        pipelineFlowRepository.saveAndFlush(joined);

        assertThat(pipelineStageService.keys("IMPORT")).doesNotContain("standards");

        pipelineStageService.ensureDefaults();

        assertThat(pipelineStageService.keys("IMPORT")).contains("standards");
    }

    @Test
    @Transactional
    void ensureDefaultsAddsMissingExportStage() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(importFlow.getId())
                .orElseThrow(IllegalStateException::new);
        joined.getStages().removeIf(stage -> "export".equals(stage.getStageKey()));
        pipelineFlowRepository.saveAndFlush(joined);

        assertThat(pipelineStageService.keys("IMPORT")).doesNotContain("export");

        pipelineStageService.ensureDefaults();

        assertThat(pipelineStageService.keys("IMPORT")).contains("export");
    }

    @Test
    @Transactional
    void ensureDefaultsAddsMissingPhotosStage() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(importFlow.getId())
                .orElseThrow(IllegalStateException::new);
        joined.getStages().removeIf(stage -> "photos".equals(stage.getStageKey()));
        pipelineFlowRepository.saveAndFlush(joined);

        assertThat(pipelineStageService.keys("IMPORT")).doesNotContain("photos");

        pipelineStageService.ensureDefaults();

        assertThat(pipelineStageService.keys("IMPORT")).contains("photos");
    }

    @Test
    @Transactional
    void ensureDefaultsAddsMissingWorksheetStage() {
        PipelineFlow customsFlow = pipelineFlowRepository.findByFlowKey("CUSTOMS")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(customsFlow.getId())
                .orElseThrow(IllegalStateException::new);
        joined.getStages().removeIf(stage -> "worksheet".equals(stage.getStageKey()));
        pipelineFlowRepository.saveAndFlush(joined);

        assertThat(pipelineStageService.keys("CUSTOMS")).doesNotContain("worksheet");

        pipelineStageService.ensureDefaults();

        assertThat(pipelineStageService.keys("CUSTOMS")).contains("worksheet");
    }

    @Test
    @Transactional
    void ensureDefaultsAddsMissingEquipmentStage() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(importFlow.getId())
                .orElseThrow(IllegalStateException::new);
        joined.getStages().removeIf(stage -> "equipment".equals(stage.getStageKey()));
        pipelineFlowRepository.saveAndFlush(joined);

        assertThat(pipelineStageService.keys("IMPORT")).doesNotContain("equipment");

        pipelineStageService.ensureDefaults();

        assertThat(pipelineStageService.keys("IMPORT")).contains("equipment");
    }

    @Test
    @Transactional
    void ensureDefaultsMovesJevicFromCustomsToImport() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey("IMPORT")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow customsFlow = pipelineFlowRepository.findByFlowKey("CUSTOMS")
                .orElseThrow(IllegalStateException::new);
        PipelineFlow importJoined = pipelineFlowRepository.findByIdWithStages(importFlow.getId())
                .orElseThrow(IllegalStateException::new);
        importJoined.getStages().removeIf(stage -> "jevic".equals(stage.getStageKey()));
        pipelineFlowRepository.saveAndFlush(importJoined);

        PipelineStage customsJevic = new PipelineStage();
        customsJevic.setStageKey("jevic");
        customsJevic.setTitle("JEVIC certificate");
        customsJevic.setSubtitle("Odometer certificate");
        customsJevic.setSortOrder(99);
        customsJevic.setFlow(customsFlow);
        pipelineStageRepository.saveAndFlush(customsJevic);

        assertThat(pipelineStageService.keys("IMPORT")).doesNotContain("jevic");
        assertThat(pipelineStageService.keys("CUSTOMS")).contains("jevic");

        pipelineStageService.ensureDefaults();

        assertThat(pipelineStageService.keys("IMPORT")).contains("jevic");
        assertThat(pipelineStageService.keys("CUSTOMS")).doesNotContain("jevic");
    }
}
