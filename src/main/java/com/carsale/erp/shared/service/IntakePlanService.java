package com.carsale.erp.shared.service;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntakePlanService {

    static final String KIND = "intakePlanKind";
    static final String IMPORT = "intakePlanImport";
    static final String CUSTOMS = "intakePlanCustoms";
    static final String PREP = "intakePlanPrep";

    private final HttpServletRequest request;
    private final VehicleRepository vehicleRepository;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PreparationProgressService preparationProgressService;

    public void remember(String intake, boolean importOn, boolean customsOn, boolean prepOn) {
        HttpSession session = request.getSession();
        session.setAttribute(KIND, normalizeKind(intake));
        session.setAttribute(IMPORT, importOn);
        session.setAttribute(CUSTOMS, customsOn);
        session.setAttribute(PREP, prepOn);
    }

    public boolean hasPlan() {
        return kind() != null;
    }

    public boolean importSelected() {
        return flag(IMPORT, true);
    }

    public String startPath() {
        String intake = kind() == null ? "company" : kind();
        if ("personal".equals(intake)) {
            return "/customers/new";
        }
        if (!importSelected()) {
            return "/vehicle/basics/new?intake=" + intake;
        }
        return "/auction/new?intake=" + intake;
    }

    public String afterCustomerPath(Long customerId) {
        if (hasPlan() && !importSelected()) {
            return "/vehicle/basics/new?customer=" + customerId;
        }
        return "/auction/new?customer=" + customerId;
    }

    public boolean usesBasics(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return false;
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return usesBasics(vehicle);
    }

    public static boolean usesBasics(Vehicle vehicle) {
        if (vehicle == null || vehicle.getSelectedPipelines() == null) {
            return false;
        }
        return !vehicle.getSelectedPipelines().contains("import");
    }

    public String destinationAfterBasics(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        String encoded = PipelineStageUtils.encode(chassisNo.trim());
        String selected = vehicle == null ? null : vehicle.getSelectedPipelines();
        if (selected != null && selected.contains("customs")) {
            return "/customs/" + encoded;
        }
        if (selected != null && selected.contains("prep")) {
            return "/workshop-yard/" + encoded;
        }
        return "/import/" + encoded;
    }

    @Transactional
    public void applyIfPending(String chassisNo) {
        if (!hasPlan() || chassisNo == null || chassisNo.trim().isEmpty()) {
            return;
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (vehicle == null) {
            return;
        }
        if (vehicle.getSelectedPipelines() != null) {
            clear();
            return;
        }
        boolean importOn = flag(IMPORT, true);
        boolean customsOn = flag(CUSTOMS, true);
        boolean prepOn = flag(PREP, true);
        vehicle.setSelectedPipelines(encode(importOn, customsOn, prepOn));
        if (vehicle.getIntake() == null) {
            vehicle.setIntake(intakeFor(kind()));
        }
        Set<String> skipped = SkippedStageService.parse(vehicle.getSkippedStages());
        skipped.addAll(keysToSkip(importOn, customsOn, prepOn));
        vehicle.setSkippedStages(SkippedStageService.join(skipped));
        vehicleRepository.save(vehicle);
        clear();
        advance(vehicle.getChassisNo());
    }

    static String encode(boolean importOn, boolean customsOn, boolean prepOn) {
        if (!importOn && !customsOn && !prepOn) {
            return "-";
        }
        StringBuilder stored = new StringBuilder();
        append(stored, importOn, "import");
        append(stored, customsOn, "customs");
        append(stored, prepOn, "prep");
        return stored.toString();
    }

    static Set<String> keysToSkip(boolean importOn, boolean customsOn, boolean prepOn) {
        Set<String> keys = new LinkedHashSet<String>();
        if (!importOn) {
            keys.addAll(PipelineStageUtils.IMPORT_STAGE_KEYS);
        }
        if (!customsOn) {
            keys.add(FlowStage.BILL_OF_LADING.getStageKey());
            keys.add(FlowStage.DECLARATION.getStageKey());
            keys.add(FlowStage.ASSESSMENT.getStageKey());
        }
        if (!prepOn) {
            keys.addAll(PipelineStageUtils.PREP_STAGE_KEYS);
        }
        return keys;
    }

    private void advance(String chassisNo) {
        importProgressService.syncVehicleStage(chassisNo);
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null || !importProgressService.progressFor(vehicle).isPipelineCompleted()) {
            return;
        }
        customsProgressService.syncVehicleStage(chassisNo);
        vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null || !customsProgressService.progressFor(vehicle).isPipelineCompleted()) {
            return;
        }
        preparationProgressService.syncVehicleStage(chassisNo);
    }

    private void clear() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(KIND);
        session.removeAttribute(IMPORT);
        session.removeAttribute(CUSTOMS);
        session.removeAttribute(PREP);
    }

    private String kind() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(KIND);
        return value instanceof String ? (String) value : null;
    }

    private boolean flag(String name, boolean fallback) {
        if (kind() == null) {
            return fallback;
        }
        Object value = request.getSession(false).getAttribute(name);
        return Boolean.TRUE.equals(value);
    }

    private static void append(StringBuilder stored, boolean on, String token) {
        if (!on) {
            return;
        }
        if (stored.length() > 0) {
            stored.append(',');
        }
        stored.append(token);
    }

    private static String normalizeKind(String intake) {
        if (intake == null) {
            return "company";
        }
        String key = intake.trim().toLowerCase();
        if ("personal".equals(key) || "local".equals(key) || "company".equals(key)) {
            return key;
        }
        return "company";
    }

    private static VehicleIntake intakeFor(String kind) {
        if ("personal".equals(kind)) {
            return VehicleIntake.PERSONAL_IMPORT;
        }
        if ("local".equals(kind)) {
            return VehicleIntake.LOCAL_UNREGISTERED;
        }
        return VehicleIntake.COMPANY_IMPORT;
    }
}
