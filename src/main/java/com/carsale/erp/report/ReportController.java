package com.carsale.erp.report;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "section", required = false) String section,
            Model model
    ) {
        ReportService.ReportPage report = reportService.build(query, section);
        model.addAttribute("pageTitle", "Reports");
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("report", report);
        model.addAttribute("searchQuery", report.getSearchQuery());
        model.addAttribute("section", report.getSection());
        return "reports";
    }
}
