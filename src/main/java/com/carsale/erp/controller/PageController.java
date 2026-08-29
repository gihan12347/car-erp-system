package com.carsale.erp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activeMenu", "dashboard");
        return "dashboard";
    }

    @GetMapping("/vehicles")
    public String legacyVehicles() {
        return "redirect:/auction";
    }

    @GetMapping("/vehicles/{id}")
    public String legacyVehicleDetail(@PathVariable Long id) {
        return "redirect:/auction/" + id;
    }

    @GetMapping("/vehicles/{id}/edit")
    public String legacyVehicleEdit(@PathVariable Long id) {
        return "redirect:/auction/" + id + "/edit";
    }

    @GetMapping("/vehicles/new")
    public String legacyVehicleNew() {
        return "redirect:/auction/new";
    }

    @GetMapping("/sales")
    public String sales(Model model) {
        return module(model, "Sales", "sales", "fa-tags",
                "Quotations, invoices, and completed vehicle sales.");
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        return module(model, "Customers", "customers", "fa-users",
                "Buyer profiles, inquiries, and communication history.");
    }

    @GetMapping("/finance")
    public String finance(Model model) {
        return module(model, "Finance", "finance", "fa-coins",
                "Costs, payments, and profit on each vehicle.");
    }

    @GetMapping("/documents")
    public String documents(Model model) {
        return module(model, "Documents", "documents", "fa-file-lines",
                "Export certificates, bills of lading, and sale papers.");
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        return module(model, "Reports", "reports", "fa-chart-pie",
                "Operational and financial reports across the pipeline.");
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("pageTitle", "Profile");
        model.addAttribute("activeMenu", "");
        return "profile";
    }

    private String module(Model model, String title, String key, String icon, String description) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("activeMenu", key);
        model.addAttribute("pageIcon", icon);
        model.addAttribute("pageDescription", description);
        return "module";
    }
}
