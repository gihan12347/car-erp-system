package com.carsale.erp.account;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.security.CustomUserDetails;

@Controller
public class SettingsController {

    private final AccountService accountService;

    public SettingsController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("activeMenu", "settings");
        return "settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "currentPassword", required = false) String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            accountService.updateProfile(
                    principal.getUser(),
                    fullName,
                    email,
                    currentPassword,
                    newPassword,
                    confirmPassword
            );
            redirectAttributes.addFlashAttribute("successMessage", "Profile details saved.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings";
    }

    @GetMapping("/settings/inspection-items")
    public String movedInspectionItems() {
        return "redirect:/inspection-items";
    }

    @GetMapping("/settings/yard-bays")
    public String movedYardBays() {
        return "redirect:/yard-bays";
    }

    @GetMapping("/settings/sale-locations")
    public String movedSaleLocations() {
        return "redirect:/sale-locations";
    }
}
