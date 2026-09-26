package com.carsale.erp.readypipeline.util;

import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.controller.ReadyForSaleController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SaleGroup {

    public static final String UNASSIGNED_KEY = "_unassigned";

    private final String saleCode;
    private final String saleName;
    private final String location;
    private final int vehicleCount;
    private final int listedCount;
    private final int soldCount;
    private final Integer capacity;
    private final Integer remaining;
    private final Integer occupied;
    private final boolean unassigned;

    private SaleGroup(
            String saleCode,
            String saleName,
            String location,
            int vehicleCount,
            int listedCount,
            int soldCount,
            Integer capacity,
            Integer remaining,
            Integer occupied,
            boolean unassigned
    ) {
        this.saleCode = saleCode;
        this.saleName = saleName;
        this.location = location;
        this.vehicleCount = vehicleCount;
        this.listedCount = listedCount;
        this.soldCount = soldCount;
        this.capacity = capacity;
        this.remaining = remaining;
        this.occupied = occupied;
        this.unassigned = unassigned;
    }

    public static List<SaleGroup> group(List<SaleLocation> locations, List<ReadyForSaleController.SaleRow> rows) {
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        if (locations != null) {
            for (SaleLocation sale : locations) {
                if (sale == null || isBlank(sale.getSaleCode())) {
                    continue;
                }
                buckets.put(key(sale.getSaleCode()), Bucket.fromLocation(sale));
            }
        }

        Bucket unassigned = Bucket.unassigned();
        if (rows != null) {
            for (ReadyForSaleController.SaleRow row : rows) {
                String code = row == null || row.getListing() == null ? null : row.getListing().getSaleCode();
                String normalized = key(code);
                if (normalized.isEmpty()) {
                    unassigned.add(row);
                    continue;
                }
                Bucket bucket = buckets.computeIfAbsent(normalized, k -> Bucket.unknown(code, row.getListing() == null ? null : row.getListing().getSaleLocation()));
                bucket.add(row);
            }
        }

        List<SaleGroup> groups = new ArrayList<>();
        for (Bucket bucket : buckets.values()) {
            groups.add(bucket.toGroup());
        }
        if (unassigned.vehicleCount > 0) {
            groups.add(unassigned.toGroup());
        }
        return groups;
    }

    public static boolean matches(ReadyForSaleController.SaleRow row, String saleParam) {
        if (isBlank(saleParam)) {
            return true;
        }
        String code = row == null || row.getListing() == null ? null : row.getListing().getSaleCode();
        if (UNASSIGNED_KEY.equalsIgnoreCase(saleParam.trim())) {
            return isBlank(code);
        }
        return !isBlank(code) && saleParam.trim().equalsIgnoreCase(code.trim());
    }

    public String getSaleCode() {
        return saleCode;
    }

    public String getSaleName() {
        return saleName;
    }

    public String getLocation() {
        return location;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    public int getListedCount() {
        return listedCount;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public Integer getOccupied() {
        return occupied;
    }

    public int getFillPercent() {
        if (capacity == null || capacity <= 0) {
            return occupied != null && occupied > 0 ? 100 : 0;
        }
        int used = occupied == null ? 0 : occupied;
        int percent = (int) Math.round(used * 100.0 / capacity);
        return Math.min(100, Math.max(0, percent));
    }

    public String getFillState() {
        if (unassigned || capacity == null) {
            return "";
        }
        if (remaining != null && remaining <= 0) {
            return "is-full";
        }
        if (remaining != null && remaining <= 3) {
            return "is-low";
        }
        return "";
    }

    public boolean isUnassigned() {
        return unassigned;
    }

    public String getTitle() {
        if (!isBlank(saleName)) {
            return saleName;
        }
        return saleCode;
    }

    private static String key(String saleCode) {
        return isBlank(saleCode) ? "" : saleCode.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Bucket {
        private final String saleCode;
        private final String saleName;
        private final String location;
        private final Integer capacity;
        private final Integer remaining;
        private final Integer occupied;
        private final boolean unassigned;
        private int vehicleCount;
        private int listedCount;
        private int soldCount;

        private Bucket(
                String saleCode,
                String saleName,
                String location,
                Integer capacity,
                Integer remaining,
                Integer occupied,
                boolean unassigned
        ) {
            this.saleCode = saleCode;
            this.saleName = saleName;
            this.location = location;
            this.capacity = capacity;
            this.remaining = remaining;
            this.occupied = occupied;
            this.unassigned = unassigned;
        }

        static Bucket fromLocation(SaleLocation sale) {
            return new Bucket(
                    sale.getSaleCode(),
                    sale.getSaleName(),
                    sale.getLocation(),
                    sale.getCapacity(),
                    sale.getRemaining(),
                    sale.getOccupied(),
                    false
            );
        }

        static Bucket unknown(String saleCode, String location) {
            return new Bucket(saleCode, saleCode, location, null, null, null, false);
        }

        static Bucket unassigned() {
            return new Bucket(UNASSIGNED_KEY, "Unassigned", "No sale selected yet", null, null, null, true);
        }

        void add(ReadyForSaleController.SaleRow row) {
            vehicleCount++;
            if (row != null && row.isListed()) {
                listedCount++;
            }
            if (row != null && row.isSold()) {
                soldCount++;
            }
        }

        SaleGroup toGroup() {
            return new SaleGroup(
                    saleCode,
                    saleName,
                    location,
                    vehicleCount,
                    listedCount,
                    soldCount,
                    capacity,
                    remaining,
                    occupied,
                    unassigned
            );
        }
    }
}
