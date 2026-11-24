package com.carsale.erp.readypipeline.controller;

import com.carsale.erp.readypipeline.service.SoldVehicleService;
import com.carsale.erp.shared.web.ListPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SoldVehicleController {

    private final SoldVehicleService soldVehicleService;

    @GetMapping("/sold")
    public String list(@RequestParam(value = "page", required = false) String page, Model model) {
        model.addAttribute("pageTitle", "Sold");
        model.addAttribute("activeMenu", "sold");
        model.addAttribute("searchQuery", "");
        model.addAttribute("rows", ListPaging.apply(soldVehicleService.list(), page, model, "/sold"));
        return "sold/list";
    }
}
