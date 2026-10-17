package com.carsale.erp.readypipeline.controller;

import com.carsale.erp.readypipeline.service.SoldVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class SoldVehicleController {

    private final SoldVehicleService soldVehicleService;

    @GetMapping("/sold")
    public String list(Model model) {
        model.addAttribute("pageTitle", "Sold");
        model.addAttribute("activeMenu", "sold");
        model.addAttribute("rows", soldVehicleService.list());
        return "sold/list";
    }
}
