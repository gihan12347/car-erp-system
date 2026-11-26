package com.carsale.erp.shared.vehicle;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.report.ReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleExtraCostService {

    private final VehicleRepository vehicleRepository;
    private final VehicleExtraCostRepository extraCostRepository;

    public List<VehicleExtraCost> findByChassisNo(String chassisNo) {
        if (isBlank(chassisNo)) {
            return java.util.Collections.emptyList();
        }
        return extraCostRepository.findByChassisNoOrderByCostDateAscIdAsc(chassisNo.trim());
    }

    public static BigDecimal total(List<VehicleExtraCost> costs) {
        if (costs == null || costs.isEmpty()) {
            return null;
        }
        BigDecimal sum = null;
        for (VehicleExtraCost cost : costs) {
            if (cost == null) {
                continue;
            }
            BigDecimal amount = ReportService.parseMoney(cost.getAmount());
            if (amount == null) {
                continue;
            }
            sum = sum == null ? amount : sum.add(amount);
        }
        return sum;
    }

    @Transactional
    public void add(String chassisNo, String costDate, String reason, String cost) {
        if (isBlank(chassisNo)) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassis = chassisNo.trim();
        vehicleRepository.findById(chassis)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassis));

        String date = requireDate(costDate);
        String why = requireReason(reason);
        String amount = requireCost(cost);

        VehicleExtraCost record = new VehicleExtraCost();
        record.setChassisNo(chassis);
        record.setCostDate(date);
        record.setReason(why);
        record.setAmount(amount);
        extraCostRepository.save(record);
    }

    static String requireDate(String raw) {
        if (isBlank(raw)) {
            throw new IllegalArgumentException("Enter the date.");
        }
        LocalDate date = parseDate(raw.trim());
        if (date == null) {
            throw new IllegalArgumentException("Enter the date.");
        }
        return date.toString();
    }

    private static final Pattern SLASH_DATE = Pattern.compile("^(\\d{4})[/.-](\\d{1,2})[/.-](\\d{1,2})$");

    static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            // The summary date field posts YYYY/MM/DD.
        }
        Matcher match = SLASH_DATE.matcher(text);
        if (!match.matches()) {
            return null;
        }
        try {
            return LocalDate.of(
                    Integer.parseInt(match.group(1)),
                    Integer.parseInt(match.group(2)),
                    Integer.parseInt(match.group(3))
            );
        } catch (DateTimeException ex) {
            return null;
        }
    }

    static String requireReason(String raw) {
        if (isBlank(raw)) {
            throw new IllegalArgumentException("Enter the reason.");
        }
        String reason = raw.trim();
        if (reason.length() > 200) {
            throw new IllegalArgumentException("Reason must be 200 characters or less.");
        }
        return reason;
    }

    static String requireCost(String raw) {
        if (isBlank(raw)) {
            throw new IllegalArgumentException("Enter the cost.");
        }
        String amount = raw.trim();
        if (amount.length() > 40) {
            amount = amount.substring(0, 40);
        }
        BigDecimal parsed = ReportService.parseMoney(amount);
        if (parsed == null || parsed.signum() <= 0) {
            throw new IllegalArgumentException("Enter a cost greater than zero.");
        }
        return amount;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
