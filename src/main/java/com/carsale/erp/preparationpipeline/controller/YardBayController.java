package com.carsale.erp.preparationpipeline.controller;

import com.carsale.erp.preparationpipeline.service.YardBayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/yard-bays")
public class YardBayController {

    private final YardBayService yardBayService;

    public YardBayController(YardBayService yardBayService) {
        this.yardBayService = yardBayService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Yard configuration");
        model.addAttribute("activeMenu", "yard-bays");
        model.addAttribute("yardBays", yardBayService.listAll());
        model.addAttribute("yardLocations", yardBayService.listActiveLocations());
        model.addAttribute("nextYardCode", yardBayService.nextYardCode());
        return "preparation-pipeline/yard/bays";
    }

    @PostMapping
    public String add(
            @RequestParam("yardName") String yardName,
            @RequestParam("capacity") Integer capacity,
            @RequestParam("location") String location,
            RedirectAttributes redirectAttributes
    ) {
        try {
            yardBayService.add(yardName, capacity, location);
            redirectAttributes.addFlashAttribute("successMessage", "Yard added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("openAddYard", true);
            redirectAttributes.addFlashAttribute("draftYardName", yardName);
            redirectAttributes.addFlashAttribute("draftCapacity", capacity);
            redirectAttributes.addFlashAttribute("draftLocation", location);
        }
        return "redirect:/yard-bays";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam("bayCode") String bayCode,
            @RequestParam("yardName") String yardName,
            @RequestParam("capacity") Integer capacity,
            @RequestParam("location") String location,
            RedirectAttributes redirectAttributes
    ) {
        try {
            yardBayService.update(id, bayCode, yardName, capacity, location);
            redirectAttributes.addFlashAttribute("successMessage", "Yard updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/yard-bays";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            yardBayService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Yard removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/yard-bays";
    }
}
