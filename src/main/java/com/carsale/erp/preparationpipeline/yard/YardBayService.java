package com.carsale.erp.preparationpipeline.yard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.regex.RegexConstants;

@Service
@Order(5)
public class YardBayService implements CommandLineRunner {

    private static final String[][] DEFAULT_YARDS = {
            {"YD-01", "Main yard", "Front lot", "40"},
            {"YD-02", "Covered yard", "Indoor", "20"},
            {"YD-03", "Overflow yard", "Rear lot", "25"}
    };

    private static final String[] LEGACY_PARKING_CODES = {
            "A-01", "A-02", "A-03", "B-01", "B-02", "C-01", "C-02"
    };

    private final YardBayRepository yardBayRepository;
    private final YardRecordRepository yardRecordRepository;

    public YardBayService(YardBayRepository yardBayRepository, YardRecordRepository yardRecordRepository) {
        this.yardBayRepository = yardBayRepository;
        this.yardRecordRepository = yardRecordRepository;
    }

    @Override
    public void run(String... args) {
        removeUnusedLegacyParkingBays();
        seedDefaults();
        backfillMissingDetails();
    }

    @Transactional
    public void removeUnusedLegacyParkingBays() {
        for (String code : LEGACY_PARKING_CODES) {
            YardBay bay = yardBayRepository.findByBayCodeIgnoreCase(code).orElse(null);
            if (bay == null) {
                continue;
            }
            if (yardRecordRepository.countByBayNoIgnoreCase(bay.getBayCode()) > 0) {
                continue;
            }
            yardBayRepository.delete(bay);
        }
    }

    @Transactional
    public void seedDefaults() {
        if (yardBayRepository.count() > 0) {
            return;
        }
        for (int i = 0; i < DEFAULT_YARDS.length; i++) {
            saveNew(
                    DEFAULT_YARDS[i][0],
                    DEFAULT_YARDS[i][1],
                    parseCapacity(DEFAULT_YARDS[i][3]),
                    DEFAULT_YARDS[i][2],
                    i
            );
        }
    }

    @Transactional
    public void backfillMissingDetails() {
        for (YardBay bay : yardBayRepository.findAllByOrderBySortOrderAscIdAsc()) {
            boolean dirty = false;
            if (isBlank(bay.getYardName())) {
                bay.setYardName(isBlank(bay.getSection()) ? bay.getBayCode() : bay.getSection().trim());
                dirty = true;
            }
            if (bay.getCapacity() <= 0) {
                bay.setCapacity(1);
                dirty = true;
            }
            if (dirty) {
                yardBayRepository.save(bay);
            }
        }
    }

    public List<YardBay> listAll() {
        return attachOccupancy(yardBayRepository.findAllByOrderBySortOrderAscIdAsc());
    }

    public List<YardBay> listActive() {
        return attachOccupancy(yardBayRepository.findByActiveTrueOrderBySortOrderAscIdAsc());
    }

    public List<String> listActiveLocations() {
        Set<String> locations = new LinkedHashSet<>();
        for (YardBay bay : listActive()) {
            if (!isBlank(bay.getLocation())) {
                locations.add(bay.getLocation().trim());
            }
        }
        return new ArrayList<>(locations);
    }

    public List<String> listActiveSections() {
        return listActiveLocations();
    }

    public String nextYardCode() {
        List<String> codes = new ArrayList<>();
        for (YardBay bay : yardBayRepository.findAllByOrderBySortOrderAscIdAsc()) {
            codes.add(bay.getBayCode());
        }
        String candidate = nextYardCode(codes);
        while (yardBayRepository.existsByBayCodeIgnoreCase(candidate)) {
            codes.add(candidate);
            candidate = nextYardCode(codes);
        }
        return candidate;
    }

