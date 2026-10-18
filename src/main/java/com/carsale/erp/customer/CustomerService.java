package com.carsale.erp.customer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.carsale.erp.preparationpipeline.model.PreviousOwner;
import com.carsale.erp.preparationpipeline.repository.PreviousOwnerRepository;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    static final String BUYER = "buyer";
    static final String OWNER = "owner";
    static final String PERSONAL = "personal";

    private final SaleListingRepository saleListingRepository;
    private final PreviousOwnerRepository previousOwnerRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CustomerRecord> list(String query) {
        Map<String, Vehicle> vehicles = new LinkedHashMap<String, Vehicle>();
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            if (vehicle.getChassisNo() != null) {
                vehicles.put(vehicle.getChassisNo(), vehicle);
            }
        }

        List<CustomerRecord> rows = new ArrayList<CustomerRecord>();
        for (Customer customer : customerRepository.findAll()) {
            if (!hasUploadedIdentity(
                    customer.getFullName(),
                    customer.getNicNo(),
                    customer.getDocumentStoredName(),
                    customer.getBackDocumentStoredName()
            )) {
                continue;
            }
            rows.add(fromPersonal(customer, vehicles.get(customer.getChassisNo())));
        }
        for (SaleListing listing : saleListingRepository.findAll()) {
            if (!hasUploadedIdentity(
                    listing.getBuyerFullName(),
                    listing.getBuyerNicNo(),
                    listing.getBuyerDocumentStoredName(),
                    listing.getBuyerBackDocumentStoredName()
            )) {
                continue;
            }
            rows.add(fromBuyer(listing, vehicles.get(listing.getChassisNo())));
        }
        for (PreviousOwner owner : previousOwnerRepository.findAll()) {
            if (!hasUploadedIdentity(
                    owner.getFullName(),
                    owner.getNicNo(),
                    owner.getDocumentStoredName(),
                    null
            )) {
                continue;
            }
            rows.add(fromOwner(owner, vehicles.get(owner.getChassisNo())));
        }

        String needle = text(query);
        if (needle != null) {
            String lowered = needle.toLowerCase(Locale.ROOT);
            List<CustomerRecord> matched = new ArrayList<CustomerRecord>();
            for (CustomerRecord row : rows) {
                if (matches(row, lowered)) {
                    matched.add(row);
                }
            }
            rows = matched;
        }

        rows.sort(new Comparator<CustomerRecord>() {
            @Override
            public int compare(CustomerRecord left, CustomerRecord right) {
                if (left.isAwaitingVehicle() != right.isAwaitingVehicle()) {
                    return left.isAwaitingVehicle() ? -1 : 1;
                }
                int byName = display(left.getFullName()).compareToIgnoreCase(display(right.getFullName()));
                if (byName != 0) {
                    return byName;
                }
                int byChassis = display(left.getChassisNo()).compareToIgnoreCase(display(right.getChassisNo()));
                if (byChassis != 0) {
                    return byChassis;
                }
                return display(left.getRoleKey()).compareToIgnoreCase(display(right.getRoleKey()));
            }
        });
        return rows;
    }

    @Transactional(readOnly = true)
    public CustomerRecord find(String role, String chassisNo) {
        String key = text(chassisNo);
        String roleKey = text(role);
        if (key == null || roleKey == null) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(key).orElse(null);
        if (BUYER.equalsIgnoreCase(roleKey)) {
            SaleListing listing = saleListingRepository.findById(key).orElse(null);
            if (listing == null || !hasUploadedIdentity(
                    listing.getBuyerFullName(),
                    listing.getBuyerNicNo(),
                    listing.getBuyerDocumentStoredName(),
                    listing.getBuyerBackDocumentStoredName()
            )) {
                return null;
            }
            return fromBuyer(listing, vehicle);
        }
        if (OWNER.equalsIgnoreCase(roleKey)) {
            PreviousOwner owner = previousOwnerRepository.findById(key).orElse(null);
            if (owner == null || !hasUploadedIdentity(
                    owner.getFullName(),
                    owner.getNicNo(),
                    owner.getDocumentStoredName(),
                    null
            )) {
                return null;
            }
            return fromOwner(owner, vehicle);
        }
        return null;
    }

    @Transactional
    public Customer savePersonal(Customer incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Enter the customer name or NIC number.");
        }
        String name = text(incoming.getFullName());
        String nic = text(incoming.getNicNo());
        if (name == null && nic == null) {
            throw new IllegalArgumentException("Enter the customer name or NIC number.");
        }
        Customer target = new Customer();
        if (incoming.getId() != null) {
            Customer existing = customerRepository.findById(incoming.getId()).orElse(null);
            if (existing != null) {
                target = existing;
            }
        }
        target.setFullName(name);
        target.setNicNo(nic);
        target.setAddress(text(incoming.getAddress()));
        target.setDateOfBirth(text(incoming.getDateOfBirth()));
        target.setGender(text(incoming.getGender()));
        target.setOcrText(incoming.getOcrText());
        if (text(incoming.getDocumentStoredName()) != null) {
            target.setDocumentOriginalName(text(incoming.getDocumentOriginalName()));
            target.setDocumentStoredName(text(incoming.getDocumentStoredName()));
            target.setDocumentContentType(text(incoming.getDocumentContentType()));
        }
        target.setBackOcrText(incoming.getBackOcrText());
        if (text(incoming.getBackDocumentStoredName()) != null) {
            target.setBackDocumentOriginalName(text(incoming.getBackDocumentOriginalName()));
            target.setBackDocumentStoredName(text(incoming.getBackDocumentStoredName()));
            target.setBackDocumentContentType(text(incoming.getBackDocumentContentType()));
        }
        return customerRepository.save(target);
    }

    @Transactional(readOnly = true)
    public CustomerRecord findPersonal(Long id) {
        if (id == null) {
            return null;
        }
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer == null || !hasUploadedIdentity(
                customer.getFullName(),
                customer.getNicNo(),
                customer.getDocumentStoredName(),
                customer.getBackDocumentStoredName()
        )) {
            return null;
        }
        Vehicle vehicle = text(customer.getChassisNo()) == null
                ? null
                : vehicleRepository.findById(customer.getChassisNo()).orElse(null);
        return fromPersonal(customer, vehicle);
    }

    static boolean hasUploadedIdentity(String name, String nic, String frontStoredName, String backStoredName) {
        return text(name) != null
                || text(nic) != null
                || text(frontStoredName) != null
                || text(backStoredName) != null;
    }

    static boolean matches(CustomerRecord row, String loweredQuery) {
        return contains(row.getFullName(), loweredQuery)
                || contains(row.getNicNo(), loweredQuery)
                || contains(row.getChassisNo(), loweredQuery)
                || contains(row.getVehicleName(), loweredQuery)
                || contains(row.getStockNo(), loweredQuery)
                || contains(row.getRoleLabel(), loweredQuery);
    }

    private static CustomerRecord fromPersonal(Customer customer, Vehicle vehicle) {
        boolean awaiting = text(customer.getChassisNo()) == null;
        return new CustomerRecord(
                PERSONAL,
                "Personal import",
                text(customer.getChassisNo()),
                text(customer.getFullName()),
                text(customer.getNicNo()),
                text(customer.getDateOfBirth()),
                text(customer.getGender()),
                text(customer.getAddress()),
                awaiting ? null : vehicleName(vehicle, customer.getChassisNo()),
                vehicle == null ? null : text(vehicle.getStockNo()),
                text(customer.getDocumentStoredName()),
                text(customer.getDocumentOriginalName()),
                text(customer.getBackDocumentStoredName()),
                text(customer.getBackDocumentOriginalName()),
                "/customers/documents/",
                customer.getId(),
                awaiting
        );
    }

    private static CustomerRecord fromBuyer(SaleListing listing, Vehicle vehicle) {
        return new CustomerRecord(
                BUYER,
                "Buyer",
                listing.getChassisNo(),
                text(listing.getBuyerFullName()),
                text(listing.getBuyerNicNo()),
                text(listing.getBuyerDateOfBirth()),
                text(listing.getBuyerGender()),
                text(listing.getBuyerAddress()),
                vehicleName(vehicle, listing.getChassisNo()),
                vehicle == null ? null : text(vehicle.getStockNo()),
                text(listing.getBuyerDocumentStoredName()),
                text(listing.getBuyerDocumentOriginalName()),
                text(listing.getBuyerBackDocumentStoredName()),
                text(listing.getBuyerBackDocumentOriginalName()),
                "/sell/documents/",
                null,
                false
        );
    }

    private static CustomerRecord fromOwner(PreviousOwner owner, Vehicle vehicle) {
        return new CustomerRecord(
                OWNER,
                "Previous owner",
                owner.getChassisNo(),
                text(owner.getFullName()),
                text(owner.getNicNo()),
                text(owner.getDateOfBirth()),
                text(owner.getGender()),
                text(owner.getAddress()),
                vehicleName(vehicle, owner.getChassisNo()),
                vehicle == null ? null : text(vehicle.getStockNo()),
                text(owner.getDocumentStoredName()),
                text(owner.getDocumentOriginalName()),
                null,
                null,
                "/registered/documents/",
                null,
                false
        );
    }

    private static String vehicleName(Vehicle vehicle, String chassisNo) {
        if (vehicle == null) {
            return chassisNo;
        }
        String make = text(vehicle.getMake());
        String model = text(vehicle.getModel());
        if (make != null && model != null) {
            if (model.toLowerCase(Locale.ROOT).startsWith(make.toLowerCase(Locale.ROOT))) {
                return model;
            }
            return make + " " + model;
        }
        if (model != null) {
            return model;
        }
        if (make != null) {
            return make;
        }
        return chassisNo;
    }

    private static boolean contains(String value, String loweredQuery) {
        String cleaned = text(value);
        return cleaned != null && cleaned.toLowerCase(Locale.ROOT).contains(loweredQuery);
    }

    private static String display(String value) {
        String cleaned = text(value);
        return cleaned == null ? "" : cleaned;
    }

    private static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class CustomerRecord {
        private final String roleKey;
        private final String roleLabel;
        private final String chassisNo;
        private final String fullName;
        private final String nicNo;
        private final String dateOfBirth;
        private final String gender;
        private final String address;
        private final String vehicleName;
        private final String stockNo;
        private final String frontStoredName;
        private final String frontOriginalName;
        private final String backStoredName;
        private final String backOriginalName;
        private final String documentBase;
        private final Long id;
        private final boolean awaitingVehicle;

        public boolean isFrontPdf() {
            return isPdf(frontStoredName);
        }

        public boolean isBackPdf() {
            return isPdf(backStoredName);
        }

        private static boolean isPdf(String storedName) {
            return storedName != null && storedName.toLowerCase(Locale.ROOT).endsWith(".pdf");
        }
    }
}
