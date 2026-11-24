package com.carsale.erp.preparationpipeline.service;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.preparationpipeline.dto.YardLane;
import com.carsale.erp.preparationpipeline.dto.YardVehicleCard;
import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.repository.YardRecordRepository;
import com.carsale.erp.preparationpipeline.util.Yard;
import com.carsale.erp.preparationpipeline.util.YardAllocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.vehicle.VehicleService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class YardService {

    private final VehicleRepository vehicleRepository;
    private final YardRecordRepository yardRecordRepository;
    private final YardBayService yardBayService;
    private final VehiclePhotoService vehiclePhotoService;

    public YardRecord findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, yardRecordRepository, null);
    }

    public YardRecord prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        YardRecord record = yardRecordRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new YardRecord();
            record.setChassisNo(chassisNo);
            record.setInspectionStatus("PENDING");
        }
        return record;
    }

    public boolean isComplete(String chassisNo) {
        YardRecord record = findByChassisNo(chassisNo);
        return Yard.isAssigned(record);
    }

    @Transactional
    public YardRecord save(YardRecord incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        if (Yard.isBlank(incoming.getInspectionStatus())) {
            incoming.setInspectionStatus("PENDING");
        }

        if (!Yard.isBlank(incoming.getBayNo())) {
            YardBay yard = yardBayService.requireAssignable(incoming.getBayNo(), chassisNo);
            incoming.setBayNo(yard.getBayCode());
            incoming.setYardSection(yard.getLocation());
        }

        incoming.setCompleted(Yard.isAssigned(incoming));

        YardRecord existing = yardRecordRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            existing.setBayNo(incoming.getBayNo());
            existing.setYardSection(incoming.getYardSection());
            existing.setArrivalDate(incoming.getArrivalDate());
            existing.setCompleted(incoming.isCompleted());
            existing.setChassisNo(chassisNo);
            return yardRecordRepository.save(existing);
        }
        incoming.setChassisNo(chassisNo);
        return yardRecordRepository.save(incoming);
    }

    @Transactional
    public int assignVehicles(String yardCode, List<String> chassisNos) {
        YardBay yard = yardBayService.lockByCode(yardCode);
        if (yard == null || !yard.isActive()) {
            throw new IllegalArgumentException("Unknown yard.");
        }
        List<String> unique = uniqueChassis(chassisNos);
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("Select at least one vehicle.");
        }
        List<String> toPlace = new ArrayList<>();
        for (String chassisNo : unique) {
            Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
            if (vehicle == null) {
                throw new IllegalArgumentException("Vehicle not found for chassis " + chassisNo + ".");
            }
            if (yardBayService.isMovedToSale(chassisNo)) {
                throw new IllegalArgumentException(chassisNo + " is already assigned to a sale.");
            }
            YardRecord existing = yardRecordRepository.findById(chassisNo).orElse(null);
            if (existing != null
                    && existing.getBayNo() != null
                    && existing.getBayNo().equalsIgnoreCase(yard.getBayCode())) {
                continue;
            }
            toPlace.add(chassisNo);
        }
        if (toPlace.isEmpty()) {
            throw new IllegalArgumentException("Those vehicles are already in this yard.");
        }
        YardAllocation.requireSpace(yard.getRemaining(), toPlace.size());
        for (String chassisNo : toPlace) {
            assignOne(chassisNo, yard.getBayCode(), false);
        }
        return toPlace.size();
    }

    @Transactional
    public void removeFromYard(String yardCode, String chassisNo) {
        YardRecord existing = requireInYard(yardCode, chassisNo);
        existing.setBayNo(null);
        existing.setYardSection(null);
        existing.setCompleted(false);
        yardRecordRepository.save(existing);
    }

    @Transactional
    public void transferVehicle(String fromYardCode, String chassisNo, String destYardCode) {
        requireInYard(fromYardCode, chassisNo);
        if (destYardCode == null || destYardCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Choose a destination yard.");
        }
        if (destYardCode.trim().equalsIgnoreCase(fromYardCode == null ? "" : fromYardCode.trim())) {
            throw new IllegalArgumentException("Vehicle is already in that yard.");
        }
        if (yardBayService.lockByCode(destYardCode.trim()) == null) {
            throw new IllegalArgumentException("Unknown yard.");
        }
        assignOne(chassisNo, destYardCode.trim(), true);
    }

    public List<YardVehicleCard> listAssignable() {
        Set<String> inYard = presentChassis();
        List<YardVehicleCard> cards = new ArrayList<>();
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            if (vehicle == null || Yard.isBlank(vehicle.getChassisNo())) {
                continue;
            }
            if (inYard.contains(YardAllocation.normalize(vehicle.getChassisNo()))) {
                continue;
            }
            if (yardBayService.isMovedToSale(vehicle.getChassisNo())) {
                continue;
            }
            cards.add(cardFor(vehicle, null, findByChassisNo(vehicle.getChassisNo())));
        }
        Collections.sort(cards, newestFirst());
        return cards;
    }

    public List<YardVehicleCard> listAllocated() {
        List<YardVehicleCard> cards = new ArrayList<>();
        for (YardLane lane : allocationLanes()) {
            if (lane.isUnassigned()) {
                continue;
            }
            cards.addAll(lane.getCards());
        }
        Collections.sort(cards, newestFirst());
        return cards;
    }

    public VehiclePhoto coverPhoto(String chassisNo) {
        List<VehiclePhoto> photos = vehiclePhotoService.list(chassisNo);
        return photos.isEmpty() ? null : photos.get(0);
    }

    public List<YardLane> allocationLanes() {
        List<YardLane> lanes = new ArrayList<>();
        for (YardBay yard : yardBayService.listAll()) {
            lanes.add(new YardLane(yard, cardsFor(yardBayService.listPresentInYard(yard.getBayCode()), yard)));
        }
        lanes.add(new YardLane(null, cardsFor(yardBayService.listUnassignedPresent(), null)));
        return lanes;
    }

    private List<YardVehicleCard> cardsFor(List<YardRecord> records, YardBay yard) {
        List<YardVehicleCard> cards = new ArrayList<>();
        if (records == null) {
            return cards;
        }
        for (YardRecord record : records) {
            if (record == null || Yard.isBlank(record.getChassisNo())) {
                continue;
            }
            Vehicle vehicle = vehicleRepository.findById(record.getChassisNo()).orElse(null);
            if (vehicle == null) {
                continue;
            }
            cards.add(cardFor(vehicle, yard, record));
        }
        Collections.sort(cards, newestFirst());
        return cards;
    }

    private void assignOne(String chassisNo, String yardCode, boolean keepArrival) {
        YardRecord incoming = prepareForm(chassisNo);
        if (incoming == null) {
            throw new IllegalArgumentException("Vehicle not found for chassis " + chassisNo + ".");
        }
        incoming.setBayNo(yardCode);
        if (!keepArrival || Yard.isBlank(incoming.getArrivalDate())) {
            incoming.setArrivalDate(LocalDate.now().toString());
        }
        save(incoming);
    }

    private YardRecord requireInYard(String yardCode, String chassisNo) {
        YardRecord existing = findByChassisNo(chassisNo);
        String code = yardCode == null ? "" : yardCode.trim();
        if (existing == null
                || Yard.isBlank(existing.getBayNo())
                || !existing.getBayNo().equalsIgnoreCase(code)) {
            throw new IllegalArgumentException("Vehicle is not in this yard.");
        }
        return existing;
    }

    private List<String> uniqueChassis(List<String> chassisNos) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (chassisNos == null) {
            return new ArrayList<>();
        }
        for (String chassisNo : chassisNos) {
            if (Yard.isBlank(chassisNo)) {
                continue;
            }
            unique.add(chassisNo.trim());
        }
        return new ArrayList<>(unique);
    }

    private Set<String> presentChassis() {
        Set<String> ids = new HashSet<>();
        for (YardBay yard : yardBayService.listAll()) {
            for (YardRecord record : yardBayService.listPresentInYard(yard.getBayCode())) {
                if (record != null && !Yard.isBlank(record.getChassisNo())) {
                    ids.add(YardAllocation.normalize(record.getChassisNo()));
                }
            }
        }
        return ids;
    }

    private YardVehicleCard cardFor(Vehicle vehicle, YardBay yard, YardRecord record) {
        String yardCode = yard == null ? null : yard.getBayCode();
        String yardTitle = yard == null ? "Unassigned" : Yard.yardTitle(yard);
        String href = yard == null
                ? "/vehicle/" + PipelineStageUtils.encode(vehicle.getChassisNo())
                : "/yards/" + PipelineStageUtils.encode(yard.getBayCode())
                + "/" + PipelineStageUtils.encode(vehicle.getChassisNo());
        String arrivalDate = record == null ? null : record.getArrivalDate();
        return new YardVehicleCard(
                vehicle,
                coverPhoto(vehicle.getChassisNo()),
                yardCode,
                yardTitle,
                href,
                arrivalDate
        );
    }

    private Comparator<YardVehicleCard> newestFirst() {
        return new Comparator<YardVehicleCard>() {
            @Override
            public int compare(YardVehicleCard left, YardVehicleCard right) {
                return VehicleService.compareNewestFirst(left.getVehicle(), right.getVehicle());
            }
        };
    }
}
