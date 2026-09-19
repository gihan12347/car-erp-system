package com.carsale.erp.preparationpipeline.workshop;

import com.carsale.erp.preparationpipeline.inspection.InspectionResults;

import java.util.*;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.regex.RegexConstants;

@Service
public class WorkshopService {

    private final VehicleRepository vehicleRepository;
    private final WorkshopJobRepository workshopJobRepository;

    public WorkshopService(VehicleRepository vehicleRepository, WorkshopJobRepository workshopJobRepository) {
        this.vehicleRepository = vehicleRepository;
        this.workshopJobRepository = workshopJobRepository;
    }

    @Transactional(readOnly = true)
    public WorkshopJob findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        WorkshopJob record = workshopJobRepository.findById(chassisNo.trim()).orElse(null);
        if (record != null) {
            migrateLegacyLine(record);
            normalizeLineDates(record);
        }
        return record;
    }

    @Transactional(readOnly = true)
    public WorkshopJob prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        WorkshopJob record = workshopJobRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new WorkshopJob();
            record.setChassisNo(chassisNo);
            record.setJobStatus("PENDING");
            // No blank job card — jobs come from inspection "No" items (or Add job).
            return record;
        }
        migrateLegacyLine(record);
        normalizeLineDates(record);
        return record;
    }

    public boolean isComplete(String chassisNo) {
        WorkshopJob record = findByChassisNo(chassisNo);
        return record != null && allJobsComplete(record);
    }

    public boolean canEnterYard(String chassisNo) {
        return isComplete(chassisNo);
    }

    private static boolean allJobsComplete(WorkshopJob record) {
        List<WorkshopJobLine> lines = record.getLines();
        if (lines == null || lines.isEmpty()) {
            return true;
        }
        boolean any = false;
        for (WorkshopJobLine line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            any = true;
            if (!"COMPLETE".equals(line.getJobStatus())) {
                return false;
            }
        }
        return any || record.isCompleted();
    }

    @Transactional
    public void addInspectionFailJob(String chassisNo, String inspectionItemKey, String itemTitle, String notes) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String id = chassisNo.trim();
        vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + id));

        WorkshopJob job = workshopJobRepository.findById(id).orElse(null);
        if (job == null) {
            job = new WorkshopJob();
            job.setChassisNo(id);
            job.setJobStatus("PENDING");
        } else {
            migrateLegacyLine(job);
        }

        String key = inspectionItemKey == null ? "" : inspectionItemKey.trim();
        if (!isBlank(key)) {
            for (WorkshopJobLine line : job.getLines()) {
                if (key.equals(line.getInspectionItemKey())) {
                    return;
                }
            }
        }

        WorkshopJobLine line = newLine(job);
        line.setInspectionItemKey(isBlank(key) ? null : key);
        line.setJobSummary(InspectionResults.jobSummary(itemTitle));
        if (!isBlank(notes)) {
            line.setNotes(notes.trim());
        }
        job.getLines().add(line);
        workshopJobRepository.save(job);
    }

    /**
     * Keep inspection-linked jobs only for the given item keys (items still marked No).
     * Manual jobs (no inspectionItemKey) are left unchanged.
     */
    @Transactional
    public void retainInspectionFailJobs(String chassisNo, Collection<String> keepKeys) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return;
        }
        String id = chassisNo.trim();
        WorkshopJob job = workshopJobRepository.findById(id).orElse(null);
        if (job == null) {
            return;
        }
        migrateLegacyLine(job);
        java.util.Set<String> keep = new java.util.LinkedHashSet<>();
        if (keepKeys != null) {
            for (String key : keepKeys) {
                if (!isBlank(key)) {
                    keep.add(key.trim());
                }
            }
        }
        List<WorkshopJobLine> next = new ArrayList<>();
        for (WorkshopJobLine line : job.getLines()) {
            if (line == null) {
                continue;
            }
            String key = line.getInspectionItemKey();
            if (isBlank(key) || keep.contains(key.trim())) {
                next.add(line);
            }
        }
        job.getLines().clear();
        job.getLines().addAll(next);
        if (job.getLines().isEmpty() && !job.isCompleted() && hasLegacyDetails(job)) {
            workshopJobRepository.delete(job);
            return;
        }
        workshopJobRepository.save(job);
    }

    @Transactional
    public void removeInspectionFailJob(String chassisNo, String inspectionItemKey) {
        if (isBlank(chassisNo) || isBlank(inspectionItemKey)) {
            findByChassisNo(chassisNo);
            return;
        }
        java.util.Set<String> keep = new java.util.LinkedHashSet<>();
        WorkshopJob job = findByChassisNo(chassisNo);
        if (job == null) {
            return;
        }
        String drop = inspectionItemKey.trim();
        for (WorkshopJobLine line : job.getLines()) {
            String key = line.getInspectionItemKey();
            if (!isBlank(key) && !drop.equals(key.trim())) {
                keep.add(key.trim());
            }
        }
        retainInspectionFailJobs(chassisNo, keep);
    }

    @Transactional
    public WorkshopJob save(WorkshopJob incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        WorkshopJob existing = workshopJobRepository.findById(chassisNo).orElse(null);
        if (existing == null) {
            existing = new WorkshopJob();
            existing.setChassisNo(chassisNo);
        }
        syncLines(existing, incoming.getLines());
        boolean complete = allJobsComplete(existing);
        existing.setCompleted(complete);
        if (complete) {
            existing.setJobStatus("COMPLETE");
        } else if (isBlank(existing.getJobStatus())) {
            existing.setJobStatus("PENDING");
        }
        return workshopJobRepository.save(existing);
    }

    private void syncLines(WorkshopJob existing, List<WorkshopJobLine> incoming) {
        List<WorkshopJobLine> source = incoming == null ? new ArrayList<>() : incoming;
        Map<Long, WorkshopJobLine> currentById = new LinkedHashMap<>();
        for (WorkshopJobLine line : existing.getLines()) {
            if (line.getId() != null) {
                currentById.put(line.getId(), line);
            }
        }

        List<WorkshopJobLine> next = new ArrayList<>();
        for (WorkshopJobLine incomingLine : source) {
            if (incomingLine == null || incomingLine.isEmpty()) {
                continue;
            }
            if (isBlank(incomingLine.getJobStatus())) {
                incomingLine.setJobStatus("PENDING");
            }
            if (incomingLine.getId() != null && currentById.containsKey(incomingLine.getId())) {
                WorkshopJobLine managed = currentById.get(incomingLine.getId());
                BeanUtils.copyProperties(incomingLine, managed, "id", "job");
                managed.setStartedOn(normalizeIsoDate(managed.getStartedOn()));
                managed.setCompletedOn(normalizeIsoDate(managed.getCompletedOn()));
                managed.setJob(existing);
                next.add(managed);
            } else {
                incomingLine.setId(null);
                incomingLine.setStartedOn(normalizeIsoDate(incomingLine.getStartedOn()));
                incomingLine.setCompletedOn(normalizeIsoDate(incomingLine.getCompletedOn()));
                incomingLine.setJob(existing);
                next.add(incomingLine);
            }
        }
        existing.getLines().clear();
        existing.getLines().addAll(next);
    }

    private void migrateLegacyLine(WorkshopJob record) {
        if (!record.getLines().isEmpty()) {
            return;
        }
        if (hasLegacyDetails(record)) {
            return;
        }
        WorkshopJobLine line = newLine(record);
        line.setJobStatus(defaultStatus(record.getJobStatus()));
        line.setJobSummary(record.getJobSummary());
        line.setTechnician(record.getTechnician());
        line.setPartsUsed(record.getPartsUsed());
        line.setLaborHours(record.getLaborHours());
        line.setEstimatedCost(record.getEstimatedCost());
        line.setActualCost(record.getActualCost());
        line.setStartedOn(record.getStartedOn());
        line.setCompletedOn(record.getCompletedOn());
        line.setNotes(record.getNotes());
        record.getLines().add(line);
    }

    private static WorkshopJobLine newLine(WorkshopJob job) {
        WorkshopJobLine line = new WorkshopJobLine();
        line.setJob(job);
        line.setJobStatus("PENDING");
        return line;
    }

    private static boolean hasLegacyDetails(WorkshopJob record) {
        return isBlank(record.getJobSummary())
                && isBlank(record.getTechnician())
                && isBlank(record.getPartsUsed())
                && isBlank(record.getLaborHours())
                && isBlank(record.getEstimatedCost())
                && isBlank(record.getActualCost())
                && isBlank(record.getStartedOn())
                && isBlank(record.getCompletedOn())
                && isBlank(record.getNotes())
                && (record.getJobStatus() == null || "PENDING".equals(record.getJobStatus()));
    }

    private static String defaultStatus(String status) {
        return isBlank(status) ? "PENDING" : status;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void normalizeLineDates(WorkshopJob record) {
        if (record == null || record.getLines() == null) {
            return;
        }
        for (WorkshopJobLine line : record.getLines()) {
            if (line == null) {
                continue;
            }
            line.setStartedOn(normalizeIsoDate(line.getStartedOn()));
            line.setCompletedOn(normalizeIsoDate(line.getCompletedOn()));
        }
    }

    static String normalizeIsoDate(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.matches(RegexConstants.Dates.ISO_DATE)) {
            return value;
        }
        java.util.regex.Matcher slash = RegexConstants.Dates.DAY_MONTH_YEAR_NUMERIC_PATTERN.matcher(value);
        if (slash.matches()) {
            return String.format("%s-%02d-%02d",
                    slash.group(3),
                    Integer.parseInt(slash.group(2)),
                    Integer.parseInt(slash.group(1)));
        }
        java.util.regex.Matcher yearFirst = RegexConstants.Dates.YEAR_MONTH_DAY_NUMERIC_PATTERN.matcher(value);
        if (yearFirst.matches()) {
            return String.format("%s-%02d-%02d",
                    yearFirst.group(1),
                    Integer.parseInt(yearFirst.group(2)),
                    Integer.parseInt(yearFirst.group(3)));
        }
        return value;
    }
}
