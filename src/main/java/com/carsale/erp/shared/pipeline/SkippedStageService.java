package com.carsale.erp.shared.pipeline;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class SkippedStageService {

    public static final String REG_BOOK = "reg-book";
    public static final String REG_PHOTOS = "reg-photos";
    public static final String REG_OWNER = "reg-owner";

    private static final List<String> EXTRA_KEYS = Arrays.asList(REG_BOOK, REG_PHOTOS, REG_OWNER);

    private final VehicleRepository vehicleRepository;

    public SkippedStageService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Set<String> matching(Vehicle vehicle, Collection<String> allowed) {
        Set<String> matched = new LinkedHashSet<String>();
        if (vehicle == null || allowed == null) {
            return matched;
        }
        for (String key : parse(vehicle.getSkippedStages())) {
            if (allowed.contains(key)) {
                matched.add(key);
            }
        }
        return matched;
    }

    public boolean isMarked(String chassisNo, String stageKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty() || stageKey == null) {
            return false;
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return vehicle != null && parse(vehicle.getSkippedStages()).contains(stageKey.trim());
    }

    public String token(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return "|";
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (vehicle == null) {
            return "|";
        }
        StringBuilder token = new StringBuilder("|");
        for (String key : parse(vehicle.getSkippedStages())) {
            token.append(key).append('|');
        }
        return token.toString();
    }

    public String skipAction(String chassisNo, String stageKey, String prefix) {
        String chassis = chassisNo == null ? "" : chassisNo.trim();
        String stage = stageKey == null ? "" : stageKey.trim();
        String base = prefix == null ? "/" : prefix;
        return base
                + UriUtils.encodePathSegment(chassis, StandardCharsets.UTF_8)
                + "/skip?stage="
                + UriUtils.encodeQueryParam(stage, StandardCharsets.UTF_8);
    }

    @Transactional
    public void add(String chassisNo, String stageKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String key = canonical(stageKey);
        if (key == null) {
            throw new IllegalArgumentException("That stage cannot be skipped.");
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        Set<String> skipped = parse(vehicle.getSkippedStages());
        skipped.add(key);
        vehicle.setSkippedStages(join(skipped));
        vehicleRepository.save(vehicle);
    }

    public static Set<String> parse(String stored) {
        Set<String> keys = new LinkedHashSet<String>();
        if (stored == null || stored.trim().isEmpty()) {
            return keys;
        }
        String[] parts = stored.split(",");
        for (String part : parts) {
            String key = canonical(part);
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    public static String canonical(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String raw = value.trim();
        FlowStage stage = FlowStage.fromKey(raw);
        if (stage != null) {
            return stage.getStageKey();
        }
        for (String extra : EXTRA_KEYS) {
            if (extra.equalsIgnoreCase(raw)) {
                return extra;
            }
        }
        return null;
    }

    public static String join(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return "";
        }
        StringBuilder stored = new StringBuilder();
        for (String key : keys) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }
}
