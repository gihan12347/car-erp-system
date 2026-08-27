package com.carsale.erp.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.WorkshopJob;
import com.carsale.erp.entity.WorkshopJobLine;
import com.carsale.erp.repository.VehicleRepository;
import com.carsale.erp.repository.WorkshopJobRepository;

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
        } else {
            migrateLegacyLine(record);
        }
        if (record.getLines().isEmpty()) {
            record.getLines().add(newLine(record));
        }
        return record;
    }

    public boolean isComplete(String chassisNo) {
        WorkshopJob record = findByChassisNo(chassisNo);
        return record != null && record.isCompleted();
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
        existing.setCompleted(incoming.isCompleted());
        if (incoming.isCompleted()) {
            existing.setJobStatus("COMPLETE");
        } else if (isBlank(existing.getJobStatus())) {
            existing.setJobStatus("PENDING");
        }
        syncLines(existing, incoming.getLines());
        return workshopJobRepository.save(existing);
    }

    private void syncLines(WorkshopJob existing, List<WorkshopJobLine> incoming) {
        List<WorkshopJobLine> source = incoming == null ? new ArrayList<WorkshopJobLine>() : incoming;
        Map<Long, WorkshopJobLine> currentById = new LinkedHashMap<Long, WorkshopJobLine>();
        for (WorkshopJobLine line : existing.getLines()) {
            if (line.getId() != null) {
                currentById.put(line.getId(), line);
            }
        }

        List<WorkshopJobLine> next = new ArrayList<WorkshopJobLine>();
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
                managed.setJob(existing);
                next.add(managed);
            } else {
                incomingLine.setId(null);
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
        if (!hasLegacyDetails(record)) {
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
        return !isBlank(record.getJobSummary())
                || !isBlank(record.getTechnician())
                || !isBlank(record.getPartsUsed())
                || !isBlank(record.getLaborHours())
                || !isBlank(record.getEstimatedCost())
                || !isBlank(record.getActualCost())
                || !isBlank(record.getStartedOn())
                || !isBlank(record.getCompletedOn())
                || !isBlank(record.getNotes())
                || (record.getJobStatus() != null && !"PENDING".equals(record.getJobStatus()));
    }

    private static String defaultStatus(String status) {
        return isBlank(status) ? "PENDING" : status;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
