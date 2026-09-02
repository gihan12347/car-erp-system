package com.carsale.erp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.PipelineFlow;
import com.carsale.erp.helperClass.FlowGroup;
import com.carsale.erp.repository.PipelineFlowRepository;

@SpringBootTest
class PipelineStageServiceTest {

    @Autowired
    private PipelineStageService pipelineStageService;

    @Autowired
    private PipelineFlowRepository pipelineFlowRepository;

    @Test
    void flowGroupsAreLoadedFromPipelineFlowsTable() {
        List<FlowGroup> groups = pipelineStageService.flowGroups();
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
    void importPipelineHasAuctionPreshipmentAndEquipmentStages() {
        assertThat(pipelineStageService.keys("IMPORT"))
                .containsExactlyInAnyOrder("auction", "preshipment", "equipment");
    }

    @Test
    void prepPipelineStartsWithInspectionThenWorkshopAndYard() {
        assertThat(pipelineStageService.keys("PREP"))
                .containsExactly("inspection", "workshop", "yard");
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
}
