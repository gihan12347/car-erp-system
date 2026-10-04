package com.carsale.erp.shared.vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VehicleSummary {

    private final Vehicle vehicle;
    private final String title;
    private final String stageLabel;
    private final String stockNo;
    private final int onFileCount;
    private final int pendingCount;
    private final int skippedCount;
    private final int progressPercent;
    private final String landingCost;
    private final String askingPrice;
    private final List<SpecLine> highlights;
    private final List<SpecLine> specs;
    private final List<PhotoCard> photos;
    private final List<DocumentCard> documents;
    private final List<PriceGroup> priceGroups;
    private final List<String> notes;

    public VehicleSummary(
            Vehicle vehicle,
            String title,
            String stageLabel,
            String stockNo,
            int onFileCount,
            int pendingCount,
            int skippedCount,
            int progressPercent,
            String landingCost,
            String askingPrice,
            List<SpecLine> highlights,
            List<SpecLine> specs,
            List<PhotoCard> photos,
            List<DocumentCard> documents,
            List<PriceGroup> priceGroups,
            List<String> notes
    ) {
        this.vehicle = vehicle;
        this.title = title;
        this.stageLabel = stageLabel;
        this.stockNo = stockNo;
        this.onFileCount = onFileCount;
        this.pendingCount = pendingCount;
        this.skippedCount = skippedCount;
        this.progressPercent = progressPercent;
        this.landingCost = landingCost;
        this.askingPrice = askingPrice;
        this.highlights = highlights;
        this.specs = specs;
        this.photos = photos;
        this.documents = documents;
        this.priceGroups = priceGroups;
        this.notes = notes;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getTitle() {
        return title;
    }

    public String getStageLabel() {
        return stageLabel;
    }

    public String getStockNo() {
        return stockNo;
    }

    public int getOnFileCount() {
        return onFileCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getTrackedCount() {
        return onFileCount + pendingCount;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public String getLandingCost() {
        return landingCost;
    }

    public String getAskingPrice() {
        return askingPrice;
    }

    public List<SpecLine> getHighlights() {
        return highlights;
    }

    public List<SpecLine> getSpecs() {
        return specs;
    }

    public List<PhotoCard> getPhotos() {
        return photos;
    }

    public List<DocumentCard> getDocuments() {
        return documents;
    }

    public List<PriceGroup> getPriceGroups() {
        return priceGroups;
    }

    public List<String> getNotes() {
        return notes;
    }

    public boolean isPricesEmpty() {
        return priceGroups.isEmpty();
    }

    public static final class SpecLine {
        private final String label;
        private final String value;
        private final boolean total;

        public SpecLine(String label, String value) {
            this(label, value, false);
        }

        public SpecLine(String label, String value, boolean total) {
            this.label = label;
            this.value = value;
            this.total = total;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public boolean isTotal() {
            return total;
        }
    }

    public static final class PhotoCard {
        private final String url;
        private final String name;
        private final boolean pdf;

        public PhotoCard(String url, String name, boolean pdf) {
            this.url = url;
            this.name = name;
            this.pdf = pdf;
        }

        public String getUrl() {
            return url;
        }

        public String getName() {
            return name;
        }

        public boolean isPdf() {
            return pdf;
        }
    }

    public static final class DocumentCard {
        private final String title;
        private final String group;
        private final String status;
        private final String statusLabel;
        private final String fileName;
        private final String fileUrl;
        private final String stageUrl;
        private final boolean pdf;
        private final String icon;

        public DocumentCard(
                String title,
                String group,
                String status,
                String statusLabel,
                String fileName,
                String fileUrl,
                String stageUrl,
                boolean pdf,
                String icon
        ) {
            this.title = title;
            this.group = group;
            this.status = status;
            this.statusLabel = statusLabel;
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.stageUrl = stageUrl;
            this.pdf = pdf;
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public String getGroup() {
            return group;
        }

        public String getStatus() {
            return status;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public String getStageUrl() {
            return stageUrl;
        }

        public boolean isPdf() {
            return pdf;
        }

        public String getIcon() {
            return icon;
        }
    }

    public static final class PriceGroup {
        private final String title;
        private final List<SpecLine> lines;

        public PriceGroup(String title, List<SpecLine> lines) {
            this.title = title;
            this.lines = lines == null ? Collections.<SpecLine>emptyList() : lines;
        }

        public String getTitle() {
            return title;
        }

        public List<SpecLine> getLines() {
            return lines;
        }
    }

    public static final class PriceBuilder {
        private final String title;
        private final List<SpecLine> lines = new ArrayList<SpecLine>();

        public PriceBuilder(String title) {
            this.title = title;
        }

        public void add(String label, String value) {
            add(label, value, false);
        }

        public void addTotal(String label, String value) {
            add(label, value, true);
        }

        private void add(String label, String value, boolean total) {
            if (value == null || value.trim().isEmpty()) {
                return;
            }
            lines.add(new SpecLine(label, value.trim(), total));
        }

        public PriceGroup build() {
            if (lines.isEmpty()) {
                return null;
            }
            return new PriceGroup(title, lines);
        }
    }
}