    static String nextYardCode(Iterable<String> existingCodes) {
        int max = 0;
        if (existingCodes != null) {
            for (String code : existingCodes) {
                if (isBlank(code)) {
                    continue;
                }
                Matcher matcher = RegexConstants.Identifiers.YARD_CODE.matcher(code.trim());
                if (!matcher.matches()) {
                    continue;
                }
                try {
                    max = Math.max(max, Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                    // skip unreadable sequences
                }
            }
        }
        return formatYardCode(max + 1);
    }

    static String formatYardCode(int sequence) {
        if (sequence < 1) {
            sequence = 1;
        }
        if (sequence < 100) {
            return String.format(Locale.ROOT, "YD-%02d", sequence);
        }
        return "YD-" + sequence;
    }

    public YardBay findByCode(String yardCode) {
        if (isBlank(yardCode)) {
            return null;
        }
        return yardBayRepository.findByBayCodeIgnoreCase(yardCode.trim()).orElse(null);
    }

    public YardBay requireAssignable(String yardCode, String chassisNo) {
        YardBay yard = findByCode(yardCode);
        if (yard == null || !yard.isActive()) {
            throw new IllegalArgumentException("Unknown yard " + (yardCode == null ? "" : yardCode.trim()) + ".");
        }
        attachOccupancy(yard);
        boolean alreadyHere = false;
        if (!isBlank(chassisNo)) {
            YardRecord existing = yardRecordRepository.findById(chassisNo.trim()).orElse(null);
            alreadyHere = existing != null
                    && existing.getBayNo() != null
                    && existing.getBayNo().equalsIgnoreCase(yard.getBayCode());
        }
        if (!yard.hasFreeSlotFor(alreadyHere)) {
            throw new IllegalArgumentException(
                    "Yard " + yard.getBayCode() + " is full (" + yard.getCapacity() + " vehicles).");
        }
        return yard;
    }

    @Transactional
    public YardBay add(String yardName, Integer capacity, String location) {
        String code = nextYardCode();
        int nextOrder = 0;
        for (YardBay existing : yardBayRepository.findAllByOrderBySortOrderAscIdAsc()) {
            nextOrder = Math.max(nextOrder, existing.getSortOrder() + 1);
        }
        return saveNew(code, requireYardName(yardName), requireCapacity(capacity), requireLocation(location), nextOrder);
    }

    @Transactional
    public void update(Long id, String bayCode, String yardName, Integer capacity, String location) {
        YardBay bay = yardBayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Yard not found."));
        String previousCode = bay.getBayCode();
        String previousLocation = blankToEmpty(bay.getLocation());
        String code = requireBayCode(bayCode);
        if (!code.equalsIgnoreCase(previousCode) && yardBayRepository.existsByBayCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Yard " + code + " already exists.");
        }
        String resolvedLocation = requireLocation(location);
        bay.setBayCode(code);
        bay.setYardName(requireYardName(yardName));
        bay.setCapacity(requireCapacity(capacity));
        bay.setLocation(resolvedLocation);
        yardBayRepository.save(bay);
        if (!code.equalsIgnoreCase(previousCode) || !resolvedLocation.equals(previousLocation)) {
            for (YardRecord record : yardRecordRepository.findByBayNoIgnoreCase(previousCode)) {
                record.setBayNo(code);
                record.setYardSection(resolvedLocation);
                yardRecordRepository.save(record);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        YardBay bay = yardBayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Yard not found."));
        if (yardRecordRepository.countByBayNoIgnoreCase(bay.getBayCode()) > 0) {
            throw new IllegalArgumentException("Cannot remove a yard that still has vehicles.");
        }
        yardBayRepository.deleteById(id);
    }

    private YardBay saveNew(String bayCode, String yardName, int capacity, String location, int sortOrder) {
        YardBay bay = new YardBay();
        bay.setBayCode(bayCode);
        bay.setYardName(yardName);
        bay.setCapacity(capacity);
        bay.setLocation(location);
        bay.setSortOrder(sortOrder);
        bay.setActive(true);
        return yardBayRepository.save(bay);
    }

    private List<YardBay> attachOccupancy(List<YardBay> bays) {
        if (bays == null || bays.isEmpty()) {
            return bays;
        }
        Map<String, Integer> occupied = occupancyByCode();
        for (YardBay bay : bays) {
            attachOccupancy(bay, occupied);
        }
        return bays;
    }

    private void attachOccupancy(YardBay bay) {
        attachOccupancy(bay, occupancyByCode());
    }

    private void attachOccupancy(YardBay bay, Map<String, Integer> occupied) {
        if (bay == null) {
            return;
        }
        Integer count = occupied.get(normalizeCode(bay.getBayCode()));
        bay.setOccupied(count == null ? 0 : count);
    }

    private Map<String, Integer> occupancyByCode() {
        Map<String, Integer> occupied = new HashMap<>();
        for (YardRecord record : yardRecordRepository.findAll()) {
            String key = normalizeCode(record.getBayNo());
            if (key.isEmpty()) {
                continue;
            }
            occupied.compute(key, (k, current) -> current == null ? 1 : current + 1);
        }
        return occupied;
    }

    private static String requireBayCode(String bayCode) {
        if (isBlank(bayCode)) {
            throw new IllegalArgumentException("Yard code is required.");
        }
        String cleaned = bayCode.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ").toUpperCase(Locale.ROOT);
        if (cleaned.length() > 40) {
            throw new IllegalArgumentException("Yard code is too long.");
        }
        return cleaned;
    }

    private static String requireYardName(String yardName) {
        if (isBlank(yardName)) {
            throw new IllegalArgumentException("Yard name is required.");
        }
        String cleaned = yardName.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        if (cleaned.length() > 80) {
            throw new IllegalArgumentException("Yard name is too long.");
        }
        return cleaned;
    }

    private static String requireLocation(String location) {
        if (isBlank(location)) {
            throw new IllegalArgumentException("Location is required.");
        }
        String cleaned = location.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        if (cleaned.length() > 80) {
            throw new IllegalArgumentException("Location is too long.");
        }
        return cleaned;
    }

    private static int requireCapacity(Integer capacity) {
        if (capacity == null) {
            throw new IllegalArgumentException("Capacity is required.");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        if (capacity > 9999) {
            throw new IllegalArgumentException("Capacity is too large.");
        }
        return capacity;
    }

    private static int parseCapacity(String value) {
        try {
            return requireCapacity(Integer.valueOf(value));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private static String normalizeCode(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
