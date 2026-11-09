package com.carsale.erp.shared.vehicle;

import java.math.BigDecimal;

import com.carsale.erp.customspipeline.model.CustomsDocument;
import com.carsale.erp.preparationpipeline.model.WorkshopJob;
import com.carsale.erp.preparationpipeline.model.WorkshopJobLine;
import com.carsale.erp.report.ReportService;

/**
 * Shared total cost for a vehicle.
 * Total cost = landing + CUSDEC + repair + assessment.
 */
public final class VehicleTotalCost {

    private final BigDecimal landing;
    private final BigDecimal cusdec;
    private final BigDecimal assessment;
    private final BigDecimal repair;

    private VehicleTotalCost(
            BigDecimal landing,
            BigDecimal cusdec,
            BigDecimal assessment,
            BigDecimal repair
    ) {
        this.landing = landing;
        this.cusdec = cusdec;
        this.assessment = assessment;
        this.repair = repair;
    }

    public static VehicleTotalCost of(CustomsDocument customs, WorkshopJob job) {
        return new VehicleTotalCost(
                landingOf(customs),
                cusdecOf(customs),
                assessmentOf(customs),
                repairOf(job)
        );
    }

    public BigDecimal getLanding() {
        return landing;
    }

    public BigDecimal getCusdec() {
        return cusdec;
    }

    public BigDecimal getAssessment() {
        return assessment;
    }

    public BigDecimal getRepair() {
        return repair;
    }

    public BigDecimal getTotal() {
        return sum(landing, cusdec, repair, assessment);
    }

    public boolean isMissingRequired() {
        return landing == null || cusdec == null || assessment == null;
    }

    public static BigDecimal landingOf(CustomsDocument customs) {
        return customs == null ? null : ReportService.parseMoney(customs.getLandingCostLkr());
    }

    public static BigDecimal cusdecOf(CustomsDocument customs) {
        return customs == null ? null : ReportService.parseMoney(customs.getValueNcy());
    }

    public static BigDecimal assessmentOf(CustomsDocument customs) {
        return customs == null ? null : ReportService.parseMoney(customs.getAssessmentTotalAssessed());
    }

    public static BigDecimal repairOf(WorkshopJob job) {
        if (job == null || job.getLines() == null) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean any = false;
        for (WorkshopJobLine line : job.getLines()) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            BigDecimal cost = lineCost(line);
            if (cost != null) {
                total = total.add(cost);
                any = true;
            }
        }
        return any ? total : null;
    }

    public static BigDecimal lineCost(WorkshopJobLine line) {
        if (line == null) {
            return null;
        }
        BigDecimal actual = ReportService.parseMoney(line.getActualCost());
        if (actual != null) {
            return actual;
        }
        return ReportService.parseMoney(line.getEstimatedCost());
    }

    public static BigDecimal sum(BigDecimal... parts) {
        BigDecimal total = null;
        if (parts == null) {
            return null;
        }
        for (BigDecimal part : parts) {
            if (part == null) {
                continue;
            }
            total = total == null ? part : total.add(part);
        }
        return total;
    }
}
