package com.carsale.erp.preparationpipeline.inspection;

import com.carsale.erp.preparationpipeline.checklist.InspectionItemService;
import com.carsale.erp.preparationpipeline.workshop.WorkshopService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.preparationpipeline.inspection.InspectionFailRequest;
import com.carsale.erp.preparationpipeline.inspection.InspectionFailResult;
import com.carsale.erp.preparationpipeline.checklist.InspectionItem;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.preparationpipeline.inspection.VehicleInspection;
import com.carsale.erp.preparationpipeline.inspection.VehicleInspectionLine;
import com.carsale.erp.preparationpipeline.workshop.WorkshopJob;
import com.carsale.erp.preparationpipeline.workshop.WorkshopJobLine;
import com.carsale.erp.preparationpipeline.inspection.VehicleInspectionRepository;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class VehicleInspectionService {

    private final VehicleRepository vehicleRepository;
    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final InspectionItemService inspectionItemService;
    private final WorkshopService workshopService;

    public VehicleInspectionService(
            VehicleRepository vehicleRepository,
            VehicleInspectionRepository vehicleInspectionRepository,
            InspectionItemService inspectionItemService,
            WorkshopService workshopService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.inspectionItemService = inspectionItemService;
        this.workshopService = workshopService;
    }

    @Transactional(readOnly = true)
    public VehicleInspection findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return vehicleInspectionRepository.findById(chassisNo.trim()).orElse(null);
    }

    @Transactional(readOnly = true)
    public VehicleInspection prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        VehicleInspection record = vehicleInspectionRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new VehicleInspection();
            record.setChassisNo(chassisNo);
            record.setInspectionDate(java.time.LocalDate.now().toString());
        } else if (record.getInspectionDate() == null || record.getInspectionDate().trim().isEmpty()) {
            record.setInspectionDate(java.time.LocalDate.now().toString());
        }
        mergeCatalogItems(record);
        return record;
    }

    public boolean isComplete(String chassisNo) {
        VehicleInspection record = findByChassisNo(chassisNo);
        return record != null && record.isCompleted();
    }

    @Transactional
    public VehicleInspection save(VehicleInspection incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        VehicleInspection existing = vehicleInspectionRepository.findById(chassisNo).orElse(null);
        if (existing == null) {
            existing = new VehicleInspection();
            existing.setChassisNo(chassisNo);
        }
        existing.setInspector(incoming.getInspector());
        existing.setInspectionDate(incoming.getInspectionDate());
        existing.setNotes(incoming.getNotes());
        existing.setCompleted(incoming.isCompleted());
        syncLines(existing, incoming.getLines());
        validateFilled(existing);
        VehicleInspection saved = vehicleInspectionRepository.save(existing);
        syncWorkshopJobsFromNoItems(saved);
        return saved;
    }

    /**
     * Workshop jobs from inspection exist only for items marked No.
     */
    private void syncWorkshopJobsFromNoItems(VehicleInspection inspection) {
        if (inspection == null || isBlank(inspection.getChassisNo())) {
            return;
        }
        java.util.LinkedHashSet<String> noKeys = new java.util.LinkedHashSet<String>();
        for (VehicleInspectionLine line : inspection.getLines()) {
            if (line == null || !InspectionResults.isNo(line.getResult()) || isBlank(line.getItemKey())) {
                continue;
            }
            noKeys.add(line.getItemKey().trim());
            workshopService.addInspectionFailJob(
                    inspection.getChassisNo(),
                    line.getItemKey(),
                    line.getItemTitle(),
                    line.getNotes()
            );
        }
        workshopService.retainInspectionFailJobs(inspection.getChassisNo(), noKeys);
    }

    @Transactional
    public InspectionFailResult recordNoSelection(String chassisNo, InspectionFailRequest request) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String id = chassisNo.trim();
        vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + id));
        if (request == null) {
            throw new IllegalArgumentException("Inspection item is required.");
        }

        VehicleInspection inspection = vehicleInspectionRepository.findById(id).orElse(null);
        if (inspection == null) {
            inspection = new VehicleInspection();
            inspection.setChassisNo(id);
        }
        mergeCatalogItems(inspection);

        VehicleInspectionLine line = findLine(inspection, request);
        if (line == null) {
            if (isBlank(request.getItemTitle())) {
                throw new IllegalArgumentException("Inspection item name is required.");
            }
            line = new VehicleInspectionLine();
            line.setItemTitle(request.getItemTitle().trim());
            line.setItemKey(isBlank(request.getItemKey())
                    ? customKey(line.getItemTitle(), inspection.getLines())
                    : request.getItemKey().trim());
            line.setCatalogItem(request.isCatalogItem());
            line.setSortOrder(inspection.getLines().size());
            line.setInspection(inspection);
            inspection.getLines().add(line);
        }
        line.setResult(InspectionResults.NO);
        if (!isBlank(request.getNotes())) {
            line.setNotes(request.getNotes().trim());
        }
        vehicleInspectionRepository.save(inspection);

        WorkshopJob before = workshopService.findByChassisNo(id);
        int beforeCount = countJobsForKey(before, line.getItemKey());
        workshopService.addInspectionFailJob(id, line.getItemKey(), line.getItemTitle(), line.getNotes());
        WorkshopJob after = workshopService.findByChassisNo(id);
        boolean created = countJobsForKey(after, line.getItemKey()) > beforeCount;
        if (created) {
            return InspectionFailResult.ok(true, "Workshop job created for " + line.getItemTitle() + ".");
        }
        return InspectionFailResult.ok(false, "Workshop job already exists for " + line.getItemTitle() + ".");
    }

    @Transactional
    public InspectionFailResult clearNoSelection(String chassisNo, InspectionFailRequest request) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String id = chassisNo.trim();
        vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + id));
        if (request == null) {
            throw new IllegalArgumentException("Inspection item is required.");
        }

        VehicleInspection inspection = vehicleInspectionRepository.findById(id).orElse(null);
        if (inspection == null) {
            return InspectionFailResult.ok(false, "No inspection record yet.");
        }
        mergeCatalogItems(inspection);

        VehicleInspectionLine line = findLine(inspection, request);
        if (line == null) {
            return InspectionFailResult.ok(false, "Inspection item not found.");
        }
        String nextResult = InspectionResults.canonical(request.getResult());
        if (InspectionResults.isNo(nextResult) || isBlank(nextResult)) {
            nextResult = InspectionResults.OK;
        }
        line.setResult(nextResult);
        vehicleInspectionRepository.save(inspection);
        workshopService.removeInspectionFailJob(id, line.getItemKey());
        return InspectionFailResult.ok(true, "Workshop job removed for " + line.getItemTitle() + ".");
    }

    public int answeredCount(VehicleInspection record) {
        if (record == null) {
            return 0;
        }
        int count = 0;
        for (VehicleInspectionLine line : record.getLines()) {
            if (line != null && !isBlank(line.getResult())) {
                count++;
            }
        }
        return count;
    }

    private void mergeCatalogItems(VehicleInspection record) {
        Map<String, VehicleInspectionLine> byKey = new LinkedHashMap<String, VehicleInspectionLine>();
        List<VehicleInspectionLine> leftovers = new ArrayList<VehicleInspectionLine>();
        for (VehicleInspectionLine line : record.getLines()) {
            if (!isBlank(line.getItemKey()) && !byKey.containsKey(line.getItemKey())) {
                byKey.put(line.getItemKey(), line);
            } else {
                leftovers.add(line);
            }
        }

        List<VehicleInspectionLine> merged = new ArrayList<VehicleInspectionLine>();
        int order = 0;
        for (InspectionItem item : inspectionItemService.listActive()) {
            VehicleInspectionLine line = byKey.remove(item.getItemKey());
            if (line == null) {
                line = new VehicleInspectionLine();
                line.setItemKey(item.getItemKey());
                line.setResult("");
            }
            line.setItemTitle(item.getTitle());
            line.setCatalogItem(true);
            line.setSortOrder(order++);
            line.setInspection(record);
            merged.add(line);
        }
        leftovers.addAll(byKey.values());
        for (VehicleInspectionLine leftover : leftovers) {
            leftover.setCatalogItem(false);
            leftover.setSortOrder(order++);
            leftover.setInspection(record);
            merged.add(leftover);
        }
        record.getLines().clear();
        record.getLines().addAll(merged);
    }

    private void syncLines(VehicleInspection existing, List<VehicleInspectionLine> incoming) {
        List<VehicleInspectionLine> source = incoming == null
                ? new ArrayList<VehicleInspectionLine>()
                : incoming;
        Map<Long, VehicleInspectionLine> currentById = new LinkedHashMap<Long, VehicleInspectionLine>();
        for (VehicleInspectionLine line : existing.getLines()) {
            if (line.getId() != null) {
                currentById.put(line.getId(), line);
            }
        }

        List<VehicleInspectionLine> next = new ArrayList<VehicleInspectionLine>();
        int order = 0;
        for (VehicleInspectionLine incomingLine : source) {
            if (incomingLine == null || incomingLine.isBlankCustom()) {
                continue;
            }
            if (isBlank(incomingLine.getItemTitle())) {
                continue;
            }
            incomingLine.setResult(InspectionResults.canonical(incomingLine.getResult()));
            if (isBlank(incomingLine.getItemKey())) {
                incomingLine.setItemKey(customKey(incomingLine.getItemTitle(), next));
            }
            if (incomingLine.getId() != null && currentById.containsKey(incomingLine.getId())) {
                VehicleInspectionLine managed = currentById.get(incomingLine.getId());
                BeanUtils.copyProperties(incomingLine, managed, "id", "inspection");
                managed.setSortOrder(order++);
                managed.setInspection(existing);
                next.add(managed);
            } else {
                incomingLine.setId(null);
                incomingLine.setSortOrder(order++);
                incomingLine.setInspection(existing);
                next.add(incomingLine);
            }
        }
        existing.getLines().clear();
        existing.getLines().addAll(next);
    }

    private void validateFilled(VehicleInspection inspection) {
        if (isBlank(inspection.getInspector()) || isBlank(inspection.getInspectionDate())) {
            throw new IllegalArgumentException("Inspector and inspection date are required.");
        }
        if (inspection.getLines().isEmpty()) {
            throw new IllegalArgumentException("Add at least one inspection item.");
        }
        for (VehicleInspectionLine line : inspection.getLines()) {
            if (line == null || line.isBlankCustom()) {
                continue;
            }
            if (isBlank(line.getItemTitle()) || isBlank(line.getResult())) {
                throw new IllegalArgumentException("Mark every inspection item OK, No, or N/A.");
            }
        }
    }

    private static VehicleInspectionLine findLine(VehicleInspection inspection, InspectionFailRequest request) {
        if (request.getLineId() != null) {
            for (VehicleInspectionLine line : inspection.getLines()) {
                if (request.getLineId().equals(line.getId())) {
                    return line;
                }
            }
        }
        if (!isBlank(request.getItemKey())) {
            for (VehicleInspectionLine line : inspection.getLines()) {
                if (request.getItemKey().equals(line.getItemKey())) {
                    return line;
                }
            }
        }
        return null;
    }

    private static int countJobsForKey(WorkshopJob job, String itemKey) {
        if (job == null || isBlank(itemKey)) {
            return 0;
        }
        int count = 0;
        for (WorkshopJobLine line : job.getLines()) {
            if (itemKey.equals(line.getInspectionItemKey())) {
                count++;
            }
        }
        return count;
    }

    private static String customKey(String title, List<VehicleInspectionLine> existing) {
        String base = "custom-" + InspectionItemService.slug(title);
        String key = base;
        int suffix = 2;
        while (keyTaken(key, existing)) {
            key = base + "-" + suffix;
            suffix++;
        }
        return key;
    }

    private static boolean keyTaken(String key, List<VehicleInspectionLine> existing) {
        for (VehicleInspectionLine line : existing) {
            if (key.equals(line.getItemKey())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
