package com.carsale.erp.readypipeline.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import com.carsale.erp.preparationpipeline.repository.RegistrationCertificateRepository;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.vehicle.VehicleStage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SoldVehicleService {

    private final SaleListingRepository saleListingRepository;
    private final VehicleRepository vehicleRepository;
    private final RegistrationCertificateRepository registrationCertificateRepository;

    public List<SoldRow> list() {
        Map<String, SaleListing> listings = new LinkedHashMap<>();
        for (SaleListing listing : saleListingRepository.findBySoldTrue()) {
            if (listing.getChassisNo() != null && !listing.getChassisNo().trim().isEmpty()) {
                listings.put(listing.getChassisNo().trim(), listing);
            }
        }
        for (Vehicle vehicle : vehicleRepository.findByStageOrderByChassisNoAsc(VehicleStage.SOLD)) {
            listings.putIfAbsent(vehicle.getChassisNo(), null);
        }
        if (listings.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Vehicle> vehicles = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicleRepository.findAllById(listings.keySet())) {
            vehicles.put(vehicle.getChassisNo(), vehicle);
        }
        Map<String, SaleListing> storedListings = new LinkedHashMap<>();
        for (SaleListing listing : saleListingRepository.findAllById(listings.keySet())) {
            storedListings.put(listing.getChassisNo(), listing);
        }
        Map<String, RegistrationCertificate> books = new LinkedHashMap<>();
        for (RegistrationCertificate book : registrationCertificateRepository.findByChassisNoIn(listings.keySet())) {
            books.put(book.getChassisNo(), book);
        }

        List<SoldRow> rows = new ArrayList<>();
        for (String chassisNo : listings.keySet()) {
            SaleListing listing = storedListings.get(chassisNo);
            if (listing == null) {
                listing = listings.get(chassisNo);
            }
            Vehicle vehicle = vehicles.get(chassisNo);
            RegistrationCertificate book = books.get(chassisNo);
            rows.add(new SoldRow(
                    chassisNo,
                    book == null ? "" : text(book.getEngineNo()),
                    vehicleName(vehicle, book, chassisNo),
                    listing == null ? "" : text(listing.getBuyerFullName()),
                    listing == null ? "" : text(listing.getAskingPrice()),
                    listing == null ? "" : text(listing.getListedOn()),
                    location(listing)
            ));
        }
        rows.sort(Comparator
                .comparing(SoldRow::getSellingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SoldRow::getChassisNo));
        return rows;
    }

    private static String location(SaleListing listing) {
        if (listing == null) {
            return "";
        }
        if (!text(listing.getSaleLocation()).isEmpty()) {
            return listing.getSaleLocation().trim();
        }
        return text(listing.getSaleCode());
    }

    private static String vehicleName(Vehicle vehicle, RegistrationCertificate book, String chassisNo) {
        String make = vehicle != null ? text(vehicle.getMake()) : "";
        String model = vehicle != null ? text(vehicle.getModel()) : "";
        if (make.isEmpty() && book != null) {
            make = text(book.getMake());
        }
        if (model.isEmpty() && book != null) {
            model = text(book.getModel());
        }
        if (!make.isEmpty() && !model.isEmpty()) {
            return model.toLowerCase().startsWith(make.toLowerCase()) ? model : make + " " + model;
        }
        if (!model.isEmpty()) {
            return model;
        }
        if (!make.isEmpty()) {
            return make;
        }
        return chassisNo;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    @Getter
    @RequiredArgsConstructor
    public static class SoldRow {
        private final String chassisNo;
        private final String engineNo;
        private final String vehicleName;
        private final String buyerName;
        private final String sellingPrice;
        private final String sellingDate;
        private final String location;
    }
}
