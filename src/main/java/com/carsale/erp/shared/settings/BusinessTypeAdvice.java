package com.carsale.erp.shared.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class BusinessTypeAdvice {

    private final BusinessTypeService businessTypeService;

    @ModelAttribute("businessType")
    public BusinessType businessType() {
        return businessTypeService.current();
    }
}
