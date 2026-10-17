package com.carsale.erp.preparationpipeline.controller;

import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.service.YardService;

@Controller
@RequestMapping("/sale")
@RequiredArgsConstructor
public class PrepSaleController {

    private final YardService yardService;

    @GetMapping
    public String list() {
        return "redirect:/yards";
    }

    @GetMapping("/{chassisNo}")
    public String form(@PathVariable String chassisNo) {
        YardRecord record = yardService.findByChassisNo(chassisNo);
        if (record != null && record.getBayNo() != null && !record.getBayNo().trim().isEmpty()) {
            return "redirect:/yards/" + PipelineStageUtils.encode(record.getBayNo()) + "/" + PipelineStageUtils.encode(chassisNo);
        }
        return "redirect:/yards";
    }
}
