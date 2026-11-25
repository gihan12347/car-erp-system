package com.carsale.erp.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.report.DashboardService;
import com.carsale.erp.shared.settings.BusinessType;
import com.carsale.erp.shared.settings.BusinessTypeService;
import com.carsale.erp.shared.settings.SalesContactService;

@Controller
@RequiredArgsConstructor
public class AppPageController {

    private final DashboardService dashboardService;
    private final BusinessTypeService businessTypeService;
    private final SalesContactService salesContactService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model
    ) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("dash", dashboardService.build(period, page));
        return "dashboard";
    }

    @GetMapping("/configuration")
    public String configuration(Model model) {
        model.addAttribute("pageTitle", "Configuration");
        model.addAttribute("activeMenu", "configuration");
        return "configuration";
    }

    @PostMapping("/configuration/business-type")
    public String saveBusinessType(
            @RequestParam("businessType") String businessType,
            RedirectAttributes redirectAttributes
    ) {
        try {
            businessTypeService.save(BusinessType.parse(businessType));
            redirectAttributes.addFlashAttribute("successMessage", "Business type saved.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/configuration";
    }

    @PostMapping("/configuration/sales-contact")
    public String saveSalesContact(
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "whatsapp", required = false) String whatsapp,
            RedirectAttributes redirectAttributes
    ) {
        try {
            salesContactService.save(phone, whatsapp);
            redirectAttributes.addFlashAttribute("successMessage", "Sales contact saved.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/configuration";
    }

    @GetMapping("/vehicles")
    public String legacyVehicles() {
        return "redirect:/auction";
    }

    @GetMapping("/vehicles/{id}")
    public String legacyVehicleDetail(@PathVariable Long id) {
        return "redirect:/import/" + id;
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

    @GetMapping("/profile")
    public String profile() {
        return "redirect:/settings";
    }

    private String module(Model model, String title, String key, String icon, String description) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("activeMenu", key);
        model.addAttribute("pageIcon", icon);
        model.addAttribute("pageDescription", description);
        return "module";
    }
}
