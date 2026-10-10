package com.carsale.erp.readypipeline.service;

import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.SkippedStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.vehicle.VehicleStage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class SaleListingService {

    private static final List<String> READY_STAGE_KEYS = Arrays.asList(
            FlowStage.DETAILS.getStageKey(),
            FlowStage.LISTING.getStageKey(),
            FlowStage.REGISTRATION.getStageKey()
    );

    private final VehicleRepository vehicleRepository;
    private final SaleListingRepository saleListingRepository;
    private final SaleLocationService saleLocationService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final SkippedStageService skippedStageService;

    public SaleListingService(
            VehicleRepository vehicleRepository,
            SaleListingRepository saleListingRepository,
            SaleLocationService saleLocationService,
            VehicleRegistrationService vehicleRegistrationService,
            SkippedStageService skippedStageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.saleListingRepository = saleListingRepository;
        this.saleLocationService = saleLocationService;
        this.vehicleRegistrationService = vehicleRegistrationService;
        this.skippedStageService = skippedStageService;
    }

    public SaleListing findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return saleListingRepository.findById(chassisNo.trim()).orElse(null);
    }

    public SaleListing prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        SaleListing record = saleListingRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new SaleListing();
            record.setChassisNo(chassisNo);
            record.setSold(vehicle.getStage() == VehicleStage.SOLD);
        }
        return record;
    }

    public boolean isAssignedToSale(String chassisNo) {
        SaleListing record = findByChassisNo(chassisNo);
        return record != null && !isBlank(record.getSaleCode());
    }

    public boolean isListed(String chassisNo) {
        SaleListing record = findByChassisNo(chassisNo);
        return record != null && record.isListed() && !record.isSold();
    }

    public boolean isAssignmentComplete(String chassisNo) {
        SaleListing record = findByChassisNo(chassisNo);
        if (record == null) {
            return false;
        }
        if (record.isAssignmentComplete()) {
            return true;
        }
        return !isBlank(record.getSaleCode()) && !isBlank(record.getListedOn());
    }

    public SaleProgress progressFor(String chassisNo) {
        SaleListing record = findByChassisNo(chassisNo);
        boolean listed = record != null && record.isListed() && !record.isSold();
        boolean sold = record != null && record.isSold();
        boolean registrationReady = vehicleRegistrationService.isComplete(chassisNo);
        Vehicle vehicle = chassisNo == null || chassisNo.trim().isEmpty()
                ? null
                : vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return new SaleProgress(
                true,
                listed || sold,
                registrationReady,
                sold,
                skippedStageService.matching(vehicle, READY_STAGE_KEYS)
        );
    }

    public boolean isStageSatisfied(String chassisNo, String stageKey) {
        return progressFor(chassisNo).isStageComplete(stageKey);
    }

    @Transactional
    public void skipStage(String chassisNo, String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage == null || !READY_STAGE_KEYS.contains(stage.getStageKey())) {
            throw new IllegalArgumentException("That stage cannot be skipped.");
        }
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        if (progressFor(vehicle.getChassisNo()).isStageFilled(stage.getStageKey())) {
            return;
        }
        skippedStageService.add(vehicle.getChassisNo(), stage.getStageKey());
    }

    @Transactional
    public void saveAssignment(SaleListing incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        if (incoming.isAssignmentComplete()
                && (isBlank(incoming.getSaleCode()) || isBlank(incoming.getListedOn()))) {
            throw new IllegalArgumentException("Select a date and an available sale to complete this step.");
        }
        if (incoming.isAssignmentComplete() && isBlank(incoming.getAskingPrice())) {
            throw new IllegalArgumentException("Enter a selling price.");
        }

        SaleListing existing = saleListingRepository.findById(chassisNo).orElse(null);
        SaleListing target = existing != null ? existing : new SaleListing();
        target.setChassisNo(chassisNo);
        target.setListedOn(incoming.getListedOn());
        target.setAskingPrice(isBlank(incoming.getAskingPrice()) ? null : incoming.getAskingPrice().trim());
        target.setAssignmentComplete(incoming.isAssignmentComplete());

        if (!isBlank(incoming.getSaleCode())) {
            SaleLocation sale = saleLocationService.requireAssignable(incoming.getSaleCode(), chassisNo);
            target.setSaleCode(sale.getSaleCode());
            target.setSaleLocation(sale.getLocation());
        } else {
            target.setSaleCode(null);
            target.setSaleLocation(null);
        }

        saleListingRepository.save(target);
    }

    @Transactional
    public SaleListing save(SaleListing incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        Vehicle vehicle = vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        if (incoming.isSold()) {
            incoming.setListed(false);
        } else if (incoming.isListed() && isBlank(incoming.getSaleCode())) {
            throw new IllegalArgumentException("Select a sale location before listing the vehicle.");
        }

        if (!incoming.isSold() && !isBlank(incoming.getSaleCode())) {
            SaleLocation sale = saleLocationService.requireAssignable(incoming.getSaleCode(), chassisNo);
            incoming.setSaleCode(sale.getSaleCode());
            incoming.setSaleLocation(sale.getLocation());
        } else if (!isBlank(incoming.getSaleCode())) {
            SaleLocation sale = saleLocationService.findByCode(incoming.getSaleCode());
            if (sale != null) {
                incoming.setSaleCode(sale.getSaleCode());
                incoming.setSaleLocation(sale.getLocation());
            }
        }

        SaleListing existing = saleListingRepository.findById(chassisNo).orElse(null);
        if (!isBlank(incoming.getSaleCode()) && !isBlank(incoming.getListedOn())) {
            incoming.setAssignmentComplete(true);
        } else if (existing != null && existing.isAssignmentComplete() && !isBlank(incoming.getSaleCode())) {
            incoming.setAssignmentComplete(true);
        }

        SaleListing saved;
        if (existing != null) {
            BeanUtils.copyProperties(incoming, existing);
            existing.setChassisNo(chassisNo);
            saved = saleListingRepository.save(existing);
        } else {
            incoming.setChassisNo(chassisNo);
            saved = saleListingRepository.save(incoming);
        }
        syncVehicleStage(vehicle, saved);
        return saved;
    }

    private void syncVehicleStage(Vehicle vehicle, SaleListing listing) {
        if (listing.isSold()) {
            if (vehicle.getStage() != VehicleStage.SOLD) {
                vehicle.setStage(VehicleStage.SOLD);
                vehicleRepository.save(vehicle);
            }
            return;
        }
        if (vehicle.getStage() == VehicleStage.SOLD) {
            vehicle.setStage(VehicleStage.READY);
            vehicleRepository.save(vehicle);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class SaleProgress implements com.carsale.erp.shared.pipeline.PipelineProgress {
        private final boolean detailsReady;
        private final boolean listingReady;
        private final boolean registrationReady;
        private final boolean sold;
        private final Set<String> skippedStages;

        public SaleProgress(boolean detailsReady, boolean listingReady, boolean registrationReady, boolean sold) {
            this(detailsReady, listingReady, registrationReady, sold, Collections.<String>emptySet());
        }

        public SaleProgress(
                boolean detailsReady,
                boolean listingReady,
                boolean registrationReady,
                boolean sold,
                Set<String> skippedStages
        ) {
            this.detailsReady = detailsReady;
            this.listingReady = listingReady;
            this.registrationReady = registrationReady;
            this.sold = sold;
            this.skippedStages = skippedStages == null ? Collections.<String>emptySet() : skippedStages;
        }

        public boolean isDetailsReady() {
            return detailsReady;
        }

        public boolean isListingReady() {
            return listingReady;
        }

        public boolean isRegistrationReady() {
            return registrationReady;
        }

        public boolean isSold() {
            return sold;
        }

        @Override
        public boolean hasAnyCompletedStage() {
            return completedCount() > 0;
        }

        @Override
        public int completedCount() {
            int count = 0;
            for (String key : READY_STAGE_KEYS) {
                if (isStageComplete(key)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean isPipelineCompleted() {
            for (String key : READY_STAGE_KEYS) {
                if (!isStageComplete(key)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isStageFilled(String stageKey) {
            if (FlowStage.DETAILS.getStageKey().equals(stageKey)) {
                return detailsReady;
            }
            if (FlowStage.LISTING.getStageKey().equals(stageKey)) {
                return listingReady;
            }
            return FlowStage.REGISTRATION.getStageKey().equals(stageKey) && registrationReady;
        }

        public boolean isStageSkipped(String stageKey) {
            return stageKey != null && skippedStages.contains(stageKey) && !isStageFilled(stageKey);
        }

        @Override
        public boolean isStageComplete(String stageKey) {
            return isStageFilled(stageKey) || (stageKey != null && skippedStages.contains(stageKey));
        }

        public String firstIncompleteStageKey(java.util.List<String> keys) {
            if (keys == null) {
                return null;
            }
            for (String key : keys) {
                if (!isStageComplete(key)) {
                    return key;
                }
            }
            return null;
        }
    }
}
