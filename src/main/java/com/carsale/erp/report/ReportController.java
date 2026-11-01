package com.carsale.erp.report;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class ReportController {

    private final StandardReportService standardReportService;

    public ReportController(StandardReportService standardReportService) {
        this.standardReportService = standardReportService;
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            Model model
    ) {
        if (section != null && !section.trim().isEmpty()) {
            return show(StandardReportService.PIPELINE, query, section, from, to, null, model);
        }
        List<StandardReportService.ReportCard> catalog = standardReportService.catalog();
        model.addAttribute("pageTitle", "Reports");
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("catalog", catalog);
        model.addAttribute("groups", grouped(catalog));
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        model.addAttribute("fromDate", from == null ? "" : from.trim());
        model.addAttribute("toDate", to == null ? "" : to.trim());
        return "reports";
    }

    @GetMapping("/reports/run")
    public String run(
            @RequestParam(value = "kind", required = false) String kind,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            RedirectAttributes redirectAttributes
    ) {
        if (!StandardReportService.isKnown(kind)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose a report to generate.");
            return "redirect:/reports";
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/reports/" + kind.trim().toLowerCase());
        if (query != null && !query.trim().isEmpty()) {
            builder.queryParam("q", query.trim());
        }
        if (from != null && !from.trim().isEmpty()) {
            builder.queryParam("from", from.trim());
        }
        if (to != null && !to.trim().isEmpty()) {
            builder.queryParam("to", to.trim());
        }
        return "redirect:" + builder.build().encode().toUriString();
    }

    @GetMapping("/reports/{kind}")
    public String report(
            @PathVariable("kind") String kind,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", required = false) String page,
            Model model
    ) {
        return show(kind, query, section, from, to, page, model);
    }

    @GetMapping(value = "/reports/{kind}", params = "export=csv")
    public ResponseEntity<byte[]> csv(
            @PathVariable("kind") String kind,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to
    ) {
        if (!StandardReportService.isKnown(kind)) {
            return ResponseEntity.notFound().build();
        }
        StandardReportService.StandardReport report = standardReportService.build(kind, query, section, from, to);
        byte[] body = StandardReportService.csv(report).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + StandardReportService.csvFileName(report) + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private String show(String kind, String query, String section, String from, String to, String page, Model model) {
        if (!StandardReportService.isKnown(kind)) {
            return "redirect:/reports";
        }
        StandardReportService.StandardReport report = standardReportService.build(kind, query, section, from, to);
        int total = report.getRowCount();
        int pages = pageCount(total);
        int current = clampPage(parsePage(page), pages);
        model.addAttribute("pageTitle", report.getTitle());
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("report", report);
        model.addAttribute("pageRows", pageSlice(report.getRows(), current));
        model.addAttribute("reportPage", Integer.valueOf(current));
        model.addAttribute("reportPages", Integer.valueOf(pages));
        model.addAttribute("reportPageNumbers", pageNumbers(current, pages));
        model.addAttribute("reportRange", pageRange(current, total));
        model.addAttribute("searchQuery", report.getSearchQuery());
        model.addAttribute("catalog", standardReportService.catalog());
        return "report-view";
    }

    private static final int PAGE_SIZE = 10;

    private static int parsePage(String page) {
        if (page == null || page.trim().isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(page.trim());
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static int pageCount(int total) {
        if (total <= 0) {
            return 0;
        }
        return (total + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private static int clampPage(int page, int pages) {
        if (pages <= 0) {
            return 1;
        }
        if (page < 1) {
            return 1;
        }
        if (page > pages) {
            return pages;
        }
        return page;
    }

    private static List<StandardReportService.DataRow> pageSlice(List<StandardReportService.DataRow> rows, int page) {
        int total = rows.size();
        if (total == 0) {
            return rows;
        }
        int safePage = clampPage(page, pageCount(total));
        int from = (safePage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        return new ArrayList<StandardReportService.DataRow>(rows.subList(from, to));
    }

    private static String pageRange(int page, int total) {
        if (total <= 0) {
            return "";
        }
        int from = (page - 1) * PAGE_SIZE + 1;
        int to = Math.min(page * PAGE_SIZE, total);
        return from + "–" + to + " of " + total;
    }

    private static List<Integer> pageNumbers(int page, int pages) {
        List<Integer> numbers = new ArrayList<Integer>();
        if (pages <= 0) {
            return numbers;
        }
        if (pages <= 7) {
            for (int number = 1; number <= pages; number++) {
                numbers.add(Integer.valueOf(number));
            }
            return numbers;
        }
        numbers.add(Integer.valueOf(1));
        int start = Math.max(2, page - 1);
        int end = Math.min(pages - 1, page + 1);
        if (page <= 3) {
            start = 2;
            end = 4;
        } else if (page >= pages - 2) {
            start = pages - 3;
            end = pages - 1;
        }
        if (start > 2) {
            numbers.add(Integer.valueOf(0));
        }
        for (int number = start; number <= end; number++) {
            numbers.add(Integer.valueOf(number));
        }
        if (end < pages - 1) {
            numbers.add(Integer.valueOf(0));
        }
        numbers.add(Integer.valueOf(pages));
        return numbers;
    }

    private static List<ReportGroup> grouped(List<StandardReportService.ReportCard> catalog) {
        Map<String, List<StandardReportService.ReportCard>> byGroup =
                new LinkedHashMap<String, List<StandardReportService.ReportCard>>();
        for (StandardReportService.ReportCard card : catalog) {
            List<StandardReportService.ReportCard> items = byGroup.get(card.getGroup());
            if (items == null) {
                items = new ArrayList<StandardReportService.ReportCard>();
                byGroup.put(card.getGroup(), items);
            }
            items.add(card);
        }
        List<ReportGroup> groups = new ArrayList<ReportGroup>();
        for (Map.Entry<String, List<StandardReportService.ReportCard>> entry : byGroup.entrySet()) {
            groups.add(new ReportGroup(entry.getKey(), entry.getValue()));
        }
        return groups;
    }

    public static final class ReportGroup {
        private final String name;
        private final List<StandardReportService.ReportCard> cards;

        public ReportGroup(String name, List<StandardReportService.ReportCard> cards) {
            this.name = name;
            this.cards = cards;
        }

        public String getName() { return name; }
        public List<StandardReportService.ReportCard> getCards() { return cards; }
    }
}
