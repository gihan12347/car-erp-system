package com.carsale.erp.account;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsController {

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("activeMenu", "settings");
        return "settings";
    }

    @GetMapping("/settings/inspection-items")
    public String movedInspectionItems() {
        return "redirect:/inspection-items";
    }
}
