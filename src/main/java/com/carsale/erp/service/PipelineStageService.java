package com.carsale.erp.service;

import java.util.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.PipelineFlow;
import com.carsale.erp.entity.PipelineStage;
import com.carsale.erp.enums.FlowPipeline;
import com.carsale.erp.helperClass.FlowGroup;
import com.carsale.erp.repository.PipelineFlowRepository;
import com.carsale.erp.repository.PipelineStageRepository;

@Service
@Order(3)
public class PipelineStageService implements CommandLineRunner {

    public static final String FLOW_IMPORT = FlowPipeline.IMPORT.getFlowKey();
    public static final String FLOW_CUSTOMS = FlowPipeline.CUSTOMS.getFlowKey();
    public static final String FLOW_PREP = FlowPipeline.PREP.getFlowKey();
    public static final String FLOW_READY = FlowPipeline.READY.getFlowKey();

    public static final String STAGE_AUCTION = "auction";
    public static final String STAGE_PRESHIP = "preshipment";
    public static final String STAGE_EQUIPMENT = "equipment";
    public static final String STAGE_JEVIC = "jevic";
    public static final String STAGE_DECLARATION = "declaration";
    public static final String STAGE_ASSESSMENT = "assessment";
    public static final String STAGE_WORKSHOP = "workshop";
    public static final String STAGE_INSPECTION = "inspection";
    public static final String STAGE_YARD = "yard";
    public static final String STAGE_LISTING = "listing";

    private final PipelineFlowRepository pipelineFlowRepository;
    private final PipelineStageRepository pipelineStageRepository;

    public PipelineStageService(PipelineFlowRepository pipelineFlowRepository,
                                PipelineStageRepository pipelineStageRepository) {
        this.pipelineFlowRepository = pipelineFlowRepository;
        this.pipelineStageRepository = pipelineStageRepository;
    }

    @Override
    public void run(String... args) {
        ensureDefaults();
    }

    @Transactional
    public void ensureDefaults() {
        seedIfMissing(FLOW_IMPORT, defaultImport());
        seedIfMissing(FLOW_CUSTOMS, defaultCustoms());
        seedIfMissing(FLOW_PREP, defaultPrep());
        seedIfMissing(FLOW_READY, defaultReady());
        placePrepInspectionFirst();
    }

    @Transactional
    public void placePrepInspectionFirst() {
        PipelineFlow flow = pipelineFlowRepository.findByFlowKey(FLOW_PREP).orElse(null);
        if (flow == null) {
            return;
        }
        PipelineFlow joined = pipelineFlowRepository.findByIdWithStages(flow.getId()).orElse(null);
        if (joined == null || joined.getStages().isEmpty()) {
            return;
        }
        List<PipelineStage> stages = new ArrayList<PipelineStage>(joined.getStages());
        stages.sort(new Comparator<PipelineStage>() {
            @Override
            public int compare(PipelineStage left, PipelineStage right) {
                return Integer.compare(left.getSortOrder(), right.getSortOrder());
            }
        });
        PipelineStage inspection = null;
        for (PipelineStage stage : stages) {
            if (STAGE_INSPECTION.equals(stage.getStageKey())) {
                inspection = stage;
                break;
            }
        }
        if (inspection == null) {
            return;
        }
        if (STAGE_INSPECTION.equals(stages.get(0).getStageKey())) {
            return;
        }
        stages.remove(inspection);
        inspection.setSortOrder(0);
        pipelineStageRepository.save(inspection);
        for (int i = 0; i < stages.size(); i++) {
            stages.get(i).setSortOrder(i + 1);
            pipelineStageRepository.save(stages.get(i));
        }
    }

    public List<PipelineStage> list(String flowKey) {
        Optional<PipelineFlow> flow = pipelineFlowRepository.findByFlowKey(flowKey);
        if (flow.isPresent()) {
            return pipelineStageRepository.findByFlowIdOrdered(flow.get().getId());
        }
        return new ArrayList<PipelineStage>();
    }

    public List<String> keys(String flowKey) {
        List<String> keys = new ArrayList<>();
        for (PipelineStage stage : list(flowKey)) {
            keys.add(stage.getStageKey());
        }
        return keys;
    }

    public int indexOf(String flowKey, String stageKey) {
        List<String> ordered = keys(flowKey);
        int index = ordered.indexOf(stageKey);
        return Math.max(index, 0);
    }

    public String keyAt(String flowKey, int index) {
        List<String> ordered = keys(flowKey);
        if (ordered.isEmpty()) {
            return null;
        }
        if (index < 0) {
            return ordered.get(0);
        }
        if (index >= ordered.size()) {
            return ordered.get(ordered.size() - 1);
        }
        return ordered.get(index);
    }

    public int size(String flowKey) {
        return keys(flowKey).size();
    }

    public int displayNumber(String flowKey, String stageKey) {
        return indexOf(flowKey, stageKey) + 1;
    }

    public String title(String flowKey, String stageKey) {
        PipelineStage stage = pipelineStageRepository
                .findByFlowIdAndStageKey(requireFlowByKey(flowKey).getId(), stageKey)
                .orElse(null);
        if (stage != null) {
            return stage.getTitle();
        }
        return stageKey;
    }

    public List<FlowGroup> flowGroups() {
        List<FlowGroup> groups = new ArrayList<>();
        List<PipelineFlow> flows = pipelineFlowRepository.findAllOrdered();
        for (PipelineFlow flow : flows) {
            groups.add(new FlowGroup(
                    flow.getId(),
                    flow.getTitle(),
                    flow.getDescription(),
                    flow.getIcon(),
                    pipelineStageRepository.findByFlowIdOrdered(flow.getId())
            ));
        }
        return groups;
    }

