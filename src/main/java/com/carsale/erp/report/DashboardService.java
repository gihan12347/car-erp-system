package com.carsale.erp.report;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final ReportService reportService;

    public DashboardService(ReportService reportService) {
        this.reportService = reportService;
    }

    public DashboardPage build() {
        ReportService.ReportPage report = reportService.build(null, ReportService.ALL);
        int max = max(
                report.getImportCount(),
                report.getCustomsCount(),
                report.getPreparationCount(),
                report.getReadyCount(),
                report.getListedCount(),
                report.getSoldCount()
        );
        List<PipelineBar> bars = new ArrayList<PipelineBar>();
        bars.add(bar("Import", report.getImportCount(), max, ""));
        bars.add(bar("Customs", report.getCustomsCount(), max, "rust"));
        bars.add(bar("Prep", report.getPreparationCount(), max, "gold"));
        bars.add(bar("Ready", report.getReadyCount(), max, "sea"));
        bars.add(bar("Listed", report.getListedCount(), max, "sea"));
        bars.add(bar("Sold", report.getSoldCount(), max, "leaf"));

        List<ActionItem> actions = new ArrayList<ActionItem>();
        addCount(actions, report.getImportCount(), "vehicle still in import", "vehicles still in import",
                "/import", "/images/dashboard/illu-ship.svg");
        addCount(actions, report.getCustomsCount(), "vehicle in customs clearance", "vehicles in customs clearance",
                "/customs", "/images/dashboard/illu-customs.svg");
        addCount(actions, report.getPreparationCount(), "vehicle in preparation", "vehicles in preparation",
                "/workshop-yard", "/images/dashboard/illu-workshop.svg");
        addCount(actions, report.getReadyCount(), "vehicle ready for sale and not listed", "vehicles ready for sale and not listed",
                "/ready-for-sale", "/images/dashboard/illu-car.svg");
        int missingPrice = 0;
        for (ReportService.ReportRow row : report.getRows()) {
            if (ReportService.LISTED.equals(row.getSection()) && "—".equals(row.getAskingPrice())) {
                missingPrice++;
            }
        }
        addCount(actions, missingPrice, "listed vehicle has no asking price", "listed vehicles have no asking price",
                "/reports?section=listed", "/images/dashboard/illu-pay.svg");

        return new DashboardPage(
                report.getTotal(),
                report.getImportCount(),
                report.getCustomsCount(),
                report.getPreparationCount(),
                report.getReadyCount(),
                report.getListedCount(),
                report.getSoldCount(),
                report.getLandingCostTotal(),
                report.getAskingTotal(),
                report.getSoldTotal(),
                bars,
                actions,
                pick(report.getRows(), false, 5),
                pick(report.getRows(), true, 5)
        );
    }

    private static void addCount(
            List<ActionItem> actions,
            int count,
            String singular,
            String plural,
            String href,
            String image
    ) {
        if (count <= 0) {
            return;
        }
        String phrase = count == 1 ? singular : plural;
        actions.add(new ActionItem(count + " " + phrase, href, image, "View"));
    }

    private static List<ReportService.ReportRow> pick(List<ReportService.ReportRow> rows, boolean sold, int limit) {
        String[] order = sold
                ? new String[] {ReportService.SOLD}
                : new String[] {
                        ReportService.IMPORT,
                        ReportService.CUSTOMS,
                        ReportService.PREPARATION,
                        ReportService.READY,
                        ReportService.LISTED
                };
        List<ReportService.ReportRow> picked = new ArrayList<ReportService.ReportRow>();
        int round = 0;
        boolean added = true;
        while (picked.size() < limit && added) {
            added = false;
            for (String section : order) {
                int seen = 0;
                for (ReportService.ReportRow row : rows) {
                    if (!section.equals(row.getSection())) {
                        continue;
                    }
                    if (seen == round) {
                        picked.add(row);
                        added = true;
                        break;
                    }
                    seen++;
                }
                if (picked.size() == limit) {
                    break;
                }
            }
            round++;
        }
        return picked;
    }

    private static PipelineBar bar(String label, int count, int max, String tone) {
        int width = max <= 0 ? 0 : (int) Math.round(count * 100.0 / max);
        return new PipelineBar(label, count, width, tone);
    }

    private static int max(int... values) {
        int max = 0;
        for (int value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static final class DashboardPage {
        private final int total;
        private final int importCount;
        private final int customsCount;
        private final int preparationCount;
        private final int readyCount;
        private final int listedCount;
        private final int soldCount;
        private final String landingCostTotal;
        private final String askingTotal;
        private final String soldTotal;
        private final List<PipelineBar> bars;
        private final List<ActionItem> actions;
        private final List<ReportService.ReportRow> inProgress;
        private final List<ReportService.ReportRow> soldVehicles;

        public DashboardPage(
                int total,
                int importCount,
                int customsCount,
                int preparationCount,
                int readyCount,
                int listedCount,
                int soldCount,
                String landingCostTotal,
                String askingTotal,
                String soldTotal,
                List<PipelineBar> bars,
                List<ActionItem> actions,
                List<ReportService.ReportRow> inProgress,
                List<ReportService.ReportRow> soldVehicles
        ) {
            this.total = total;
            this.importCount = importCount;
            this.customsCount = customsCount;
            this.preparationCount = preparationCount;
            this.readyCount = readyCount;
            this.listedCount = listedCount;
            this.soldCount = soldCount;
            this.landingCostTotal = landingCostTotal;
            this.askingTotal = askingTotal;
            this.soldTotal = soldTotal;
            this.bars = bars;
            this.actions = actions;
            this.inProgress = inProgress;
            this.soldVehicles = soldVehicles;
        }

        public int getTotal() { return total; }
        public int getImportCount() { return importCount; }
        public int getCustomsCount() { return customsCount; }
        public int getPreparationCount() { return preparationCount; }
        public int getReadyCount() { return readyCount; }
        public int getListedCount() { return listedCount; }
        public int getSoldCount() { return soldCount; }
        public String getLandingCostTotal() { return landingCostTotal; }
        public String getAskingTotal() { return askingTotal; }
        public String getSoldTotal() { return soldTotal; }
        public List<PipelineBar> getBars() { return bars; }
        public List<ActionItem> getActions() { return actions; }
        public List<ReportService.ReportRow> getInProgress() { return inProgress; }
        public List<ReportService.ReportRow> getSoldVehicles() { return soldVehicles; }
    }

    public static final class PipelineBar {
        private final String label;
        private final int count;
        private final int width;
        private final String tone;

        public PipelineBar(String label, int count, int width, String tone) {
            this.label = label;
            this.count = count;
            this.width = width;
            this.tone = tone;
        }

        public String getLabel() { return label; }
        public int getCount() { return count; }
        public int getWidth() { return width; }
        public String getTone() { return tone; }
    }

    public static final class ActionItem {
        private final String message;
        private final String href;
        private final String image;
        private final String linkLabel;

        public ActionItem(String message, String href, String image, String linkLabel) {
            this.message = message;
            this.href = href;
            this.image = image;
            this.linkLabel = linkLabel;
        }

        public String getMessage() { return message; }
        public String getHref() { return href; }
        public String getImage() { return image; }
        public String getLinkLabel() { return linkLabel; }
    }
}
