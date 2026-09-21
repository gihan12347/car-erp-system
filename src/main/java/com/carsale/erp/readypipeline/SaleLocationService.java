package com.carsale.erp.readypipeline;

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
@Order(6)
public class SaleLocationService implements CommandLineRunner {

    private static final String[][] DEFAULT_SALES = {
            {"SL-01", "Showroom", "Front showroom", "15"},
            {"SL-02", "Outdoor lot", "Street display", "30"},
            {"SL-03", "Reserved bay", "Indoor", "10"}
    };

    private final SaleLocationRepository saleLocationRepository;
    private final SaleListingRepository saleListingRepository;

    public SaleLocationService(
            SaleLocationRepository saleLocationRepository,
            SaleListingRepository saleListingRepository
    ) {
        this.saleLocationRepository = saleLocationRepository;
        this.saleListingRepository = saleListingRepository;
    }

    @Override
    public void run(String... args) {
        seedDefaults();
        backfillMissingDetails();
    }

    @Transactional
    public void seedDefaults() {
        if (saleLocationRepository.count() > 0) {
            return;
        }
        for (int i = 0; i < DEFAULT_SALES.length; i++) {
            saveNew(
                    DEFAULT_SALES[i][0],
                    DEFAULT_SALES[i][1],
                    parseCapacity(DEFAULT_SALES[i][3]),
                    DEFAULT_SALES[i][2],
                    i
            );
        }
    }

    @Transactional
    public void backfillMissingDetails() {
        for (SaleLocation sale : saleLocationRepository.findAllByOrderBySortOrderAscIdAsc()) {
            boolean dirty = false;
            if (isBlank(sale.getSaleName())) {
                sale.setSaleName(isBlank(sale.getLocation()) ? sale.getSaleCode() : sale.getLocation().trim());
                dirty = true;
            }
            if (sale.getCapacity() <= 0) {
                sale.setCapacity(1);
                dirty = true;
            }
            if (dirty) {
                saleLocationRepository.save(sale);
            }
        }
    }

    public List<SaleLocation> listAll() {
        return attachOccupancy(saleLocationRepository.findAllByOrderBySortOrderAscIdAsc());
    }

    public List<SaleLocation> listActive() {
        return attachOccupancy(saleLocationRepository.findByActiveTrueOrderBySortOrderAscIdAsc());
    }

    public List<String> listActiveLocations() {
        Set<String> locations = new LinkedHashSet<>();
        for (SaleLocation sale : listActive()) {
            if (!isBlank(sale.getLocation())) {
                locations.add(sale.getLocation().trim());
            }
        }
        return new ArrayList<>(locations);
    }

    public String nextSaleCode() {
        List<String> codes = new ArrayList<>();
        for (SaleLocation sale : saleLocationRepository.findAllByOrderBySortOrderAscIdAsc()) {
            codes.add(sale.getSaleCode());
        }
        String candidate = nextSaleCode(codes);
        while (saleLocationRepository.existsBySaleCodeIgnoreCase(candidate)) {
            codes.add(candidate);
            candidate = nextSaleCode(codes);
        }
        return candidate;
    }