    @Transactional
    public List<PipelineStage> reorder(Long flowId, List<String> stageKeys) {
        PipelineFlow flow = requireFlowById(flowId);
        List<String> expected = getStrings(flow);
        Set<String> incoming = new HashSet<>(stageKeys);
        Set<String> known = new HashSet<>(expected);
        if (!incoming.equals(known)) {
            throw new IllegalArgumentException("Stage list does not match this pipeline.");
        }
        for (int i = 0; i < stageKeys.size(); i++) {
            PipelineStage stage = pipelineStageRepository
                    .findByFlowIdAndStageKey(flow.getId(), stageKeys.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown stage."));
            stage.setSortOrder(i);
            pipelineStageRepository.save(stage);
        }
        return pipelineStageRepository.findByFlowIdOrdered(flow.getId());
    }

    private static List<String> getStrings(PipelineFlow flow) {
        List<PipelineStage> existing = flow.getStages();
        List<String> expected = new ArrayList<>();
        for (PipelineStage pipelineStage : existing) {
            expected.add(pipelineStage.getStageKey());
        }
        return expected;
    }

    @Transactional
    public List<PipelineStage> reset(Long flowId) {
        PipelineFlow flow = requireFlowById(flowId);
        List<DefaultStage> defaults = defaultsFor(flow.getFlowKey());
        for (int i = 0; i < defaults.size(); i++) {
            DefaultStage def = defaults.get(i);
            PipelineStage stage = pipelineStageRepository
                    .findByFlowIdAndStageKey(flow.getId(), def.stageKey)
                    .orElse(null);
            if (stage == null) {
                continue;
            }
            stage.setSortOrder(i);
            stage.setTitle(def.title);
            stage.setSubtitle(def.subtitle);
            pipelineStageRepository.save(stage);
        }
        return pipelineStageRepository.findByFlowIdOrdered(flow.getId());
    }

    //TODO:: need to check
    private void seedIfMissing(String flowKey, List<DefaultStage> defaults) {
        PipelineFlow flow = requireFlowByKey(flowKey);
        if (pipelineStageRepository.countByFlowId(flow.getId()) == 0) {
            for (int i = 0; i < defaults.size(); i++) {
                DefaultStage def = defaults.get(i);
                saveStage(flow, def, i);
            }
            return;
        }
        int nextOrder = 0;
        for (PipelineStage existing : pipelineStageRepository.findByFlowIdOrdered(flow.getId())) {
            nextOrder = Math.max(nextOrder, existing.getSortOrder() + 1);
        }
        for (int i = 0; i < defaults.size(); i++) {
            DefaultStage def = defaults.get(i);
            if (pipelineStageRepository.findByFlowIdAndStageKey(flow.getId(), def.stageKey).isPresent()) {
                continue;
            }
            saveStage(flow, def, nextOrder);
            nextOrder++;
        }
    }

    private void saveStage(PipelineFlow flow, DefaultStage def, int sortOrder) {
        PipelineStage stage = new PipelineStage();
        stage.setStageKey(def.stageKey);
        stage.setTitle(def.title);
        stage.setSubtitle(def.subtitle);
        stage.setSortOrder(sortOrder);
        stage.setFlow(flow);
        pipelineStageRepository.save(stage);
    }

    private PipelineFlow requireFlowByKey(String flowKey) {
        if (flowKey == null || flowKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Pipeline is required.");
        }
        return pipelineFlowRepository.findByFlowKey(flowKey.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown pipeline."));
    }

    private PipelineFlow requireFlowById(Long flowId) {
        if (flowId == null) {
            throw new IllegalArgumentException("Pipeline is required.");
        }
        return pipelineFlowRepository.findByIdWithStages(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown pipeline."));
    }

    private static List<DefaultStage> defaultsFor(String flowKey) {
        if (FLOW_IMPORT.equals(flowKey)) {
            return defaultImport();
        }
        if (FLOW_CUSTOMS.equals(flowKey)) {
            return defaultCustoms();
        }
        if (FLOW_PREP.equals(flowKey)) {
            return defaultPrep();
        }
        if (FLOW_READY.equals(flowKey)) {
            return defaultReady();
        }
        return Collections.emptyList();
    }

    private static List<DefaultStage> defaultImport() {
        return Arrays.asList(
                new DefaultStage(STAGE_AUCTION, "Auction lot", "Japanese auction sheet"),
                new DefaultStage(STAGE_PRESHIP, "Pre-shipment", "BV inspection certificate"),
                new DefaultStage(STAGE_EQUIPMENT, "Equipment condition", "Interior, exterior, and safety")
        );
    }

    private static List<DefaultStage> defaultCustoms() {
        return Arrays.asList(
                new DefaultStage(STAGE_JEVIC, "JEVIC certificate", "Odometer certificate"),
                new DefaultStage(STAGE_DECLARATION, "Customs declaration", "Sri Lanka CUSDEC"),
                new DefaultStage(STAGE_ASSESSMENT, "Assessment notice", "ASYCUDA assessment")
        );
    }

    private static List<DefaultStage> defaultPrep() {
        return Arrays.asList(
                new DefaultStage(STAGE_INSPECTION, "Inspection", "Checklist items and results"),
                new DefaultStage(STAGE_WORKSHOP, "Workshop", "Repairs, parts, completion"),
                new DefaultStage(STAGE_YARD, "Yard", "Bay, keys, inspection")
        );
    }

    private static List<DefaultStage> defaultReady() {
        return Collections.singletonList(
                new DefaultStage(STAGE_LISTING, "Sale listing", "Price and list the vehicle")
        );
    }

    public static class DefaultStage {
        public final String stageKey;
        public final String title;
        public final String subtitle;

        public DefaultStage(String stageKey, String title, String subtitle) {
            this.stageKey = stageKey;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

}
