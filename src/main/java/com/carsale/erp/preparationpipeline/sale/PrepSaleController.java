package com.carsale.erp.preparationpipeline.sale;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.preparationpipeline.yard.YardRecord;
import com.carsale.erp.preparationpipeline.yard.YardService;

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
            return "redirect:/yards/" + encode(record.getBayNo()) + "/" + encode(chassisNo);
        }
        return "redirect:/yards";
    }

    private String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