    static String nextSaleCode(Iterable<String> existingCodes) {
        int max = 0;
        if (existingCodes != null) {
            for (String code : existingCodes) {
                if (isBlank(code)) {
                    continue;
                }
                Matcher matcher = RegexConstants.Identifiers.SALE_CODE.matcher(code.trim());
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
        return formatSaleCode(max + 1);
    }

    static String formatSaleCode(int sequence) {
        if (sequence < 1) {
            sequence = 1;
        }
        if (sequence < 100) {
            return String.format(Locale.ROOT, "SL-%02d", sequence);
        }
        return "SL-" + sequence;
    }

    public SaleLocation findByCode(String saleCode) {
        if (isBlank(saleCode)) {
            return null;
        }
        SaleLocation sale = saleLocationRepository.findBySaleCodeIgnoreCase(saleCode.trim()).orElse(null);
        attachOccupancy(sale);
        return sale;
    }

    public SaleLocation requireAssignable(String saleCode, String chassisNo) {
        SaleLocation sale = findByCode(saleCode);
        if (sale == null || !sale.isActive()) {
            throw new IllegalArgumentException("Unknown sale location " + (saleCode == null ? "" : saleCode.trim()) + ".");
        }
        attachOccupancy(sale);
        boolean alreadyHere = false;
        if (!isBlank(chassisNo)) {
            SaleListing existing = saleListingRepository.findById(chassisNo.trim()).orElse(null);
            alreadyHere = occupies(existing, sale.getSaleCode());
        }
        if (!sale.hasFreeSlotFor(alreadyHere)) {
            throw new IllegalArgumentException(
                    "Sale location " + sale.getSaleCode() + " is full (" + sale.getCapacity() + " vehicles).");
        }
        return sale;
    }

    @Transactional
    public SaleLocation add(String saleName, Integer capacity, String location) {
        String code = nextSaleCode();
        int nextOrder = 0;
        for (SaleLocation existing : saleLocationRepository.findAllByOrderBySortOrderAscIdAsc()) {
            nextOrder = Math.max(nextOrder, existing.getSortOrder() + 1);
        }
        return saveNew(code, requireSaleName(saleName), requireCapacity(capacity), requireLocation(location), nextOrder);
    }

    @Transactional
    public void update(Long id, String saleCode, String saleName, Integer capacity, String location) {
        SaleLocation sale = saleLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale location not found."));
        String previousCode = sale.getSaleCode();
        String previousLocation = blankToEmpty(sale.getLocation());
        String code = requireSaleCode(saleCode);
        if (!code.equalsIgnoreCase(previousCode) && saleLocationRepository.existsBySaleCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Sale location " + code + " already exists.");
        }
        String resolvedLocation = requireLocation(location);
        sale.setSaleCode(code);
        sale.setSaleName(requireSaleName(saleName));
        sale.setCapacity(requireCapacity(capacity));
        sale.setLocation(resolvedLocation);
        saleLocationRepository.save(sale);
        if (!code.equalsIgnoreCase(previousCode) || !resolvedLocation.equals(previousLocation)) {
            for (SaleListing listing : saleListingRepository.findBySaleCodeIgnoreCase(previousCode)) {
                listing.setSaleCode(code);
                listing.setSaleLocation(resolvedLocation);
                saleListingRepository.save(listing);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        SaleLocation sale = saleLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale location not found."));
        if (unsoldCount(sale.getSaleCode()) > 0) {
            throw new IllegalArgumentException("Cannot remove a sale location that still has unsold vehicles.");
        }
        saleLocationRepository.deleteById(id);
    }

    private long unsoldCount(String saleCode) {
        long count = 0;
        for (SaleListing listing : saleListingRepository.findBySaleCodeIgnoreCase(saleCode)) {
            if (occupies(listing, saleCode)) {
                count++;
            }
        }
        return count;
    }

    private SaleLocation saveNew(String saleCode, String saleName, int capacity, String location, int sortOrder) {
        SaleLocation sale = new SaleLocation();
        sale.setSaleCode(saleCode);
        sale.setSaleName(saleName);
        sale.setCapacity(capacity);
        sale.setLocation(location);
        sale.setSortOrder(sortOrder);
        sale.setActive(true);
        return saleLocationRepository.save(sale);
    }

    private List<SaleLocation> attachOccupancy(List<SaleLocation> sales) {
        if (sales == null || sales.isEmpty()) {
            return sales;
        }
        Map<String, Integer> occupied = occupancyByCode();
        for (SaleLocation sale : sales) {
            attachOccupancy(sale, occupied);
        }
        return sales;
    }

    private void attachOccupancy(SaleLocation sale) {
        attachOccupancy(sale, occupancyByCode());
    }

    private void attachOccupancy(SaleLocation sale, Map<String, Integer> occupied) {
        if (sale == null) {
            return;
        }
        Integer count = occupied.get(normalizeCode(sale.getSaleCode()));
        sale.setOccupied(count == null ? 0 : count);
    }

    private Map<String, Integer> occupancyByCode() {
        Map<String, Integer> occupied = new HashMap<>();
        for (SaleListing listing : saleListingRepository.findAll()) {
            if (!listing.isSold()) {
                String key = listing.getSaleCode();
                occupied.compute(key, (k, current) -> current == null ? 1 : current + 1);
            }
        }
        return occupied;
    }

    private static boolean occupies(SaleListing listing, String saleCode) {
        return listing != null
                && !listing.isSold()
                && listing.getSaleCode() != null
                && listing.getSaleCode().equalsIgnoreCase(saleCode);
    }

    private static String requireSaleCode(String saleCode) {
        if (isBlank(saleCode)) {
            throw new IllegalArgumentException("Sale code is required.");
        }
        String cleaned = saleCode.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ").toUpperCase(Locale.ROOT);
        if (cleaned.length() > 40) {
            throw new IllegalArgumentException("Sale code is too long.");
        }
        return cleaned;
    }

    private static String requireSaleName(String saleName) {
        if (isBlank(saleName)) {
            throw new IllegalArgumentException("Sale name is required.");
        }
        String cleaned = saleName.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        if (cleaned.length() > 80) {
            throw new IllegalArgumentException("Sale name is too long.");
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
        return value.trim().toUpperCase();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
