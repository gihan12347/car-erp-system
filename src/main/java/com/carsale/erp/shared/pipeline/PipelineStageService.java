package com.carsale.erp.shared.pipeline;

import java.util.*;

import com.carsale.erp.shared.utils.PipelineStageUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

@Service
@Order(3)
public class PipelineStageService implements CommandLineRunner {

    public static final String FLOW_IMPORT = FlowPipeline.IMPORT.getFlowKey();
    public static final String FLOW_CUSTOMS = FlowPipeline.CUSTOMS.getFlowKey();
    public static final String FLOW_PREP = FlowPipeline.PREP.getFlowKey();
    public static final String FLOW_READY = FlowPipeline.READY.getFlowKey();

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
        moveJevicToImport();
        seedIfMissing(FLOW_IMPORT, defaultImport());
        seedIfMissing(FLOW_CUSTOMS, defaultCustoms());
        seedIfMissing(FLOW_PREP, defaultPrep());
        seedIfMissing(FLOW_READY, defaultReady());
        refreshImportDocumentCopy();
        placePrepInspectionFirst();
    }

    @Transactional
    public void moveJevicToImport() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey(FLOW_IMPORT).orElse(null);
        PipelineFlow customsFlow = pipelineFlowRepository.findByFlowKey(FLOW_CUSTOMS).orElse(null);
        if (importFlow == null || customsFlow == null) {
            return;
        }
        PipelineStage customsJevic = pipelineStageRepository
                .findByFlowIdAndStageKey(customsFlow.getId(), FlowStage.JEVIC.getStageKey())
                .orElse(null);
        if (customsJevic == null) {
            return;
        }
        PipelineStage importJevic = pipelineStageRepository
                .findByFlowIdAndStageKey(importFlow.getId(), FlowStage.JEVIC.getStageKey())
                .orElse(null);
        if (importJevic != null) {
            pipelineStageRepository.delete(customsJevic);
            pipelineStageRepository.flush();
            refreshJevicCopy(importJevic);
            reindex(importFlow.getId());
            reindex(customsFlow.getId());
            return;
        }
        int nextOrder = 0;
        for (PipelineStage existing : pipelineStageRepository.findByFlowIdOrdered(importFlow.getId())) {
            nextOrder = Math.max(nextOrder, existing.getSortOrder() + 1);
        }
        customsJevic.setFlow(importFlow);
        customsJevic.setSortOrder(nextOrder);
        refreshJevicCopy(customsJevic);
        pipelineStageRepository.save(customsJevic);
        reindex(importFlow.getId());
        reindex(customsFlow.getId());
    }

    private void refreshImportDocumentCopy() {
        PipelineFlow importFlow = pipelineFlowRepository.findByFlowKey(FLOW_IMPORT).orElse(null);
        if (importFlow == null) {
            return;
        }
        refreshJevicCopy(pipelineStageRepository
                .findByFlowIdAndStageKey(importFlow.getId(), FlowStage.JEVIC.getStageKey())
                .orElse(null));
        refreshCoiCopy(pipelineStageRepository
                .findByFlowIdAndStageKey(importFlow.getId(), FlowStage.COI.getStageKey())
                .orElse(null));
    }

    private void refreshJevicCopy(PipelineStage jevic) {
        if (jevic == null) {
            return;
        }
        if (blankOrOneOf(jevic.getTitle(), "JEVIC certificate", "Certificate of inspection")) {
            jevic.setTitle("Odometer certificate");
        }
        if (blankOrOneOf(jevic.getSubtitle(), "Odometer certificate", "Certificate of inspection")) {
            jevic.setSubtitle("JEVIC");
        }
        pipelineStageRepository.save(jevic);
    }

    private void refreshCoiCopy(PipelineStage coi) {
        if (coi == null) {
            return;
        }
        if (blankOrOneOf(coi.getTitle(), "JEVIC certificate")) {
            coi.setTitle("Certificate of inspection");
        }
        if (blankOrOneOf(coi.getSubtitle(), "JEVIC certificate", "Odometer certificate")) {
            coi.setSubtitle("JEVIC");
        }
        pipelineStageRepository.save(coi);
    }

    private static boolean blankOrOneOf(String value, String... matches) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        for (String match : matches) {
            if (match.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private void reindex(Long flowId) {
        List<PipelineStage> stages = pipelineStageRepository.findByFlowIdOrdered(flowId);
        for (int i = 0; i < stages.size(); i++) {
            PipelineStage stage = stages.get(i);
            if (stage.getSortOrder() != i) {
                stage.setSortOrder(i);
                pipelineStageRepository.save(stage);
            }
        }
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
        List<PipelineStage> stages = new ArrayList<>(joined.getStages());
        stages.sort(Comparator.comparingInt(PipelineStage::getSortOrder));
        PipelineStage inspection = null;
        for (PipelineStage stage : stages) {
            if (FlowStage.INSPECTION.getStageKey().equals(stage.getStageKey())) {
                inspection = stage;
                break;
            }
        }
        if (inspection == null) {
            return;
        }
        if (FlowStage.INSPECTION.getStageKey().equals(stages.get(0).getStageKey())) {
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
        return new ArrayList<>();
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

    public List<PipelineFlowGroup> flowGroups() {
        List<PipelineFlowGroup> groups = new ArrayList<>();
        List<PipelineFlow> flows = pipelineFlowRepository.findAllOrdered();
        for (PipelineFlow flow : flows) {
            groups.add(new PipelineFlowGroup(
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
        for (DefaultStage def : defaults) {
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
                new DefaultStage(FlowStage.AUCTION.getStageKey(), "Auction lot", "Japanese auction sheet"),
                new DefaultStage(FlowStage.PRESHIP.getStageKey(), "Pre-shipment", "BV inspection certificate"),
                new DefaultStage(FlowStage.EQUIPMENT.getStageKey(), "Equipment condition", "Interior, exterior, and safety"),
                new DefaultStage(FlowStage.JEVIC.getStageKey(), "Odometer certificate", "JEVIC"),
                new DefaultStage(FlowStage.COI.getStageKey(), "Certificate of inspection", "JEVIC"),
                new DefaultStage(FlowStage.STANDARDS.getStageKey(), "Standards certificate", "Emission and safety"),
                new DefaultStage(FlowStage.EXPORT.getStageKey(), "Export certificate", "English or Japanese"),
                new DefaultStage(FlowStage.PHOTOS.getStageKey(), "Vehicle images", "Up to 5 photos")
        );
    }

    private static List<DefaultStage> defaultCustoms() {
        return Arrays.asList(
                new DefaultStage(FlowStage.DECLARATION.getStageKey(), "Customs declaration", "Sri Lanka CUSDEC"),
                new DefaultStage(FlowStage.ASSESSMENT.getStageKey(), "Assessment notice", "ASYCUDA assessment"),
                new DefaultStage(FlowStage.WORKSHEET.getStageKey(), "Working sheet", "Motor vehicle valuation")
        );
    }

    private static List<DefaultStage> defaultPrep() {
        return Arrays.asList(
                new DefaultStage(FlowStage.INSPECTION.getStageKey(), "Inspection", "Checklist items and results"),
                new DefaultStage(FlowStage.WORKSHOP.getStageKey(), "Workshop", "Repairs, parts, completion"),
                new DefaultStage(FlowStage.YARD.getStageKey(), "Yard", "Bay, keys, inspection")
        );
    }

    private static List<DefaultStage> defaultReady() {
        return Collections.singletonList(
                new DefaultStage(FlowStage.LISTING.getStageKey(), "Sale listing", "Price and list the vehicle")
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

    public static NavLinks viewLinks(String chassisNo, int stageIndex, PipelineProgress status, List<PipelineStage> stages, FlowPipeline flowPipeline) {
        String encoded = encode(chassisNo);
        String viewBase = flowPipeline.getCurrentBase() + encoded;
        FlowPipeline pipeline = FlowPipeline.findPipelineBySortOrder(flowPipeline.getSortOrder() + 1);
        String key = stageKeyAt(stages, stageIndex);
        String prevUrl = stageIndex > 0
                ? PipelineStageUtils.viewUrl(viewBase, stageKeyAt(stages, stageIndex - 1))
                : null;
        String nextUrl;
        String nextLabel = "Next";
        if (stageIndex < stages.size() - 1) {
            String nextKey = stageKeyAt(stages, stageIndex + 1);
            if (isStageComplete(nextKey, status)) {
                nextUrl = PipelineStageUtils.viewUrl(viewBase, nextKey);
                nextLabel = labeled("Next · ", titleFor(nextKey), "Next");
            } else {
                nextUrl = editUrlFor(encoded, nextKey);
                nextLabel = nextUrl != null
                        ? labeled("Continue ", lower(titleFor(nextKey)), "Continue")
                        : "Next";
            }
        } else if (pipeline != null && status.isPipelineCompleted()) {
            nextUrl = pipeline.getCurrentBase() + encoded;
            nextLabel = pipeline.getTitle();
        } else {
            nextUrl = null;
        }
        return new NavLinks(
                stageIndex,
                labeled((stageIndex + 1) + " · ", titleFor(key), (stageIndex + 1) + " · Stage"),
                prevUrl,
                nextUrl,
                nextLabel,
                editUrlFor(encoded, key)
        );
    }

    private static String encode(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static String stageKeyAt(List<PipelineStage> stages, int index) {
        if (stages == null || stages.isEmpty()) {
            return FlowStage.AUCTION.getStageKey();
        }
        if (index < 0) {
            return stages.get(0).getStageKey();
        }
        if (index >= stages.size()) {
            return stages.get(stages.size() - 1).getStageKey();
        }
        return stages.get(index).getStageKey();
    }

    public static boolean isStageComplete(String stageKey, PipelineProgress status) {
        return status.isStageComplete(stageKey);
    }

    public static String editUrlFor(String encodedChassis, String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage != null) {
            return stage.editUrl(encodedChassis);
        }
        return null;
    }

    public static String editUrlOrFallback(String encodedChassis, String stageKey, String fallback) {
        String url = editUrlFor(encodedChassis, stageKey);
        return url != null ? url : fallback;
    }

    private static String labeled(String prefix, String title, String fallback) {
        if (title == null || title.trim().isEmpty()) {
            return fallback;
        }
        return prefix + title;
    }

    private static String lower(String value) {
        return value != null ? value.toLowerCase() : null;
    }

    public static String titleFor(String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage != null) {
            return stage.getTitle();
        }
        return null;
    }

    public static String shortTitleFor(String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage != null) {
            return stage.getShortTitle();
        }
        return null;
    }

    public static String keyAt(List<String> keys, int index) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        if (index < 0) {
            return keys.get(0);
        }
        if (index >= keys.size()) {
            return keys.get(keys.size() - 1);
        }
        return keys.get(index);
    }

    public static Integer parseStageIndex(List<String> keys, String requested) {
        if (requested == null || requested.trim().isEmpty()) {
            return null;
        }
        String value = requested.trim();
        if (keys != null) {
            for (int i = 0; i < keys.size(); i++) {
                if (value.equalsIgnoreCase(keys.get(i))) {
                    return i;
                }
            }
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
