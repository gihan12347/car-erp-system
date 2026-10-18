package com.carsale.erp.customer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalImportLinker {

    static final String SESSION_KEY = "personalImportCustomerId";

    private final HttpServletRequest request;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    public void remember(Long customerId) {
        HttpSession session = request.getSession();
        if (customerId == null) {
            session.removeAttribute(SESSION_KEY);
            return;
        }
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null || hasChassis(customer)) {
            session.removeAttribute(SESSION_KEY);
            return;
        }
        session.setAttribute(SESSION_KEY, customer.getId());
    }

    public void clear() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    public Customer current() {
        Long id = pendingId();
        if (id == null) {
            return null;
        }
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer == null || hasChassis(customer)) {
            clear();
            return null;
        }
        return customer;
    }

    @Transactional
    public void linkPending(String chassisNo) {
        Long id = pendingId();
        if (id == null || chassisNo == null || chassisNo.trim().isEmpty()) {
            return;
        }
        String chassis = chassisNo.trim();
        Vehicle vehicle = vehicleRepository.findById(chassis).orElse(null);
        if (vehicle == null) {
            return;
        }
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer == null) {
            clear();
            return;
        }
        if (hasChassis(customer) && !chassis.equals(customer.getChassisNo().trim())) {
            clear();
            return;
        }
        customer.setChassisNo(chassis);
        customerRepository.save(customer);
        if (vehicle.getIntake() == null) {
            vehicle.setIntake(VehicleIntake.PERSONAL_IMPORT);
            vehicleRepository.save(vehicle);
        }
        clear();
    }

    private Long pendingId() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return Long.valueOf(((Integer) value).longValue());
        }
        return null;
    }

    private static boolean hasChassis(Customer customer) {
        return customer.getChassisNo() != null && !customer.getChassisNo().trim().isEmpty();
    }
}
