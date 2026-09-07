package com.carsale.erp.preparationpipeline.checklist;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carsale.erp.preparationpipeline.checklist.InspectionItemService;

@Controller
@RequestMapping("/inspection-items")
public class InspectionChecklistController {

    private final InspectionItemService inspectionItemService;

    public InspectionChecklistController(InspectionItemService inspectionItemService) {
        this.inspectionItemService = inspectionItemService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Inspection items");
        model.addAttribute("activeMenu", "inspection-items");
        model.addAttribute("inspectionItems", inspectionItemService.listAll());
        return "preparation-pipeline/inspection/items";
    }

    @PostMapping
    public String add(
            @RequestParam("title") String title,
            RedirectAttributes redirectAttributes
    ) {
        try {
            inspectionItemService.add(title);
            redirectAttributes.addFlashAttribute("successMessage", "Inspection item added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/inspection-items";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam("title") String title,
            RedirectAttributes redirectAttributes
    ) {
        try {
            inspectionItemService.updateTitle(id, title);
            redirectAttributes.addFlashAttribute("successMessage", "Inspection item updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/inspection-items";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            inspectionItemService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Inspection item removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/inspection-items";
    }
}
