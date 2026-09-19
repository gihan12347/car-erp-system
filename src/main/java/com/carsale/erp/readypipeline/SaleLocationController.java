package com.carsale.erp.readypipeline;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sale-locations")
public class SaleLocationController {

    private final SaleLocationService saleLocationService;

    public SaleLocationController(SaleLocationService saleLocationService) {
        this.saleLocationService = saleLocationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Sale configuration");
        model.addAttribute("activeMenu", "sale-locations");
        model.addAttribute("saleLocations", saleLocationService.listAll());
        model.addAttribute("saleLocationNames", saleLocationService.listActiveLocations());
        model.addAttribute("nextSaleCode", saleLocationService.nextSaleCode());
        return "ready-for-sale/locations";
    }

    @PostMapping
    public String add(
            @RequestParam("saleName") String saleName,
            @RequestParam("capacity") Integer capacity,
            @RequestParam("location") String location,
            RedirectAttributes redirectAttributes
    ) {
        try {
            saleLocationService.add(saleName, capacity, location);
            redirectAttributes.addFlashAttribute("successMessage", "Sale location added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("openAddSale", true);
            redirectAttributes.addFlashAttribute("draftSaleName", saleName);
            redirectAttributes.addFlashAttribute("draftCapacity", capacity);
            redirectAttributes.addFlashAttribute("draftLocation", location);
        }
        return "redirect:/sale-locations";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam("saleCode") String saleCode,
            @RequestParam("saleName") String saleName,
            @RequestParam("capacity") Integer capacity,
            @RequestParam("location") String location,
            RedirectAttributes redirectAttributes
    ) {
        try {
            saleLocationService.update(id, saleCode, saleName, capacity, location);
            redirectAttributes.addFlashAttribute("successMessage", "Sale location updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/sale-locations";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            saleLocationService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Sale location removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/sale-locations";
    }
}
