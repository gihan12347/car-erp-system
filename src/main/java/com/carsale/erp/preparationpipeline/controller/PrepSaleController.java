package com.carsale.erp.preparationpipeline.controller;

import java.nio.charset.StandardCharsets;

import com.carsale.erp.shared.utils.PipelineStageUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.service.YardService;

@Controller
@RequestMapping("/sale")
public class PrepSaleController {

    private final YardService yardService;

    public PrepSaleController(YardService yardService) {
        this.yardService = yardService;
    }

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
