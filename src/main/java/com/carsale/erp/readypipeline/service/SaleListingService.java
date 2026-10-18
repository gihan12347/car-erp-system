package com.carsale.erp.readypipeline.service;

import com.carsale.erp.customer.Customer;
import com.carsale.erp.customer.CustomerRepository;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.shared.dto.SaleProgress;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.service.SkippedStageService;
import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.vehicle.VehicleStage;

@Service
@RequiredArgsConstructor
public class SaleListingService {

    private final VehicleRepository vehicleRepository;
    private final SaleListingRepository saleListingRepository;
    private final CustomerRepository customerRepository;
    private final SaleLocationService saleLocationService;
    private final VehicleRegistrationService vehicleRegistrationService;
    private final SkippedStageService skippedStageService;

    public SaleListing findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, saleListingRepository, null);
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
        String listedOn = SaleListing.toIsoDate(record.getListedOn());
        if (listedOn != null) {
            record.setListedOn(listedOn);
        }
        String dateOfBirth = SaleListing.toIsoDate(record.getBuyerDateOfBirth());
        if (dateOfBirth != null) {
            record.setBuyerDateOfBirth(dateOfBirth);
        }
        rememberCustomerIdentity(record);
        return record;
    }

    @Transactional
    public SaleListing listingForView(String chassisNo) {
        SaleListing listing = findByChassisNo(chassisNo);
        Customer customer = customerRepository.findFirstByChassisNo(chassisNo).orElse(null);
        if (!hasCustomerIdentity(customer)) {
            return listing;
        }
        if (listing == null) {
            listing = new SaleListing();
            listing.setChassisNo(chassisNo);
        }
        if (copyBuyerIdentity(listing, customer)) {
            listing = saleListingRepository.save(listing);
        }
        return listing;
    }

    private void rememberCustomerIdentity(SaleListing record) {
        if (record == null || isBlank(record.getChassisNo())) {
            return;
        }
        Customer customer = customerRepository.findFirstByChassisNo(record.getChassisNo().trim()).orElse(null);
        if (!hasCustomerIdentity(customer)) {
            return;
        }
        if (copyBuyerIdentity(record, customer)) {
            saleListingRepository.save(record);
        }
    }

    static boolean copyBuyerIdentity(SaleListing listing, Customer customer) {
        if (listing == null || !hasCustomerIdentity(customer)) {
            return false;
        }
        boolean changed = false;
        changed = fill(listing.getBuyerFullName(), customer.getFullName(), listing::setBuyerFullName) || changed;
        changed = fill(listing.getBuyerNicNo(), customer.getNicNo(), listing::setBuyerNicNo) || changed;
        changed = fill(listing.getBuyerAddress(), customer.getAddress(), listing::setBuyerAddress) || changed;
        changed = fill(listing.getBuyerGender(), customer.getGender(), listing::setBuyerGender) || changed;
        String dob = SaleListing.toIsoDate(customer.getDateOfBirth());
        if (dob == null) {
            dob = textOrNull(customer.getDateOfBirth());
        }
        changed = fill(listing.getBuyerDateOfBirth(), dob, listing::setBuyerDateOfBirth) || changed;
        changed = fill(listing.getBuyerOcrText(), customer.getOcrText(), listing::setBuyerOcrText) || changed;
        changed = fill(listing.getBuyerDocumentOriginalName(), customer.getDocumentOriginalName(), listing::setBuyerDocumentOriginalName) || changed;
        changed = fill(listing.getBuyerDocumentStoredName(), customer.getDocumentStoredName(), listing::setBuyerDocumentStoredName) || changed;
        changed = fill(listing.getBuyerDocumentContentType(), customer.getDocumentContentType(), listing::setBuyerDocumentContentType) || changed;
        changed = fill(listing.getBuyerBackOcrText(), customer.getBackOcrText(), listing::setBuyerBackOcrText) || changed;
        changed = fill(listing.getBuyerBackDocumentOriginalName(), customer.getBackDocumentOriginalName(), listing::setBuyerBackDocumentOriginalName) || changed;
        changed = fill(listing.getBuyerBackDocumentStoredName(), customer.getBackDocumentStoredName(), listing::setBuyerBackDocumentStoredName) || changed;
        changed = fill(listing.getBuyerBackDocumentContentType(), customer.getBackDocumentContentType(), listing::setBuyerBackDocumentContentType) || changed;
        return changed;
    }

    private static boolean hasCustomerIdentity(Customer customer) {
        if (customer == null) {
            return false;
        }
        return !isBlank(customer.getFullName())
                || !isBlank(customer.getNicNo())
                || !isBlank(customer.getDocumentStoredName())
                || !isBlank(customer.getBackDocumentStoredName());
    }

    private static boolean fill(String current, String incoming, java.util.function.Consumer<String> setter) {
        if (!isBlank(current) || isBlank(incoming)) {
            return false;
        }
        setter.accept(incoming.trim());
        return true;
    }

    private static String textOrNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public boolean isAssignedToSale(String chassisNo) {
        SaleListing record = findByChassisNo(chassisNo);
        return record != null && !isBlank(record.getSaleCode());
    }

    public SaleProgress progressFor(String chassisNo) {
        SaleListing record = findByChassisNo(chassisNo);
        boolean listed = record != null && record.isListed() && !record.isSold();
        boolean sold = record != null && record.isSold();
        boolean saleRecorded = record != null
                && !isBlank(record.getAskingPrice())
                && !isBlank(record.getListedOn())
                && (!isBlank(record.getBuyerFullName()) || !isBlank(record.getBuyerNicNo()));
        boolean registrationReady = vehicleRegistrationService.isComplete(chassisNo);
        Vehicle vehicle = chassisNo == null || chassisNo.trim().isEmpty()
                ? null
                : vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return new SaleProgress(
                true,
                listed || sold || saleRecorded,
                registrationReady,
                sold,
                skippedStageService.matching(vehicle, PipelineStageUtils.READY_STAGE_KEYS)
        );
    }

    public boolean isStageSatisfied(String chassisNo, String stageKey) {
        return !progressFor(chassisNo).isStageComplete(stageKey);
    }

    @Transactional
    public void skipStage(String chassisNo, String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage == null || !PipelineStageUtils.READY_STAGE_KEYS.contains(stage.getStageKey())) {
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

        if (isBlank(incoming.getAskingPrice())) {
            throw new IllegalArgumentException("Enter the selling price.");
        }
        incoming.setAskingPrice(incoming.getAskingPrice().trim());
        String sellingDate = SaleListing.toIsoDate(incoming.getListedOn());
        if (sellingDate == null) {
            throw new IllegalArgumentException("Enter the selling date.");
        }
        incoming.setListedOn(sellingDate);
        if (!isBlank(incoming.getBuyerDateOfBirth()) && SaleListing.toIsoDate(incoming.getBuyerDateOfBirth()) == null) {
            throw new IllegalArgumentException("Enter the buyer's date of birth as a date.");
        }
        incoming.setBuyerDateOfBirth(SaleListing.toIsoDate(incoming.getBuyerDateOfBirth()));
        incoming.setBuyerFullName(trimToNull(incoming.getBuyerFullName()));
        incoming.setBuyerNicNo(trimToNull(incoming.getBuyerNicNo()));
        incoming.setBuyerAddress(trimToNull(incoming.getBuyerAddress()));
        incoming.setBuyerGender(trimToNull(incoming.getBuyerGender()));
        if (incoming.getBuyerFullName() == null && incoming.getBuyerNicNo() == null) {
            throw new IllegalArgumentException("Enter the buyer name or NIC number.");
        }

        SaleListing existing = saleListingRepository.findById(chassisNo).orElse(null);
        if (incoming.isSold()) {
            incoming.setListed(false);
        } else if (existing != null) {
            incoming.setListed(existing.isListed());
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

        if (existing != null) {
            incoming.setAdvertisedPrice(existing.getAdvertisedPrice());
            incoming.setSaleCondition(existing.getSaleCondition());
            incoming.setListingNotes(existing.getListingNotes());
            incoming.setNotes(existing.getNotes());
        }

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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
