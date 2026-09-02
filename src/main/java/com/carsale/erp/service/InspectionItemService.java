package com.carsale.erp.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.InspectionItem;
import com.carsale.erp.repository.InspectionItemRepository;

@Service
@Order(4)
public class InspectionItemService implements CommandLineRunner {

    private static final List<String> DEFAULT_TITLES = Arrays.asList(
            "Engine",
            "Transmission",
            "Brakes",
            "Suspension",
            "Steering",
            "Lights",
            "Air conditioning",
            "Battery / electrical",
            "Tyres",
            "Exhaust",
            "Body / exterior",
            "Interior",
            "Fluids / leaks"
    );

    private final InspectionItemRepository inspectionItemRepository;

    public InspectionItemService(InspectionItemRepository inspectionItemRepository) {
        this.inspectionItemRepository = inspectionItemRepository;
    }

    @Override
    public void run(String... args) {
        seedDefaults();
    }

    @Transactional
    public void seedDefaults() {
        if (inspectionItemRepository.count() > 0) {
            return;
        }
        for (int i = 0; i < DEFAULT_TITLES.size(); i++) {
            saveNew(DEFAULT_TITLES.get(i), i);
        }
    }

    public List<InspectionItem> listAll() {
        return inspectionItemRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    public List<InspectionItem> listActive() {
        return inspectionItemRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public InspectionItem add(String title) {
        String cleaned = requireTitle(title);
        int nextOrder = 0;
        for (InspectionItem existing : inspectionItemRepository.findAllByOrderBySortOrderAscIdAsc()) {
            nextOrder = Math.max(nextOrder, existing.getSortOrder() + 1);
        }
        return saveNew(cleaned, nextOrder);
    }

    @Transactional
    public InspectionItem updateTitle(Long id, String title) {
        InspectionItem item = inspectionItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspection item not found."));
        item.setTitle(requireTitle(title));
        return inspectionItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null || !inspectionItemRepository.existsById(id)) {
            throw new IllegalArgumentException("Inspection item not found.");
        }
        inspectionItemRepository.deleteById(id);
    }

    private InspectionItem saveNew(String title, int sortOrder) {
        InspectionItem item = new InspectionItem();
        item.setTitle(title);
        item.setItemKey(uniqueKey(title));
        item.setSortOrder(sortOrder);
        item.setActive(true);
        return inspectionItemRepository.save(item);
    }

    private String uniqueKey(String title) {
        String base = slug(title);
        String key = base;
        int suffix = 2;
        while (inspectionItemRepository.existsByItemKey(key)) {
            key = base + "-" + suffix;
            suffix++;
        }
        return key;
    }

    static String slug(String title) {
        if (title == null) {
            return "item";
        }
        String slug = title.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            return "item";
        }
        if (slug.length() > 70) {
            return slug.substring(0, 70).replaceAll("-+$", "");
        }
        return slug;
    }

    private static String requireTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Inspection item name is required.");
        }
        String cleaned = title.trim().replaceAll("\\s+", " ");
        if (cleaned.length() > 160) {
            throw new IllegalArgumentException("Inspection item name is too long.");
        }
        return cleaned;
    }
}
